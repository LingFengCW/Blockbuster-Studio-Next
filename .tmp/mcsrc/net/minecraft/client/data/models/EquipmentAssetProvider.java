/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.data.CachedOutput
 *  net.minecraft.data.DataProvider
 *  net.minecraft.data.PackOutput
 *  net.minecraft.data.PackOutput$PathProvider
 *  net.minecraft.data.PackOutput$Target
 *  net.minecraft.resources.Identifier
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.equipment.EquipmentAsset
 *  net.minecraft.world.item.equipment.EquipmentAssets
 */
package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class EquipmentAssetProvider
implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public EquipmentAssetProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment");
    }

    private static void bootstrap(BiConsumer<ResourceKey<EquipmentAsset>, EquipmentClientInfo> consumer) {
        ResourceKey id;
        DyeColor color;
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.LEATHER, EquipmentClientInfo.builder().addHumanoidLayers(Identifier.withDefaultNamespace((String)"leather"), true).addHumanoidLayers(Identifier.withDefaultNamespace((String)"leather_overlay"), false).addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace((String)"leather"), true), EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace((String)"leather_overlay"), false)).build());
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.CHAINMAIL, EquipmentAssetProvider.onlyHumanoid("chainmail"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.COPPER, EquipmentAssetProvider.humanoidAndMountArmor("copper"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.IRON, EquipmentAssetProvider.humanoidAndMountArmor("iron"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.GOLD, EquipmentAssetProvider.humanoidAndMountArmor("gold"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.DIAMOND, EquipmentAssetProvider.humanoidAndMountArmor("diamond"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.TURTLE_SCUTE, EquipmentClientInfo.builder().addMainHumanoidLayer(Identifier.withDefaultNamespace((String)"turtle_scute"), false).build());
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.NETHERITE, EquipmentAssetProvider.humanoidAndMountArmor("netherite"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.ARMADILLO_SCUTE, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace((String)"armadillo_scute"), false)).addLayers(EquipmentClientInfo.LayerType.WOLF_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace((String)"armadillo_scute_overlay"), true)).build());
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.ELYTRA, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.WINGS, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace((String)"elytra"), Optional.empty(), true)).build());
        EquipmentClientInfo.Layer saddleLayer = new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace((String)"saddle"));
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.SADDLE, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.PIG_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.STRIDER_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.CAMEL_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.HORSE_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.DONKEY_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.MULE_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE, saddleLayer).addLayers(EquipmentClientInfo.LayerType.NAUTILUS_SADDLE, saddleLayer).build());
        for (Map.Entry entry : EquipmentAssets.HARNESSES.entrySet()) {
            color = (DyeColor)entry.getKey();
            id = (ResourceKey)entry.getValue();
            consumer.accept((ResourceKey<EquipmentAsset>)id, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY, EquipmentClientInfo.Layer.onlyIfDyed(Identifier.withDefaultNamespace((String)(color.getSerializedName() + "_harness")), false)).build());
        }
        for (Map.Entry entry : EquipmentAssets.CARPETS.entrySet()) {
            color = (DyeColor)entry.getKey();
            id = (ResourceKey)entry.getValue();
            consumer.accept((ResourceKey<EquipmentAsset>)id, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace((String)color.getSerializedName()))).build());
        }
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.TRADER_LLAMA, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace((String)"trader_llama"))).build());
        consumer.accept((ResourceKey<EquipmentAsset>)EquipmentAssets.TRADER_LLAMA_BABY, EquipmentClientInfo.builder().addLayers(EquipmentClientInfo.LayerType.LLAMA_BODY, new EquipmentClientInfo.Layer(Identifier.withDefaultNamespace((String)"trader_llama_baby"))).build());
    }

    private static EquipmentClientInfo onlyHumanoid(String name) {
        return EquipmentClientInfo.builder().addHumanoidLayers(Identifier.withDefaultNamespace((String)name)).build();
    }

    private static EquipmentClientInfo humanoidAndMountArmor(String name) {
        return EquipmentClientInfo.builder().addHumanoidLayers(Identifier.withDefaultNamespace((String)name)).addLayers(EquipmentClientInfo.LayerType.HORSE_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace((String)name), false)).addLayers(EquipmentClientInfo.LayerType.NAUTILUS_BODY, EquipmentClientInfo.Layer.leatherDyeable(Identifier.withDefaultNamespace((String)name), false)).build();
    }

    public CompletableFuture<?> run(CachedOutput cache) {
        HashMap equipmentAssets = new HashMap();
        EquipmentAssetProvider.bootstrap((id, asset) -> {
            if (equipmentAssets.putIfAbsent(id, asset) != null) {
                throw new IllegalStateException("Tried to register equipment asset twice for id: " + String.valueOf(id));
            }
        });
        return DataProvider.saveAll((CachedOutput)cache, EquipmentClientInfo.CODEC, arg_0 -> ((PackOutput.PathProvider)this.pathProvider).json(arg_0), equipmentAssets);
    }

    public String getName() {
        return "Equipment Asset Definitions";
    }
}

