/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.random.Weighted
 *  net.minecraft.util.random.WeightedList
 */
package net.minecraft.client.data.models;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

public record MultiVariant(WeightedList<Variant> variants) {
    public MultiVariant {
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Variant list must contain at least one element");
        }
    }

    public MultiVariant with(VariantMutator mutator) {
        return new MultiVariant((WeightedList<Variant>)this.variants.map((Function)mutator));
    }

    public BlockStateModel.Unbaked toUnbaked() {
        List entries = this.variants.unwrap();
        return entries.size() == 1 ? new SingleVariant.Unbaked((Variant)((Weighted)entries.getFirst()).value()) : new WeightedVariants.Unbaked((WeightedList<BlockStateModel.Unbaked>)this.variants.map(SingleVariant.Unbaked::new));
    }
}

