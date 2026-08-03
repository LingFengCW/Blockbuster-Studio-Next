/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.LightCoordsUtil
 *  net.minecraft.world.level.BlockAndLightGetter
 *  net.minecraft.world.level.block.DoubleBlockCombiner$Combiner
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package net.minecraft.client.renderer.blockentity;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BrightnessCombiner<S extends BlockEntity>
implements DoubleBlockCombiner.Combiner<S, Int2IntFunction> {
    public Int2IntFunction acceptDouble(S first, S second) {
        return i -> LightCoordsUtil.max((int)LightCoordsUtil.getLightCoords((BlockAndLightGetter)first.getLevel(), (BlockPos)first.getBlockPos()), (int)LightCoordsUtil.getLightCoords((BlockAndLightGetter)second.getLevel(), (BlockPos)second.getBlockPos()));
    }

    public Int2IntFunction acceptSingle(S single) {
        return i -> i;
    }

    public Int2IntFunction acceptNone() {
        return i -> i;
    }
}

