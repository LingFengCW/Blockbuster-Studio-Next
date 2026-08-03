/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.BubbleColumnBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package net.minecraft.client.resources.sounds;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BubbleColumnAmbientSoundHandler
implements AmbientSoundHandler {
    private final LocalPlayer player;
    private boolean wasInBubbleColumn;
    private boolean firstTick = true;

    public BubbleColumnAmbientSoundHandler(LocalPlayer player) {
        this.player = player;
    }

    @Override
    public void tick() {
        Level level = this.player.level();
        BlockState state = level.getBlockStatesIfLoaded(this.player.getBoundingBox().inflate(0.0, (double)-0.4f, 0.0).deflate(1.0E-6)).filter(s -> s.is((Object)Blocks.BUBBLE_COLUMN)).findFirst().orElse(null);
        if (state != null) {
            if (!this.wasInBubbleColumn && !this.firstTick && state.is((Object)Blocks.BUBBLE_COLUMN) && !this.player.isSpectator()) {
                boolean dragDown = (Boolean)state.getValue((Property)BubbleColumnBlock.DRAG_DOWN);
                if (dragDown) {
                    this.player.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 1.0f);
                } else {
                    this.player.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.0f, 1.0f);
                }
            }
            this.wasInBubbleColumn = true;
        } else {
            this.wasInBubbleColumn = false;
        }
        this.firstTick = false;
    }
}

