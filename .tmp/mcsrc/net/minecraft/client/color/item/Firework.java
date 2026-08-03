/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntList
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.ExtraCodecs
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.FireworkExplosion
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.color.item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import org.jspecify.annotations.Nullable;

public record Firework(int defaultColor) implements ItemTintSource
{
    public static final MapCodec<Firework> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Firework::defaultColor)).apply((Applicative)i, Firework::new));

    public Firework() {
        this(-7697782);
    }

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        FireworkExplosion explosion = (FireworkExplosion)itemStack.get(DataComponents.FIREWORK_EXPLOSION);
        IntList explosionColors = explosion != null ? explosion.colors() : IntList.of();
        int colorCount = explosionColors.size();
        if (colorCount == 0) {
            return this.defaultColor;
        }
        if (colorCount == 1) {
            return ARGB.opaque((int)explosionColors.getInt(0));
        }
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        for (int i = 0; i < colorCount; ++i) {
            int color = explosionColors.getInt(i);
            totalRed += ARGB.red((int)color);
            totalGreen += ARGB.green((int)color);
            totalBlue += ARGB.blue((int)color);
        }
        return ARGB.color((int)(totalRed / colorCount), (int)(totalGreen / colorCount), (int)(totalBlue / colorCount));
    }

    public MapCodec<Firework> type() {
        return MAP_CODEC;
    }
}

