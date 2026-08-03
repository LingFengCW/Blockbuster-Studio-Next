/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.ChatFormatting
 *  net.minecraft.locale.Language
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.Style
 *  net.minecraft.util.FormattedCharSequence
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class ComponentRenderUtils {
    private static final FormattedCharSequence INDENT = FormattedCharSequence.codepoint((int)32, (Style)Style.EMPTY);

    private static String stripColor(String input) {
        return Minecraft.getInstance().options.chatColors().get() != false ? input : ChatFormatting.stripFormatting((String)input);
    }

    public static List<FormattedCharSequence> wrapComponents(FormattedText message, int maxWidth, Font font) {
        ComponentCollector collector = new ComponentCollector();
        message.visit((style, contents) -> {
            collector.append(FormattedText.of((String)ComponentRenderUtils.stripColor(contents), (Style)style));
            return Optional.empty();
        }, Style.EMPTY);
        ArrayList result = Lists.newArrayList();
        font.getSplitter().splitLines(collector.getResultOrEmpty(), maxWidth, Style.EMPTY, (text, wrapped) -> {
            FormattedCharSequence reorderedText = Language.getInstance().getVisualOrder(text);
            result.add(wrapped != false ? FormattedCharSequence.composite((FormattedCharSequence)INDENT, (FormattedCharSequence)reorderedText) : reorderedText);
        });
        if (result.isEmpty()) {
            return Lists.newArrayList((Object[])new FormattedCharSequence[]{FormattedCharSequence.EMPTY});
        }
        return result;
    }

    public static FormattedCharSequence clipText(Component text, Font font, int width) {
        FormattedText clippedText = font.substrByWidth((FormattedText)text, width - font.width((FormattedText)CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite((FormattedText[])new FormattedText[]{clippedText, CommonComponents.ELLIPSIS}));
    }
}

