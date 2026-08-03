/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  org.apache.commons.lang3.ArrayUtils
 */
package net.minecraft.client.gui.screens.options.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.ArrayUtils;

public class KeyBindsList
extends ContainerObjectSelectionList<Entry> {
    private static final int ITEM_HEIGHT = 20;
    private final KeyBindsScreen keyBindsScreen;
    private int maxNameWidth;

    public KeyBindsList(KeyBindsScreen keyBindsScreen, Minecraft minecraft) {
        super(minecraft, keyBindsScreen.width, keyBindsScreen.layout.getContentHeight(), keyBindsScreen.layout.getHeaderHeight(), 20);
        this.keyBindsScreen = keyBindsScreen;
        Object[] keyMappings = (KeyMapping[])ArrayUtils.clone((Object[])minecraft.options.keyMappings);
        Arrays.sort(keyMappings);
        KeyMapping.Category previousCategory = null;
        for (Object key : keyMappings) {
            MutableComponent name;
            int width;
            KeyMapping.Category category = ((KeyMapping)key).getCategory();
            if (category != previousCategory) {
                previousCategory = category;
                this.addEntry(new CategoryEntry(this, category));
            }
            if ((width = minecraft.font.width((FormattedText)(name = Component.translatable((String)((KeyMapping)key).getName())))) > this.maxNameWidth) {
                this.maxNameWidth = width;
            }
            this.addEntry(new KeyEntry(this, (KeyMapping)key, (Component)name));
        }
    }

    public void resetMappingAndUpdateButtons() {
        KeyMapping.resetMapping();
        this.refreshEntries();
    }

    public void refreshEntries() {
        this.children().forEach(Entry::refreshEntry);
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    public class CategoryEntry
    extends Entry {
        private final FocusableTextWidget categoryName;
        final /* synthetic */ KeyBindsList this$0;

        public CategoryEntry(KeyBindsList this$0, KeyMapping.Category category) {
            KeyBindsList keyBindsList = this$0;
            Objects.requireNonNull(keyBindsList);
            this.this$0 = keyBindsList;
            this.categoryName = FocusableTextWidget.builder(category.label(), ((KeyBindsList)this$0).minecraft.font).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.categoryName.setPosition(this.this$0.width / 2 - this.categoryName.getWidth() / 2, this.getContentBottom() - this.categoryName.getHeight());
            this.categoryName.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.categoryName);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.categoryName);
        }

        @Override
        public void refreshEntry() {
        }
    }

    public class KeyEntry
    extends Entry {
        private static final Component RESET_BUTTON_TITLE = Component.translatable((String)"controls.reset");
        private static final int PADDING = 10;
        private final KeyMapping key;
        private final Component name;
        private final Button changeButton;
        private final Button resetButton;
        private boolean hasCollision;
        final /* synthetic */ KeyBindsList this$0;

        private KeyEntry(KeyBindsList this$0, KeyMapping key, Component name) {
            KeyBindsList keyBindsList = this$0;
            Objects.requireNonNull(keyBindsList);
            this.this$0 = keyBindsList;
            this.hasCollision = false;
            this.key = key;
            this.name = name;
            this.changeButton = Button.builder(name, button -> {
                this$0.keyBindsScreen.selectedKey = key;
                this$0.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 75, 20).createNarration(defaultNarrationSupplier -> {
                if (key.isUnbound()) {
                    return Component.translatable((String)"narrator.controls.unbound", (Object[])new Object[]{name});
                }
                return Component.translatable((String)"narrator.controls.bound", (Object[])new Object[]{name, defaultNarrationSupplier.get()});
            }).build();
            this.resetButton = Button.builder(RESET_BUTTON_TITLE, button -> {
                key.setKey(key.getDefaultKey());
                this$0.resetMappingAndUpdateButtons();
            }).bounds(0, 0, 50, 20).createNarration(defaultNarrationSupplier -> Component.translatable((String)"narrator.controls.reset", (Object[])new Object[]{name})).build();
            this.refreshEntry();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int resetButtonX = this.this$0.scrollBarX() - this.resetButton.getWidth() - 10;
            int buttonY = this.getContentY() - 2;
            this.resetButton.setPosition(resetButtonX, buttonY);
            this.resetButton.extractRenderState(graphics, mouseX, mouseY, a);
            int changeButtonX = resetButtonX - 5 - this.changeButton.getWidth();
            this.changeButton.setPosition(changeButtonX, buttonY);
            this.changeButton.extractRenderState(graphics, mouseX, mouseY, a);
            graphics.text(((KeyBindsList)this.this$0).minecraft.font, this.name, this.getContentX(), this.getContentYMiddle() - ((KeyBindsList)this.this$0).minecraft.font.lineHeight / 2, -1);
            if (this.hasCollision) {
                int stripeWidth = 3;
                int stripeLeft = this.changeButton.getX() - 6;
                graphics.fill(stripeLeft, this.getContentY() - 1, stripeLeft + 3, this.getContentBottom(), -256);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of((Object)this.changeButton, (Object)this.resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of((Object)this.changeButton, (Object)this.resetButton);
        }

        @Override
        public void refreshEntry() {
            this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
            this.resetButton.active = !this.key.isDefault();
            this.hasCollision = false;
            MutableComponent tooltip = Component.empty();
            if (!this.key.isUnbound()) {
                for (KeyMapping otherKey : ((KeyBindsList)this.this$0).minecraft.options.keyMappings) {
                    if (otherKey == this.key || !this.key.same(otherKey) || otherKey.isDefault() && this.key.isDefault()) continue;
                    if (this.hasCollision) {
                        tooltip.append(", ");
                    }
                    this.hasCollision = true;
                    tooltip.append((Component)Component.translatable((String)otherKey.getName()));
                }
            }
            if (this.hasCollision) {
                this.changeButton.setMessage((Component)Component.literal((String)"[ ").append((Component)this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.YELLOW));
                this.changeButton.setTooltip(Tooltip.create((Component)Component.translatable((String)"controls.keybinds.duplicateKeybinds", (Object[])new Object[]{tooltip})));
            } else {
                this.changeButton.setTooltip(null);
            }
            if (this.this$0.keyBindsScreen.selectedKey == this.key) {
                this.changeButton.setMessage((Component)Component.literal((String)"> ").append((Component)this.changeButton.getMessage().copy().withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.UNDERLINE})).append(" <").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    public static abstract class Entry
    extends ContainerObjectSelectionList.Entry<Entry> {
        public abstract void refreshEntry();
    }
}

