package lingfeng.bbsnext.ultralight;

import com.labymedia.ultralight.UltralightJava;
import com.labymedia.ultralight.UltralightRenderer;
import com.labymedia.ultralight.UltralightView;
import com.labymedia.ultralight.databind.Databind;
import com.labymedia.ultralight.databind.DatabindConfiguration;
import com.labymedia.ultralight.input.UltralightKey;
import com.labymedia.ultralight.input.UltralightKeyEvent;
import com.labymedia.ultralight.input.UltralightKeyEventType;
import com.labymedia.ultralight.input.UltralightMouseEvent;
import com.labymedia.ultralight.input.UltralightMouseEventButton;
import com.labymedia.ultralight.input.UltralightMouseEventType;
import com.labymedia.ultralight.input.UltralightScrollEvent;
import com.labymedia.ultralight.input.UltralightScrollEventType;
import com.labymedia.ultralight.javascript.JavascriptContext;
import com.labymedia.ultralight.javascript.JavascriptContextLock;
import com.labymedia.ultralight.javascript.JavascriptObject;
import com.mojang.blaze3d.platform.NativeImage;
import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ultralight HTML UI engine integration.
 *
 * The LabyMod Java bindings are bundled into the mod; the Ultralight SDK
 * native binaries are loaded at runtime from either:
 *   1. <gameDir>/bbsnext/ultralight/   (SDK bin folder, user-provided)
 *   2. the mod's classpath (assets/bbsnext/ultralight/, if packaged)
 *
 * Rendering pipeline (MC 26.2 safe, works on both GL and Vulkan backends):
 *   Ultralight renders the HTML into its own GPU surface -> we read the
 *   pixels back on the CPU (BGRA) -> copy into a {@link NativeImage} ->
 *   update a {@link DynamicTexture} -> draw it through the vanilla
 *   GuiGraphicsExtractor.blit() pipeline. No raw GL calls are made.
 *
 * JS interop: a Java object (typically {@link EditorBridge}) is bound into
 * the page as the global `bbs` object through the ultralight-java-databind
 * module, so the HTML editor can drive the real BBS data model.
 */
public class UltralightUI
{
    private static UltralightRenderer renderer;
    private static UltralightView view;
    private static boolean initialized;
    private static boolean loaded;

    /* JS bridge */
    private static Databind databind;
    private static Object bridge;
    private static boolean bound;

    /* Uploaded MC texture */
    private static DynamicTexture dynTex;
    private static Identifier dynTexId;
    private static int dynTexW = -1;
    private static int dynTexH = -1;
    private static int textureCounter;

    /* -------- lifecycle -------- */

    /** Load natives and create the renderer. Safe to call multiple times. */
    public static void init()
    {
        if (initialized)
        {
            return;
        }

        try
        {
            loadNatives();

            if (loaded)
            {
                databind = new Databind(DatabindConfiguration.builder().build());
                renderer = UltralightRenderer.create();
                initialized = renderer != null;
            }
        }
        catch (Throwable e)
        {
            BBSMod.LOGGER.error("[Ultralight] init failed", e);
            initialized = false;
        }
    }

    private static void loadNatives() throws Exception
    {
        if (loaded)
        {
            return;
        }

        Path sdk = null;

        /* 1) gameDir/bbsnext/ultralight */
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath();
        Path userSdk = gameDir.resolve("bbsnext/ultralight");

        if (Files.exists(userSdk.resolve(sdkBinaryName())))
        {
            sdk = userSdk;
        }
        else
        {
            /* 2) classpath resource (assets/bbsnext/ultralight) */
            sdk = extractFromClasspath();
        }

        if (sdk == null)
        {
            BBSMod.LOGGER.warn("[Ultralight] SDK natives not found. Put the Ultralight SDK bin/ contents into {} to enable the HTML UI.", gameDir.resolve("bbsnext/ultralight"));
            return;
        }

        UltralightJava.load(sdk);
        loaded = true;
    }

    private static String sdkBinaryName()
    {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win"))
        {
            return "Ultralight.dll";
        }
        if (os.contains("linux"))
        {
            return "libUltralight.so";
        }
        return "libUltralight.dylib";
    }

    private static String ext()
    {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win"))
        {
            return "dll";
        }
        if (os.contains("linux"))
        {
            return "so";
        }
        return "dylib";
    }

