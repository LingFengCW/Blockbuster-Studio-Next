/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 *  net.minecraft.util.Util
 *  net.minecraft.world.BossEvent
 *  net.minecraft.world.BossEvent$BossBarColor
 *  net.minecraft.world.BossEvent$BossBarOverlay
 */
package net.minecraft.client.gui.components;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.BossEvent;

public class LerpingBossEvent
extends BossEvent {
    private static final long LERP_MILLISECONDS = 100L;
    protected float targetPercent;
    protected long setTime;

    public LerpingBossEvent(UUID id, Component name, float progress, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
        super(id, name, color, overlay);
        this.targetPercent = progress;
        this.progress = progress;
        this.setTime = Util.getMillis();
        this.setDarkenScreen(darkenScreen);
        this.setPlayBossMusic(playMusic);
        this.setCreateWorldFog(createWorldFog);
    }

    public void setProgress(float progress) {
        this.progress = this.getProgress();
        this.targetPercent = progress;
        this.setTime = Util.getMillis();
    }

    public float getProgress() {
        long timeSinceSet = Util.getMillis() - this.setTime;
        float lerpPercent = Mth.clamp((float)((float)timeSinceSet / 100.0f), (float)0.0f, (float)1.0f);
        return Mth.lerp((float)lerpPercent, (float)this.progress, (float)this.targetPercent);
    }
}

