/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 */
package net.minecraft.client.gui.components;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public abstract class TabButton
extends AbstractWidget.WithInactiveMessage {
    private final TabManager tabManager;
    private final Tab tab;

    public TabButton(TabManager tabManager, Tab tab, int width, int height) {
        super(0, 0, width, height, tab.getTabTitle());
        this.tabManager = tabManager;
        this.tab = tab;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, (Component)Component.translatable((String)"gui.narrate.tab", (Object[])new Object[]{this.tab.getTabTitle()}));
        output.add(NarratedElementType.HINT, this.tab().getTabExtraNarration());
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    public Tab tab() {
        return this.tab;
    }

    public boolean isSelected() {
        return this.tabManager.getCurrentTab() == this.tab;
    }
}

