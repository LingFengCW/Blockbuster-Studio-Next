/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.entity.monster.zombie.Zombie
 *  net.minecraft.world.item.SwingAnimationType
 *  net.minecraft.world.item.component.SwingAnimation
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;

public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>>
extends HumanoidMobRenderer<T, S, M> {
    private static final Identifier ZOMBIE_LOCATION = Identifier.withDefaultNamespace((String)"textures/entity/zombie/zombie.png");
    private static final Identifier BABY_ZOMBIE_LOCATION = Identifier.withDefaultNamespace((String)"textures/entity/zombie/zombie_baby.png");

    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M model, M babyModel, ArmorModelSet<M> armorSet, ArmorModelSet<M> babyArmorSet) {
        super(context, model, babyModel, 0.5f);
        this.addLayer(new HumanoidArmorLayer(this, armorSet, babyArmorSet, context.getEquipmentRenderer()));
    }

    @Override
    public Identifier getTextureLocation(S state) {
        return ((ZombieRenderState)state).isBaby ? BABY_ZOMBIE_LOCATION : ZOMBIE_LOCATION;
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((ZombieRenderState)state).isAggressive = entity.isAggressive();
        ((ZombieRenderState)state).isConverting = entity.isUnderWaterConverting();
    }

    @Override
    protected boolean isShaking(S state) {
        return super.isShaking(state) || ((ZombieRenderState)state).isConverting;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
        SwingAnimation otherAnim = (SwingAnimation)mob.getItemHeldByArm(arm.getOpposite()).get(DataComponents.SWING_ANIMATION);
        if (otherAnim != null && otherAnim.type() == SwingAnimationType.STAB) {
            return HumanoidModel.ArmPose.SPEAR;
        }
        return super.getArmPose(mob, arm);
    }
}

