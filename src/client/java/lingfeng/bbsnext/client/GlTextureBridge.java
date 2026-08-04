package lingfeng.bbsnext.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridges raw OpenGL textures (as produced by BBS's own world/film
 * rendering into FBOs) into the vanilla texture system so they can be drawn
 * through GuiGraphicsExtractor.blit() on MC 26.2.
 *
 * Why: BBS's Batcher2D.texturedBox() used to draw a GL texture directly.
 * MC 26.2 moved GUI drawing onto the vanilla submit pipeline, so a raw GL id
 * can no longer be blitted. This class reads the GL texture back on the CPU
 * and registers it as a DynamicTexture (cached per GL id + size).
 *
 * GL-only: on Vulkan backends there is no current GL context, so the bridge
 * returns null and the caller falls back to a solid fill.
 */
public class GlTextureBridge
{
    private static final Map<Integer, Entry> cache = new HashMap<>();
    private static int counter;

    private static final class Entry
    {
        final DynamicTexture dynTex;
        final Identifier id;
        final int width;
        final int height;

        Entry(DynamicTexture dynTex, Identifier id, int width, int height)
        {
            this.dynTex = dynTex;
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Returns the vanilla Identifier of the given GL texture, reading the
     * pixels back on the CPU if needed (size change -> re-read). Returns
     * null when no GL context is available.
     */
    public static Identifier getOrCreate(int glId, int width, int height)
    {
        if (glId < 0 || width <= 0 || height <= 0 || !hasGlContext())
        {
            return null;
        }

        Entry entry = cache.get(glId);

        if (entry != null && entry.width == width && entry.height == height)
        {
            refresh(glId, entry);

            return entry.id;
        }

        /* New GL id (or size change): create a fresh DynamicTexture. */
        if (entry != null)
        {
            Minecraft.getInstance().getTextureManager().release(entry.id);
            cache.remove(glId);
        }

        try
        {
            ByteBuffer buffer = readPixels(glId, width, height);

            if (buffer == null)
            {
                return null;
            }

            NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, true);

            /* GL readback is RGBA byte order; NativeImage RGBA stores the
             * same bytes (little-endian ABGR int), copy straight through. */
            int[] abgr = image.getPixelsABGR();

            if (abgr == null || abgr.length < width * height)
            {
                return null;
            }

            buffer.position(0);

            for (int i = 0; i < width * height; i++)
            {
                int r = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int b = buffer.get() & 0xFF;
                int a = buffer.get() & 0xFF;

                abgr[i] = (a << 24) | (b << 16) | (g << 8) | r;
            }

            DynamicTexture dynTex = new DynamicTexture(() -> "bbs gl bridge " + (counter++), image);
            Identifier id = Identifier.fromNamespaceAndPath("bbs_mod", "glbridge/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, dynTex);

            entry = new Entry(dynTex, id, width, height);
            cache.put(glId, entry);

            return id;
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    /** Re-read the GL texture into the cached DynamicTexture. */
    private static void refresh(int glId, Entry entry)
    {
        try
        {
            ByteBuffer buffer = readPixels(glId, entry.width, entry.height);

            if (buffer == null)
            {
                return;
            }

            NativeImage image = entry.dynTex.getPixels();

            if (image == null || image.getWidth() != entry.width || image.getHeight() != entry.height)
            {
                return;
            }

            int[] abgr = image.getPixelsABGR();

            if (abgr == null || abgr.length < entry.width * entry.height)
            {
                return;
            }

            buffer.position(0);

            for (int i = 0; i < entry.width * entry.height; i++)
            {
                int r = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int b = buffer.get() & 0xFF;
                int a = buffer.get() & 0xFF;

                abgr[i] = (a << 24) | (b << 16) | (g << 8) | r;
            }

            entry.dynTex.setPixels(image);
            entry.dynTex.upload();
        }
        catch (Throwable ignored)
        {
        }
    }

    private static ByteBuffer readPixels(int glId, int width, int height)
    {
        if (!hasGlContext())
        {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());

        try
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            buffer.position(0);

            /* A zero-filled readback usually means the texture had no
             * valid contents yet - treat it as unavailable. */
            boolean allZero = true;

            for (int i = 0; i < 64 && i < width * height * 4; i++)
            {
                if (buffer.get(i) != 0)
                {
                    allZero = false;

                    break;
                }
            }

            return allZero ? null : buffer;
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private static boolean hasGlContext()
    {
        try
        {
            return org.lwjgl.glfw.GLFW.glfwGetCurrentContext() != 0L;
        }
        catch (Throwable t)
        {
            return false;
        }
    }
}
