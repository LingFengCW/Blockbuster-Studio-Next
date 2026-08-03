/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.util.Mth
 *  net.minecraft.util.worldupdate.UpgradeProgress
 *  net.minecraft.util.worldupdate.UpgradeProgress$FileFixStats
 *  net.minecraft.util.worldupdate.UpgradeProgress$Type
 */
package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class FileFixerProgressScreen
extends Screen {
    private static final int PROGRESS_BAR_WIDTH = 200;
    private static final int PROGRESS_BAR_HEIGHT = 2;
    private static final int LINE_SPACING = 3;
    private static final int SECTION_SPACING = 30;
    private static final Component SCANNING = Component.translatable((String)"upgradeWorld.info.scanning");
    private final UpgradeProgress upgradeProgress;
    private Button cancelButton;

    public FileFixerProgressScreen(UpgradeProgress upgradeProgress) {
        super((Component)Component.translatable((String)"upgradeWorld.title"));
        this.upgradeProgress = upgradeProgress;
    }

    @Override
    protected void init() {
        super.init();
        this.cancelButton = Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.upgradeProgress.setCanceled();
            button.active = false;
        }).bounds((this.width - 200) / 2, this.height / 2 + 100, 200, 20).build();
        this.addRenderableWidget(this.cancelButton);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int xCenter = this.width / 2;
        int yCenter = this.height / 2;
        int textTop = yCenter - 50;
        this.extractTitle(graphics, xCenter, textTop);
        int totalFiles = this.upgradeProgress.getTotalFileFixStats().totalOperations();
        if (totalFiles > 0) {
            this.extractProgress(graphics, xCenter, textTop);
        } else {
            this.extractScanning(graphics, xCenter, textTop);
        }
    }

    private void extractTitle(GuiGraphicsExtractor graphics, int xCenter, int yTop) {
        graphics.centeredText(this.font, this.title, xCenter, yTop, -1);
    }

    private void extractProgress(GuiGraphicsExtractor graphics, int xCenter, int textTop) {
        UpgradeProgress.FileFixStats typeFileStats = this.upgradeProgress.getTypeFileFixStats();
        UpgradeProgress.FileFixStats totalFileStats = this.upgradeProgress.getTotalFileFixStats();
        UpgradeProgress.FileFixStats runningFileFixerStats = this.upgradeProgress.getRunningFileFixerStats();
        int y = textTop + this.font.lineHeight + 3;
        this.extractProgressBar(graphics, xCenter, y, runningFileFixerStats.getProgress());
        this.extractFileStats(graphics, xCenter, y += 7, totalFileStats.finishedOperations(), totalFileStats.totalOperations());
        this.extractFileFixerCount(graphics, xCenter, y += this.font.lineHeight * 2 + 6, runningFileFixerStats.finishedOperations(), runningFileFixerStats.totalOperations());
        this.extractTypeText(graphics, xCenter, y += this.font.lineHeight + 30 - 5);
        this.extractProgressBar(graphics, xCenter, y += this.font.lineHeight + 3, typeFileStats.getProgress());
        this.extractTypeProgress(graphics, xCenter, y += 7, typeFileStats.getProgress());
    }

    private void extractProgressBar(GuiGraphicsExtractor graphics, int xCenter, int y, float progress) {
        int barLeft = xCenter - 100;
        int barRight = barLeft + 200;
        int barBottom = y + 2;
        graphics.fill(barLeft, y, barRight, barBottom, -16777216);
        graphics.fill(barLeft, y, barLeft + Math.round(progress * 200.0f), barBottom, -16711936);
    }

    private void extractTypeText(GuiGraphicsExtractor graphics, int xCenter, int y) {
        UpgradeProgress.Type upgradeProgressType = this.upgradeProgress.getType();
        if (upgradeProgressType != null) {
            graphics.centeredText(this.font, upgradeProgressType.label(), xCenter, y, -6250336);
        }
    }

    private void extractTypeProgress(GuiGraphicsExtractor graphics, int xCenter, int y, float progress) {
        MutableComponent percentageText = Component.translatable((String)"upgradeWorld.progress.percentage", (Object[])new Object[]{Mth.floor((float)(progress * 100.0f))});
        graphics.centeredText(this.font, (Component)percentageText, xCenter, y, -6250336);
    }

    private void extractFileStats(GuiGraphicsExtractor graphics, int xCenter, int yStart, int converted, int total) {
        int lineHeight = this.font.lineHeight + 3;
        graphics.centeredText(this.font, (Component)Component.translatable((String)"upgradeWorld.info.converted", (Object[])new Object[]{converted}), xCenter, yStart, -6250336);
        graphics.centeredText(this.font, (Component)Component.translatable((String)"upgradeWorld.info.total", (Object[])new Object[]{total}), xCenter, yStart + lineHeight, -6250336);
    }

    private void extractScanning(GuiGraphicsExtractor graphics, int xCenter, int textTop) {
        graphics.centeredText(this.font, SCANNING, xCenter, textTop + this.font.lineHeight + 3, -6250336);
    }

    private void extractFileFixerCount(GuiGraphicsExtractor graphics, int xCenter, int y, int current, int total) {
        MutableComponent percentageText = Component.translatable((String)"upgradeWorld.info.file_fix_stage", (Object[])new Object[]{current, total});
        graphics.centeredText(this.font, (Component)percentageText, xCenter, y, -6250336);
    }
}

