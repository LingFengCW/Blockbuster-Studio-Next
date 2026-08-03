/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.world.inventory.ContainerInput
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.BundleItem
 *  net.minecraft.world.item.ItemStack
 *  org.joml.Vector2i
 */
package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

public class BundleMouseActions
implements ItemSlotMouseAction {
    private final Minecraft minecraft;
    private final ScrollWheelHandler scrollWheelHandler;

    public BundleMouseActions(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.scrollWheelHandler = new ScrollWheelHandler();
    }

    @Override
    public boolean matches(Slot slot) {
        return slot.getItem().is(ItemTags.BUNDLES);
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY, int slotIndex, ItemStack itemStack) {
        int updatedSelectedItem;
        int selectedItem;
        int wheel;
        int amountOfShownItems = BundleItem.getNumberOfItemsToShow((ItemStack)itemStack);
        if (amountOfShownItems == 0) {
            return false;
        }
        Vector2i wheelXY = this.scrollWheelHandler.onMouseScroll(scrollX, scrollY);
        int n = wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
        if (wheel != 0 && (selectedItem = BundleItem.getSelectedItemIndex((ItemStack)itemStack)) != (updatedSelectedItem = ScrollWheelHandler.getNextScrollWheelSelection(wheel, selectedItem, amountOfShownItems))) {
            this.toggleSelectedBundleItem(itemStack, slotIndex, updatedSelectedItem);
        }
        return true;
    }

    @Override
    public void onStopHovering(Slot hoveredSlot) {
        this.unselectedBundleItem(hoveredSlot.getItem(), hoveredSlot.index);
    }

    @Override
    public void onSlotClicked(Slot slot, ContainerInput containerInput) {
        if (containerInput == ContainerInput.QUICK_MOVE || containerInput == ContainerInput.SWAP) {
            this.unselectedBundleItem(slot.getItem(), slot.index);
        }
    }

    private void toggleSelectedBundleItem(ItemStack bundleItem, int slotIndex, int selectedItem) {
        if (this.minecraft.getConnection() != null && selectedItem < BundleItem.getNumberOfItemsToShow((ItemStack)bundleItem)) {
            ClientPacketListener connection = this.minecraft.getConnection();
            BundleItem.toggleSelectedItem((ItemStack)bundleItem, (int)selectedItem);
            connection.send((Packet<?>)new ServerboundSelectBundleItemPacket(slotIndex, selectedItem));
        }
    }

    public void unselectedBundleItem(ItemStack bundleItem, int slotIndex) {
        this.toggleSelectedBundleItem(bundleItem, slotIndex, -1);
    }
}

