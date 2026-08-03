/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.Block
 */
package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelLocationUtils {
    @Deprecated
    public static Identifier decorateBlockModelLocation(String id) {
        return Identifier.withDefaultNamespace((String)("block/" + id));
    }

    public static Identifier decorateItemModelLocation(String id) {
        return Identifier.withDefaultNamespace((String)("item/" + id));
    }

    public static Identifier getModelLocation(Block block, String suffix) {
        Identifier key = BuiltInRegistries.BLOCK.getKey((Object)block);
        return key.withPath(path -> "block/" + path + suffix);
    }

    public static Identifier getModelLocation(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey((Object)block);
        return key.withPrefix("block/");
    }

    public static Identifier getModelLocation(Item item) {
        Identifier key = BuiltInRegistries.ITEM.getKey((Object)item);
        return key.withPrefix("item/");
    }

    public static Identifier getModelLocation(Item item, String suffix) {
        Identifier key = BuiltInRegistries.ITEM.getKey((Object)item);
        return key.withPath(path -> "item/" + path + suffix);
    }
}

