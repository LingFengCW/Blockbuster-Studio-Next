/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.ProgressListener
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;
import org.jspecify.annotations.Nullable;

public class ProgressScreen
extends Screen
implements ProgressListener {
    private @Nullable Component header;
    private @Nullable Component stage;
    private int progress;
    private boolean stop;
    private final boolean clearScreenAfterStop;

    public ProgressScreen(boolean clearScreenAfterStop) {
        super(GameNarrator.NO_TITLE);
        this.clearScreenAfterStop = clearScreenAfterStop;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    public void progressStartNoAbort(Component string) {
        this.progressStart(string);
    }

    public void progressStart(Component string) {
        this.header = string;
        this.progressStage((Component)Component.translatable((String)"menu.working"));
    }

    public void progressStage(Component string) {
        this.stage = string;
        this.progressStagePercentage(0);
    }

    public void progressStagePercentage(int i) {
        this.progress = i;
    }

    public void stop() {
        this.stop = true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.stop) {
            if (this.clearScreenAfterStop) {
                this.minecraft.gui.setScreen(null);
            }
            return;
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (this.header != null) {
            graphics.centeredText(this.font, this.header, this.width / 2, 70, -1);
        }
        if (this.stage != null && this.progress != 0) {
            graphics.centeredText(this.font, (Component)Component.empty().append(this.stage).append(" " + this.progress + "%"), this.width / 2, 90, -1);
        }
    }
}

