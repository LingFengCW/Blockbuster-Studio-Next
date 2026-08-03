/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.ItemStack
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;

public class HappyGhastRenderState
extends LivingEntityRenderState {
    public ItemStack bodyItem = ItemStack.EMPTY;
    public boolean isRidden;
    public boolean isLeashHolder;
}

