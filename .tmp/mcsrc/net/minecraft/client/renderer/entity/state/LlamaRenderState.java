/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.animal.equine.Llama$Variant
 *  net.minecraft.world.item.ItemStack
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.ItemStack;

public class LlamaRenderState
extends LivingEntityRenderState {
    public Llama.Variant variant = Llama.Variant.DEFAULT;
    public boolean hasChest;
    public ItemStack bodyItem = ItemStack.EMPTY;
    public boolean isTraderLlama;
}

