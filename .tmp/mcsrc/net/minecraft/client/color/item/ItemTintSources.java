/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ExtraCodecs$LateBoundIdMapper
 */
package net.minecraft.client.color.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.CustomModelDataSource;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.color.item.Firework;
import net.minecraft.client.color.item.GrassColorSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.MapColor;
import net.minecraft.client.color.item.Potion;
import net.minecraft.client.color.item.TeamColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

public class ItemTintSources {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper();
    public static final Codec<ItemTintSource> CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatch(ItemTintSource::type, c -> c);

    public static void bootstrap() {
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"custom_model_data"), CustomModelDataSource.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"constant"), Constant.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"dye"), Dye.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"grass"), GrassColorSource.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"firework"), Firework.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"potion"), Potion.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"map_color"), MapColor.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"team"), TeamColor.MAP_CODEC);
    }
}

