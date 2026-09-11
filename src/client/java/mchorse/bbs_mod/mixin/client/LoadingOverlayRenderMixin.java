package mchorse.bbs_mod.mixin.client;

import lingfeng.bbsnext.mcef.EditorBridge;
import lingfeng.bbsnext.mcef.MCEFUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Task: keep the MCEF editor browser painted on top of the vanilla
 * {@link LoadingOverlay} while the editor is entering (or switching into) its
 * preview world.
 *
 * <p>The editor browser is normally composited only from
 * {@code UIScreen.extractRenderState} (the dashboard screen). During
 * {@code openWorld(...)} MC swaps that screen away and shows the native
 * LoadingOverlay, leaving the browser un-drawn for the load duration - which is
 * exactly the window where the user sees the vanilla spinner "push the editor
 * off". We draw the live browser texture again here, on top of the overlay, so
 * the editor (and its own in-page loading spinner) stays visible the whole time.
 *
 * <p>We deliberately do NOT cancel the overlay's own render: the overlay drives
 * world-load finalisation, and cancelling it freezes the world (project iron
 * rule). Drawing on top is safe.
 *
 * <p>Guarded by {@link EditorBridge#isBrowserOverlayActive()} so a normal
 * single-player world join (which the editor does not own) is completely
 * untouched - its native LoadingOverlay shows as usual.
 *
 * <p>Registered as {@code required: false}: if the 26.2
 * {@code LoadingOverlay.extractRenderState} signature ever drifts, the mixin
 * simply skips instead of crashing the game.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayRenderMixin
{
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void bbs$drawBrowserOnTop(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        if (!EditorBridge.isBrowserOverlayActive())
        {
            return;
        }

        int w = extractor.guiWidth();
        int h = extractor.guiHeight();

        MCEFUI.renderBrowserOnTop(extractor, w, h);
    }
}
