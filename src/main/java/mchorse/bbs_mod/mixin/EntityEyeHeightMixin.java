package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [MC 26.2] getEyeHeight(Pose) is declared in Entity, not LivingEntity or Player.
 * We target Entity here so the hook applies to all living entities.
 * The actual morph check only proceeds for Player instances.
 */
@Mixin(value = Entity.class, remap = false)
public class EntityEyeHeightMixin
{
    @Inject(method = "getEyeHeight", at = @At("RETURN"), cancellable = true)
    public void onGetEyeHeight(CallbackInfoReturnable<Float> info)
    {
        if (!((Object) this instanceof Player player)) return;
        if (!(player instanceof IMorphProvider provider)) return;

        Morph morph = provider.getMorph();
        if (morph == null) return;

        Form form = morph.getForm();
        if (form == null || !form.hitbox.get()) return;

        float sneaking = player.isShiftKeyDown() ? form.hitboxSneakMultiplier.get() : 1F;
        float height = form.hitboxHeight.get() * sneaking;
        info.setReturnValue(form.hitboxEyeHeight.get() * height);
    }
}
