/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.block.entity.PotDecorations
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.jspecify.annotations.Nullable;

public class DecoratedPotRenderState
extends BlockEntityRenderState {
    public float yRot;
    public // Could not load outer class - annotation placement on inner may be incorrect
     @Nullable DecoratedPotBlockEntity.WobbleStyle wobbleStyle;
    public float wobbleProgress;
    public PotDecorations decorations = PotDecorations.EMPTY;
    public Direction direction = Direction.NORTH;
}

