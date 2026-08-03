/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockAndLightGetter
 *  net.minecraft.world.level.CardinalLighting
 *  net.minecraft.world.level.ColorResolver
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.lighting.LevelLightEngine
 *  net.minecraft.world.level.material.FluidState
 *  net.minecraft.world.level.material.Fluids
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public interface BlockAndTintGetter
extends BlockAndLightGetter {
    public static final BlockAndTintGetter EMPTY = new BlockAndTintGetter(){

        @Override
        public CardinalLighting cardinalLighting() {
            return CardinalLighting.DEFAULT;
        }

        public LevelLightEngine getLightEngine() {
            return LevelLightEngine.EMPTY;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return -1;
        }

        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        public BlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.defaultBlockState();
        }

        public FluidState getFluidState(BlockPos pos) {
            return Fluids.EMPTY.defaultFluidState();
        }

        public int getHeight() {
            return 0;
        }

        public int getMinY() {
            return 0;
        }
    };

    public CardinalLighting cardinalLighting();

    public int getBlockTint(BlockPos var1, ColorResolver var2);
}

