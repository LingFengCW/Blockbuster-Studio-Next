/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Crackiness$Level
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Crackiness;

public class IronGolemRenderState
extends LivingEntityRenderState {
    public float attackTicksRemaining;
    public int offerFlowerTick;
    public final BlockModelRenderState flowerBlock = new BlockModelRenderState();
    public Crackiness.Level crackiness = Crackiness.Level.NONE;
}

