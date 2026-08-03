/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.math.Transformation
 *  net.minecraft.world.level.block.SkullBlock$Type
 *  net.minecraft.world.level.block.SkullBlock$Types
 */
package net.minecraft.client.renderer.blockentity.state;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.SkullBlock;

public class SkullBlockRenderState
extends BlockEntityRenderState {
    public float animationProgress;
    public Transformation transformation = Transformation.IDENTITY;
    public SkullBlock.Type skullType = SkullBlock.Types.ZOMBIE;
    public RenderType renderType;
}

