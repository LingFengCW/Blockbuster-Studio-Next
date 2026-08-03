package mchorse.bbs_mod.ui.titlebar;

import mchorse.bbs_mod.ui.dashboard.topbar.TopBarContext;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarItem;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarMenu;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarRegistry;

import org.lwjgl.glfw.GLFWNativeWin32;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

/**
 * A real Win32 window menu bar (the native File/Edit/... menu under the
 * OS title bar) built through the JDK FFM API against user32. The menu
 * structure mirrors {@link TopBarRegistry}; WM_COMMAND is caught by
 * subclassing the window procedure and routed back to the registry items.
 * Installs only on Windows (when the GLFW window has a Win32 HWND).
 */
public class Win32MenuBar
{
    private static final int GWLP_WNDPROC = -4;
    private static final int WM_COMMAND = 0x0111;
    private static final int MF_STRING = 0;
    private static final int MF_POPUP = 0x10;
    private static final int MF_GRAYED = 0x1;

    private static final Arena ARENA = Arena.ofShared();
    private static final Map<Integer, Runnable> COMMANDS = new HashMap<>();

    private static volatile long PREV_PROC;
    private static volatile boolean installed;

    /** Install the native menu bar on the current game window. Safe to call once. */
    public static void install()
    {
        if (installed)
        {
            return;
        }

        try
        {
            /* The current GLFW window (MC does not expose its handle field). */
            long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(window);

            if (hwnd == 0)
            {
                mchorse.bbs_mod.BBSMod.LOGGER.warn("Win32 menu bar: not a Win32 window");

                return;
            }

            Linker linker = Linker.nativeLinker();
            SymbolLookup user32 = SymbolLookup.libraryLookup("user32", ARENA);

            /* HWND/HMENU are passed as 64-bit pointer values (JAVA_LONG) so
             * callers can use plain longs instead of MemorySegments. */
            MethodHandle createMenu = linker.downcallHandle(user32.find("CreateMenu").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG));
            MethodHandle createPopup = linker.downcallHandle(user32.find("CreatePopupMenu").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG));
            MethodHandle appendMenu = linker.downcallHandle(user32.find("AppendMenuW").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            MethodHandle setMenu = linker.downcallHandle(user32.find("SetMenu").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
            MethodHandle setWindowLongPtr = linker.downcallHandle(user32.find("SetWindowLongPtrW").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));
            MethodHandle callWindowProc = linker.downcallHandle(user32.find("CallWindowProcW").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));

            long hMenu = (long) createMenu.invoke();

            if (hMenu == 0)
            {
                mchorse.bbs_mod.BBSMod.LOGGER.warn("Win32 menu bar: CreateMenu failed");

                return;
            }

            TopBarRegistry.setup();

            TopBarContext ctx = new TopBarContext(null, null);

            for (TopBarMenu menu : TopBarRegistry.getMenus())
            {
                long popup = (long) createPopup.invoke();

                for (TopBarItem item : menu.getItems(ctx))
                {
                    if (!item.isVisible(ctx))
                    {
                        continue;
                    }

                    boolean enabled = item.isEnabled(ctx);
                    int id = enabled ? COMMANDS.size() + 1 : 0;

                    if (enabled)
                    {
                        COMMANDS.put(id, () -> item.run(new TopBarContext(null, null)));
                    }

                    appendMenu.invoke(popup, MF_STRING | (enabled ? 0 : MF_GRAYED), (long) id, toWide(item.label.get()));
                }

                appendMenu.invoke(hMenu, MF_POPUP, popup, toWide(menu.label.get()));
            }

            setMenu.invoke(hwnd, hMenu);

            /* Subclass the window proc to catch WM_COMMAND. */
            FunctionDescriptor wndProcDesc = FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG);

            WndProcRef.PROC = callWindowProc;

            MemorySegment stub = Linker.nativeLinker().upcallStub(WND_PROC, wndProcDesc, ARENA);

            long prev = (long) setWindowLongPtr.invoke(hwnd, GWLP_WNDPROC, stub.address());

            PREV_PROC = prev;
            installed = true;

            mchorse.bbs_mod.BBSMod.LOGGER.info("Win32 menu bar installed");
        }
        catch (Throwable t)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to install the Win32 menu bar", t);
        }
    }

    /** Upcall target: (hWnd, msg, wParam, lParam) -> LRESULT. */
    private static final MethodHandle WND_PROC = buildWndProc();

    private static MethodHandle buildWndProc()
    {
        try
        {
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();

            return lookup.findStatic(Win32MenuBar.class, "onWndProc",
                java.lang.invoke.MethodType.methodType(long.class,
                    long.class, int.class, long.class, long.class));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /** Invoked by the upcall stub for every window message. */
    public static long onWndProc(long hwnd, int msg, long wParam, long lParam)
    {
        if (msg == WM_COMMAND)
        {
            int id = (int) (wParam & 0xFFFF);
            Runnable command = COMMANDS.get(id);

            if (command != null)
            {
                try
                {
                    /* WM_COMMAND arrives on the window message thread, not
                     * the render thread. UIScreen.open / setScreen must run
                     * on the render thread, otherwise the new screen renders
                     * black/empty. Hop back to the render thread. */
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

                    if (mc != null)
                    {
                        mc.execute(command);
                    }
                    else
                    {
                        command.run();
                    }
                }
                catch (Throwable t)
                {
                    mchorse.bbs_mod.BBSMod.LOGGER.error("Win32 menu command failed", t);
                }

                return 0;
            }
        }

        try
        {
            MethodHandle call = WndProcRef.PROC;

            return call == null ? 0 : (long) call.invoke(PREV_PROC, hwnd, msg, wParam, lParam);
        }
        catch (Throwable t)
        {
            return 0;
        }
    }

    /** Ref holder so onWndProc can reach the CallWindowProcW handle. */
    private static final class WndProcRef
    {
        static volatile MethodHandle PROC;
    }

    private static MemorySegment toWide(String text)
    {
        char[] chars = new char[text.length() + 1];

        text.getChars(0, text.length(), chars, 0);

        return ARENA.allocateFrom(ValueLayout.JAVA_CHAR, chars);
    }
}
