/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.monster.Vex
 */
package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.vex.VexModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;

public class VexRenderer
extends MobRenderer<Vex, VexRenderState, VexModel> {
    private static final Identifier VEX_LOCATION = Identifier.withDefaultNamespace((String)"textures/entity/illager/vex.png");
    private static final Identifier VEX_CHARGING_LOCATION = Identifier.withDefaultNamespace((String)"textures/entity/illager/vex_charging.png");

    public VexRenderer(EntityRendererProvider.Context context) {
        super(context, new VexModel(context.bakeLayer(ModelLayers.VEX)), 0.3f);
        this.addLayer(new ItemInHandLayer<VexRenderState, VexModel>(this));
    }

    @Override
    protected int getBlockLightLevel(Vex entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        if (state.isCharging) {
            return VEX_CHARGING_LOCATION;
        }
        return VEX_LOCATION;
    }

    @Override
    public VexRenderState createRenderState() {
        return new VexRenderState();
    }

    @Override
    public void extractRenderState(Vex entity, VexRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState((LivingEntity)entity, state, this.itemModelResolver, partialTicks);
        state.isCharging = entity.isCharging();
    }
}

