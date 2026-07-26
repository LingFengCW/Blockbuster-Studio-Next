package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * For some unknown reason to me, if these methods are used in {@link PlayerEntityMorphMixin}
 * then the world will be locked for some reason... by extracting write/read NBT method to
 * a separate mixin fixes it...
 */
@Mixin(LivingEntity.class)
public class PlayerEntityMixin
{
    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("TAIL"))
    public void onAddAdditionalSaveData(ValueOutput output, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            output.store("BBSMorph", CompoundTag.CODEC, (CompoundTag) provider.getMorph().toNbt());
        }
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
    public void onReadAdditionalSaveData(ValueInput input, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Optional<CompoundTag> tag = input.read("BBSMorph", CompoundTag.CODEC);
            tag.ifPresent(t -> provider.getMorph().fromNbt(t));
        }
    }

    @Inject(method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;", at = @At("RETURN"), cancellable = true)
    public void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                LivingEntity living = (LivingEntity) (Object) this;
                EntityDimensions dimensions = info.getReturnValue();
                float height = form.hitboxHeight.get() * (living.isCrouching() ? form.hitboxSneakMultiplier.get() : 1F);

                EntityDimensions newDim;
                if (dimensions.fixed())
                {
                    newDim = EntityDimensions.fixed(form.hitboxWidth.get(), height);
                }
                else
                {
                    newDim = EntityDimensions.scalable(form.hitboxWidth.get(), height);
                }

                // MC 26.2: eyeHeight is part of EntityDimensions. Set it from the form.
                newDim = newDim.withEyeHeight(form.hitboxEyeHeight.get() * height);
                info.setReturnValue(newDim);
            }
        }
    }
}