    /** Pull natives from the mod jar (assets/bbsnext/ultralight/) into a temp dir. */
    private static Path extractFromClasspath() throws Exception
    {
        String e = ext();
        Path dir = Files.createTempDirectory("bbs-ultralight");

        extractResource("/assets/bbsnext/ultralight/UltralightCore." + e, dir);
        extractResource("/assets/bbsnext/ultralight/WebCore." + e, dir);
        extractResource("/assets/bbsnext/ultralight/Ultralight." + e, dir);

        return Files.exists(dir.resolve("Ultralight." + e)) ? dir : null;
    }

    private static void extractResource(String path, Path dir)
    {
        try
        {
            var stream = UltralightUI.class.getResourceAsStream(path);

            if (stream != null)
            {
                Files.copy(stream, dir.resolve(path.substring(path.lastIndexOf('/') + 1)));
            }
        }
        catch (Exception ignored)
        {
        }
    }

    /** Create (or resize) the editor view. */
    public static void createView(int width, int height, Object jsBridge)
    {
        init();

        if (renderer == null)
        {
            return;
        }

        if (view != null)
        {
            view.resize(width, height);

            return;
        }

        bridge = jsBridge;
        bound = false;

        view = renderer.createView(width, height, null);
        view.loadHTML(editorPage());
    }

    /* -------- per-frame: render + upload pixels -------- */

    /**
     * Advance the engine, render one frame and upload the page pixels into a
     * vanilla DynamicTexture. Must be called from the render thread.
     */
    public static void renderFrame()
    {
        if (view == null || renderer == null)
        {
            return;
        }

        tryBind();

        renderer.update();
        renderer.render();

        var surface = view.surface();

        if (surface == null)
        {
            return;
        }

        int w = (int) surface.width();
        int h = (int) surface.height();

        if (w <= 0 || h <= 0)
        {
            return;
        }

        ByteBuffer pixels = surface.lockPixels();

        try
        {
            if (pixels == null)
            {
                return;
            }

            ensureTexture(w, h);

            if (dynTex == null)
            {
                return;
            }

            NativeImage image = dynTex.getPixels();

            if (image == null || image.getWidth() != w || image.getHeight() != h)
            {
                return;
            }

            /* Ultralight surfaces are BGRA; MC NativeImage RGBA stores the
             * same byte order (little-endian ABGR int), so we can copy the
             * bytes straight through. */
            int[] abgr = image.getPixelsABGR();

            if (abgr == null || abgr.length < w * h)
            {
                return;
            }

            pixels.position(0);
            pixels.limit(w * h * 4);

            for (int i = 0; i < w * h; i++)
            {
                int b = pixels.get() & 0xFF;
                int g = pixels.get() & 0xFF;
                int r = pixels.get() & 0xFF;
                int a = pixels.get() & 0xFF;

                abgr[i] = (a << 24) | (b << 16) | (g << 8) | r;
            }

            dynTex.setPixels(image);
            dynTex.upload();
        }
        finally
        {
            surface.unlockPixels();
        }
    }

