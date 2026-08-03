/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.SharedConstants
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.resources.Identifier
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.chunk.LevelChunk
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryBiome
implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace((String)"biome");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity == null || minecraft.level == null) {
            return;
        }
        BlockPos feetPos = entity.blockPosition();
        if (minecraft.level.isInsideBuildHeight(feetPos.getY())) {
            if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && serverOrClientLevel instanceof ServerLevel) {
                displayer.addToGroup(GROUP, List.of("Biome: " + DebugEntryBiome.printBiome((Holder<Biome>)minecraft.level.getBiome(feetPos)), "Server Biome: " + DebugEntryBiome.printBiome((Holder<Biome>)serverOrClientLevel.getBiome(feetPos))));
            } else {
                displayer.addLine("Biome: " + DebugEntryBiome.printBiome((Holder<Biome>)minecraft.level.getBiome(feetPos)));
            }
        }
    }

    private static String printBiome(Holder<Biome> biome) {
        return (String)biome.unwrap().map(key -> key.identifier().toString(), l -> "[unregistered " + String.valueOf(l) + "]");
    }
}

