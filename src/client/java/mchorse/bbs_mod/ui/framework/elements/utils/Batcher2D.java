package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.ShaderProgram;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

/**
 * Batcher2D - 2D rendering batcher for MC 26.2.
 * Uses Minecraft Font directly for text, GuiGraphicsExtractor.fill for boxes.
 */
public class Batcher2D
{
    private static FontRenderer fontRenderer = new FontRenderer();

    private GuiGraphicsExtractor context;
    private FontRenderer font;

    public static FontRenderer getDefaultTextRenderer()
    {
        fontRenderer.setRenderer(Minecraft.getInstance().font);
        return fontRenderer;
    }

    public Batcher2D(GuiGraphicsExtractor context)
    {
        this.context = context;
        this.font = getDefaultTextRenderer();
    }

    public GuiGraphicsExtractor getContext()
    {
        return this.context;
    }

    /**
     * MC 26.2: rebind this batcher to the extractor provided by the game for
     * the current frame. All extraction calls (fill, text, etc.) then go into
     * the game's own GuiRenderState, which the engine draws in the draw phase.
     */
    public void setContext(GuiGraphicsExtractor context)
    {
        this.context = context;
    }

    public FontRenderer getFont()
    {
        return this.font;
    }

    public void setFont(FontRenderer font)
    {
        this.font = font;
    }

    /* ======== Box drawing ======== */

    /* All box/gradient methods take (x1, y1, x2, y2) boundary coordinates,
     * matching every call site in the codebase. During the MC 26.2 migration
     * these were mistakenly changed to (x, y, width, height), which made every
     * rectangle absurdly large and misaligned (e.g. selection outlines and
     * dashboard highlights). */
    public void box(int x1, int y1, int x2, int y2, int color)
    {
        this.context.fill(x1, y1, x2, y2, color);
    }

    /**
     * Rounded rectangle via a stepped corner approximation. The middle body
     * is a plain fill; every corner is a 1px-wide staircase following a
     * quarter circle of radius {@code r}. Good enough for panel corners at
     * typical UI sizes without any texture or shader work.
     */
    public void roundedBox(int x1, int y1, int x2, int y2, int r, int color)
    {
        if (x2 <= x1 || y2 <= y1)
        {
            return;
        }

        r = Math.max(0, Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));

        this.box(x1 + r, y1, x2 - r, y2, color);
        this.box(x1, y1 + r, x1 + r, y2 - r, color);
        this.box(x2 - r, y1 + r, x2, y2 - r, color);

