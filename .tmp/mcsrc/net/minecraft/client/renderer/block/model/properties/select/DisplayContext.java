/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.state.BlockState
 */
package net.minecraft.client.renderer.block.model.properties.select;

import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.properties.select.SelectBlockModelProperty;
import net.minecraft.world.level.block.state.BlockState;

public record DisplayContext() implements SelectBlockModelProperty<BlockDisplayContext>
{
    @Override
    public BlockDisplayContext get(BlockState blockState, BlockDisplayContext displayContext) {
        return displayContext;
    }
}

