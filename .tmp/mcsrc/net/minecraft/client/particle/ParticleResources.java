/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.data.AtlasIds
 *  net.minecraft.resources.FileToIdConverter
 *  net.minecraft.resources.Identifier
 *  net.minecraft.server.packs.resources.PreparableReloadListener
 *  net.minecraft.server.packs.resources.PreparableReloadListener$PreparationBarrier
 *  net.minecraft.server.packs.resources.PreparableReloadListener$SharedState
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.util.RandomSource
 *  net.minecraft.util.Util
 *  net.minecraft.util.profiling.Profiler
 *  net.minecraft.util.profiling.ProfilerFiller
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.client.particle.AshParticle;
import net.minecraft.client.particle.AttackSweepParticle;
import net.minecraft.client.particle.BlockMarker;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.BubblePopParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.DragonBreathParticle;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.DustColorTransitionParticle;
import net.minecraft.client.particle.DustParticle;
import net.minecraft.client.particle.DustPlumeParticle;
import net.minecraft.client.particle.ElderGuardianParticle;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.FallingDustParticle;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.FireflyParticle;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.FlyStraightTowardsParticle;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.particle.GeyserBaseParticle;
import net.minecraft.client.particle.GeyserEruptionParticle;
import net.minecraft.client.particle.GeyserPlumeParticle;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.GustParticle;
import net.minecraft.client.particle.GustSeedParticle;
import net.minecraft.client.particle.HeartParticle;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.LargeSmokeParticle;
import net.minecraft.client.particle.LavaParticle;
import net.minecraft.client.particle.NoteParticle;
import net.minecraft.client.particle.NoxiousGasCloudParticle;
import net.minecraft.client.particle.NoxiousGasParticle;
import net.minecraft.client.particle.ParticleDescription;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.PlayerCloudParticle;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.ReversePortalParticle;
import net.minecraft.client.particle.SculkChargeParticle;
import net.minecraft.client.particle.SculkChargePopParticle;
import net.minecraft.client.particle.ShriekParticle;
import net.minecraft.client.particle.SimpleVerticalParticle;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.client.particle.SnowflakeParticle;
import net.minecraft.client.particle.SonicBoomParticle;
import net.minecraft.client.particle.SoulParticle;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.client.particle.SpitParticle;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SquidInkParticle;
import net.minecraft.client.particle.SulfurBubbleParticle;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraft.client.particle.SuspendedTownParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.particle.TrailParticle;
import net.minecraft.client.particle.TrialSpawnerDetectionParticle;
import net.minecraft.client.particle.VibrationSignalParticle;
import net.minecraft.client.particle.WakeParticle;
import net.minecraft.client.particle.WaterCurrentDownParticle;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.client.particle.WhiteAshParticle;
import net.minecraft.client.particle.WhiteSmokeParticle;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParticleResources
implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json((String)"particles");
    private final Map<Identifier, MutableSpriteSet> spriteSets = Maps.newHashMap();
    private final Int2ObjectMap<ParticleProvider<?>> providers = new Int2ObjectOpenHashMap();
    private @Nullable Runnable onReload;

    public ParticleResources() {
        this.registerProviders();
    }

    public void onReload(Runnable onReload) {
        this.onReload = onReload;
    }

    private void registerProviders() {
        this.register((ParticleType)ParticleTypes.ANGRY_VILLAGER, HeartParticle.AngryVillagerProvider::new);
        this.register(ParticleTypes.BLOCK_MARKER, new BlockMarker.Provider());
        this.register(ParticleTypes.BLOCK, new TerrainParticle.Provider());
        this.register((ParticleType)ParticleTypes.BUBBLE, BubbleParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.BUBBLE_COLUMN_UP, BubbleColumnUpParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.BUBBLE_POP, BubblePopParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SULFUR_BUBBLES, SulfurBubbleParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.NOXIOUS_GAS, NoxiousGasParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.NOXIOUS_GAS_CLOUD, new NoxiousGasCloudParticle.Provider());
        this.register(ParticleTypes.GEYSER, new GeyserEruptionParticle.Provider());
        this.register(ParticleTypes.GEYSER_BASE, GeyserBaseParticle.Provider::new);
        this.register(ParticleTypes.GEYSER_POOF, GeyserBaseParticle.Provider::new);
        this.register(ParticleTypes.GEYSER_PLUME, GeyserPlumeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.CAMPFIRE_COSY_SMOKE, CampfireSmokeParticle.CosyProvider::new);
        this.register((ParticleType)ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, CampfireSmokeParticle.SignalProvider::new);
        this.register((ParticleType)ParticleTypes.CLOUD, PlayerCloudParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.COMPOSTER, SuspendedTownParticle.ComposterFillProvider::new);
        this.register((ParticleType)ParticleTypes.COPPER_FIRE_FLAME, FlameParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.CRIT, CritParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.CURRENT_DOWN, WaterCurrentDownParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.DAMAGE_INDICATOR, CritParticle.DamageIndicatorProvider::new);
        this.register(ParticleTypes.DRAGON_BREATH, DragonBreathParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.DOLPHIN, SuspendedTownParticle.DolphinSpeedProvider::new);
        this.register((ParticleType)ParticleTypes.DRIPPING_LAVA, DripParticle.LavaHangProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_LAVA, DripParticle.LavaFallProvider::new);
        this.register((ParticleType)ParticleTypes.LANDING_LAVA, DripParticle.LavaLandProvider::new);
        this.register((ParticleType)ParticleTypes.DRIPPING_WATER, DripParticle.WaterHangProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_WATER, DripParticle.WaterFallProvider::new);
        this.register(ParticleTypes.DUST, DustParticle.Provider::new);
        this.register(ParticleTypes.DUST_COLOR_TRANSITION, DustColorTransitionParticle.Provider::new);
        this.register(ParticleTypes.EFFECT, SpellParticle.InstantProvider::new);
        this.register((ParticleType)ParticleTypes.ELDER_GUARDIAN, new ElderGuardianParticle.Provider());
        this.register((ParticleType)ParticleTypes.ENCHANTED_HIT, CritParticle.MagicProvider::new);
        this.register((ParticleType)ParticleTypes.ENCHANT, FlyTowardsPositionParticle.EnchantProvider::new);
        this.register((ParticleType)ParticleTypes.END_ROD, EndRodParticle.Provider::new);
        this.register(ParticleTypes.ENTITY_EFFECT, SpellParticle.MobEffectProvider::new);
        this.register((ParticleType)ParticleTypes.EXPLOSION_EMITTER, new HugeExplosionSeedParticle.Provider());
        this.register((ParticleType)ParticleTypes.EXPLOSION, HugeExplosionParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SONIC_BOOM, SonicBoomParticle.Provider::new);
        this.register(ParticleTypes.FALLING_DUST, FallingDustParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.GUST, GustParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SMALL_GUST, GustParticle.SmallProvider::new);
        this.register((ParticleType)ParticleTypes.GUST_EMITTER_LARGE, new GustSeedParticle.Provider(3.0, 7, 0));
        this.register((ParticleType)ParticleTypes.GUST_EMITTER_SMALL, new GustSeedParticle.Provider(1.0, 3, 2));
        this.register((ParticleType)ParticleTypes.FIREWORK, FireworkParticles.SparkProvider::new);
        this.register((ParticleType)ParticleTypes.FISHING, WakeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.FLAME, FlameParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.INFESTED, SpellParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SCULK_SOUL, SoulParticle.EmissiveProvider::new);
        this.register(ParticleTypes.SCULK_CHARGE, SculkChargeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SCULK_CHARGE_POP, SculkChargePopParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SOUL, SoulParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SOUL_FIRE_FLAME, FlameParticle.Provider::new);
        this.register(ParticleTypes.FLASH, FireworkParticles.FlashProvider::new);
        this.register((ParticleType)ParticleTypes.HAPPY_VILLAGER, SuspendedTownParticle.HappyVillagerProvider::new);
        this.register((ParticleType)ParticleTypes.HEART, HeartParticle.Provider::new);
        this.register(ParticleTypes.INSTANT_EFFECT, SpellParticle.InstantProvider::new);
        this.register(ParticleTypes.ITEM, new BreakingItemParticle.Provider());
        this.register((ParticleType)ParticleTypes.ITEM_SLIME, new BreakingItemParticle.SlimeProvider());
        this.register((ParticleType)ParticleTypes.ITEM_COBWEB, new BreakingItemParticle.CobwebProvider());
        this.register((ParticleType)ParticleTypes.ITEM_SNOWBALL, new BreakingItemParticle.SnowballProvider());
        this.register((ParticleType)ParticleTypes.LARGE_SMOKE, LargeSmokeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.LAVA, LavaParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.MYCELIUM, SuspendedTownParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.NAUTILUS, FlyTowardsPositionParticle.NautilusProvider::new);
        this.register((ParticleType)ParticleTypes.NOTE, NoteParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.POOF, ExplodeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.PORTAL, PortalParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.RAIN, WaterDropParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SMOKE, SmokeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.WHITE_SMOKE, WhiteSmokeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SNEEZE, PlayerCloudParticle.SneezeProvider::new);
        this.register((ParticleType)ParticleTypes.SNOWFLAKE, SnowflakeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SPIT, SpitParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SWEEP_ATTACK, AttackSweepParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.TOTEM_OF_UNDYING, TotemParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SQUID_INK, SquidInkParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.UNDERWATER, SuspendedParticle.UnderwaterProvider::new);
        this.register((ParticleType)ParticleTypes.SPLASH, SplashParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.WITCH, SpellParticle.WitchProvider::new);
        this.register((ParticleType)ParticleTypes.DRIPPING_HONEY, DripParticle.HoneyHangProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_HONEY, DripParticle.HoneyFallProvider::new);
        this.register((ParticleType)ParticleTypes.LANDING_HONEY, DripParticle.HoneyLandProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_NECTAR, DripParticle.NectarFallProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_SPORE_BLOSSOM, DripParticle.SporeBlossomFallProvider::new);
        this.register((ParticleType)ParticleTypes.SPORE_BLOSSOM_AIR, SuspendedParticle.SporeBlossomAirProvider::new);
        this.register((ParticleType)ParticleTypes.ASH, AshParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.CRIMSON_SPORE, SuspendedParticle.CrimsonSporeProvider::new);
        this.register((ParticleType)ParticleTypes.WARPED_SPORE, SuspendedParticle.WarpedSporeProvider::new);
        this.register((ParticleType)ParticleTypes.DRIPPING_OBSIDIAN_TEAR, DripParticle.ObsidianTearHangProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_OBSIDIAN_TEAR, DripParticle.ObsidianTearFallProvider::new);
        this.register((ParticleType)ParticleTypes.LANDING_OBSIDIAN_TEAR, DripParticle.ObsidianTearLandProvider::new);
        this.register((ParticleType)ParticleTypes.REVERSE_PORTAL, ReversePortalParticle.ReversePortalProvider::new);
        this.register((ParticleType)ParticleTypes.WHITE_ASH, WhiteAshParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.SMALL_FLAME, FlameParticle.SmallFlameProvider::new);
        this.register((ParticleType)ParticleTypes.DRIPPING_DRIPSTONE_WATER, DripParticle.DripstoneWaterHangProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_DRIPSTONE_WATER, DripParticle.DripstoneWaterFallProvider::new);
        this.register((ParticleType)ParticleTypes.CHERRY_LEAVES, FallingLeavesParticle.CherryProvider::new);
        this.register((ParticleType)ParticleTypes.PALE_OAK_LEAVES, FallingLeavesParticle.PaleOakProvider::new);
        this.register(ParticleTypes.TINTED_LEAVES, FallingLeavesParticle.TintedLeavesProvider::new);
        this.register((ParticleType)ParticleTypes.DRIPPING_DRIPSTONE_LAVA, DripParticle.DripstoneLavaHangProvider::new);
        this.register((ParticleType)ParticleTypes.FALLING_DRIPSTONE_LAVA, DripParticle.DripstoneLavaFallProvider::new);
        this.register(ParticleTypes.VIBRATION, VibrationSignalParticle.Provider::new);
        this.register(ParticleTypes.TRAIL, TrailParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.PAUSE_MOB_GROWTH, SimpleVerticalParticle.PauseMobGrowthProvider::new);
        this.register((ParticleType)ParticleTypes.RESET_MOB_GROWTH, SimpleVerticalParticle.ResetMobGrowthProvider::new);
        this.register((ParticleType)ParticleTypes.GLOW_SQUID_INK, SquidInkParticle.GlowInkProvider::new);
        this.register((ParticleType)ParticleTypes.GLOW, GlowParticle.GlowSquidProvider::new);
        this.register((ParticleType)ParticleTypes.WAX_ON, GlowParticle.WaxOnProvider::new);
        this.register((ParticleType)ParticleTypes.WAX_OFF, GlowParticle.WaxOffProvider::new);
        this.register((ParticleType)ParticleTypes.ELECTRIC_SPARK, GlowParticle.ElectricSparkProvider::new);
        this.register((ParticleType)ParticleTypes.SCRAPE, GlowParticle.ScrapeProvider::new);
        this.register(ParticleTypes.SHRIEK, ShriekParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.EGG_CRACK, SuspendedTownParticle.EggCrackProvider::new);
        this.register((ParticleType)ParticleTypes.DUST_PLUME, DustPlumeParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, TrialSpawnerDetectionParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, TrialSpawnerDetectionParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.VAULT_CONNECTION, FlyTowardsPositionParticle.VaultConnectionProvider::new);
        this.register(ParticleTypes.DUST_PILLAR, new TerrainParticle.DustPillarProvider());
        this.register((ParticleType)ParticleTypes.RAID_OMEN, SpellParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.TRIAL_OMEN, SpellParticle.Provider::new);
        this.register((ParticleType)ParticleTypes.OMINOUS_SPAWNING, FlyStraightTowardsParticle.OminousSpawnProvider::new);
        this.register(ParticleTypes.BLOCK_CRUMBLE, new TerrainParticle.CrumblingProvider());
        this.register((ParticleType)ParticleTypes.FIREFLY, FireflyParticle.FireflyProvider::new);
        this.register((ParticleType)ParticleTypes.SULFUR_CUBE_GOO, BreakingItemParticle.SulfurCubeProvider::new);
    }

    private <T extends ParticleOptions> void register(ParticleType<T> type, ParticleProvider<T> provider) {
        this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(type), provider);
    }

    private <T extends ParticleOptions> void register(ParticleType<T> type, SpriteParticleRegistration<T> provider) {
        MutableSpriteSet spriteSet = new MutableSpriteSet();
        this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(type), spriteSet);
        this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(type), provider.create(spriteSet));
    }

    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        ResourceManager manager = currentReload.resourceManager();
        CompletionStage spriteSetsToLoad = CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(manager), taskExecutor).thenCompose(definitionsToScan -> {
            ArrayList loadTasks = new ArrayList(definitionsToScan.size());
            definitionsToScan.forEach((resourceId, resource) -> {
                Identifier particleId = PARTICLE_LISTER.fileToId(resourceId);
                loadTasks.add(CompletableFuture.supplyAsync(() -> {
                    record ParticleDefinition(Identifier id, Optional<List<Identifier>> sprites) {
                    }
                    return new ParticleDefinition(particleId, this.loadParticleDescription(particleId, (Resource)resource));
                }, taskExecutor));
            });
            return Util.sequence(loadTasks);
        });
        CompletableFuture<SpriteLoader.Preparations> pendingSprites = ((AtlasManager.PendingStitchResults)currentReload.get(AtlasManager.PENDING_STITCH)).get(AtlasIds.PARTICLES);
        return ((CompletableFuture)CompletableFuture.allOf(new CompletableFuture[]{spriteSetsToLoad, pendingSprites}).thenCompose(arg_0 -> ((PreparableReloadListener.PreparationBarrier)preparationBarrier).wait(arg_0))).thenAcceptAsync(arg_0 -> this.lambda$reload$4(pendingSprites, (CompletableFuture)spriteSetsToLoad, arg_0), reloadExecutor);
    }

    private Optional<List<Identifier>> loadParticleDescription(Identifier id, Resource resource) {
        Optional<List<Identifier>> optional;
        block9: {
            if (!this.spriteSets.containsKey(id)) {
                LOGGER.debug("Redundant texture list for particle: {}", (Object)id);
                return Optional.empty();
            }
            BufferedReader reader = resource.openAsReader();
            try {
                ParticleDescription description = ParticleDescription.fromJson(GsonHelper.parse((Reader)reader));
                optional = Optional.of(description.getTextures());
                if (reader == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    throw new IllegalStateException("Failed to load description for particle " + String.valueOf(id), e);
                }
            }
            ((Reader)reader).close();
        }
        return optional;
    }

    public Int2ObjectMap<ParticleProvider<?>> getProviders() {
        return this.providers;
    }

    private /* synthetic */ void lambda$reload$4(CompletableFuture pendingSprites, CompletableFuture spriteSetsToLoad, Void unused) {
        if (this.onReload != null) {
            this.onReload.run();
        }
        ProfilerFiller reloadProfiler = Profiler.get();
        reloadProfiler.push("upload");
        SpriteLoader.Preparations sprites = (SpriteLoader.Preparations)pendingSprites.join();
        reloadProfiler.popPush("bindSpriteSets");
        HashSet missingSprites = new HashSet();
        TextureAtlasSprite missingSprite = sprites.missing();
        ((List)spriteSetsToLoad.join()).forEach(p -> {
            Optional<List<Identifier>> spriteIds = p.sprites();
            if (spriteIds.isEmpty()) {
                return;
            }
            ArrayList<TextureAtlasSprite> contents = new ArrayList<TextureAtlasSprite>();
            for (Identifier spriteId : spriteIds.get()) {
                TextureAtlasSprite sprite = sprites.getSprite(spriteId);
                if (sprite == null) {
                    missingSprites.add(spriteId);
                    contents.add(missingSprite);
                    continue;
                }
                contents.add(sprite);
            }
            if (contents.isEmpty()) {
                contents.add(missingSprite);
            }
            this.spriteSets.get(p.id()).rebind(contents);
        });
        if (!missingSprites.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", (Object)missingSprites.stream().sorted().map(Identifier::toString).collect(Collectors.joining(",")));
        }
        reloadProfiler.pop();
    }

    @FunctionalInterface
    private static interface SpriteParticleRegistration<T extends ParticleOptions> {
        public ParticleProvider<T> create(SpriteSet var1);
    }

    private static class MutableSpriteSet
    implements SpriteSet {
        private List<TextureAtlasSprite> sprites;

        private MutableSpriteSet() {
        }

        @Override
        public TextureAtlasSprite get(int index, int max) {
            return this.sprites.get(index * (this.sprites.size() - 1) / max);
        }

        @Override
        public TextureAtlasSprite get(RandomSource random) {
            return this.sprites.get(random.nextInt(this.sprites.size()));
        }

        @Override
        public TextureAtlasSprite first() {
            return this.sprites.getFirst();
        }

        public void rebind(List<TextureAtlasSprite> ids) {
            this.sprites = ImmutableList.copyOf(ids);
        }
    }
}