    private static void ensureTexture(int w, int h)
    {
        if (dynTex != null && dynTexW == w && dynTexH == h)
        {
            return;
        }

        if (dynTex != null)
        {
            Minecraft.getInstance().getTextureManager().release(dynTexId);
        }

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, true);
        dynTex = new DynamicTexture(() -> "bbs ultralight " + (textureCounter++), image);
        dynTexId = Identifier.fromNamespaceAndPath("bbs_mod", "ultralight/" + (textureCounter++));
        Minecraft.getInstance().getTextureManager().register(dynTexId, dynTex);
        dynTexW = w;
        dynTexH = h;
    }

    /* -------- JS bridge -------- */

    /**
     * Bind the bridge object as the global `bbs` on the page. Retried every
     * frame until the page's JS context is available (the context only
     * exists after the HTML document has been loaded by the engine).
     */
    private static void tryBind()
    {
        if (bound || view == null || bridge == null || databind == null)
        {
            return;
        }

        try
        {
            JavascriptContextLock lock = view.lockJavascriptContext();

            try
            {
                JavascriptContext context = lock.getContext();
                JavascriptObject global = context.getGlobalObject();

                if (global.getProperty("document").isUndefined())
                {
                    return; /* page not loaded yet, try again next frame */
                }

                global.setProperty("bbs", databind.getConversionUtils().toJavascript(context, bridge), 0);
                bound = true;

                BBSMod.LOGGER.info("[Ultralight] JS bridge 'bbs' bound");
            }
            finally
            {
                lock.unlock();
            }
        }
        catch (Throwable t)
        {
            /* page not ready yet - retry next frame */
        }
    }

    /** Run a script in the page. Used to push updates from Java to JS. */
    public static void evaluate(String script)
    {
        if (view == null)
        {
            return;
        }

        try
        {
            view.evaluateScript(script);
        }
        catch (Throwable ignored)
        {
        }
    }

    /* -------- input bridge -------- */

    public static void fireMouse(int x, int y, UltralightMouseEventType type, UltralightMouseEventButton button)
    {
        if (view == null)
        {
            return;
        }

        UltralightMouseEvent event = new UltralightMouseEvent()
            .type(type)
            .x(x)
            .y(y)
            .button(button);

        view.fireMouseEvent(event);
    }

    public static void fireScroll(double deltaY)
    {
        if (view == null)
        {
            return;
        }

        UltralightScrollEvent event = new UltralightScrollEvent()
            .type(UltralightScrollEventType.BY_PIXEL)
            .deltaY((int) deltaY);

        view.fireScrollEvent(event);
    }

    /** Forward a key press (GLFW key code) to the page. Only special keys
     *  are forwarded as key-down events; typed characters go through
     *  {@link #fireChar(char)}. */
    public static void fireKey(int glfwKey, boolean repeat)
    {
        if (view == null)
        {
            return;
        }

        UltralightKey key = glfwToUltralightKey(glfwKey);

        if (key == null)
        {
            return;
        }

        UltralightKeyEvent event = new UltralightKeyEvent()
            .type(UltralightKeyEventType.DOWN)
            .keyIdentifier(UltralightKeyEvent.getKeyIdentifierFromVirtualKeyCode(key))
            .keypad(false)
            .autoRepeat(repeat)
            .systemKey(false);

        view.fireKeyEvent(event);
    }

    /** Forward a typed character to the page (for text inputs). */
    public static void fireChar(char c)
    {
        if (view == null || c == 0)
        {
            return;
        }

        UltralightKeyEvent event = new UltralightKeyEvent()
            .type(UltralightKeyEventType.CHAR)
            .keyIdentifier(String.valueOf(c))
            .keypad(false)
            .autoRepeat(false)
            .systemKey(false);

        view.fireKeyEvent(event);
    }

    /** GLFW key code -> UltralightKey (Windows VK style numbering). Only
     *  special keys that matter for an HTML editor UI are mapped; letters
     *  and digits are delivered as CHAR events instead. */
    private static UltralightKey glfwToUltralightKey(int key)
    {
        switch (key)
        {
            case 32: return UltralightKey.SPACE;
            case 256: return UltralightKey.ESCAPE;
            case 257: return UltralightKey.RETURN;
            case 258: return UltralightKey.TAB;
            case 259: return UltralightKey.BACK;
            case 261: return UltralightKey.DELETE;
            case 262: return UltralightKey.RIGHT;
            case 263: return UltralightKey.LEFT;
            case 264: return UltralightKey.DOWN;
            case 265: return UltralightKey.UP;
            case 266: return UltralightKey.PRIOR;
            case 267: return UltralightKey.NEXT;
            case 268: return UltralightKey.HOME;
            case 269: return UltralightKey.END;
            default: return null;
        }
    }

    /* -------- state -------- */

    public static boolean isReady()
    {
        return view != null && dynTex != null;
    }

    public static Identifier getTextureId()
    {
        return dynTexId;
    }

    public static int getWidth()
    {
        return dynTexW;
    }

    public static int getHeight()
    {
        return dynTexH;
    }

    /* -------- page -------- */

    private static String editorPage()
    {
        try
        {
            var stream = UltralightUI.class.getResourceAsStream("/assets/bbs/ultralight/editor_ui.html");

            if (stream != null)
            {
                return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        catch (Exception ignored)
        {
        }

        return "<html><body style='background:#222;color:#fff;font-family:sans-serif'><h2>Ultralight OK</h2></body></html>";
    }
}
