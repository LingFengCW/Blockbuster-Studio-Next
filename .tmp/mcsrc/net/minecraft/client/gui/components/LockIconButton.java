/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.Identifier
 */
package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public class LockIconButton
extends Button {
    private boolean locked;

    public LockIconButton(int x, int y, Button.OnPress onPress) {
        super(x, y, 20, 20, (Component)Component.translatable((String)"narrator.button.difficulty_lock"), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return CommonComponents.joinForNarration((Component[])new Component[]{super.createNarrationMessage(), this.isLocked() ? Component.translatable((String)"narrator.button.difficulty_lock.locked") : Component.translatable((String)"narrator.button.difficulty_lock.unlocked")});
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Icon icon = !this.active ? (this.locked ? Icon.LOCKED_DISABLED : Icon.UNLOCKED_DISABLED) : (this.isHoveredOrFocused() ? (this.locked ? Icon.LOCKED_HOVER : Icon.UNLOCKED_HOVER) : (this.locked ? Icon.LOCKED : Icon.UNLOCKED));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.sprite, this.getX(), this.getY(), this.width, this.height);
    }

    private static enum Icon {
        LOCKED(Identifier.withDefaultNamespace((String)"widget/locked_button")),
        LOCKED_HOVER(Identifier.withDefaultNamespace((String)"widget/locked_button_highlighted")),
        LOCKED_DISABLED(Identifier.withDefaultNamespace((String)"widget/locked_button_disabled")),
        UNLOCKED(Identifier.withDefaultNamespace((String)"widget/unlocked_button")),
        UNLOCKED_HOVER(Identifier.withDefaultNamespace((String)"widget/unlocked_button_highlighted")),
        UNLOCKED_DISABLED(Identifier.withDefaultNamespace((String)"widget/unlocked_button_disabled"));

        private final Identifier sprite;

        private Icon(Identifier sprite) {
            this.sprite = sprite;
        }
    }
}

