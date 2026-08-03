/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ExtraCodecs$LateBoundIdMapper
 */
package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.BundleSelectedItemSpecialRenderer;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

public class ItemModels {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemModel.Unbaked>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper();
    public static final Codec<ItemModel.Unbaked> CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatch(ItemModel.Unbaked::type, c -> c);

    public static void bootstrap() {
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"empty"), EmptyModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"model"), CuboidItemModelWrapper.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"range_dispatch"), RangeSelectItemModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"special"), SpecialModelWrapper.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"composite"), CompositeModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"bundle/selected_item"), BundleSelectedItemSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"select"), SelectItemModel.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"condition"), ConditionalItemModel.Unbaked.MAP_CODEC);
    }
}

