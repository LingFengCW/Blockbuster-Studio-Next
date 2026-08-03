/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 *  net.minecraft.util.random.Weighted
 *  net.minecraft.util.random.WeightedList
 */
package net.minecraft.client.renderer.block.dispatch;

import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

public class WeightedVariants
implements BlockStateModel {
    private final WeightedList<BlockStateModel> list;
    private final Material.Baked particleMaterial;
    private final @BakedQuad.MaterialFlags int materialFlags;

    public WeightedVariants(WeightedList<BlockStateModel> list) {
        this.list = list;
        BlockStateModel firstModel = (BlockStateModel)((Weighted)list.unwrap().getFirst()).value();
        this.particleMaterial = firstModel.particleMaterial();
        this.materialFlags = WeightedVariants.computeMaterialFlags(list);
    }

    private static @BakedQuad.MaterialFlags int computeMaterialFlags(WeightedList<BlockStateModel> list) {
        int flags = 0;
        for (Weighted entry : list.unwrap()) {
            flags |= ((BlockStateModel)entry.value()).materialFlags();
        }
        return flags;
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.materialFlags;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        ((BlockStateModel)this.list.getRandomOrThrow(random)).collectParts(random, output);
    }

    public record Unbaked(WeightedList<BlockStateModel.Unbaked> entries) implements BlockStateModel.Unbaked
    {
        @Override
        public BlockStateModel bake(ModelBaker modelBakery) {
            return new WeightedVariants((WeightedList<BlockStateModel>)this.entries.map(m -> m.bake(modelBakery)));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.entries.unwrap().forEach(v -> ((BlockStateModel.Unbaked)v.value()).resolveDependencies(resolver));
        }
    }
}

