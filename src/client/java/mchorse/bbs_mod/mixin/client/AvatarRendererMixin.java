package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.PlayerMorphCapture;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 26.2: players are rendered by AvatarRenderer (PlayerRenderer was removed).
 * AvatarRenderer extends LivingEntityRenderer but does NOT override submit, so the
 * body is actually drawn by the inherited LivingEntityRenderer.submit — that hook
 * lives in LivingEntityRendererMixin. This mixin only captures the player entity
 * from extractRenderState (the one hook that still receives the real entity) and
 * hands it to LivingEntityRendererMixin via PlayerMorphCapture.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin
{
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void bbs$onExtract(Entity entity, AvatarRenderState state, float partialTick, CallbackInfo ci)
    {
        if (entity instanceof Player player)
        {
            PlayerMorphCapture.PLAYER.set(player);
            PlayerMorphCapture.TICK.set(partialTick);
        }
    }
}
