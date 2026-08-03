/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.animal.frog.Frog
 *  net.minecraft.world.entity.animal.frog.FrogVariant
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.frog.FrogModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.FrogRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;

public class FrogRenderer
extends MobRenderer<Frog, FrogRenderState, FrogModel> {
    public FrogRenderer(EntityRendererProvider.Context context) {
        super(context, new FrogModel(context.bakeLayer(ModelLayers.FROG)), 0.3f);
    }

    @Override
    public Identifier getTextureLocation(FrogRenderState state) {
        return state.texture;
    }

    @Override
    public FrogRenderState createRenderState() {
        return new FrogRenderState();
    }

    @Override
    public void extractRenderState(Frog entity, FrogRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isSwimming = entity.isInWater();
        state.jumpAnimationState.copyFrom(entity.jumpAnimationState);
        state.croakAnimationState.copyFrom(entity.croakAnimationState);
        state.tongueAnimationState.copyFrom(entity.tongueAnimationState);
        state.swimIdleAnimationState.copyFrom(entity.swimIdleAnimationState);
        state.texture = ((FrogVariant)entity.getVariant().value()).assetInfo().texturePath();
    }
}