        for (int i = 0; i < r; i++)
        {
            int dy = (int) Math.round(Math.sqrt(r * r - i * i));

            this.box(x1 + i, y1, x1 + i + 1, y1 + dy, color);
            this.box(x2 - i - 1, y1, x2 - i, y1 + dy, color);
            this.box(x1 + i, y2 - dy, x1 + i + 1, y2, color);
            this.box(x2 - i - 1, y2 - dy, x2 - i, y2, color);
        }
    }

    public void box(Area area, int color)
    {
        this.context.fill(area.x, area.y, area.x + area.w, area.y + area.h, color);
    }

    public void box(double x1, double y1, double x2, double y2, int color)
    {
        this.context.fill((int)x1, (int)y1, (int)x2, (int)y2, color);
    }

    public void box(float x1, float y1, float x2, float y2, int color)
    {
        this.context.fill((int)x1, (int)y1, (int)x2, (int)y2, color);
    }

    public void box(float x1, float y1, float x2, float y2, int color1, int color2, int color3, int color4)
    {
        this.context.fill((int)x1, (int)y1, (int)x2, (int)y2, color1);
    }

    /* fillRect keeps (x, y, width, height) semantics - every call site uses
     * widths/heights (unlike box/gradient methods which use x2/y2). */
    public void fillRect(int x, int y, int w, int h, int color)
    {
        this.context.fill(x, y, x + w, y + h, color);
    }

    public void normalizedBox(float x1, float y1, float x2, float y2, int color)
    {
        int minX = (int)Math.min(x1, x2);
        int minY = (int)Math.min(y1, y2);
        int maxX = (int)Math.max(x1, x2);
        int maxY = (int)Math.max(y1, y2);
        this.context.fill(minX, minY, maxX, maxY, color);
    }

    public void gradientVBox(int x1, int y1, int x2, int y2, int color1, int color2)
    {
        this.context.fill(x1, y1, x2, y2, color1);
    }

    public void gradientVBox(float x1, float y1, float x2, float y2, int color1, int color2)
    {
        this.context.fill((int)x1, (int)y1, (int)x2, (int)y2, color1);
    }

    public void gradientHBox(int x1, int y1, int x2, int y2, int color1, int color2)
    {
        this.context.fill(x1, y1, x2, y2, color1);
    }

    public void gradientHBox(float x1, float y1, float x2, float y2, int color1, int color2)
    {
        this.context.fill((int)x1, (int)y1, (int)x2, (int)y2, color1);
    }

    public void gradientBox(float x1, float y1, float x2, float y2, int topLeft, int topRight, int bottomRight, int bottomLeft)
    {
        this.context.fill((int)x1, (int)y1, (int)x2, (int)y2, topLeft);
    }

    public void dropShadow(float x, float y, float w, float h, int offset)
    {
        this.context.fill((int)(x + offset), (int)(y + offset), (int)(x + w + offset), (int)(y + h + offset), 0x44000000);
    }

    public void dropShadow(int x1, int y1, int x2, int y2, int offset, int color1, int color2)
    {
        this.context.fill(x1 + offset, y1 + offset, x2 + offset, y2 + offset, color1);
    }

    public void dropCircleShadow()
    {
    }

    public void area(float x, float y, float w, float h, int color)
    {
        this.context.fill((int)x, (int)y, (int)(x + w), (int)(y + h), color);
    }

    public void outline(int x1, int y1, int x2, int y2, int color)
    {
        this.context.fill(x1, y1, x2, y1 + 1, color);
        this.context.fill(x1, y2 - 1, x2, y2, color);
        this.context.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        this.context.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    public void outlineCenter(int x, int y, int w, int h, int color)
    {
        this.outline(x - w / 2, y - h / 2, x + w / 2, y + h / 2, color);
    }

    public void texturedBox(Supplier<ShaderProgram> shader, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        this.texturedBox(texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
    }

    public void texturedBox(int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        if (texture < 0 || textureW <= 0 || textureH <= 0 || this.context == null)
        {
            this.context.fill((int)x, (int)y, (int)(x + w), (int)(y + h), color);

            return;
        }

        /* The GL texture cannot be blitted directly on MC 26.2 - read it
         * back through GlTextureBridge and draw it via the vanilla blit
         * pipeline (works on GL backends; Vulkan falls back to a fill). */
        try
        {
            Identifier id = lingfeng.bbsnext.client.GlTextureBridge.getOrCreate(texture, textureW, textureH);

            if (id != null)
            {
                /* Input UVs are GL-style pixel coords with v flipped (v=0 at
                 * bottom); convert to normalized MC uvs (v=0 at top). */
                float u0 = Math.min(u1, u2) / (float) textureW;
                float u1n = Math.max(u1, u2) / (float) textureW;
                float v0 = 1F - Math.max(v1, v2) / (float) textureH;
                float v1n = 1F - Math.min(v1, v2) / (float) textureH;

                this.context.blit(id, (int) x, (int) y, (int) (x + w), (int) (y + h), u0, u1n, v0, v1n);

                return;
            }
        }
        catch (Throwable ignored)
        {
        }

        this.context.fill((int)x, (int)y, (int)(x + w), (int)(y + h), color);
    }

    public void fullTexturedBox(Texture texture, int x, int y, int w, int h)
    {
        this.context.fill(x, y, x + w, y + h, Colors.WHITE);
    }

    public void fullTexturedBox(Texture texture, float x, float y, float w, float h)
    {
        this.context.fill((int)x, (int)y, (int)(x + w), (int)(y + h), Colors.WHITE);
    }

    /* ======== Text rendering (via Minecraft Font) ======== */

    public void text(String string, int x, int y)
    {
        drawText(string, x, y, Colors.WHITE, false);
    }

    public void text(String string, int x, int y, int color)
    {
        drawText(string, x, y, color, false);
    }

    public void text(String string, int x, int y, int color, boolean shadow)
    {
        drawText(string, x, y, color, shadow);
    }

    public void text(String string, float x, float y, int color, boolean shadow)
    {
        drawText(string, (int)x, (int)y, color, shadow);
    }

    public void textShadow(String string, int x, int y)
    {
        drawText(string, x, y, Colors.WHITE, true);
    }

    public void textShadow(String string, int x, int y, int color)
    {
        drawText(string, x, y, color, true);
    }

    public void textShadow(String string, float x, float y)
    {
        drawText(string, (int)x, (int)y, Colors.WHITE, true);
    }

    public void textCard(String string, int x, int y)
    {
        Font minecraftFont = Minecraft.getInstance().font;
        int w = minecraftFont.width(string);
        int h = minecraftFont.lineHeight;
        this.context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xaa000000);
        drawText(string, x, y, Colors.WHITE, false);
    }

    public void textCard(String string, int x, int y, int color, int bgColor)
    {
        Font minecraftFont = Minecraft.getInstance().font;
        int w = minecraftFont.width(string);
        int h = minecraftFont.lineHeight;
        this.context.fill(x - 2, y - 2, x + w + 2, y + h + 2, bgColor);
        drawText(string, x, y, color, false);
    }

    public void textCard(String string, int x, int y, int color, int bgColor, int bgOffset)
    {
        Font minecraftFont = Minecraft.getInstance().font;
        int w = minecraftFont.width(string);
        int h = minecraftFont.lineHeight;
        this.context.fill(x - bgOffset, y - 2, x + w + bgOffset, y + h + 2, bgColor);
        drawText(string, x, y, color, false);
    }

    public void textCard(String string, int x, int y, int color, int bgColor, int bgOffset, boolean shadow)
    {
        Font minecraftFont = Minecraft.getInstance().font;
        int w = minecraftFont.width(string);
        int h = minecraftFont.lineHeight;
        this.context.fill(x - bgOffset, y - 2, x + w + bgOffset, y + h + 2, bgColor);
        drawText(string, x, y, color, shadow);
    }

    private void drawText(String str, int x, int y, int color, boolean shadow)
    {
        Font font = Minecraft.getInstance().font;

        /* MC 26.2: text(font, str, x, y, ARGB color, dropShadow). Colors with
         * a zero alpha channel are treated as fully transparent by the font
         * renderer, so force opaque alpha for legacy 0xRRGGBB values. */
        if ((color & 0xFF000000) == 0)
        {
            color |= 0xFF000000;
        }

        this.context.text(font, str, x, y, color, shadow);
    }

    public void wallText(String string, int x, int y, int color)
    {
        drawText(string, x, y, color, false);
    }

    /* ======== Icon rendering ======== */

    /** Render an icon sub-rectangle through the extractor's blit pipeline.
     *  The blit methods expect u/v in pixels (not normalized), and the
     *  srcWidth/srcHeight defaults to width/height. */
    private void renderIcon(Icon icon, int color, int dx, int dy)
    {
        if (icon == null || icon.texture == null || this.context == null) return;

        try
        {
            Identifier id = mchorse.bbs_mod.client.PipGeometry.bridge(icon.texture);

            /* This blit overload takes normalized UVs (0..1) */
            this.context.blit(id, dx, dy, dx + icon.w, dy + icon.h,
                icon.x / (float) icon.textureW,
                (icon.x + icon.w) / (float) icon.textureW,
                icon.y / (float) icon.textureH,
                (icon.y + icon.h) / (float) icon.textureH);
        }
        catch (Exception e)
        {
            this.context.fill(dx, dy, dx + icon.w, dy + icon.h,
                color >= 0 ? color | 0xFF000000 : 0x88FFFFFF);
        }
    }

    public void icon(Icon icon, int x, int y)
    {
        this.renderIcon(icon, -1, x, y);
    }

    public void icon(Icon icon, int x, int y, float ax, float ay)
    {
        if (icon == null) return;
        this.renderIcon(icon, -1, x - Math.round(icon.w * ax), y - Math.round(icon.h * ay));
    }

    public void icon(Icon icon, int color, int x, int y, float ax, float ay)
    {
        if (icon == null) return;
        this.renderIcon(icon, color, x - Math.round(icon.w * ax), y - Math.round(icon.h * ay));
    }

    public void icon(Icon icon, int color, int x, int y)
    {
        this.renderIcon(icon, color, x, y);
    }

    public void iconArea(Icon icon, int x, int y, int w, int h)
    {
        if (icon == null || icon.texture == null || this.context == null) return;
        try
        {
            Identifier id = mchorse.bbs_mod.client.PipGeometry.bridge(icon.texture);
            this.context.blit(id, x, y, x + w, y + h,
                icon.x / (float) icon.textureW,
                (icon.x + icon.w) / (float) icon.textureW,
                icon.y / (float) icon.textureH,
                (icon.y + icon.h) / (float) icon.textureH);
        }
        catch (Exception e)
        {
            this.context.fill(x, y, x + w, y + h, 0x88FFFFFF);
        }
    }

    public void iconArea(Icon icon, int color, int x, int y, int w, int h)
    {
        if (icon == null || icon.texture == null || this.context == null) return;
        try
        {
            Identifier id = mchorse.bbs_mod.client.PipGeometry.bridge(icon.texture);
            this.context.blit(id, x, y, x + w, y + h,
                icon.x / (float) icon.textureW,
                (icon.x + icon.w) / (float) icon.textureW,
                icon.y / (float) icon.textureH,
                (icon.y + icon.h) / (float) icon.textureH);
        }
        catch (Exception e)
        {
            this.context.fill(x, y, x + w, y + h, color >= 0 ? color | 0xFF000000 : 0x88FFFFFF);
        }
    }

    public void outlinedIcon(Icon icon, int x, int y, float ax, float ay)
    {
        this.icon(icon, x, y, ax, ay);
    }

    /* ======== Clipping ======== */

    private static boolean glCheckDone;
    private static boolean glAvailable;

    private static boolean isGLAvailable()
    {
        if (!glCheckDone)
        {
            glCheckDone = true;
            try
            {
                /* MC 26.2: detect backend by checking Window.backend()
                 * class name. "GlBackend" → OpenGL, anything else → Vulkan
                 * (which has no current GL context). */
                Object backend = net.minecraft.client.Minecraft.getInstance().getWindow().backend();
                String cls = backend.getClass().getName();
                glAvailable = cls.contains("GlBackend") || cls.contains("opengl");
            }
            catch (Throwable t)
            {
                glAvailable = false;
            }
        }
        return glAvailable;
    }

    public void clip(Area area, UIContext context)
    {
        if (context != null)
        {
            this.clip(area.x, area.y, area.w, area.h, 0, 0);
        }
    }

    public void clip(int x, int y, int w, int h, int sw, int sh)
    {
        if (this.context != null)
        {
            this.context.enableScissor(x, y, x + w, y + h);
        }
    }

    public void unclip(UIContext context)
    {
        this.unclip();
    }

    public void unclip(int sw, int sh)
    {
        this.unclip();
    }

    public void unclip()
    {
        if (this.context != null)
        {
            this.context.disableScissor();
        }
    }

    public void flush()
    {
    }

    public void reset()
    {
    }
}
