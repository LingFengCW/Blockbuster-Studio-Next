/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.world.level.CardinalLighting
 *  net.minecraft.world.level.ColorResolver
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.lighting.LevelLightEngine
 *  net.minecraft.world.level.material.FluidState
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class MovingBlockRenderState
implements BlockAndTintGetter {
    public BlockPos randomSeedPos = BlockPos.ZERO;
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public @Nullable Holder<Biome> biome;
    public CardinalLighting cardinalLighting = CardinalLighting.DEFAULT;
    public LevelLightEngine lightEngine = LevelLightEngine.EMPTY;

    @Override
    public CardinalLighting cardinalLighting() {
        return this.cardinalLighting;
    }

    public LevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        if (this.biome == null) {
            return -1;
        }
        return color.getColor((Biome)this.biome.value(), (double)pos.getX(), (double)pos.getZ());
    }

    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    public BlockState getBlockState(BlockPos pos) {
        if (pos.equals((Object)this.blockPos)) {
            return this.blockState;
        }
        return Blocks.AIR.defaultBlockState();
    }

    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    public int getHeight() {
        return 1;
    }

    public int getMinY() {
        return this.blockPos.getY();
    }
}

