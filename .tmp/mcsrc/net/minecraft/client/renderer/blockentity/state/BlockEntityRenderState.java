/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.CrashReportCategory
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.LightCoordsUtil
 *  net.minecraft.world.level.BlockAndLightGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityTypes
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    private BlockState blockState = Blocks.AIR.defaultBlockState();
    public BlockEntityType<?> blockEntityType = BlockEntityTypes.TEST_BLOCK;
    public int lightCoords;
    public @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress;

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
        state.blockEntityType = blockEntity.getType();
        state.lightCoords = blockEntity.getLevel() != null ? LightCoordsUtil.getLightCoords((BlockAndLightGetter)blockEntity.getLevel(), (BlockPos)blockEntity.getBlockPos()) : 0xF000F0;
        state.breakProgress = breakProgress;
    }

    public void fillCrashReportCategory(CrashReportCategory category) {
        category.setDetail("BlockEntityRenderState", (Object)this.getClass().getCanonicalName());
        category.setDetail("Position", (Object)this.blockPos);
        category.setDetail("Block state", () -> ((BlockState)this.blockState).toString());
    }
}

