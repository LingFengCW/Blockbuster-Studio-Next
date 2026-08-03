/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.ItemParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.core.particles.PowerParticleOption
 *  net.minecraft.core.particles.SculkChargeParticleOptions
 *  net.minecraft.core.particles.ShriekParticleOption
 *  net.minecraft.core.particles.SimpleParticleType
 *  net.minecraft.core.particles.SpellParticleOption
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.util.ParticleUtils
 *  net.minecraft.util.RandomSource
 *  net.minecraft.util.valueproviders.IntProvider
 *  net.minecraft.util.valueproviders.UniformInt
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.BoneMealItem
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.JukeboxSong
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.BrushableBlock
 *  net.minecraft.world.level.block.ComposterBlock
 *  net.minecraft.world.level.block.MultifaceBlock
 *  net.minecraft.world.level.block.PointedDripstoneBlock
 *  net.minecraft.world.level.block.SculkShriekerBlock
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.trialspawner.TrialSpawner
 *  net.minecraft.world.level.block.entity.trialspawner.TrialSpawner$FlameParticle
 *  net.minecraft.world.level.block.entity.vault.VaultBlockEntity
 *  net.minecraft.world.level.block.entity.vault.VaultBlockEntity$Client
 *  net.minecraft.world.level.block.entity.vault.VaultSharedData
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LevelEventHandler {
    private final Minecraft minecraft;
    private final ClientLevel level;
    private final Map<BlockPos, SoundInstance> playingJukeboxSongs = new HashMap<BlockPos, SoundInstance>();

    public LevelEventHandler(Minecraft minecraft, ClientLevel level) {
        this.minecraft = minecraft;
        this.level = level;
    }

    public void globalLevelEvent(int type, BlockPos pos, int data) {
        switch (type) {
            case 1023: 
            case 1028: 
            case 1038: {
                Camera camera = this.minecraft.gameRenderer.mainCamera();
                if (!camera.isInitialized()) break;
                Vec3 directionToEvent = Vec3.atCenterOf((Vec3i)pos).subtract(camera.position()).normalize();
                Vec3 soundPos = camera.position().add(directionToEvent.scale(2.0));
                if (type == 1023) {
                    this.level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.0f, false);
                    break;
                }
                if (type == 1038) {
                    this.level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0f, 1.0f, false);
                    break;
                }
                this.level.playLocalSound(soundPos.x, soundPos.y, soundPos.z, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0f, 1.0f, false);
            }
        }
    }

    public void levelEvent(int eventType, BlockPos pos, int data) {
        RandomSource random = this.level.getRandom();
        switch (eventType) {
            case 1035: {
                this.level.playLocalSound(pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 1033: {
                this.level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 1034: {
                this.level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 1032: {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, random.nextFloat() * 0.4f + 0.8f, 0.25f));
                break;
            }
            case 1001: {
                this.level.playLocalSound(pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                break;
            }
            case 1000: {
                this.level.playLocalSound(pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 1049: {
                this.level.playLocalSound(pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 1050: {
                this.level.playLocalSound(pos, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 1004: {
                this.level.playLocalSound(pos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0f, 1.2f, false);
                break;
            }
            case 1002: {
                this.level.playLocalSound(pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0f, 1.2f, false);
                break;
            }
            case 1051: {
                this.level.playLocalSound(pos, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5f, 0.4f / (this.level.getRandom().nextFloat() * 0.4f + 0.8f), false);
                break;
            }
            case 2010: {
                this.shootParticles(data, pos, random, ParticleTypes.WHITE_SMOKE);
                break;
            }
            case 2000: {
                this.shootParticles(data, pos, random, ParticleTypes.SMOKE);
                break;
            }
            case 2003: {
                double x = (double)pos.getX() + 0.5;
                double y = pos.getY();
                double z = (double)pos.getZ() + 0.5;
                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, Items.ENDER_EYE);
                for (int i = 0; i < 8; ++i) {
                    this.level.addParticle((ParticleOptions)breakParticle, x, y, z, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);
                }
                for (double angle = 0.0; angle < Math.PI * 2; angle += 0.15707963267948966) {
                    this.level.addParticle((ParticleOptions)ParticleTypes.PORTAL, x + Math.cos(angle) * 5.0, y - 0.4, z + Math.sin(angle) * 5.0, Math.cos(angle) * -5.0, 0.0, Math.sin(angle) * -5.0);
                    this.level.addParticle((ParticleOptions)ParticleTypes.PORTAL, x + Math.cos(angle) * 5.0, y - 0.4, z + Math.sin(angle) * 5.0, Math.cos(angle) * -7.0, 0.0, Math.sin(angle) * -7.0);
                }
                break;
            }
            case 2002: 
            case 2007: {
                Vec3 particlePos = Vec3.atBottomCenterOf((Vec3i)pos);
                ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, Items.SPLASH_POTION);
                for (int i = 0; i < 8; ++i) {
                    this.level.addParticle((ParticleOptions)breakParticle, particlePos.x, particlePos.y, particlePos.z, random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);
                }
                float red = (float)(data >> 16 & 0xFF) / 255.0f;
                float green = (float)(data >> 8 & 0xFF) / 255.0f;
                float blue = (float)(data >> 0 & 0xFF) / 255.0f;
                ParticleType particleType = eventType == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;
                for (int i = 0; i < 100; ++i) {
                    double dist = random.nextDouble() * 4.0;
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double velocityX = Math.cos(angle) * dist;
                    double velocityY = 0.01 + random.nextDouble() * 0.5;
                    double velocityZ = Math.sin(angle) * dist;
                    float randomBrightness = 0.75f + random.nextFloat() * 0.25f;
                    SpellParticleOption particle = SpellParticleOption.create((ParticleType)particleType, (float)(red * randomBrightness), (float)(green * randomBrightness), (float)(blue * randomBrightness), (float)((float)dist));
                    this.level.addParticle((ParticleOptions)particle, particlePos.x + velocityX * 0.1, particlePos.y + 0.3, particlePos.z + velocityZ * 0.1, velocityX, velocityY, velocityZ);
                }
                this.level.playLocalSound(pos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 2001: {
                BlockState blockState = Block.stateById((int)data);
                if (!blockState.isAir()) {
                    SoundType soundType = blockState.getSoundType();
                    this.level.playLocalSound(pos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f, false);
                }
                this.level.addDestroyBlockEffect(pos, blockState);
                break;
            }
            case 3008: {
                BlockState blockStateForBrushing = Block.stateById((int)data);
                Block red = blockStateForBrushing.getBlock();
                if (red instanceof BrushableBlock) {
                    BrushableBlock brushableBlock = (BrushableBlock)red;
                    this.level.playLocalSound(pos, brushableBlock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0f, 1.0f, false);
                }
                this.level.addDestroyBlockEffect(pos, blockStateForBrushing);
                break;
            }
            case 2004: {
                for (int i = 0; i < 20; ++i) {
                    double x = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    double y = (double)pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    double z = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    this.level.addParticle((ParticleOptions)ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                    this.level.addParticle((ParticleOptions)ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
                }
                break;
            }
            case 3011: {
                TrialSpawner.addSpawnParticles((Level)this.level, (BlockPos)pos, (RandomSource)random, (SimpleParticleType)TrialSpawner.FlameParticle.decode((int)data).particleType);
                break;
            }
            case 3012: {
                this.level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                TrialSpawner.addSpawnParticles((Level)this.level, (BlockPos)pos, (RandomSource)random, (SimpleParticleType)TrialSpawner.FlameParticle.decode((int)data).particleType);
                break;
            }
            case 3021: {
                this.level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                TrialSpawner.addSpawnParticles((Level)this.level, (BlockPos)pos, (RandomSource)random, (SimpleParticleType)TrialSpawner.FlameParticle.decode((int)data).particleType);
                break;
            }
            case 3013: {
                this.level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                TrialSpawner.addDetectPlayerParticles((Level)this.level, (BlockPos)pos, (RandomSource)random, (int)data, (ParticleOptions)ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
                break;
            }
            case 3019: {
                this.level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                TrialSpawner.addDetectPlayerParticles((Level)this.level, (BlockPos)pos, (RandomSource)random, (int)data, (ParticleOptions)ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                break;
            }
            case 3020: {
                this.level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE, SoundSource.BLOCKS, data == 0 ? 0.3f : 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                TrialSpawner.addDetectPlayerParticles((Level)this.level, (BlockPos)pos, (RandomSource)random, (int)0, (ParticleOptions)ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                TrialSpawner.addBecomeOminousParticles((Level)this.level, (BlockPos)pos, (RandomSource)random);
                break;
            }
            case 3014: {
                this.level.playLocalSound(pos, SoundEvents.TRIAL_SPAWNER_EJECT_ITEM, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                TrialSpawner.addEjectItemParticles((Level)this.level, (BlockPos)pos, (RandomSource)random);
                break;
            }
            case 3017: {
                TrialSpawner.addEjectItemParticles((Level)this.level, (BlockPos)pos, (RandomSource)random);
                break;
            }
            case 3015: {
                BlockEntity x = this.level.getBlockEntity(pos);
                if (!(x instanceof VaultBlockEntity)) break;
                VaultBlockEntity entity = (VaultBlockEntity)x;
                VaultBlockEntity.Client.emitActivationParticles((Level)this.level, (BlockPos)entity.getBlockPos(), (BlockState)entity.getBlockState(), (VaultSharedData)entity.getSharedData(), (ParticleOptions)(data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME));
                this.level.playLocalSound(pos, SoundEvents.VAULT_ACTIVATE, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                break;
            }
            case 3016: {
                VaultBlockEntity.Client.emitDeactivationParticles((Level)this.level, (BlockPos)pos, (ParticleOptions)(data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME));
                this.level.playLocalSound(pos, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                break;
            }
            case 3018: {
                for (int i = 0; i < 10; ++i) {
                    double velocityX = random.nextGaussian() * 0.02;
                    double velocityY = random.nextGaussian() * 0.02;
                    double velocityZ = random.nextGaussian() * 0.02;
                    this.level.addParticle((ParticleOptions)ParticleTypes.POOF, (double)pos.getX() + random.nextDouble(), (double)pos.getY() + random.nextDouble(), (double)pos.getZ() + random.nextDouble(), velocityX, velocityY, velocityZ);
                }
                this.level.playLocalSound(pos, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, true);
                break;
            }
            case 1505: {
                BoneMealItem.addGrowthParticles((LevelAccessor)this.level, (BlockPos)pos, (int)data);
                this.level.playLocalSound(pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 2011: {
                ParticleUtils.spawnParticleInBlock((LevelAccessor)this.level, (BlockPos)pos, (int)data, (ParticleOptions)ParticleTypes.HAPPY_VILLAGER);
                break;
            }
            case 2012: {
                ParticleUtils.spawnParticleInBlock((LevelAccessor)this.level, (BlockPos)pos, (int)data, (ParticleOptions)ParticleTypes.HAPPY_VILLAGER);
                break;
            }
            case 3009: {
                ParticleUtils.spawnParticlesOnBlockFaces((Level)this.level, (BlockPos)pos, (ParticleOptions)ParticleTypes.EGG_CRACK, (IntProvider)UniformInt.of((int)3, (int)6));
                break;
            }
            case 3002: {
                if (data >= 0 && data < Direction.Axis.VALUES.length) {
                    ParticleUtils.spawnParticlesAlongAxis((Direction.Axis)Direction.Axis.VALUES[data], (Level)this.level, (BlockPos)pos, (double)0.125, (ParticleOptions)ParticleTypes.ELECTRIC_SPARK, (UniformInt)UniformInt.of((int)10, (int)19));
                    break;
                }
                ParticleUtils.spawnParticlesOnBlockFaces((Level)this.level, (BlockPos)pos, (ParticleOptions)ParticleTypes.ELECTRIC_SPARK, (IntProvider)UniformInt.of((int)3, (int)5));
                break;
            }
            case 2013: {
                ParticleUtils.spawnSmashAttackParticles((LevelAccessor)this.level, (BlockPos)pos, (int)data);
                break;
            }
            case 3006: {
                int count = data >> 6;
                if (count > 0) {
                    if (random.nextFloat() < 0.3f + (float)count * 0.1f) {
                        float volume = 0.15f + 0.02f * (float)count * (float)count * random.nextFloat();
                        float pitch = 0.4f + 0.3f * (float)count * random.nextFloat();
                        this.level.playLocalSound(pos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, volume, pitch, false);
                    }
                    byte particleData = (byte)(data & 0x3F);
                    UniformInt repetition = UniformInt.of((int)0, (int)count);
                    float speedVar = 0.005f;
                    Supplier<Vec3> speedSupplier = () -> new Vec3(Mth.nextDouble((RandomSource)random, (double)-0.005f, (double)0.005f), Mth.nextDouble((RandomSource)random, (double)-0.005f, (double)0.005f), Mth.nextDouble((RandomSource)random, (double)-0.005f, (double)0.005f));
                    if (particleData == 0) {
                        for (Direction direction : Direction.values()) {
                            float fullBlockRotation = direction == Direction.DOWN ? (float)Math.PI : 0.0f;
                            double fullBlockFactor = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                            ParticleUtils.spawnParticlesOnBlockFace((Level)this.level, (BlockPos)pos, (ParticleOptions)new SculkChargeParticleOptions(fullBlockRotation), (IntProvider)repetition, (Direction)direction, speedSupplier, (double)fullBlockFactor);
                        }
                    } else {
                        for (Direction direction : MultifaceBlock.unpack((byte)particleData)) {
                            float facesBlockRotation = direction == Direction.UP ? (float)Math.PI : 0.0f;
                            double facesBlockFactor = 0.35;
                            ParticleUtils.spawnParticlesOnBlockFace((Level)this.level, (BlockPos)pos, (ParticleOptions)new SculkChargeParticleOptions(facesBlockRotation), (IntProvider)repetition, (Direction)direction, speedSupplier, (double)0.35);
                        }
                    }
                } else {
                    this.level.playLocalSound(pos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                    boolean isSolid = this.level.getBlockState(pos).isCollisionShapeFullBlock((BlockGetter)this.level, pos);
                    int particleCount = isSolid ? 40 : 20;
                    float spread = isSolid ? 0.45f : 0.25f;
                    float speed = 0.07f;
                    for (int i = 0; i < particleCount; ++i) {
                        float velocityX = 2.0f * random.nextFloat() - 1.0f;
                        float velocityY = 2.0f * random.nextFloat() - 1.0f;
                        float velocityZ = 2.0f * random.nextFloat() - 1.0f;
                        this.level.addParticle((ParticleOptions)ParticleTypes.SCULK_CHARGE_POP, (double)pos.getX() + 0.5 + (double)(velocityX * spread), (double)pos.getY() + 0.5 + (double)(velocityY * spread), (double)pos.getZ() + 0.5 + (double)(velocityZ * spread), velocityX * 0.07f, velocityY * 0.07f, velocityZ * 0.07f);
                    }
                }
                break;
            }
            case 3007: {
                boolean isWaterlogged;
                for (int i = 0; i < 10; ++i) {
                    this.level.addParticle((ParticleOptions)new ShriekParticleOption(i * 5), (double)pos.getX() + 0.5, (double)pos.getY() + SculkShriekerBlock.TOP_Y, (double)pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                }
                BlockState state = this.level.getBlockState(pos);
                boolean bl = isWaterlogged = state.hasProperty((Property)BlockStateProperties.WATERLOGGED) && (Boolean)state.getValue((Property)BlockStateProperties.WATERLOGGED) != false;
                if (isWaterlogged) break;
                this.level.playLocalSound((double)pos.getX() + 0.5, (double)pos.getY() + SculkShriekerBlock.TOP_Y, (double)pos.getZ() + 0.5, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0f, 0.6f + random.nextFloat() * 0.4f, false);
                break;
            }
            case 3003: {
                ParticleUtils.spawnParticlesOnBlockFaces((Level)this.level, (BlockPos)pos, (ParticleOptions)ParticleTypes.WAX_ON, (IntProvider)UniformInt.of((int)3, (int)5));
                this.level.playLocalSound(pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                break;
            }
            case 3004: {
                ParticleUtils.spawnParticlesOnBlockFaces((Level)this.level, (BlockPos)pos, (ParticleOptions)ParticleTypes.WAX_OFF, (IntProvider)UniformInt.of((int)3, (int)5));
                break;
            }
            case 3005: {
                ParticleUtils.spawnParticlesOnBlockFaces((Level)this.level, (BlockPos)pos, (ParticleOptions)ParticleTypes.SCRAPE, (IntProvider)UniformInt.of((int)3, (int)5));
                break;
            }
            case 2008: {
                this.level.addParticle((ParticleOptions)ParticleTypes.EXPLOSION, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                break;
            }
            case 1500: {
                ComposterBlock.handleFill((Level)this.level, (BlockPos)pos, (data > 0 ? 1 : 0) != 0);
                break;
            }
            case 1504: {
                PointedDripstoneBlock.spawnDripParticle((Level)this.level, (BlockPos)pos, (BlockState)this.level.getBlockState(pos));
                break;
            }
            case 1501: {
                this.level.playLocalSound(pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f, false);
                for (int i = 0; i < 8; ++i) {
                    this.level.addParticle((ParticleOptions)ParticleTypes.LARGE_SMOKE, (double)pos.getX() + random.nextDouble(), (double)pos.getY() + 1.2, (double)pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
                }
                break;
            }
            case 1502: {
                this.level.playLocalSound(pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f, false);
                for (int i = 0; i < 5; ++i) {
                    double x = (double)pos.getX() + random.nextDouble() * 0.6 + 0.2;
                    double y = (double)pos.getY() + random.nextDouble() * 0.6 + 0.2;
                    double z = (double)pos.getZ() + random.nextDouble() * 0.6 + 0.2;
                    this.level.addParticle((ParticleOptions)ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                }
                break;
            }
            case 1503: {
                this.level.playLocalSound(pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                for (int i = 0; i < 16; ++i) {
                    double x = (double)pos.getX() + (5.0 + random.nextDouble() * 6.0) / 16.0;
                    double y = (double)pos.getY() + 0.8125;
                    double z = (double)pos.getZ() + (5.0 + random.nextDouble() * 6.0) / 16.0;
                    this.level.addParticle((ParticleOptions)ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                }
                break;
            }
            case 2006: {
                for (int i = 0; i < 200; ++i) {
                    float dist = random.nextFloat() * 4.0f;
                    float angle = random.nextFloat() * ((float)Math.PI * 2);
                    double velocityX = Mth.cos((double)angle) * dist;
                    double velocityY = 0.01 + random.nextDouble() * 0.5;
                    double velocityZ = Mth.sin((double)angle) * dist;
                    this.level.addParticle((ParticleOptions)PowerParticleOption.create((ParticleType)ParticleTypes.DRAGON_BREATH, (float)dist), (double)pos.getX() + velocityX * 0.1, (double)pos.getY() + 0.3, (double)pos.getZ() + velocityZ * 0.1, velocityX, velocityY, velocityZ);
                }
                if (data != 1) break;
                this.level.playLocalSound(pos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 2009: {
                for (int i = 0; i < 8; ++i) {
                    this.level.addParticle((ParticleOptions)ParticleTypes.CLOUD, (double)pos.getX() + random.nextDouble(), (double)pos.getY() + 1.2, (double)pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
                }
                break;
            }
            case 1009: {
                if (data == 0) {
                    this.level.playLocalSound(pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f, false);
                    break;
                }
                if (data != 1) break;
                this.level.playLocalSound(pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7f, 1.6f + (random.nextFloat() - random.nextFloat()) * 0.4f, false);
                break;
            }
            case 1029: {
                this.level.playLocalSound(pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1030: {
                this.level.playLocalSound(pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1044: {
                this.level.playLocalSound(pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1031: {
                this.level.playLocalSound(pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1039: {
                this.level.playLocalSound(pos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1010: {
                this.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(data).ifPresent(song -> this.playJukeboxSong((Holder<JukeboxSong>)song, pos));
                break;
            }
            case 1011: {
                this.stopJukeboxSongAndNotifyNearby(pos);
                break;
            }
            case 1015: {
                this.level.playLocalSound(pos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1017: {
                this.level.playLocalSound(pos, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1016: {
                this.level.playLocalSound(pos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1019: {
                this.level.playLocalSound(pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1022: {
                this.level.playLocalSound(pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1021: {
                this.level.playLocalSound(pos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1020: {
                this.level.playLocalSound(pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1018: {
                this.level.playLocalSound(pos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1024: {
                this.level.playLocalSound(pos, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1026: {
                this.level.playLocalSound(pos, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1027: {
                this.level.playLocalSound(pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1040: {
                this.level.playLocalSound(pos, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1041: {
                this.level.playLocalSound(pos, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1025: {
                this.level.playLocalSound(pos, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1042: {
                this.level.playLocalSound(pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1043: {
                this.level.playLocalSound(pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 3000: {
                this.level.addAlwaysVisibleParticle((ParticleOptions)ParticleTypes.EXPLOSION_EMITTER, true, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                this.level.playLocalSound(pos, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 10.0f, (1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f) * 0.7f, false);
                break;
            }
            case 3001: {
                this.level.playLocalSound(pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0f, 0.8f + random.nextFloat() * 0.3f, false);
                break;
            }
            case 1045: {
                this.level.playLocalSound(pos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1046: {
                this.level.playLocalSound(pos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1047: {
                this.level.playLocalSound(pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0f, random.nextFloat() * 0.1f + 0.9f, false);
                break;
            }
            case 1048: {
                this.level.playLocalSound(pos, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f, false);
                break;
            }
            case 1052: {
                this.level.playLocalSound(pos, SoundEvents.SULFUR_SPIKE_LAND, SoundSource.BLOCKS, 2.0f, random.nextFloat() * 0.1f + 0.9f, false);
            }
        }
    }

    private void shootParticles(int data, BlockPos pos, RandomSource random, SimpleParticleType particle) {
        Direction direction = Direction.from3DDataValue((int)data);
        int normalX = direction.getStepX();
        int normalY = direction.getStepY();
        int normalZ = direction.getStepZ();
        for (int i = 0; i < 10; ++i) {
            double pow = random.nextDouble() * 0.2 + 0.01;
            double x = (double)pos.getX() + (double)normalX * 0.6 + 0.5 + (double)normalX * 0.01 + (random.nextDouble() - 0.5) * (double)normalZ * 0.5;
            double y = (double)pos.getY() + (double)normalY * 0.6 + 0.5 + (double)normalY * 0.01 + (random.nextDouble() - 0.5) * (double)normalY * 0.5;
            double z = (double)pos.getZ() + (double)normalZ * 0.6 + 0.5 + (double)normalZ * 0.01 + (random.nextDouble() - 0.5) * (double)normalX * 0.5;
            double velocityX = (double)normalX * pow + random.nextGaussian() * 0.01;
            double velocityY = (double)normalY * pow + random.nextGaussian() * 0.01;
            double velocityZ = (double)normalZ * pow + random.nextGaussian() * 0.01;
            this.level.addParticle((ParticleOptions)particle, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    private void playJukeboxSong(Holder<JukeboxSong> songHolder, BlockPos pos) {
        this.stopJukeboxSong(pos);
        JukeboxSong song = (JukeboxSong)songHolder.value();
        SoundEvent sound = (SoundEvent)song.soundEvent().value();
        SimpleSoundInstance instance = SimpleSoundInstance.forJukeboxSong(sound, Vec3.atCenterOf((Vec3i)pos));
        this.playingJukeboxSongs.put(pos, instance);
        this.minecraft.getSoundManager().play(instance);
        this.minecraft.gui.hud.setNowPlaying(song.description());
        this.notifyNearbyEntities(this.level, pos, true);
    }

    private void stopJukeboxSong(BlockPos pos) {
        SoundInstance removedInstance = this.playingJukeboxSongs.remove(pos);
        if (removedInstance != null) {
            this.minecraft.getSoundManager().stop(removedInstance);
        }
    }

    private void stopJukeboxSongAndNotifyNearby(BlockPos pos) {
        this.stopJukeboxSong(pos);
        this.notifyNearbyEntities(this.level, pos, false);
    }

    private void notifyNearbyEntities(Level level, BlockPos pos, boolean isPlaying) {
        List entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0));
        for (LivingEntity entity : entities) {
            entity.setRecordPlayingNearby(pos, isPlaying);
        }
    }
}

