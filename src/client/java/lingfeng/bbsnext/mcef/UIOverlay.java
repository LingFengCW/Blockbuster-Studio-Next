package lingfeng.bbsnext.mcef;

import com.mojang.blaze3d.textures.GpuTextureView;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Full-screen overlay that renders the MCEF browser texture onto the screen
 * while the HTML editor is open.
 *
 * The browser texture (GpuTextureView) is blitted straight into the GUI
 * render state; input events are forwarded from {@link UIScreen} through
 * {@link MCEFUI}, so this element only handles rendering and the lazy
 * browser creation.
 */
public class UIOverlay extends UIElement
{
    private final UIFilmPanel panel;
    private boolean created;
    private long lastPush;

    public UIOverlay(UIFilmPanel panel)
    {
        this.panel = panel;
        this.markContainer();
    }

    /** (Re)create the browser after the first time, e.g. when re-opening. */
    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible)
        {
            this.created = false;

            if (this.area.w > 0 && this.area.h > 0)
            {
                this.created = MCEFUI.createBrowser(this.area.w, this.area.h, new EditorBridge(this.panel));
            }
        }
        else
        {
            MCEFUI.close();
        }
    }

    /** Keep the browser viewport in sync with the overlay size (handles
     *  window resizing / fullscreen toggles). */
    private int lastW = -1;
    private int lastH = -1;

    private void syncSize()
    {
        if (this.area.w != this.lastW || this.area.h != this.lastH)
        {
            this.lastW = this.area.w;
            this.lastH = this.area.h;

            if (this.created && this.area.w > 0 && this.area.h > 0)
            {
                MCEFUI.resizeBrowser(this.area.w, this.area.h);
            }
        }
    }

    @Override
    public void render(UIContext context)
    {
        if (!this.isVisible())
        {
            return;
        }

        /* Fallback creation if setVisible ran before the area was known, or
         * while the CEF engine was still initializing - retry until ready. */
        if (!this.created && this.area.w > 0 && this.area.h > 0)
        {
            this.created = MCEFUI.createBrowser(this.area.w, this.area.h, new EditorBridge(this.panel));
        }

        this.syncSize();

        GpuTextureView texture = MCEFUI.renderTexture();

        if (texture != null)
        {
            try
            {
                var graphics = context.batcher.getContext();

                if (graphics != null)
                {
                    graphics.guiRenderState.addGuiElement(new net.minecraft.client.renderer.state.gui.BlitRenderState(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        net.minecraft.client.gui.render.TextureSetup.singleTexture(texture,
                            com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR)),
                        new org.joml.Matrix3x2f(graphics.pose()),
                        this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                        0.0F, 1.0F, 0.0F, 1.0F,
                        0xFFFFFFFF,
                        graphics.scissorStack.peek()
                    ));
                }
            }
            catch (Throwable e)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
            }
        }
        else if (MCEFUI.isReady())
        {
            /* Browser exists but has no frame yet - show a dark backdrop. */
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
        }

        /* Push editor state into the page periodically (Java -> JS). */
        long now = System.currentTimeMillis();

        if (now - this.lastPush > 150)
        {
            this.lastPush = now;
            MCEFUI.pushState();
        }

        super.render(context);
    }
}
