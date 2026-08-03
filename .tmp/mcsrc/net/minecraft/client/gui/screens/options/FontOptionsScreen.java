/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 */
package net.minecraft.client.gui.screens.options;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class FontOptionsScreen
extends OptionsSubScreen {
    private static OptionInstance<?>[] options(Options options) {
        return new OptionInstance[]{options.forceUnicodeFont(), options.japaneseGlyphVariants()};
    }

    public FontOptionsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, (Component)Component.translatable((String)"options.font.title"));
    }

    @Override
    protected void addOptions() {
        this.list.addSmall(FontOptionsScreen.options(this.options));
    }
}

