/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ExtraCodecs$LateBoundIdMapper
 */
package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.ComponentContents;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ContextEntityType;
import net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.LocalTime;
import net.minecraft.client.renderer.item.properties.select.MainHand;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

public class SelectItemModelProperties {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, SelectItemModelProperty.Type<?, ?>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper();
    public static final Codec<SelectItemModelProperty.Type<?, ?>> CODEC = ID_MAPPER.codec(Identifier.CODEC);

    public static void bootstrap() {
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"custom_model_data"), CustomModelDataProperty.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"main_hand"), MainHand.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"charge_type"), Charge.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"trim_material"), TrimMaterialProperty.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"block_state"), ItemBlockState.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"display_context"), DisplayContext.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"local_time"), LocalTime.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"context_entity_type"), ContextEntityType.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"context_dimension"), ContextDimension.TYPE);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"component"), ComponentContents.castType());
    }
}

