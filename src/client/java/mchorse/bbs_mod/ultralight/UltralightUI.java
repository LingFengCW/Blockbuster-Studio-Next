package mchorse.bbs_mod.ultralight;

import com.labymedia.ultralight.UltralightJava;
import com.labymedia.ultralight.UltralightRenderer;
import com.labymedia.ultralight.UltralightView;
import com.labymedia.ultralight.input.UltralightMouseEvent;
import com.labymedia.ultralight.input.UltralightMouseEventButton;
import com.labymedia.ultralight.input.UltralightMouseEventType;
import net.minecraft.client.Minecraft;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
 * Rendering pipeline: Ultralight renders the HTML into a pixel surface,
 * we upload those pixels into an OpenGL texture and draw it through BBS's
 * own texture path - no vanilla renderer dependency.
 */
public class UltralightUI
{
    private static UltralightRenderer renderer;
    private static UltralightView view;
    private static boolean initialized;
    private static boolean loaded;

    /** The most recently rendered frame, as RGBA. */
    private static int texWidth;
    private static int texHeight;
    private static int glTexture = -1;

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
                renderer = UltralightRenderer.create();
                initialized = renderer != null;
            }
        }
        catch (Throwable e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("[Ultralight] init failed", e);
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
            mchorse.bbs_mod.BBSMod.LOGGER.warn("[Ultralight] SDK natives not found. Put the Ultralight SDK bin/ contents into {} to enable the HTML UI.", gameDir.resolve("bbsnext/ultralight"));
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
    public static void createView(int width, int height)
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

        view = renderer.createView(width, height, null);
        view.loadHTML(editorPage());
    }

    /* -------- rendering -------- */

    /** Render one frame and upload the pixels to a GL texture. */
    public static void render()
    {
        if (view == null || renderer == null)
        {
            return;
        }

        renderer.update();
        renderer.render();

        var surface = view.surface();

        if (surface == null)
        {
            return;
        }

        ByteBuffer pixels = surface.lockPixels();

        try
        {
            int w = (int) surface.width();
            int h = (int) surface.height();

            if (pixels == null || w <= 0 || h <= 0)
            {
                return;
            }

            upload(w, h, pixels);
        }
        finally
        {
            surface.unlockPixels();
        }
    }

    private static void upload(int w, int h, ByteBuffer bgra)
    {
        if (glTexture < 0 || texWidth != w || texHeight != h)
        {
            texWidth = w;
            texHeight = h;

            if (glTexture >= 0)
            {
                org.lwjgl.opengl.GL11.glDeleteTextures(glTexture);
            }

            glTexture = org.lwjgl.opengl.GL11.glGenTextures();
        }

        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, glTexture);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);

        /* Ultralight surfaces are BGRA; swizzle to RGBA into a direct buffer. */
        ByteBuffer rgba = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
        ByteBuffer src = bgra.duplicate();

        for (int i = 0; i < w * h; i++)
        {
            int b = src.get() & 0xFF;
            int g = src.get() & 0xFF;
            int r = src.get() & 0xFF;
            int a = src.get() & 0xFF;
            rgba.put((byte) r);
            rgba.put((byte) g);
            rgba.put((byte) b);
            rgba.put((byte) a);
        }

        rgba.flip();
        org.lwjgl.opengl.GL11.glTexImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_RGBA8, w, h, 0, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, rgba);
    }

    public static boolean isReady()
    {
        return view != null && glTexture >= 0;
    }

    public static int getTexture()
    {
        return glTexture;
    }

    public static int getWidth()
    {
        return texWidth;
    }

    public static int getHeight()
    {
        return texHeight;
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
