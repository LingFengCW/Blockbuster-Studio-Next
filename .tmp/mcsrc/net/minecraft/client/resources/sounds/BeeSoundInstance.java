/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.animal.bee.Bee
 */
package net.minecraft.client.resources.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.bee.Bee;

public abstract class BeeSoundInstance
extends AbstractTickableSoundInstance {
    private static final float VOLUME_MIN = 0.0f;
    private static final float VOLUME_MAX = 1.2f;
    private static final float PITCH_MIN = 0.0f;
    protected final Bee bee;
    private boolean hasSwitched;

    public BeeSoundInstance(Bee bee, SoundEvent event, SoundSource source) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.bee = bee;
        this.x = (float)bee.getX();
        this.y = (float)bee.getY();
        this.z = (float)bee.getZ();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
    }

    @Override
    public void tick() {
        boolean shouldSwitchSounds = this.shouldSwitchSounds();
        if (shouldSwitchSounds && !this.isStopped()) {
            Minecraft.getInstance().getSoundManager().queueTickingSound(this.getAlternativeSoundInstance());
            this.hasSwitched = true;
        }
        if (this.bee.isRemoved() || this.hasSwitched) {
            this.stop();
            return;
        }
        this.x = (float)this.bee.getX();
        this.y = (float)this.bee.getY();
        this.z = (float)this.bee.getZ();
        float speed = (float)this.bee.getDeltaMovement().horizontalDistance();
        if (speed >= 0.01f) {
            this.pitch = Mth.lerp((float)Mth.clamp((float)speed, (float)this.getMinPitch(), (float)this.getMaxPitch()), (float)this.getMinPitch(), (float)this.getMaxPitch());
            this.volume = Mth.lerp((float)Mth.clamp((float)speed, (float)0.0f, (float)0.5f), (float)0.0f, (float)1.2f);
        } else {
            this.pitch = 0.0f;
            this.volume = 0.0f;
        }
    }

    private float getMinPitch() {
        if (this.bee.isBaby()) {
            return 1.1f;
        }
        return 0.7f;
    }

    private float getMaxPitch() {
        if (this.bee.isBaby()) {
            return 1.5f;
        }
        return 1.1f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !this.bee.isSilent();
    }

    protected abstract AbstractTickableSoundInstance getAlternativeSoundInstance();

    protected abstract boolean shouldSwitchSounds();
}

