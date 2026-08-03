/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.inventory.ContainerInput
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package net.minecraft.client.gui;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface ItemSlotMouseAction {
    public boolean matches(Slot var1);

    public boolean onMouseScrolled(double var1, double var3, int var5, ItemStack var6);

    public void onStopHovering(Slot var1);

    public void onSlotClicked(Slot var1, ContainerInput var2);
}

