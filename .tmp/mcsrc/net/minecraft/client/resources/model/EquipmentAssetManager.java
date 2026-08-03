/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.FileToIdConverter
 *  net.minecraft.resources.Identifier
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.minecraft.world.item.equipment.EquipmentAsset
 *  net.minecraft.world.item.equipment.EquipmentAssets
 */
package net.minecraft.client.resources.model;

import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class EquipmentAssetManager
extends SimpleJsonResourceReloadListener<EquipmentClientInfo> {
    public static final EquipmentClientInfo MISSING = new EquipmentClientInfo(Map.of());
    private static final FileToIdConverter ASSET_LISTER = FileToIdConverter.json((String)"equipment");
    private Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentAssets = Map.of();

    public EquipmentAssetManager() {
        super(EquipmentClientInfo.CODEC, ASSET_LISTER);
    }

    protected void apply(Map<Identifier, EquipmentClientInfo> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.equipmentAssets = preparations.entrySet().stream().collect(Collectors.toUnmodifiableMap(e -> ResourceKey.create((ResourceKey)EquipmentAssets.ROOT_ID, (Identifier)((Identifier)e.getKey())), Map.Entry::getValue));
    }

    public EquipmentClientInfo get(ResourceKey<EquipmentAsset> id) {
        return this.equipmentAssets.getOrDefault(id, MISSING);
    }
}

