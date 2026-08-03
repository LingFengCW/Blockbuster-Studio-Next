/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.animal.equine.AbstractHorse
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public abstract class AbstractHorseRenderer<T extends AbstractHorse, S extends EquineRenderState, M extends EntityModel<? super S>>
extends AgeableMobRenderer<T, S, M> {
    public AbstractHorseRenderer(EntityRendererProvider.Context context, M model, M babyModel) {
        super(context, model, babyModel, 0.75f);
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((EquineRenderState)state).saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
        ((EquineRenderState)state).bodyArmorItem = entity.getBodyArmorItem().copy();
        ((EquineRenderState)state).isRidden = entity.isVehicle();
        ((EquineRenderState)state).eatAnimation = entity.getEatAnim(partialTicks);
        ((EquineRenderState)state).standAnimation = entity.getStandAnim(partialTicks);
        ((EquineRenderState)state).feedingAnimation = entity.getMouthAnim(partialTicks);
        ((EquineRenderState)state).animateTail = ((AbstractHorse)entity).tailCounter > 0;
    }
}

