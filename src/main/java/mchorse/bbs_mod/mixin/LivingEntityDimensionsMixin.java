package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [MC 26.2] Player no longer overrides getDimensions(Pose); the method now lives
 * in LivingEntity (and is final there). This mixin targets LivingEntity so the
 * morph's hitbox dimensions are applied for player entities as well.
 */
@Mixin(value = LivingEntity.class, remap = false)
public class LivingEntityDimensionsMixin
{
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    public void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> info)
    {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof IMorphProvider provider)
        {
            Morph morph = provider.getMorph();

            if (morph != null)
            {
                Form form = morph.getForm();

                if (form != null && form.hitbox.get())
                {
                    float width = form.hitboxWidth.get();
                    float height = form.hitboxHeight.get() * (self.isShiftKeyDown() ? form.hitboxSneakMultiplier.get() : 1F);

                    info.setReturnValue(EntityDimensions.fixed(width, height));
                }
            }
        }
    }
}
