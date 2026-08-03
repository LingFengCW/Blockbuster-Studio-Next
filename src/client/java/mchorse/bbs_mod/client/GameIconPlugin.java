package mchorse.bbs_mod.client;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Plugin that replaces the Minecraft window icon after the game has launched.
 *
 * The icon is loaded from the mod's classpath resource {@code bbs_mod/icon.png}.
 * The title is applied here for the initial window, and re-applied continuously
 * in BBSModClient's END_CLIENT_TICK so it isn't overwritten by MC's own
 * window title changes during startup/world loading.
 */
public class GameIconPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GameIconPlugin.class);
    private static final String ICON_RESOURCE = "/bbs_mod/icon.png";
    public static final String WINDOW_TITLE = "Blockbuster Studio Next";

    public static void unsafeApplyTitle()
    {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        if (window != null)
        {
            GLFW.glfwSetWindowTitle(window.handle(), WINDOW_TITLE);
        }
    }

    public static void apply()
    {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        if (window == null)
        {
            return;
        }

        byte[] bytes;

        try (InputStream stream = GameIconPlugin.class.getResourceAsStream(ICON_RESOURCE))
        {
            if (stream == null)
            {
                LOGGER.info("Custom game icon not applied: {} not found on classpath (icon pending).", ICON_RESOURCE);

                return;
            }

            bytes = stream.readAllBytes();
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to read custom game icon resource", e);

            return;
        }

        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);

        encoded.put(bytes);
        encoded.flip();

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, w, h, channels, 4);

            if (pixels == null)
            {
                LOGGER.warn("Failed to decode custom game icon: {}", STBImage.stbi_failure_reason());

                return;
            }

            try
            {
                GLFWImage.Buffer icons = GLFWImage.malloc(1, stack);

                icons.position(0);
                icons.width(w.get(0));
                icons.height(h.get(0));
                icons.pixels(pixels);

                GLFW.glfwSetWindowIcon(window.handle(), icons);

                LOGGER.info("Applied custom game window icon ({}x{}).", w.get(0), h.get(0));
            }
            finally
            {
                STBImage.stbi_image_free(pixels);
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to apply custom game window icon", e);
        }
        finally
        {
            MemoryUtil.memFree(encoded);
        }
    }
}
