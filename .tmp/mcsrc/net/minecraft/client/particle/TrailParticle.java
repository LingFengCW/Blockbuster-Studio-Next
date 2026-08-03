/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.particles.TrailParticleOption
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class TrailParticle
extends SingleQuadParticle {
    private final Vec3 target;

    private TrailParticle(ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, Vec3 target, int color, TextureAtlasSprite sprite) {
        super(level, x, y, z, xAux, yAux, zAux, sprite);
        color = ARGB.scaleRGB((int)color, (float)(0.875f + this.random.nextFloat() * 0.25f), (float)(0.875f + this.random.nextFloat() * 0.25f), (float)(0.875f + this.random.nextFloat() * 0.25f));
        this.rCol = (float)ARGB.red((int)color) / 255.0f;
        this.gCol = (float)ARGB.green((int)color) / 255.0f;
        this.bCol = (float)ARGB.blue((int)color) / 255.0f;
        this.quadSize = 0.26f;
        this.target = target;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        int ticksRemaining = this.lifetime - this.age;
        double alpha = 1.0 / (double)ticksRemaining;
        this.x = Mth.lerp((double)alpha, (double)this.x, (double)this.target.x());
        this.y = Mth.lerp((double)alpha, (double)this.y, (double)this.target.y());
        this.z = Mth.lerp((double)alpha, (double)this.z, (double)this.target.z());
    }

    @Override
    public int getLightCoords(float a) {
        return 0xF000F0;
    }

    public static class Provider
    implements ParticleProvider<TrailParticleOption> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(TrailParticleOption options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            TrailParticle particle = new TrailParticle(level, x, y, z, xAux, yAux, zAux, options.target(), options.color(), this.sprite.get(random));
            particle.setLifetime(options.duration());
            return particle;
        }
    }
}

