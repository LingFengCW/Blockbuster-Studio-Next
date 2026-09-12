package mchorse.bbs_mod.mixin.client;

import lingfeng.bbsnext.mcef.EditorBridge;
import lingfeng.bbsnext.mcef.MCEFUI;
import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the editor on screen while a preview world loads, by NOT drawing the
 * vanilla {@link LoadingOverlay} at all.
 *
 * <p><b>Why drawing over it can never work.</b> {@code Gui.extractRenderState}
 * is structured as:
 *
 * <pre>
 * if (this.overlay != null) {
 *     this.overlay.extractRenderState(graphics, ...);          // vanilla loading screen
 * } else if (resourcesLoaded &amp;&amp; this.screen != null) {
 *     this.screen.extractRenderStateWithTooltipAndSubtitles(...);  // the editor
 * }
 * </pre>
 *
 * That is an {@code else if}: while a loading overlay exists the screen branch
 * never runs, and the editor is a screen-backed MCEF browser. So nothing drawn
 * "on top of" the overlay could ever be composited - the canvas it was drawn
 * onto was never built. That is exactly why the previous
 * "reset the overlay and paint the browser over it" attempt produced a vanilla
 * loading screen with a misplaced browser behind it.
 *
 * <p><b>What this does instead.</b> The overlay's own draw call is redirected:
 * the vanilla loading screen is never rendered (not covered - never drawn), and
 * the browser's last frame is composited in its place. The overlay object is
 * left installed, so {@code Minecraft}'s load loop still ticks it and
 * {@code onFinish} still completes world loading normally (setting the overlay
 * aside instead would stall the load forever).
 */
@Mixin(Gui.class)
public abstract class GuiLoadingOverlayMixin
{
    private static boolean bbs$loggedInjected = false;

    /**
     * Redirect for the call site as javac emits it against the field's static
     * type ({@code Overlay}). {@code require = 0} on purpose: the owner may
     * instead be {@link Renderable} (the interface that actually declares the
     * method), in which case the sibling redirect below is the one that binds.
     * Exactly one of the two matches, so requiring both would fail the build.
     */
    @Redirect(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Overlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"),
        require = 0
    )
    private void bbs$skipVanillaLoadingOverlay(Overlay overlay, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        this.bbs$drawOrSuppress(overlay, graphics, mouseX, mouseY, delta);
    }

    /** Sibling redirect for the {@link Renderable}-owned call site. */
    @Redirect(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Renderable;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"),
        require = 0
    )
    private void bbs$skipVanillaLoadingOverlayRenderable(Renderable overlay, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        this.bbs$drawOrSuppress(overlay, graphics, mouseX, mouseY, delta);
    }

    /**
     * Suppress the vanilla loading screen and composite the editor browser
     * instead, but only while the editor owns a preview world - a normal
     * single-player join that the editor does not own keeps the vanilla
     * loading screen untouched.
     */
    private void bbs$drawOrSuppress(Renderable overlay, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        if (overlay instanceof LoadingOverlay && EditorBridge.isBrowserOverlayActive())
        {
            if (!bbs$loggedInjected)
            {
                bbs$loggedInjected = true;

                BBSMod.LOGGER.info("[EditorBridge] vanilla LoadingOverlay draw suppressed; compositing the editor browser in its place");
            }

            MCEFUI.renderBrowserOnTop(graphics, graphics.guiWidth(), graphics.guiHeight());

            return;
        }

        overlay.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
