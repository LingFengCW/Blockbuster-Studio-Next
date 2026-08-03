/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.booleans.BooleanConsumer
 *  it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.RegistryAccess$Frozen
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.WorldStem
 *  net.minecraft.server.packs.repository.PackRepository
 *  net.minecraft.server.packs.repository.ServerPacksSource
 *  net.minecraft.util.Mth
 *  net.minecraft.util.Util
 *  net.minecraft.util.datafix.DataFixTypes
 *  net.minecraft.util.datafix.DataFixers
 *  net.minecraft.util.worldupdate.WorldUpgrader
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess
 *  net.minecraft.world.level.storage.WorldData
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class OptimizeWorldScreen
extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ToIntFunction<ResourceKey<Level>> DIMENSION_COLORS = (ToIntFunction)Util.make((Object)new Reference2IntOpenHashMap(), map -> {
        map.put((Object)Level.OVERWORLD, -13408734);
        map.put((Object)Level.NETHER, -10075085);
        map.put((Object)Level.END, -8943531);
        map.defaultReturnValue(-2236963);
    });
    private final BooleanConsumer callback;
    private final WorldUpgrader upgrader;

    public static @Nullable OptimizeWorldScreen create(Minecraft minecraft, BooleanConsumer callback, DataFixer dataFixer, LevelStorageSource.LevelStorageAccess levelSourceAccess, boolean eraseCache) {
        OptimizeWorldScreen optimizeWorldScreen;
        block9: {
            WorldOpenFlows worldOpenFlows = minecraft.createWorldOpenFlows();
            PackRepository packRepository = ServerPacksSource.createPackRepository((LevelStorageSource.LevelStorageAccess)levelSourceAccess);
            Dynamic unfixedDataTag = levelSourceAccess.getUnfixedDataTagWithFallback();
            int dataVersion = NbtUtils.getDataVersion((Dynamic)unfixedDataTag);
            if (DataFixers.getFileFixer().requiresFileFixing(dataVersion)) {
                throw new IllegalStateException("Can't optimize world before file fixing; shouldn't be able to get here");
            }
            Dynamic dataTag = DataFixTypes.LEVEL.updateToCurrentVersion(DataFixers.getDataFixer(), unfixedDataTag, dataVersion);
            WorldStem worldStem = worldOpenFlows.loadWorldStem(levelSourceAccess, dataTag, false, packRepository);
            try {
                WorldData worldData = worldStem.worldDataAndGenSettings().data();
                RegistryAccess.Frozen registryAccess = worldStem.registries().compositeAccess();
                levelSourceAccess.saveDataTag(worldData);
                optimizeWorldScreen = new OptimizeWorldScreen(callback, dataFixer, levelSourceAccess, worldData, eraseCache, (RegistryAccess)registryAccess);
                if (worldStem == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (worldStem != null) {
                        try {
                            worldStem.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to load datapacks, can't optimize world", (Throwable)e);
                    return null;
                }
            }
            worldStem.close();
        }
        return optimizeWorldScreen;
    }

    private OptimizeWorldScreen(BooleanConsumer callback, DataFixer dataFixer, LevelStorageSource.LevelStorageAccess levelSource, WorldData worldData, boolean eraseCache, RegistryAccess registryAccess) {
        super((Component)Component.translatable((String)"optimizeWorld.title", (Object[])new Object[]{worldData.getLevelSettings().levelName()}));
        this.callback = callback;
        this.upgrader = new WorldUpgrader(levelSource, dataFixer, registryAccess, eraseCache, false);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.upgrader.cancel();
            this.callback.accept(false);
        }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
    }

    @Override
    public void tick() {
        if (this.upgrader.isFinished()) {
            this.callback.accept(true);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @Override
    public void removed() {
        this.upgrader.cancel();
        this.upgrader.close();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, -1);
        int x0 = this.width / 2 - 150;
        int x1 = this.width / 2 + 150;
        int y0 = this.height / 4 + 100;
        int y1 = y0 + 10;
        graphics.centeredText(this.font, this.upgrader.getStatus(), this.width / 2, y0 - this.font.lineHeight - 2, -6250336);
        if (this.upgrader.getTotalChunks() > 0) {
            graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, -16777216);
            graphics.text(this.font, (Component)Component.translatable((String)"optimizeWorld.info.converted", (Object[])new Object[]{this.upgrader.getConverted()}), x0, 40, -6250336);
            graphics.text(this.font, (Component)Component.translatable((String)"optimizeWorld.info.skipped", (Object[])new Object[]{this.upgrader.getSkipped()}), x0, 40 + this.font.lineHeight + 3, -6250336);
            graphics.text(this.font, (Component)Component.translatable((String)"optimizeWorld.info.total", (Object[])new Object[]{this.upgrader.getTotalChunks()}), x0, 40 + (this.font.lineHeight + 3) * 2, -6250336);
            int progress = 0;
            for (ResourceKey dimension : this.upgrader.levels()) {
                int length = Mth.floor((float)(this.upgrader.dimensionProgress(dimension) * (float)(x1 - x0)));
                graphics.fill(x0 + progress, y0, x0 + progress + length, y1, DIMENSION_COLORS.applyAsInt((ResourceKey<Level>)dimension));
                progress += length;
            }
            int totalProgress = this.upgrader.getConverted() + this.upgrader.getSkipped();
            MutableComponent countStr = Component.translatable((String)"optimizeWorld.progress.counter", (Object[])new Object[]{totalProgress, this.upgrader.getTotalChunks()});
            MutableComponent progressStr = Component.translatable((String)"optimizeWorld.progress.percentage", (Object[])new Object[]{Mth.floor((float)(this.upgrader.getTotalProgress() * 100.0f))});
            graphics.centeredText(this.font, (Component)countStr, this.width / 2, y0 + 2 * this.font.lineHeight + 2, -6250336);
            graphics.centeredText(this.font, (Component)progressStr, this.width / 2, y0 + (y1 - y0) / 2 - this.font.lineHeight / 2, -6250336);
        }
    }
}

