package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class LivingEntityMixin
{
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))
    public void onHurt(DamageSource source, float amount, CallbackInfo info)
    {
        if (!(((Object) this) instanceof LivingEntity)) return;
        if (!source.isDirect()) return;

        Entity attacker = source.getEntity();

        if (attacker != null && attacker.getClass() == ServerPlayer.class)
        {
            BBSMod.getActions().addAction((ServerPlayer) attacker, () ->
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(amount);

                return clip;
            });
        }
    }

    /* @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    public void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo info)
    {
        info.cancel();
    } */
}