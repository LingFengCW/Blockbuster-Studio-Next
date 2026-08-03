/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.animal.allay.Allay
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.allay.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.allay.Allay;

public class AllayRenderer
extends MobRenderer<Allay, AllayRenderState, AllayModel> {
    private static final Identifier ALLAY_TEXTURE = Identifier.withDefaultNamespace((String)"textures/entity/allay/allay.png");

    public AllayRenderer(EntityRendererProvider.Context context) {
        super(context, new AllayModel(context.bakeLayer(ModelLayers.ALLAY)), 0.4f);
        this.addLayer(new ItemInHandLayer<AllayRenderState, AllayModel>(this));
    }

    @Override
    public Identifier getTextureLocation(AllayRenderState state) {
        return ALLAY_TEXTURE;
    }

    @Override
    public AllayRenderState createRenderState() {
        return new AllayRenderState();
    }

    @Override
    public void extractRenderState(Allay entity, AllayRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState((LivingEntity)entity, state, this.itemModelResolver, partialTicks);
        state.isDancing = entity.isDancing();
        state.isSpinning = entity.isSpinning();
        state.spinningProgress = entity.getSpinningProgress(partialTicks);
        state.holdingAnimationProgress = entity.getHoldingItemAnimationProgress(partialTicks);
    }

    @Override
    protected int getBlockLightLevel(Allay entity, BlockPos blockPos) {
        return 15;
    }
}

