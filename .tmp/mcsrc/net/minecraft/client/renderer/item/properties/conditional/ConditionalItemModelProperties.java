/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ExtraCodecs$LateBoundIdMapper
 */
package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.BundleHasSelectedItem;
import net.minecraft.client.renderer.item.properties.conditional.ComponentMatches;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.conditional.ExtendedView;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.IsCarried;
import net.minecraft.client.renderer.item.properties.conditional.IsKeybindDown;
import net.minecraft.client.renderer.item.properties.conditional.IsSelected;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.conditional.IsViewEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

public class ConditionalItemModelProperties {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper();
    public static final MapCodec<ConditionalItemModelProperty> MAP_CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatchMap("property", ConditionalItemModelProperty::type, c -> c);

    public static void bootstrap() {
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"custom_model_data"), CustomModelDataProperty.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"using_item"), IsUsingItem.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"broken"), Broken.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"damaged"), Damaged.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"fishing_rod/cast"), FishingRodCast.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"has_component"), HasComponent.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"bundle/has_selected_item"), BundleHasSelectedItem.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"selected"), IsSelected.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"carried"), IsCarried.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"extended_view"), ExtendedView.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"keybind_down"), IsKeybindDown.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"view_entity"), IsViewEntity.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"component"), ComponentMatches.MAP_CODEC);
    }
}

