/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.Lifecycle
 *  it.unimi.dsi.fastutil.booleans.BooleanConsumer
 *  net.minecraft.ChatFormatting
 *  net.minecraft.CrashReport
 *  net.minecraft.CrashReportCategory
 *  net.minecraft.ReportedException
 *  net.minecraft.SharedConstants
 *  net.minecraft.commands.Commands$CommandSelection
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.LayeredRegistryAccess
 *  net.minecraft.core.MappedRegistry
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.NbtException
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.ReportedNbtException
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.RegistryLayer
 *  net.minecraft.server.ReloadableServerResources
 *  net.minecraft.server.WorldLoader
 *  net.minecraft.server.WorldLoader$DataLoadOutput
 *  net.minecraft.server.WorldLoader$InitConfig
 *  net.minecraft.server.WorldLoader$PackConfig
 *  net.minecraft.server.WorldLoader$ResultFactory
 *  net.minecraft.server.WorldLoader$WorldDataSupplier
 *  net.minecraft.server.WorldStem
 *  net.minecraft.server.packs.repository.PackRepository
 *  net.minecraft.server.packs.repository.ServerPacksSource
 *  net.minecraft.server.packs.resources.CloseableResourceManager
 *  net.minecraft.server.permissions.LevelBasedPermissionSet
 *  net.minecraft.server.permissions.PermissionSet
 *  net.minecraft.util.MemoryReserve
 *  net.minecraft.util.Util
 *  net.minecraft.util.datafix.DataFixTypes
 *  net.minecraft.util.datafix.DataFixers
 *  net.minecraft.util.filefix.AbortedFileFixException
 *  net.minecraft.util.filefix.CanceledFileFixException
 *  net.minecraft.util.filefix.FailedCleanupFileFixException
 *  net.minecraft.util.filefix.FileFixException
 *  net.minecraft.util.filefix.virtualfilesystem.exception.CowFSSymlinkException
 *  net.minecraft.util.worldupdate.UpgradeProgress
 *  net.minecraft.world.level.LevelSettings
 *  net.minecraft.world.level.WorldDataConfiguration
 *  net.minecraft.world.level.dimension.LevelStem
 *  net.minecraft.world.level.gamerules.GameRuleMap
 *  net.minecraft.world.level.gamerules.GameRules
 *  net.minecraft.world.level.levelgen.WorldDimensions
 *  net.minecraft.world.level.levelgen.WorldDimensions$Complete
 *  net.minecraft.world.level.levelgen.WorldGenSettings
 *  net.minecraft.world.level.levelgen.WorldOptions
 *  net.minecraft.world.level.saveddata.SavedDataType
 *  net.minecraft.world.level.storage.LevelDataAndDimensions
 *  net.minecraft.world.level.storage.LevelDataAndDimensions$WorldDataAndGenSettings
 *  net.minecraft.world.level.storage.LevelResource
 *  net.minecraft.world.level.storage.LevelStorageSource
 *  net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess
 *  net.minecraft.world.level.storage.LevelSummary
 *  net.minecraft.world.level.storage.LevelSummary$BackupStatus
 *  net.minecraft.world.level.storage.PrimaryLevelData
 *  net.minecraft.world.level.storage.WorldData
 *  net.minecraft.world.level.validation.ContentValidationException
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.FileFixerAbortedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.RecoverWorldDataScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.FileFixerProgressScreen;
import net.minecraft.client.gui.screens.worldselection.InitialWorldCreationOptions;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.filefix.AbortedFileFixException;
import net.minecraft.util.filefix.CanceledFileFixException;
import net.minecraft.util.filefix.FailedCleanupFileFixException;
import net.minecraft.util.filefix.FileFixException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSSymlinkException;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.validation.ContentValidationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class WorldOpenFlows {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID WORLD_PACK_ID = UUID.fromString("640a6a92-b6cb-48a0-b391-831586500359");
    private final Minecraft minecraft;
    private final LevelStorageSource levelSource;

    public WorldOpenFlows(Minecraft minecraft, LevelStorageSource levelSource) {
        this.minecraft = minecraft;
        this.levelSource = levelSource;
    }

    public void createFreshLevel(String levelId, LevelSettings levelSettings, WorldOptions options, Function<HolderLookup.Provider, WorldDimensions> dimensionsProvider, Screen parentScreen) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen((Component)Component.translatable((String)"selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess levelSourceAccess = this.createWorldAccess(levelId);
        if (levelSourceAccess == null) {
            return;
        }
        PackRepository packRepository = ServerPacksSource.createPackRepository((LevelStorageSource.LevelStorageAccess)levelSourceAccess);
        WorldDataConfiguration dataConfiguration = levelSettings.dataConfiguration();
        try {
            WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, dataConfiguration, false, false);
            WorldStem worldStem = (WorldStem)this.loadWorldDataBlocking(packConfig, context -> {
                WorldDimensions dimensions = (WorldDimensions)dimensionsProvider.apply(context.datapackWorldgen());
                WorldDimensions.Complete completeDimensions = dimensions.bake(context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
                return new WorldLoader.DataLoadOutput((Object)new LevelDataAndDimensions.WorldDataAndGenSettings((WorldData)new PrimaryLevelData(levelSettings, completeDimensions.specialWorldProperty(), completeDimensions.lifecycle()), new WorldGenSettings(options, dimensions)), completeDimensions.dimensionsRegistryAccess());
            }, WorldStem::new);
            this.minecraft.doWorldLoad(levelSourceAccess, packRepository, worldStem, Optional.empty(), true);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to load datapacks, can't proceed with server load", (Throwable)e);
            levelSourceAccess.safeClose();
            this.minecraft.gui.setScreen(parentScreen);
        }
    }

    private // Could not load outer class - annotation placement on inner may be incorrect
    @Nullable LevelStorageSource.LevelStorageAccess createWorldAccess(String levelId) {
        try {
            return this.levelSource.validateAndCreateAccess(levelId);
        }
        catch (IOException e) {
            LOGGER.warn("Failed to read level {} data", (Object)levelId, (Object)e);
            SystemToast.onWorldAccessFailure(this.minecraft, levelId);
            this.minecraft.gui.setScreen(null);
            return null;
        }
        catch (ContentValidationException e) {
            LOGGER.warn("{}", (Object)e.getMessage());
            this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(null)));
            return null;
        }
    }

    public void createLevelFromExistingSettings(LevelStorageSource.LevelStorageAccess levelSourceAccess, ReloadableServerResources serverResources, LayeredRegistryAccess<RegistryLayer> registryAccess, LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings, Optional<GameRules> gameRules) {
        PackRepository packRepository = ServerPacksSource.createPackRepository((LevelStorageSource.LevelStorageAccess)levelSourceAccess);
        CloseableResourceManager resourceManager = (CloseableResourceManager)new WorldLoader.PackConfig(packRepository, worldDataAndGenSettings.data().getDataConfiguration(), false, false).createResourceManager().getSecond();
        this.minecraft.doWorldLoad(levelSourceAccess, packRepository, new WorldStem(resourceManager, serverResources, registryAccess, worldDataAndGenSettings), gameRules, true);
    }

    public WorldStem loadWorldStem(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, boolean safeMode, PackRepository packRepository) throws Exception {
        WorldLoader.PackConfig packConfig = LevelStorageSource.getPackConfig(levelDataTag, (PackRepository)packRepository, (boolean)safeMode);
        return (WorldStem)this.loadWorldDataBlocking(packConfig, context -> {
            Registry datapackDimensions = context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM);
            LevelDataAndDimensions data = LevelStorageSource.getLevelDataAndDimensions((LevelStorageSource.LevelStorageAccess)worldAccess, (Dynamic)levelDataTag, (WorldDataConfiguration)context.dataConfiguration(), (Registry)datapackDimensions, (HolderLookup.Provider)context.datapackWorldgen());
            return new WorldLoader.DataLoadOutput((Object)data.worldDataAndGenSettings(), data.dimensions().dimensionsRegistryAccess());
        }, WorldStem::new);
    }

    public Pair<LevelSettings, WorldCreationContext> recreateWorldData(LevelStorageSource.LevelStorageAccess levelSourceAccess) throws Exception {
        record Data(LevelSettings levelSettings, WorldOptions options, Registry<LevelStem> existingDimensions) {
        }
        PackRepository packRepository = ServerPacksSource.createPackRepository((LevelStorageSource.LevelStorageAccess)levelSourceAccess);
        Dynamic unfixedDataTag = levelSourceAccess.getUnfixedDataTag(false);
        int dataVersion = NbtUtils.getDataVersion((Dynamic)unfixedDataTag);
        if (DataFixers.getFileFixer().requiresFileFixing(dataVersion)) {
            throw new IllegalStateException("Can't recreate world before file fixing; shouldn't be able to get here");
        }
        Dynamic levelDataTag = DataFixTypes.LEVEL.updateToCurrentVersion(DataFixers.getDataFixer(), unfixedDataTag, dataVersion);
        WorldLoader.PackConfig packConfig = LevelStorageSource.getPackConfig((Dynamic)levelDataTag, (PackRepository)packRepository, (boolean)false);
        return (Pair)this.loadWorldDataBlocking(packConfig, context -> {
            Registry noDatapackDimensions = new MappedRegistry(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
            LevelDataAndDimensions existingData = LevelStorageSource.getLevelDataAndDimensions((LevelStorageSource.LevelStorageAccess)levelSourceAccess, (Dynamic)levelDataTag, (WorldDataConfiguration)context.dataConfiguration(), (Registry)noDatapackDimensions, (HolderLookup.Provider)context.datapackWorldgen());
            return new WorldLoader.DataLoadOutput((Object)new Data(existingData.worldDataAndGenSettings().data().getLevelSettings(), existingData.worldDataAndGenSettings().genSettings().options(), (Registry<LevelStem>)existingData.dimensions().dimensions()), context.datapackDimensions());
        }, (resources, managers, registries, loadedData) -> {
            resources.close();
            DataResult existingGameRules = LevelStorageSource.readExistingSavedData((LevelStorageSource.LevelStorageAccess)levelSourceAccess, (HolderLookup.Provider)registries.compositeAccess(), (SavedDataType)GameRuleMap.TYPE);
            existingGameRules.ifError(e -> LOGGER.error("Failed to parse existing game rules: {}", (Object)e.message()));
            InitialWorldCreationOptions initialWorldCreationOptions = new InitialWorldCreationOptions(WorldCreationUiState.SelectedGameMode.SURVIVAL, existingGameRules.result().orElse(GameRuleMap.of()), null);
            return Pair.of((Object)loadedData.levelSettings, (Object)new WorldCreationContext(loadedData.options, new WorldDimensions(loadedData.existingDimensions), (LayeredRegistryAccess<RegistryLayer>)registries, managers, loadedData.levelSettings.dataConfiguration(), initialWorldCreationOptions));
        });
    }

    private <D, R> R loadWorldDataBlocking(WorldLoader.PackConfig packConfig, WorldLoader.WorldDataSupplier<D> worldDataGetter, WorldLoader.ResultFactory<D, R> worldDataSupplier) throws Exception {
        long start = Util.getMillis();
        WorldLoader.InitConfig config = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, (PermissionSet)LevelBasedPermissionSet.GAMEMASTER);
        CompletableFuture resourceLoad = WorldLoader.load((WorldLoader.InitConfig)config, worldDataGetter, worldDataSupplier, (Executor)Util.backgroundExecutor(), (Executor)((Object)this.minecraft));
        this.minecraft.managedBlock(resourceLoad::isDone);
        long end = Util.getMillis();
        LOGGER.debug("World resource load blocked for {} ms", (Object)(end - start));
        return (R)resourceLoad.get();
    }

    private void askForBackup(LevelStorageSource.LevelStorageAccess levelAccess, boolean oldCustomized, Runnable proceedCallback, Runnable cancelCallback) {
        MutableComponent backupWarning;
        MutableComponent backupQuestion;
        if (oldCustomized) {
            backupQuestion = Component.translatable((String)"selectWorld.backupQuestion.customized");
            backupWarning = Component.translatable((String)"selectWorld.backupWarning.customized");
        } else {
            backupQuestion = Component.translatable((String)"selectWorld.backupQuestion.experimental");
            backupWarning = Component.translatable((String)"selectWorld.backupWarning.experimental");
        }
        this.minecraft.gui.setScreen(new BackupConfirmScreen(cancelCallback, (backup, eraseCache) -> EditWorldScreen.conditionallyMakeBackupAndShowToast(backup, levelAccess).thenAcceptAsync(bl -> proceedCallback.run(), (Executor)((Object)this.minecraft)), (Component)backupQuestion, (Component)backupWarning, false));
    }

    public static void confirmWorldCreation(Minecraft minecraft, CreateWorldScreen parent, Lifecycle lifecycle, Runnable task, boolean skipWarning) {
        BooleanConsumer callback = confirmed -> {
            if (confirmed) {
                task.run();
            } else {
                minecraft.gui.setScreen(parent);
            }
        };
        if (skipWarning || lifecycle == Lifecycle.stable()) {
            task.run();
        } else if (lifecycle == Lifecycle.experimental()) {
            minecraft.gui.setScreen(new ConfirmScreen(callback, (Component)Component.translatable((String)"selectWorld.warning.experimental.title"), (Component)Component.translatable((String)"selectWorld.warning.experimental.question")));
        } else {
            minecraft.gui.setScreen(new ConfirmScreen(callback, (Component)Component.translatable((String)"selectWorld.warning.deprecated.title"), (Component)Component.translatable((String)"selectWorld.warning.deprecated.question")));
        }
    }

    public void openWorld(String levelId, Runnable onCancel) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen((Component)Component.translatable((String)"selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess worldAccess = this.createWorldAccess(levelId);
        if (worldAccess == null) {
            return;
        }
        this.openWorldLoadLevelData(worldAccess, onCancel);
    }

    private void openWorldLoadLevelData(LevelStorageSource.LevelStorageAccess worldAccess, Runnable onCancel) {
        LevelSummary summary;
        Dynamic levelDataTag;
        this.minecraft.setScreenAndShow(new GenericMessageScreen((Component)Component.translatable((String)"selectWorld.data_read")));
        try {
            levelDataTag = worldAccess.getUnfixedDataTag(false);
            summary = worldAccess.fixAndGetSummaryFromTag(levelDataTag);
        }
        catch (IOException | NbtException | ReportedNbtException e) {
            this.minecraft.gui.setScreen(new RecoverWorldDataScreen(this.minecraft, success -> {
                if (success) {
                    this.openWorldLoadLevelData(worldAccess, onCancel);
                } else {
                    worldAccess.safeClose();
                    onCancel.run();
                }
            }, worldAccess));
            return;
        }
        catch (OutOfMemoryError e) {
            MemoryReserve.release();
            String detailedMessage = "Ran out of memory trying to read level data of world folder \"" + worldAccess.getLevelId() + "\"";
            LOGGER.error(LogUtils.FATAL_MARKER, detailedMessage);
            OutOfMemoryError detailedException = new OutOfMemoryError("Ran out of memory reading level data");
            detailedException.initCause(e);
            CrashReport crashReport = CrashReport.forThrowable((Throwable)detailedException, (String)detailedMessage);
            CrashReportCategory worldDetails = crashReport.addCategory("World details");
            worldDetails.setDetail("World folder", (Object)worldAccess.getLevelId());
            throw new ReportedException(crashReport);
        }
        this.openWorldCheckVersionCompatibility(worldAccess, summary, levelDataTag, onCancel);
    }

    private void openWorldCheckVersionCompatibility(LevelStorageSource.LevelStorageAccess worldAccess, LevelSummary summary, Dynamic<?> levelDataTag, Runnable onCancel) {
        if (!summary.isCompatible()) {
            worldAccess.safeClose();
            this.minecraft.gui.setScreen(new AlertScreen(onCancel, (Component)Component.translatable((String)"selectWorld.incompatible.title").withColor(-65536), (Component)Component.translatable((String)"selectWorld.incompatible.description", (Object[])new Object[]{summary.getWorldVersionName()})));
            return;
        }
        LevelSummary.BackupStatus backupStatus = summary.backupStatus();
        if (backupStatus.shouldBackup()) {
            String questionKey = "selectWorld.backupQuestion." + backupStatus.getTranslationKey();
            String warningKey = "selectWorld.backupWarning." + backupStatus.getTranslationKey();
            MutableComponent backupQuestion = Component.translatable((String)questionKey);
            if (backupStatus.isSevere()) {
                backupQuestion.withColor(-2142128);
            }
            MutableComponent backupWarning = Component.translatable((String)warningKey, (Object[])new Object[]{summary.getWorldVersionName(), SharedConstants.getCurrentVersion().name()});
            this.minecraft.gui.setScreen(new BackupConfirmScreen(() -> {
                worldAccess.safeClose();
                onCancel.run();
            }, (backup, eraseCache) -> this.createBackupAndOpenWorld(worldAccess, levelDataTag, onCancel, backup), (Component)backupQuestion, (Component)backupWarning, false));
        } else {
            this.upgradeAndOpenWorld(worldAccess, levelDataTag, onCancel);
        }
    }

    private void createBackupAndOpenWorld(LevelStorageSource.LevelStorageAccess levelAccess, Dynamic<?> levelDataTag, Runnable onCancel, boolean backup) {
        EditWorldScreen.conditionallyMakeBackupAndShowToast(backup, levelAccess).thenAcceptAsync(bl -> this.upgradeAndOpenWorld(levelAccess, levelDataTag, onCancel), (Executor)((Object)this.minecraft));
    }

    private void upgradeAndOpenWorld(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, Runnable onCancel) {
        Runnable cleanup = () -> {
            worldAccess.safeClose();
            onCancel.run();
        };
        int dataVersion = NbtUtils.getDataVersion(levelDataTag);
        boolean requiresFileFixing = DataFixers.getFileFixer().requiresFileFixing(dataVersion);
        UpgradeProgress upgradeProgress = new UpgradeProgress();
        if (requiresFileFixing) {
            FileFixerProgressScreen progressScreen = new FileFixerProgressScreen(upgradeProgress);
            this.minecraft.setScreenAndShow(progressScreen);
        }
        Util.backgroundExecutor().execute(() -> {
            Dynamic<?> levelDataTagFixed = this.tryFileFixAndReportErrors(worldAccess, levelDataTag, upgradeProgress, cleanup);
            if (levelDataTagFixed == null) {
                return;
            }
            this.minecraft.execute(() -> {
                if (requiresFileFixing) {
                    ConfirmScreen loadConfirmScreen = new ConfirmScreen(result -> {
                        if (result) {
                            this.openWorldLoadLevelStem(worldAccess, levelDataTagFixed, false, onCancel);
                        } else {
                            cleanup.run();
                        }
                    }, (Component)Component.translatable((String)"upgradeWorld.done"), (Component)Component.translatable((String)"upgradeWorld.joinNow"));
                    this.minecraft.setScreenAndShow(loadConfirmScreen);
                } else {
                    this.openWorldLoadLevelStem(worldAccess, levelDataTagFixed, false, onCancel);
                }
            });
        });
    }

    private @Nullable Dynamic<?> tryFileFixAndReportErrors(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, UpgradeProgress upgradeProgress, Runnable cleanup) {
        Dynamic levelDataTagFixed;
        try {
            levelDataTagFixed = DataFixers.getFileFixer().fix(worldAccess, levelDataTag, upgradeProgress);
        }
        catch (CanceledFileFixException e) {
            this.minecraft.execute(() -> this.minecraft.setScreenAndShow(new AlertScreen(cleanup, (Component)Component.translatable((String)"upgradeWorld.canceled.title"), (Component)Component.translatable((String)"upgradeWorld.canceled.message"), CommonComponents.GUI_OK, true)));
            return null;
        }
        catch (AbortedFileFixException e) {
            this.minecraft.execute(() -> {
                if (e.getCause() instanceof CowFSSymlinkException) {
                    this.minecraft.setScreenAndShow(new AlertScreen(cleanup, (Component)Component.translatable((String)"upgradeWorld.symlink.title"), (Component)Component.translatable((String)"upgradeWorld.symlink.message")));
                } else {
                    this.minecraft.setScreenAndShow(new FileFixerAbortedScreen(cleanup, (Component)Component.translatable((String)"upgradeWorld.aborted.message")));
                }
            });
            return null;
        }
        catch (FailedCleanupFileFixException e) {
            this.minecraft.execute(() -> this.minecraft.setScreenAndShow(new AlertScreen(cleanup, (Component)Component.translatable((String)"upgradeWorld.failed_cleanup.title"), (Component)Component.translatable((String)"upgradeWorld.failed_cleanup.message", (Object[])new Object[]{Component.literal((String)e.newWorldFolderName()).withColor(-8355712)}))));
            return null;
        }
        catch (FileFixException e) {
            this.minecraft.delayCrash(e.makeReportedException().getReport());
            return null;
        }
        catch (Exception e) {
            LOGGER.error("Failed to upgrade the file structure of the world.", (Throwable)e);
            CrashReport report = CrashReport.forThrowable((Throwable)e, (String)"Failed to update file structure");
            this.minecraft.delayCrash(report);
            return null;
        }
        return levelDataTagFixed;
    }

    private void openWorldLoadLevelStem(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, boolean safeMode, Runnable onCancel) {
        WorldStem worldStem;
        this.minecraft.setScreenAndShow(new GenericMessageScreen((Component)Component.translatable((String)"selectWorld.resource_load")));
        PackRepository packRepository = ServerPacksSource.createPackRepository((LevelStorageSource.LevelStorageAccess)worldAccess);
        try {
            worldStem = this.loadWorldStem(worldAccess, levelDataTag, safeMode, packRepository);
            for (LevelStem levelStem : worldStem.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM)) {
                levelStem.generator().validate();
            }
        }
        catch (Exception e) {
            LOGGER.warn("Failed to load level data or datapacks, can't proceed with server load", (Throwable)e);
            if (!safeMode) {
                this.minecraft.gui.setScreen(new DatapackLoadFailureScreen(() -> {
                    worldAccess.safeClose();
                    onCancel.run();
                }, () -> this.openWorldLoadLevelStem(worldAccess, levelDataTag, true, onCancel)));
            } else {
                worldAccess.safeClose();
                this.minecraft.gui.setScreen(new AlertScreen(onCancel, (Component)Component.translatable((String)"datapackFailure.safeMode.failed.title"), (Component)Component.translatable((String)"datapackFailure.safeMode.failed.description"), CommonComponents.GUI_BACK, true));
            }
            return;
        }
        this.openWorldCheckWorldStemCompatibility(worldAccess, worldStem, packRepository, onCancel);
    }

    private void openWorldCheckWorldStemCompatibility(LevelStorageSource.LevelStorageAccess worldAccess, WorldStem worldStem, PackRepository packRepository, Runnable onCancel) {
        boolean unstable;
        LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings = worldStem.worldDataAndGenSettings();
        WorldData data = worldDataAndGenSettings.data();
        boolean oldCustomized = worldDataAndGenSettings.genSettings().options().isOldCustomizedWorld();
        boolean bl = unstable = data.worldGenSettingsLifecycle() != Lifecycle.stable();
        if (oldCustomized || unstable) {
            this.askForBackup(worldAccess, oldCustomized, () -> this.openWorldLoadBundledResourcePack(worldAccess, worldStem, packRepository, onCancel), () -> {
                worldStem.close();
                worldAccess.safeClose();
                onCancel.run();
            });
            return;
        }
        this.openWorldLoadBundledResourcePack(worldAccess, worldStem, packRepository, onCancel);
    }

    private void openWorldLoadBundledResourcePack(LevelStorageSource.LevelStorageAccess worldAccess, WorldStem worldStem, PackRepository packRepository, Runnable onCancel) {
        DownloadedPackSource packSource = this.minecraft.getDownloadedPackSource();
        ((CompletableFuture)((CompletableFuture)((CompletableFuture)this.loadBundledResourcePack(packSource, worldAccess).thenApply(unused -> true)).exceptionallyComposeAsync(t -> {
            LOGGER.warn("Failed to load pack: ", t);
            return this.promptBundledPackLoadFailure();
        }, (Executor)((Object)this.minecraft))).thenAcceptAsync(result -> {
            if (result.booleanValue()) {
                this.openWorldCheckDiskSpace(worldAccess, worldStem, packSource, packRepository, onCancel);
            } else {
                packSource.popAll();
                worldStem.close();
                worldAccess.safeClose();
                onCancel.run();
            }
        }, (Executor)((Object)this.minecraft))).exceptionally(e -> {
            this.minecraft.delayCrash(CrashReport.forThrowable((Throwable)e, (String)"Load world"));
            return null;
        });
    }

    private void openWorldCheckDiskSpace(LevelStorageSource.LevelStorageAccess worldAccess, WorldStem worldStem, DownloadedPackSource packSource, PackRepository packRepository, Runnable onCancel) {
        if (worldAccess.checkForLowDiskSpace()) {
            ConfirmScreen screen = new ConfirmScreen(skip -> {
                if (skip) {
                    this.openWorldDoLoad(worldAccess, worldStem, packRepository);
                } else {
                    packSource.popAll();
                    worldStem.close();
                    worldAccess.safeClose();
                    onCancel.run();
                }
            }, (Component)Component.translatable((String)"selectWorld.warning.lowDiskSpace.title").withStyle(ChatFormatting.RED), (Component)Component.translatable((String)"selectWorld.warning.lowDiskSpace.description"), CommonComponents.GUI_CONTINUE, CommonComponents.GUI_BACK);
            this.minecraft.gui.setScreen(screen);
        } else {
            this.openWorldDoLoad(worldAccess, worldStem, packRepository);
        }
    }

    private void openWorldDoLoad(LevelStorageSource.LevelStorageAccess worldAccess, WorldStem worldStem, PackRepository packRepository) {
        this.minecraft.doWorldLoad(worldAccess, packRepository, worldStem, Optional.empty(), false);
    }

    private CompletableFuture<Void> loadBundledResourcePack(DownloadedPackSource packSource, LevelStorageSource.LevelStorageAccess levelSourceAccess) {
        Path mapResourceFile = levelSourceAccess.getLevelPath(LevelResource.MAP_RESOURCE_FILE);
        if (Files.exists(mapResourceFile, new LinkOption[0]) && !Files.isDirectory(mapResourceFile, new LinkOption[0])) {
            packSource.configureForLocalWorld();
            CompletableFuture<Void> result = packSource.waitForPackFeedback(WORLD_PACK_ID);
            packSource.pushLocalPack(WORLD_PACK_ID, mapResourceFile);
            return result;
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Boolean> promptBundledPackLoadFailure() {
        CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
        ConfirmScreen screen = new ConfirmScreen(result::complete, (Component)Component.translatable((String)"multiplayer.texturePrompt.failure.line1"), (Component)Component.translatable((String)"multiplayer.texturePrompt.failure.line2"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL);
        this.minecraft.gui.setScreen(screen);
        return result;
    }
}

