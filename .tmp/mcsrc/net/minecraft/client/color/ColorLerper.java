/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.DyeColor
 */
package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public class ColorLerper {
    public static final DyeColor[] MUSIC_NOTE_COLORS = new DyeColor[]{DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.CYAN, DyeColor.GREEN, DyeColor.LIME, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.PINK, DyeColor.RED, DyeColor.MAGENTA};

    public static int getLerpedColor(Type type, float tick) {
        int tickCount = Mth.floor((float)tick);
        int value = tickCount / type.colorDuration;
        int colorCount = type.colors.length;
        int c1 = value % colorCount;
        int c2 = (value + 1) % colorCount;
        float subStep = ((float)(tickCount % type.colorDuration) + Mth.frac((float)tick)) / (float)type.colorDuration;
        int color1 = type.getColor(type.colors[c1]);
        int color2 = type.getColor(type.colors[c2]);
        return ARGB.srgbLerp((float)subStep, (int)color1, (int)color2);
    }

    private static int getModifiedColor(DyeColor color, float brightness) {
        if (color == DyeColor.WHITE) {
            return -1644826;
        }
        int src = color.getTextureDiffuseColor();
        return ARGB.color((int)255, (int)Mth.clamp((int)Mth.floor((float)((float)ARGB.red((int)src) * brightness)), (int)0, (int)255), (int)Mth.clamp((int)Mth.floor((float)((float)ARGB.green((int)src) * brightness)), (int)0, (int)255), (int)Mth.clamp((int)Mth.floor((float)((float)ARGB.blue((int)src) * brightness)), (int)0, (int)255));
    }

    public static enum Type {
        SHEEP(25, DyeColor.values(), 0.75f),
        MUSIC_NOTE(30, MUSIC_NOTE_COLORS, 1.25f);

        private final int colorDuration;
        private final Map<DyeColor, Integer> colorByDye;
        private final DyeColor[] colors;

        private Type(int colorDuration, DyeColor[] colors, float brightness) {
            this.colorDuration = colorDuration;
            this.colorByDye = Maps.newHashMap(Arrays.stream(colors).collect(Collectors.toMap(d -> d, color -> ColorLerper.getModifiedColor(color, brightness))));
            this.colors = colors;
        }

        public final int getColor(DyeColor dyeColor) {
            return this.colorByDye.get(dyeColor);
        }
    }
}

