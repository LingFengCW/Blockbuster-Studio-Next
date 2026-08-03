/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.SharedConstants
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 *  net.minecraft.server.packs.PackLocationInfo
 *  net.minecraft.server.packs.PackResources
 *  net.minecraft.server.packs.PackSelectionConfig
 *  net.minecraft.server.packs.PackType
 *  net.minecraft.server.packs.VanillaPackResources
 *  net.minecraft.server.packs.VanillaPackResourcesBuilder
 *  net.minecraft.server.packs.metadata.MetadataSectionType
 *  net.minecraft.server.packs.metadata.pack.PackMetadataSection
 *  net.minecraft.server.packs.repository.BuiltInPackSource
 *  net.minecraft.server.packs.repository.KnownPack
 *  net.minecraft.server.packs.repository.Pack
 *  net.minecraft.server.packs.repository.Pack$Position
 *  net.minecraft.server.packs.repository.Pack$ResourcesSupplier
 *  net.minecraft.server.packs.repository.PackSource
 *  net.minecraft.server.packs.resources.ResourceMetadata
 *  net.minecraft.world.level.validation.DirectoryValidator
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jspecify.annotations.Nullable;

public class ClientPackSource
extends BuiltInPackSource {
    private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection((Component)Component.translatable((String)"resourcePack.vanilla.description"), SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).minorRange());
    private static final ResourceMetadata BUILT_IN_METADATA = ResourceMetadata.of((MetadataSectionType)PackMetadataSection.CLIENT_TYPE, (Object)VERSION_METADATA_SECTION);
    public static final String HIGH_CONTRAST_PACK = "high_contrast";
    private static final Map<String, Component> SPECIAL_PACK_NAMES = Map.of("programmer_art", Component.translatable((String)"resourcePack.programmer_art.name"), "high_contrast", Component.translatable((String)"resourcePack.high_contrast.name"));
    private static final PackLocationInfo VANILLA_PACK_INFO = new PackLocationInfo("vanilla", (Component)Component.translatable((String)"resourcePack.vanilla.name"), PackSource.BUILT_IN, Optional.of(CORE_PACK_INFO));
    private static final PackSelectionConfig VANILLA_SELECTION_CONFIG = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
    private static final PackSelectionConfig BUILT_IN_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    private static final Identifier PACKS_DIR = Identifier.withDefaultNamespace((String)"resourcepacks");
    private final @Nullable Path externalAssetDir;

    public ClientPackSource(Path externalAssetSource, DirectoryValidator validator) {
        super(PackType.CLIENT_RESOURCES, ClientPackSource.createVanillaPackSource(externalAssetSource), PACKS_DIR, validator);
        this.externalAssetDir = this.findExplodedAssetPacks(externalAssetSource);
    }

    private static PackLocationInfo createBuiltInPackLocation(String id, Component title) {
        return new PackLocationInfo(id, title, PackSource.BUILT_IN, Optional.of(KnownPack.vanilla((String)id)));
    }

    private @Nullable Path findExplodedAssetPacks(Path externalAssetSource) {
        Path devAssetDir;
        if (SharedConstants.IS_RUNNING_IN_IDE && externalAssetSource.getFileSystem() == FileSystems.getDefault() && Files.isDirectory(devAssetDir = externalAssetSource.getParent().resolve("resourcepacks"), new LinkOption[0])) {
            return devAssetDir;
        }
        return null;
    }

    private static VanillaPackResources createVanillaPackSource(Path externalAssetRoot) {
        return new VanillaPackResourcesBuilder().setMetadata(BUILT_IN_METADATA).exposeNamespace(new String[]{"minecraft", "realms"}).applyDevelopmentConfig().pushJarResources().pushAssetPath(PackType.CLIENT_RESOURCES, externalAssetRoot).build(VANILLA_PACK_INFO);
    }

    protected Component getPackTitle(String id) {
        Component title = SPECIAL_PACK_NAMES.get(id);
        return title != null ? title : Component.literal((String)id);
    }

    protected @Nullable Pack createVanillaPack(PackResources resources) {
        return Pack.readMetaAndCreate((PackLocationInfo)VANILLA_PACK_INFO, (Pack.ResourcesSupplier)ClientPackSource.fixedResources((PackResources)resources), (PackType)PackType.CLIENT_RESOURCES, (PackSelectionConfig)VANILLA_SELECTION_CONFIG);
    }

    protected @Nullable Pack createBuiltinPack(String id, Pack.ResourcesSupplier resources, Component name) {
        return Pack.readMetaAndCreate((PackLocationInfo)ClientPackSource.createBuiltInPackLocation(id, name), (Pack.ResourcesSupplier)resources, (PackType)PackType.CLIENT_RESOURCES, (PackSelectionConfig)BUILT_IN_SELECTION_CONFIG);
    }

    protected void populatePackList(BiConsumer<String, Function<String, Pack>> discoveredPacks) {
        super.populatePackList(discoveredPacks);
        if (this.externalAssetDir != null) {
            this.discoverPacksInPath(this.externalAssetDir, discoveredPacks);
        }
    }
}

