package lingfeng.bbsnext.mcef;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.Minecraft;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;
import com.mojang.blaze3d.textures.GpuTextureView;

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

    /** Keep the browser viewport in sync with the actual game window size.
     *  MCEF's OSR texture is reallocated on resize(), so calling it whenever
     *  the window (or fullscreen state) changes is the correct way to keep
     *  the browser rendering across fullscreen toggles (no texture rebuild
     *  needed - MC 26.2 preserves the GL context on fullscreen). */
    private int lastGW = -1;
    private int lastGH = -1;
    private int dbgFrames = 0;

    private void syncSize()
    {
        Minecraft mc = Minecraft.getInstance();
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();

        if (gw != this.lastGW || gh != this.lastGH)
        {
            this.lastGW = gw;
            this.lastGH = gh;

            if (this.created && gw > 0 && gh > 0)
            {
                MCEFUI.resizeBrowser(gw, gh);
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

        /* Keep the input coordinate origin in sync with where the texture is
         * actually blitted (area.x/area.y), so clicks map to the right spot. */
        MCEFUI.setViewportOffset(this.area.x, this.area.y);

        /* The browser frame is composited onto the screen by MCEFUI.renderBrowser,
         * called from UIScreen.extractRenderState (the only path that reaches the
         * screen on MC 26.2). Here we only draw a dark backdrop until the first
         * frame is available, so the overlay is visibly active while MCEF warms
         * up. No direct blit() - it would never draw. */
        GpuTextureView view = MCEFUI.getTextureView();

        if (this.dbgFrames < 6)
        {
            this.dbgFrames++;
            BBSMod.LOGGER.info("[MCEF-DBG] render#{} visible={} created={} area=({},{},{},{}) viewNull={} ready={}",
                this.dbgFrames, this.isVisible(), this.created, this.area.x, this.area.y, this.area.w, this.area.h,
                view == null, MCEFUI.isReady());
        }

        if (view == null && MCEFUI.isReady())
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
