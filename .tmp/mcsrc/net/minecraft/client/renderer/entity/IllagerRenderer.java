/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.monster.illager.AbstractIllager
 *  net.minecraft.world.entity.monster.illager.AbstractIllager$IllagerArmPose
 *  net.minecraft.world.item.CrossbowItem
 *  net.minecraft.world.item.ItemStack
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public abstract class IllagerRenderer<T extends AbstractIllager, S extends IllagerRenderState>
extends MobRenderer<T, S, IllagerModel<S>> {
    protected IllagerRenderer(EntityRendererProvider.Context context, IllagerModel<S> model, float shadow) {
        super(context, model, shadow);
        this.addLayer(new CustomHeadLayer(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
        ((IllagerRenderState)state).isRiding = entity.isPassenger();
        ((IllagerRenderState)state).mainArm = entity.getMainArm();
        ((IllagerRenderState)state).armPose = entity.getArmPose();
        ((IllagerRenderState)state).maxCrossbowChargeDuration = ((IllagerRenderState)state).armPose == AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE ? CrossbowItem.getChargeDuration((ItemStack)entity.getUseItem(), entity) : 0;
        ((IllagerRenderState)state).ticksUsingItem = entity.getTicksUsingItem(partialTicks);
        ((IllagerRenderState)state).attackAnim = entity.getAttackAnim(partialTicks);
        ((IllagerRenderState)state).isAggressive = entity.isAggressive();
    }
}

