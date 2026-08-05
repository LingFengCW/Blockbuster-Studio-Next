package lingfeng.bbsnext.mcef;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * MCEF (Minecraft Chromium Embedded Framework) HTML editor integration.
 *
 * Replaces the Ultralight path: MCEF ships a maintained Chromium renderer
 * with a working GPU driver for MC 26.2, so the HTML editor no longer
 * crashes in native code.
 *
 * Architecture:
 *   - The browser is created lazily when the editor opens
 *     ({@link #createBrowser(int, int, EditorBridge)}).
 *   - Events flow in from {@link UIScreen} (which already receives the
 *     native MC 26.2 MouseButtonEvent/KeyEvent/CharacterEvent objects).
 *   - Rendering: {@link MCEFBrowser#getTextureView()} is blitted straight
 *     into the GUI via a BlitRenderState.
 *   - Java -> JS: executeJavaScript pushes the editor state as
 *     `window.bbsState`.
 *   - JS -> Java: CEF JSQuery (`window.javaQuery`) routes actions to
 *     {@link EditorBridge}.
 */
public class MCEFUI
{
    private static MCEFApi api;
    private static MCEFBrowser browser;
    private static EditorBridge bridge;
    private static boolean initialized;

    /* -------- lifecycle -------- */

    /** Start MCEF initialization (async). Safe to call multiple times. */
    public static void initialize()
    {
        if (initialized || api != null)
        {
            return;
        }

        try
        {
            MCEFApi.initialize();
            initialized = true;

            /* Non-blocking: complete asynchronously in the background. */
            MCEFApi.getInstanceFuture().thenAccept(a ->
            {
                api = a;
                BBSMod.LOGGER.info("[MCEF] Browser engine ready");
            }).exceptionally(e ->
            {
                BBSMod.LOGGER.error("[MCEF] Initialization failed", e);

                return null;
            });
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] initialize failed", t);
            initialized = false;
        }
    }

    /**
     * Non-blocking API accessor. Returns null while the CEF engine is still
     * initializing (Chromium binaries may be downloading on a background
     * thread). Never join() on the render thread - that freezes the game.
     */
    private static MCEFApi getApi()
    {
        if (api == null)
        {
            MCEFApi.Initialization init = MCEFApi.initialize();

            if (init.isDone())
            {
                try
                {
                    api = MCEFApi.getInstance();
                }
                catch (Throwable t)
                {
                    BBSMod.LOGGER.error("[MCEF] getInstance failed", t);
                }
            }
        }

        return api;
    }

    /** Create (or resize) the editor browser. Returns false while the CEF
     *  engine is not ready yet (caller should retry next frame).
     *  {@code width}/{@code height} are GUI (scaled) units; the browser
     *  viewport is sized in physical pixels so the texture stays sharp. */
    public static boolean createBrowser(int width, int height, EditorBridge jsBridge)
    {
        MCEFApi mcef = getApi();

        if (mcef == null)
        {
            return false;
        }

        if (browser != null)
        {
            bridge = jsBridge;
            browser.resize(scale(width), scale(height));

            return true;
        }

        bridge = jsBridge;

        try
        {
            browser = mcef.createBrowser(editorPageDataUrl(), true);
            browser.resize(scale(width), scale(height));
            browser.setFocus(true);

            installJsQuery(browser.getCefBrowser());

            /* The JSQuery bridge (window.javaQuery) is injected during page
             * navigation, so reload once after the router is attached. */
            browser.getCefBrowser().reload();

            BBSMod.LOGGER.info("[MCEF] Editor browser created ({}x{})", width, height);

            return true;
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] createBrowser failed", t);
            browser = null;

            return false;
        }
    }

    /** Resize the browser to follow the overlay area (GUI units). */
    public static void resizeBrowser(int width, int height)
    {
        if (browser != null)
        {
            browser.resize(scale(width), scale(height));
        }
    }

    /** Convert GUI (scaled) units to physical pixels. */
    private static int scale(int value)
    {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        return Math.max(1, (int) Math.round(value * guiScale));
    }

    public static void close()
    {
        if (browser != null)
        {
            try
            {
                browser.close();
            }
            catch (Throwable ignored)
            {
            }

            browser = null;
        }
    }

    /* -------- JS bridge -------- */

    /** Register the JSQuery handler (window.javaQuery -> EditorBridge). */
    private static void installJsQuery(CefBrowser cef)
    {
        try
        {
            CefMessageRouter router = CefMessageRouter.create(
                new CefMessageRouter.CefMessageRouterConfig("javaQuery", "cancelJavaQuery"));
            router.addHandler(new CefMessageRouterHandlerAdapter()
            {
                @Override
                public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request,
                    boolean persistent, CefQueryCallback callback)
                {
                    try
                    {
                        String result = EditorBridge.handle(request, MCEFUI.bridge);
                        callback.success(result);

                        return true;
                    }
                    catch (Throwable t)
                    {
                        callback.failure(0, t.getMessage());

                        return true;
                    }
                }
            }, false);

            cef.getClient().addMessageRouter(router);
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] installJsQuery failed", t);
        }
    }

    /** Push the current editor state into the page (Java -> JS). */
    public static void pushState()
    {
        if (browser == null || bridge == null)
        {
            return;
        }

        try
        {
            String json = EditorBridge.getStateJson(bridge);
            browser.getCefBrowser().executeJavaScript(
                "window.bbsState = " + json + "; if (window.__onState) __onState();",
                "", 0);
        }
        catch (Throwable ignored)
        {
        }
    }

    /* -------- input (called from UIScreen) -------- */

    /** Whether the given menu currently shows the HTML editor. */
    public static boolean isActive(UIBaseMenu menu)
    {
        if (browser == null || !(menu instanceof UIDashboard))
        {
            return false;
        }

        UIDashboard dashboard = (UIDashboard) menu;

        return dashboard.getPanels().panel instanceof UIFilmPanel panel
            && panel.ultralightOverlay != null
            && panel.ultralightOverlay.isVisible();
    }

    /** Convert GUI (scaled) coordinates to the browser's physical pixels. */
    private static double scaleCoord(double value)
    {
        return value * Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static boolean onMouseClicked(UIBaseMenu menu, MouseButtonEvent event, boolean doubled)
    {
        if (isActive(menu))
        {
            browser.onMouseClicked(new MouseButtonEvent(
                scaleCoord(event.x()), scaleCoord(event.y()), event.buttonInfo()), doubled);

            return true;
        }

        return false;
    }

    public static boolean onMouseReleased(UIBaseMenu menu, MouseButtonEvent event)
    {
        if (isActive(menu))
        {
            browser.onMouseReleased(new MouseButtonEvent(
                scaleCoord(event.x()), scaleCoord(event.y()), event.buttonInfo()));

            return true;
        }

        return false;
    }

    public static boolean onMouseScrolled(UIBaseMenu menu, double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (isActive(menu))
        {
            browser.onMouseScrolled((int) scaleCoord(mouseX), (int) scaleCoord(mouseY), verticalAmount);

            return true;
        }

        return false;
    }

    public static boolean onMouseMoved(UIBaseMenu menu, double x, double y)
    {
        if (isActive(menu))
        {
            browser.onMouseMoved((int) scaleCoord(x), (int) scaleCoord(y));

            return true;
        }

        return false;
    }

    public static boolean onKeyPressed(UIBaseMenu menu, KeyEvent event)
    {
        if (isActive(menu))
        {
            browser.onKeyPressed(event);

            return true;
        }

        return false;
    }

    public static boolean onKeyReleased(UIBaseMenu menu, KeyEvent event)
    {
        if (isActive(menu))
        {
            browser.onKeyReleased(event);

            return true;
        }

        return false;
    }

    public static boolean onCharTyped(UIBaseMenu menu, CharacterEvent event)
    {
        if (isActive(menu))
        {
            browser.onCharTyped(event);

            return true;
        }

        return false;
    }

    /* -------- rendering -------- */

    /**
     * Add the browser texture to the GUI render state. Returns the texture
     * view, or null when the browser has no frame yet.
     */
    public static GpuTextureView renderTexture()
    {
        if (browser == null)
        {
            return null;
        }

        try
        {
            return browser.getTextureView();
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    public static boolean isReady()
    {
        return browser != null;
    }

    /* -------- page -------- */

    /** Embed the editor HTML as a data: URL (works offline, no file server). */
    private static String editorPageDataUrl()
    {
        try
        {
            var stream = MCEFUI.class.getResourceAsStream("/assets/bbs/ultralight/editor_ui.html");

            if (stream != null)
            {
                String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                String b64 = Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));

                return "data:text/html;charset=utf-8;base64," + b64;
            }
        }
        catch (Exception ignored)
        {
        }

        return "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(
            "<html><body style='background:#222;color:#fff'>MCEF OK</body></html>".getBytes(StandardCharsets.UTF_8));
    }
}
