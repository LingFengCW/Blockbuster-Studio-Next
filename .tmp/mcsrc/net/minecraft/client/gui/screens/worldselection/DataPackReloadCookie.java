/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.WorldDataConfiguration
 *  net.minecraft.world.level.levelgen.WorldGenSettings
 */
package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public record DataPackReloadCookie(WorldGenSettings worldGenSettings, WorldDataConfiguration dataConfiguration) {
}

