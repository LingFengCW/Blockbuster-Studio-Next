package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block creation of a single-player world named after BBS's reserved preview
 * world (bbs_preview). The vanilla "Create World" screen lets the player type
 * any folder name; we cancel the creation so the reserved name can never be
 * clobbered by a manually created world. (The BBS editor's own world picker
 * already filters it on the Java side via EditorBridge.isReservedWorld.)
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin
{
    private static final String RESERVED = "bbs_preview";

    @Shadow
    protected abstract WorldCreationUiState getUiState();

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void bbsBlockReservedWorldName(CallbackInfo ci)
    {
        WorldCreationUiState uiState = this.getUiState();

        if (uiState == null)
        {
            return;
        }

        String name = uiState.getName();

        if (name != null && RESERVED.equals(name))
        {
            /* Hard block: do not let a world folder named bbs_preview be
             * created. Cancelling a void method is always safe (no NPE risk). */
            ci.cancel();
        }
    }
}
