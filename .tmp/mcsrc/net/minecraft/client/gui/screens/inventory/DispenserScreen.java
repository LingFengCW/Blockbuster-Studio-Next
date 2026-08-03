/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.DispenserMenu
 */
package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DispenserMenu;

public class DispenserScreen
extends AbstractContainerScreen<DispenserMenu> {
    private static final Identifier CONTAINER_LOCATION = Identifier.withDefaultNamespace((String)"textures/gui/container/dispenser.png");

    public DispenserScreen(DispenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width((FormattedText)this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_LOCATION, xo, yo, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }
}

