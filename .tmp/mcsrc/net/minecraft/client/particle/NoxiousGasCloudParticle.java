/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.core.particles.SimpleParticleType
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.PotentSulfurBlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class NoxiousGasCloudParticle
extends NoRenderParticle {
    private static final int PARTICLE_TICKS = 2;

    protected NoxiousGasCloudParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.lifetime = 20;
    }

    @Override
    public void tick() {
        Vec3 particlePos;
        super.tick();
        if (this.age % 2 != 0) {
            return;
        }
        BlockPos sourceBlock = BlockPos.containing((double)this.x, (double)this.y, (double)this.z);
        if (PotentSulfurBlockEntity.canBeReachedByNoxiousGas((Level)this.level, (BlockPos)sourceBlock, (Vec3)(particlePos = NoxiousGasCloudParticle.pickRandomParticleSpawnPoint(this.level, sourceBlock)))) {
            NoxiousGasCloudParticle.spawnNoxiousGasParticle(this.level, particlePos);
        }
    }

    private static Vec3 pickRandomParticleSpawnPoint(Level level, BlockPos centerBlock) {
        RandomSource random = level.getRandom();
        Vec3 horizontalDirection = new Vec3((double)(random.nextFloat() - 0.5f), 0.0, (double)(random.nextFloat() - 0.5f)).normalize();
        float distance = random.nextFloat() * 3.0f;
        return Vec3.atCenterOf((Vec3i)centerBlock).add(horizontalDirection.scale((double)distance)).subtract(0.0, 0.25, 0.0);
    }

    private static void spawnNoxiousGasParticle(Level level, Vec3 pos) {
        level.addAlwaysVisibleParticle((ParticleOptions)ParticleTypes.NOXIOUS_GAS, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    public static class Provider
    implements ParticleProvider<SimpleParticleType> {
        @Override
        public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new NoxiousGasCloudParticle(level, x, y, z);
        }
    }
}

