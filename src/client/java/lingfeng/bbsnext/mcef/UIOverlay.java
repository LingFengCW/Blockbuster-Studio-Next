package lingfeng.bbsnext.mcef;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.Minecraft;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;
import com.mojang.blaze3d.textures.GpuTextureView;

/**
 * Full-screen overlay that renders the MCEF browser texture onto the screen
 * while an HTML page (editor or dashboard) is open.
 *
 * The browser texture (GpuTextureView) is blitted straight into the GUI
 * render state; input events are forwarded from {@link UIScreen} through
 * {@link MCEFUI}, so this element only handles rendering and the lazy
 * browser creation for whichever {@link IHtmlBridge} it was given.
 */
public class UIOverlay extends UIElement
{
    private final IHtmlBridge bridge;
    private boolean created;
    private long lastPush;

    public UIOverlay(UIElement owner, IHtmlBridge bridge)
    {
        this.bridge = bridge;
        this.markContainer();
    }

    public IHtmlBridge getBridge()
    {
        return this.bridge;
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
                this.created = MCEFUI.createBrowser(this.area.w, this.area.h, this.bridge);
            }

            MCEFUI.setActiveOverlay(this);
        }
        else
        {
            MCEFUI.clearActiveOverlay(this);
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
    private int lastGuiScale = -1;
    private int dbgFrames = 0;

    private void syncSize()
    {
        Minecraft mc = Minecraft.getInstance();
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int scale = mc.getWindow().getGuiScale();

        /* The browser OSR texture is allocated in physical pixels
         * (GUI units * guiScale). Entering fullscreen can keep the GUI-unit
         * dimensions identical while the guiScale changes (e.g. 1280x720
         * windowed @ scale 2 vs 1920x1080 fullscreen @ scale 3 -> both 640x360
         * GUI units). The old code only compared GUI width/height, so it never
         * re-sized the texture and every click landed shifted (input was
         * multiplied by the new scale, but the browser was still the old pixel
         * size). Track guiScale too and resize whenever any of them differ. */
        if (gw != this.lastGW || gh != this.lastGH || scale != this.lastGuiScale)
        {
            this.lastGW = gw;
            this.lastGH = gh;
            this.lastGuiScale = scale;

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
            this.created = MCEFUI.createBrowser(this.area.w, this.area.h, this.bridge);
        }

        this.syncSize();

        /* The browser frame is composited fullscreen at (0,0) by
         * MCEFUI.renderBrowser (called from UIScreen with the whole screen's
         * GUI width/height). Input must use the SAME origin as the render
         * rect, otherwise clicks land shifted by the panel's top margin
         * (here area.y ~= 34 because the dashboard root carries the top bar +
         * actor toolbar above this overlay). So the input offset is pinned to
         * (0,0) to match the fullscreen blit - this is what makes clicks line
         * up exactly with what is drawn. */
        MCEFUI.setViewportOffset(0, 0);

        /* The browser frame is composited onto the screen by MCEFUI.renderBrowser,
         * called from UIScreen.extractRenderState (the only path that reaches the
         * screen on MC 26.2). Here we only draw a dark backdrop until the first
         * frame is available, so the overlay is visibly active while MCEF warms
         * up. No direct blit() - it would never draw. */
        GpuTextureView view = MCEFUI.getTextureView();

        /* Periodic diagnostic: report whether the GPU texture has appeared yet
         * (every ~2s for the first ~40s of the overlay being visible). */
        this.dbgFrames++;

        if (this.dbgFrames <= 20 || this.dbgFrames % 120 == 0)
        {
            BBSMod.LOGGER.info("[MCEF-DBG] frame#{} visible={} created={} area=({},{},{},{}) viewNull={} ready={} url={}",
                this.dbgFrames, this.isVisible(), this.created, this.area.x, this.area.y, this.area.w, this.area.h,
                view == null, MCEFUI.isReady(), this.bridge.pageUrl());
        }

        if (view == null)
        {
            /* No browser frame yet (engine still warming up, or first paint
             * pending). Cover the ENTIRE screen with an opaque backdrop (not
             * just below the top bar) so the native dashboard top bar and native
             * project-selection page - which render underneath this overlay -
             * never poke through during warm-up. */
            Minecraft mc = Minecraft.getInstance();
            int gw = mc.getWindow().getGuiScaledWidth();
            int gh = mc.getWindow().getGuiScaledHeight();
            context.batcher.box(0, 0, gw, gh, 0xFF14181E);
        }

        /* Push page state into the page periodically (Java -> JS). */
        long now = System.currentTimeMillis();

        if (now - this.lastPush > 150)
        {
            this.lastPush = now;
            MCEFUI.pushState();
        }

        super.render(context);
    }
}
