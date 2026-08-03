/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ExtraCodecs$LateBoundIdMapper
 */
package net.minecraft.client.renderer.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.BannerSpecialRenderer;
import net.minecraft.client.renderer.special.BellSpecialRenderer;
import net.minecraft.client.renderer.special.BookSpecialRenderer;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.special.ConduitSpecialRenderer;
import net.minecraft.client.renderer.special.CopperGolemStatueSpecialRenderer;
import net.minecraft.client.renderer.special.DecoratedPotSpecialRenderer;
import net.minecraft.client.renderer.special.EndCubeSpecialRenderer;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.TridentSpecialRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

public class SpecialModelRenderers {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper();
    public static final Codec<SpecialModelRenderer.Unbaked<?>> CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatch(SpecialModelRenderer.Unbaked::type, c -> c);

    public static void bootstrap() {
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"bell"), BellSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"banner"), BannerSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"book"), BookSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"conduit"), ConduitSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"chest"), ChestSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"copper_golem_statue"), CopperGolemStatueSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"head"), SkullSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"player_head"), PlayerHeadSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"shulker_box"), ShulkerBoxSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"shield"), ShieldSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"trident"), TridentSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"decorated_pot"), DecoratedPotSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put((Object)Identifier.withDefaultNamespace((String)"end_cube"), EndCubeSpecialRenderer.Unbaked.MAP_CODEC);
    }
}

