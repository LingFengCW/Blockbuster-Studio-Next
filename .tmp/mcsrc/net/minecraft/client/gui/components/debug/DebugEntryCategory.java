/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 */
package net.minecraft.client.gui.components.debug;

import net.minecraft.network.chat.Component;

public record DebugEntryCategory(Component label, float sortKey) {
    public static final DebugEntryCategory SCREEN_TEXT = new DebugEntryCategory((Component)Component.translatable((String)"debug.options.category.text"), 1.0f);
    public static final DebugEntryCategory RENDERER = new DebugEntryCategory((Component)Component.translatable((String)"debug.options.category.renderer"), 2.0f);
}

