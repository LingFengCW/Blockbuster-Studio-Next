/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.Mth
 */
package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class WitherArmorLayer
extends EnergySwirlLayer<WitherRenderState, WitherBossModel> {
    private static final Identifier WITHER_ARMOR_LOCATION = Identifier.withDefaultNamespace((String)"textures/entity/wither/wither_armor.png");
    private final WitherBossModel model;

    public WitherArmorLayer(RenderLayerParent<WitherRenderState, WitherBossModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new WitherBossModel(modelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
    }

    @Override
    protected boolean isPowered(WitherRenderState state) {
        return state.isPowered;
    }

    @Override
    protected float xOffset(float t) {
        return Mth.cos((double)(t * 0.02f)) * 3.0f;
    }

    @Override
    protected Identifier getTextureLocation() {
        return WITHER_ARMOR_LOCATION;
    }

    @Override
    protected WitherBossModel model() {
        return this.model;
    }
}

