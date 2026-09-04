package lingfeng.bbsnext.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

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
 * The GL-only {@link #getOrCreate} bridge (used for BBS's own FBO textures)
 * still requires an OpenGL context; the editor live-preview capture
 * ({@link #captureMainRenderTarget}) is backend-agnostic and works on both
 * OpenGL and Vulkan via the vanilla screenshot readback.
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
     * Returns the vanilla Identifier already cached for {@code glId} without
     * touching the GPU, or null when that GL id is not (yet) bridged. Used to
     * avoid a per-frame pixel readback when the source texture has not
     * changed.
     */
    public static Identifier peek(int glId)
    {
        Entry entry = cache.get(glId);

        return entry == null ? null : entry.id;
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

    public static boolean hasGlContext()
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

    /**
     * Reads a GL texture (by id) back to the CPU and returns it as PNG bytes,
     * downscaled so the longer side is at most {@code maxSide}. Used to push
     * the live 3D world into the HTML editor preview. Returns null when no GL
     * context is available (Vulkan backend) or the readback is empty.
     *
     * <p>The GL readback is bottom-up RGBA; we flip vertically and convert to
     * ARGB so the produced PNG is upright.
     */
    public static byte[] captureTextureToPng(int glId, int width, int height, int maxSide)
    {
        if (!hasGlContext() || glId < 0 || width <= 0 || height <= 0 || maxSide <= 0)
        {
            return null;
        }

        ByteBuffer buffer = readPixels(glId, width, height);

        if (buffer == null)
        {
            return null;
        }

        int tgtW = Math.min(maxSide, width);
        int tgtH = Math.max(1, Math.round((float) tgtW * height / width));

        int[] argb = new int[tgtW * tgtH];

        for (int ty = 0; ty < tgtH; ty++)
        {
            int sy = (height - 1) - (int) ((ty + 0.5F) * height / tgtH);

            for (int tx = 0; tx < tgtW; tx++)
            {
                int sx = (int) ((tx + 0.5F) * width / tgtW);
                int src = (sy * width + sx) * 4;

                int r = buffer.get(src) & 0xFF;
                int g = buffer.get(src + 1) & 0xFF;
                int b = buffer.get(src + 2) & 0xFF;
                int a = buffer.get(src + 3) & 0xFF;

                argb[ty * tgtW + tx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        BufferedImage image = new BufferedImage(tgtW, tgtH, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, tgtW, tgtH, argb, 0, tgtW);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try
        {
            ImageIO.write(image, "png", baos);
        }
        catch (IOException e)
        {
            return null;
        }

        return baos.toByteArray();
    }

    /**
     * Captures Minecraft's main render target (the live 3D world) into a PNG,
     * downscaled so the longer side is at most {@code maxSide}. This is how the
     * HTML editor's centre "player" gets a real picture of the world instead of
     * a dead decoration.
     *
     * <p>Backend-agnostic: it uses the vanilla {@link Screenshot#takeScreenshot}
     * readback, the same code path the F2 screenshot takes, which works on both
     * the OpenGL and the Vulkan renderers. The returned image is already
     * screen-upright, so no vertical flip is applied (unlike the raw GL
     * readback used elsewhere). Returns null for any failure (null target,
     * empty readback, encode error, etc).
     */
    public static byte[] captureMainRenderTarget(RenderTarget rt, int maxSide)
    {
        if (rt == null || maxSide <= 0)
        {
            return null;
        }

        /* MC 26.2: Screenshot.takeScreenshot is callback-based and
         * backend-agnostic (works on both OpenGL and Vulkan). The provided
         * NativeImage is only valid inside the callback, so we encode + scale
         * it here and stash the resulting bytes in a holder. */
        final byte[][] holder = new byte[1][];

        Screenshot.takeScreenshot(rt, image -> {
            try
            {
                int w = image.getWidth();
                int h = image.getHeight();

                if (w <= 0 || h <= 0)
                {
                    return;
                }

                /* Let Minecraft's own PNG writer produce a correctly oriented
                 * (screen-upright) image, then decode + scale on the CPU. We do
                 * not close `image` here: the screenshot utility owns its
                 * lifetime and releases it after the callback returns. */
                Path tmp = Files.createTempFile("bbs_preview", ".png");

                try
                {
                    image.writeToFile(tmp);

                    BufferedImage full = ImageIO.read(tmp.toFile());

                    if (full == null)
                    {
                        return;
                    }

                    int tgtW = Math.min(maxSide, w);
                    int tgtH = Math.max(1, Math.round((float) tgtW * h / w));

                    BufferedImage scaled = new BufferedImage(tgtW, tgtH, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(full, 0, 0, tgtW, tgtH, null);
                    g.dispose();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    if (ImageIO.write(scaled, "png", baos))
                    {
                        holder[0] = baos.toByteArray();
                    }
                }
                finally
                {
                    Files.deleteIfExists(tmp);
                }
            }
            catch (Throwable t)
            {
                /* swallow; holder stays null */
            }
        });

        return holder[0];
    }
}
