/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Direction$AxisDirection
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Position
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundClientCommandPacket
 *  net.minecraft.network.protocol.game.ServerboundClientCommandPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundContainerClosePacket
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Rot
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$StatusOnly
 *  net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
 *  net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket
 *  net.minecraft.network.protocol.game.ServerboundSwingPacket
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.server.dialog.Dialog
 *  net.minecraft.server.permissions.LevelBasedPermissionSet
 *  net.minecraft.server.permissions.PermissionSet
 *  net.minecraft.server.permissions.Permissions
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.stats.StatsCounter
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.util.Mth
 *  net.minecraft.util.TickThrottler
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntitySelector
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.entity.MoverType
 *  net.minecraft.world.entity.PlayerRideableJumping
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.animal.happyghast.HappyGhast
 *  net.minecraft.world.entity.animal.nautilus.AbstractNautilus
 *  net.minecraft.world.entity.player.Abilities
 *  net.minecraft.world.entity.player.Input
 *  net.minecraft.world.entity.projectile.ProjectileUtil
 *  net.minecraft.world.entity.vehicle.boat.AbstractBoat
 *  net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
 *  net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock
 *  net.minecraft.world.inventory.ClickAction
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.AttackRange
 *  net.minecraft.world.item.component.UseEffects
 *  net.minecraft.world.item.component.WritableBookContent
 *  net.minecraft.world.item.crafting.display.RecipeDisplayId
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.block.Portal$Transition
 *  net.minecraft.world.level.block.entity.CommandBlockEntity
 *  net.minecraft.world.level.block.entity.HangingSignBlockEntity
 *  net.minecraft.world.level.block.entity.JigsawBlockEntity
 *  net.minecraft.world.level.block.entity.SignBlockEntity
 *  net.minecraft.world.level.block.entity.StructureBlockEntity
 *  net.minecraft.world.level.block.entity.TestBlockEntity
 *  net.minecraft.world.level.block.entity.TestInstanceBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec2
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.player;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.TestBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.gui.screens.options.HasGamemasterPermissionReaction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingEntitySoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.TickThrottler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LocalPlayer
extends AbstractClientPlayer {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final int POSITION_REMINDER_INTERVAL = 20;
    private static final int WATER_VISION_MAX_TIME = 600;
    private static final int WATER_VISION_QUICK_TIME = 100;
    private static final float WATER_VISION_QUICK_PERCENT = 0.6f;
    private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35;
    private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962633907794952;
    public final ClientPacketListener connection;
    private final StatsCounter stats;
    private final ClientRecipeBook recipeBook;
    private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1280);
    private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
    private PermissionSet permissions = PermissionSet.NO_PERMISSIONS;
    private ChatAbilities chatAbilities;
    private double xLast;
    private double yLast;
    private double zLast;
    private float yRotLast;
    private float xRotLast;
    private boolean lastOnGround;
    private boolean lastHorizontalCollision;
    private boolean crouching;
    private boolean wasSprinting;
    private int positionReminder;
    private boolean flashOnSetHealth;
    public ClientInput input = new ClientInput();
    private Input lastSentInput;
    protected final Minecraft minecraft;
    protected int sprintTriggerTime;
    private static final int EXPERIENCE_DISPLAY_UNREADY_TO_SET = Integer.MIN_VALUE;
    private static final int EXPERIENCE_DISPLAY_READY_TO_SET = -2147483647;
    public int experienceDisplayStartTick = Integer.MIN_VALUE;
    public float yBob;
    public float xBob;
    public float yBobO;
    public float xBobO;
    private int jumpRidingTicks;
    private float jumpRidingScale;
    public float portalEffectIntensity;
    public float oPortalEffectIntensity;
    private boolean startedUsingItem;
    private @Nullable InteractionHand usingItemHand;
    private boolean handsBusy;
    private boolean autoJumpEnabled = true;
    private int autoJumpTime;
    private boolean wasFallFlying;
    private int waterVisionTime;
    private boolean showDeathScreen = true;
    private boolean doLimitedCrafting = false;

    public LocalPlayer(Minecraft minecraft, ClientLevel level, ClientPacketListener connection, StatsCounter stats, ClientRecipeBook recipeBook, Input lastSentInput, boolean wasSprinting, ChatAbilities chatAbilities) {
        super(level, connection.getLocalGameProfile());
        this.minecraft = minecraft;
        this.connection = connection;
        this.stats = stats;
        this.recipeBook = recipeBook;
        this.lastSentInput = lastSentInput;
        this.wasSprinting = wasSprinting;
        this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, minecraft.getSoundManager()));
        this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
        this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, minecraft.getSoundManager()));
        this.chatAbilities = chatAbilities;
    }

    public void heal(float heal) {
    }

    public boolean startRiding(Entity entity, boolean force, boolean sendEventAndTriggers) {
        if (!super.startRiding(entity, force, sendEventAndTriggers)) {
            return false;
        }
        if (entity instanceof AbstractMinecart) {
            AbstractMinecart minecart = (AbstractMinecart)entity;
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, minecart, true, SoundEvents.MINECART_INSIDE_UNDERWATER, 0.0f, 0.75f, 1.0f));
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, minecart, false, SoundEvents.MINECART_INSIDE, 0.0f, 0.75f, 1.0f));
        } else if (entity instanceof HappyGhast) {
            HappyGhast happyGhast = (HappyGhast)entity;
            this.minecraft.getSoundManager().play(new RidingEntitySoundInstance(this, (Entity)happyGhast, false, SoundEvents.HAPPY_GHAST_RIDING, happyGhast.getSoundSource(), 0.0f, 1.0f, 5.0f));
        } else if (entity instanceof AbstractNautilus) {
            AbstractNautilus nautilus = (AbstractNautilus)entity;
            this.minecraft.getSoundManager().play(new RidingEntitySoundInstance(this, (Entity)nautilus, true, SoundEvents.NAUTILUS_RIDING, nautilus.getSoundSource(), 0.0f, 1.0f, 5.0f));
        }
        return true;
    }

    public void removeVehicle() {
        super.removeVehicle();
        this.handsBusy = false;
    }

    public float getViewXRot(float a) {
        return this.getXRot();
    }

    public float getViewYRot(float a) {
        if (this.isPassenger()) {
            return super.getViewYRot(a);
        }
        return this.getYRot();
    }

    @Override
    public void tick() {
        if (!this.connection.hasClientLoaded()) {
            return;
        }
        this.dropSpamThrottler.tick();
        super.tick();
        if (!this.lastSentInput.equals((Object)this.input.keyPresses)) {
            this.connection.send((Packet<?>)new ServerboundPlayerInputPacket(this.input.keyPresses));
            this.lastSentInput = this.input.keyPresses;
        }
        if (this.isPassenger()) {
            this.connection.send((Packet<?>)new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
            Entity vehicle = this.getRootVehicle();
            if (vehicle != this && vehicle.isLocalInstanceAuthoritative()) {
                this.connection.send((Packet<?>)ServerboundMoveVehiclePacket.fromEntity((Entity)vehicle));
                this.sendIsSprintingIfNeeded();
            }
        } else {
            this.sendPosition();
        }
        for (AmbientSoundHandler soundHandler : this.ambientSoundHandlers) {
            soundHandler.tick();
        }
    }

    public float getCurrentMood() {
        for (AmbientSoundHandler ambientSoundHandler : this.ambientSoundHandlers) {
            if (!(ambientSoundHandler instanceof BiomeAmbientSoundsHandler)) continue;
            BiomeAmbientSoundsHandler biomeAmbientSoundsHandler = (BiomeAmbientSoundsHandler)ambientSoundHandler;
            return biomeAmbientSoundsHandler.getMoodiness();
        }
        return 0.0f;
    }

    private void sendPosition() {
        this.sendIsSprintingIfNeeded();
        if (this.isControlledCamera()) {
            boolean rot;
            double deltaX = this.getX() - this.xLast;
            double deltaY = this.getY() - this.yLast;
            double deltaZ = this.getZ() - this.zLast;
            double deltaYRot = this.getYRot() - this.yRotLast;
            double deltaXRot = this.getXRot() - this.xRotLast;
            ++this.positionReminder;
            boolean move = Mth.lengthSquared((double)deltaX, (double)deltaY, (double)deltaZ) > Mth.square((double)2.0E-4) || this.positionReminder >= 20;
            boolean bl = rot = deltaYRot != 0.0 || deltaXRot != 0.0;
            if (move && rot) {
                this.connection.send((Packet<?>)new ServerboundMovePlayerPacket.PosRot(this.position(), this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
            } else if (move) {
                this.connection.send((Packet<?>)new ServerboundMovePlayerPacket.Pos(this.position(), this.onGround(), this.horizontalCollision));
            } else if (rot) {
                this.connection.send((Packet<?>)new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
            } else if (this.lastOnGround != this.onGround() || this.lastHorizontalCollision != this.horizontalCollision) {
                this.connection.send((Packet<?>)new ServerboundMovePlayerPacket.StatusOnly(this.onGround(), this.horizontalCollision));
            }
            if (move) {
                this.xLast = this.getX();
                this.yLast = this.getY();
                this.zLast = this.getZ();
                this.positionReminder = 0;
            }
            if (rot) {
                this.yRotLast = this.getYRot();
                this.xRotLast = this.getXRot();
            }
            this.lastOnGround = this.onGround();
            this.lastHorizontalCollision = this.horizontalCollision;
            this.autoJumpEnabled = this.minecraft.options.autoJump().get();
        }
    }

    private void sendIsSprintingIfNeeded() {
        boolean isSprinting = this.isSprinting();
        if (isSprinting != this.wasSprinting) {
            ServerboundPlayerCommandPacket.Action action = isSprinting ? ServerboundPlayerCommandPacket.Action.START_SPRINTING : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
            this.connection.send((Packet<?>)new ServerboundPlayerCommandPacket((Entity)this, action));
            this.wasSprinting = isSprinting;
        }
    }

    public boolean drop(boolean all) {
        ServerboundPlayerActionPacket.Action action = all ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
        ItemStack prediction = this.getInventory().removeFromSelected(all);
        this.connection.send((Packet<?>)new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
        return !prediction.isEmpty();
    }

    public void swing(InteractionHand hand) {
        super.swing(hand);
        this.connection.send((Packet<?>)new ServerboundSwingPacket(hand));
    }

    public void respawn() {
        this.connection.send((Packet<?>)new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        KeyMapping.resetToggleKeys();
    }

    public void closeContainer() {
        this.connection.send((Packet<?>)new ServerboundContainerClosePacket(this.containerMenu.containerId));
        this.clientSideCloseContainer();
    }

    public void clientSideCloseContainer() {
        super.closeContainer();
        this.minecraft.gui.setScreen(null);
    }

    public void hurtTo(float newHealth) {
        if (this.flashOnSetHealth) {
            float dmg = this.getHealth() - newHealth;
            if (dmg <= 0.0f) {
                this.setHealth(newHealth);
                if (dmg < 0.0f) {
                    this.invulnerableTime = 10;
                }
            } else {
                this.lastHurt = dmg;
                this.invulnerableTime = 20;
                this.setHealth(newHealth);
                this.hurtTime = this.hurtDuration = 10;
            }
        } else {
            this.setHealth(newHealth);
            this.flashOnSetHealth = true;
        }
    }

    public void onUpdateAbilities() {
        this.connection.send((Packet<?>)new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
    }

    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        super.setReducedDebugInfo(reducedDebugInfo);
        this.minecraft.debugEntries.rebuildCurrentList();
    }

    public boolean isLocalPlayer() {
        return true;
    }

    public boolean isSuppressingSlidingDownLadder() {
        return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
    }

    public boolean canSpawnSprintParticle() {
        return !this.getAbilities().flying && super.canSpawnSprintParticle();
    }

    protected void sendRidingJump() {
        this.connection.send((Packet<?>)new ServerboundPlayerCommandPacket((Entity)this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor((float)(this.getJumpRidingScale() * 100.0f))));
    }

    public void sendOpenInventory() {
        this.connection.send((Packet<?>)new ServerboundPlayerCommandPacket((Entity)this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
    }

    public StatsCounter getStats() {
        return this.stats;
    }

    public ClientRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    public void removeRecipeHighlight(RecipeDisplayId recipe) {
        if (this.recipeBook.willHighlight(recipe)) {
            this.recipeBook.removeHighlight(recipe);
            this.connection.send((Packet<?>)new ServerboundRecipeBookSeenRecipePacket(recipe));
        }
    }

    public PermissionSet permissions() {
        return this.permissions;
    }

    public void setPermissions(PermissionSet newPermissions) {
        Screen screen;
        boolean previousGamemasterPermission = this.permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
        boolean newGamemasterPermission = newPermissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
        this.permissions = newPermissions;
        if (previousGamemasterPermission != newGamemasterPermission && (screen = this.minecraft.gui.screen()) instanceof HasGamemasterPermissionReaction) {
            HasGamemasterPermissionReaction screen2 = (HasGamemasterPermissionReaction)((Object)screen);
            screen2.onGamemasterPermissionChanged(newGamemasterPermission);
        }
    }

    public ChatAbilities chatAbilities() {
        return this.chatAbilities;
    }

    public void refreshChatAbilities() {
        this.chatAbilities = this.minecraft.computeChatAbilities();
        this.minecraft.gui.hud.getChat().setVisibleMessageFilter(this.chatAbilities.visibleMessagesFilter());
    }

    public void sendSystemMessage(Component message) {
        this.minecraft.gui.chatListener().handleSystemMessage(message, true);
    }

    public void sendOverlayMessage(Component message) {
        this.minecraft.gui.chatListener().handleOverlay(message);
    }

    private void moveTowardsClosestSpace(double x, double z) {
        Direction[] directions;
        BlockPos pos = BlockPos.containing((double)x, (double)this.getY(), (double)z);
        if (!this.suffocatesAt(pos)) {
            return;
        }
        double xd = x - (double)pos.getX();
        double zd = z - (double)pos.getZ();
        Direction dir = null;
        double closest = Double.MAX_VALUE;
        for (Direction direction : directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
            double distanceToEdge;
            double axisDistance = direction.getAxis().choose(xd, 0.0, zd);
            double d = distanceToEdge = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - axisDistance : axisDistance;
            if (!(distanceToEdge < closest) || this.suffocatesAt(pos.relative(direction))) continue;
            closest = distanceToEdge;
            dir = direction;
        }
        if (dir != null) {
            Vec3 oldMovement = this.getDeltaMovement();
            if (dir.getAxis() == Direction.Axis.X) {
                this.setDeltaMovement(0.1 * (double)dir.getStepX(), oldMovement.y, oldMovement.z);
            } else {
                this.setDeltaMovement(oldMovement.x, oldMovement.y, 0.1 * (double)dir.getStepZ());
            }
        }
    }

    private boolean suffocatesAt(BlockPos pos) {
        AABB boundingBox = this.getBoundingBox();
        AABB testArea = new AABB((double)pos.getX(), boundingBox.minY, (double)pos.getZ(), (double)pos.getX() + 1.0, boundingBox.maxY, (double)pos.getZ() + 1.0).deflate(1.0E-7);
        return this.level().collidesWithSuffocatingBlock((Entity)this, testArea);
    }

    public void setExperienceValues(float experienceProgress, int totalExp, int experienceLevel) {
        if (experienceProgress != this.experienceProgress) {
            this.setExperienceDisplayStartTickToTickCount();
        }
        this.experienceProgress = experienceProgress;
        this.totalExperience = totalExp;
        this.experienceLevel = experienceLevel;
    }

    private void setExperienceDisplayStartTickToTickCount() {
        this.experienceDisplayStartTick = this.experienceDisplayStartTick == Integer.MIN_VALUE ? -2147483647 : this.tickCount;
    }

    public void handleEntityEvent(byte id) {
        switch (id) {
            case 24: {
                this.setPermissions(PermissionSet.NO_PERMISSIONS);
                break;
            }
            case 25: {
                this.setPermissions((PermissionSet)LevelBasedPermissionSet.MODERATOR);
                break;
            }
            case 26: {
                this.setPermissions((PermissionSet)LevelBasedPermissionSet.GAMEMASTER);
                break;
            }
            case 27: {
                this.setPermissions((PermissionSet)LevelBasedPermissionSet.ADMIN);
                break;
            }
            case 28: {
                this.setPermissions((PermissionSet)LevelBasedPermissionSet.OWNER);
                break;
            }
            default: {
                super.handleEntityEvent(id);
            }
        }
    }

    public void setShowDeathScreen(boolean show) {
        this.showDeathScreen = show;
    }

    public boolean shouldShowDeathScreen() {
        return this.showDeathScreen;
    }

    public void setDoLimitedCrafting(boolean value) {
        this.doLimitedCrafting = value;
    }

    public boolean getDoLimitedCrafting() {
        return this.doLimitedCrafting;
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch, false);
    }

    public void startUsingItem(InteractionHand hand) {
        ItemStack itemStack = this.getItemInHand(hand);
        if (itemStack.isEmpty() || this.isUsingItem()) {
            return;
        }
        super.startUsingItem(hand);
        this.startedUsingItem = true;
        this.usingItemHand = hand;
    }

    public boolean isUsingItem() {
        return this.startedUsingItem;
    }

    private boolean isSlowDueToUsingItem() {
        return this.isUsingItem() && !((UseEffects)this.useItem.getOrDefault(DataComponents.USE_EFFECTS, (Object)UseEffects.DEFAULT)).canSprint();
    }

    private float itemUseSpeedMultiplier() {
        return ((UseEffects)this.useItem.getOrDefault(DataComponents.USE_EFFECTS, (Object)UseEffects.DEFAULT)).speedMultiplier();
    }

    public void stopUsingItem() {
        super.stopUsingItem();
        this.startedUsingItem = false;
    }

    public InteractionHand getUsedItemHand() {
        return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_LIVING_ENTITY_FLAGS.equals(accessor)) {
            InteractionHand serverUsingHand;
            boolean serverUsingItem = ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
            InteractionHand interactionHand = serverUsingHand = ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (serverUsingItem && !this.startedUsingItem) {
                this.startUsingItem(serverUsingHand);
            } else if (!serverUsingItem && this.startedUsingItem) {
                this.stopUsingItem();
            }
        }
        if (DATA_SHARED_FLAGS_ID.equals(accessor) && this.isFallFlying() && !this.wasFallFlying) {
            this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
        }
    }

    public @Nullable PlayerRideableJumping jumpableVehicle() {
        PlayerRideableJumping playerRideableJumping;
        Entity entity = this.getControlledVehicle();
        return entity instanceof PlayerRideableJumping && (playerRideableJumping = (PlayerRideableJumping)entity).canJump() ? playerRideableJumping : null;
    }

    public float getJumpRidingScale() {
        return this.jumpRidingScale;
    }

    public boolean isTextFilteringEnabled() {
        return this.minecraft.isTextFilteringEnabled();
    }

    public void openTextEdit(SignBlockEntity sign, boolean isFrontText) {
        if (sign instanceof HangingSignBlockEntity) {
            HangingSignBlockEntity hangingSign = (HangingSignBlockEntity)sign;
            this.minecraft.gui.setScreen(new HangingSignEditScreen((SignBlockEntity)hangingSign, isFrontText, this.minecraft.isTextFilteringEnabled()));
        } else {
            this.minecraft.gui.setScreen(new SignEditScreen(sign, isFrontText, this.minecraft.isTextFilteringEnabled()));
        }
    }

    public void openMinecartCommandBlock(MinecartCommandBlock commandBlock) {
        this.minecraft.gui.setScreen(new MinecartCommandBlockEditScreen(commandBlock));
    }

    public void openCommandBlock(CommandBlockEntity commandBlock) {
        this.minecraft.gui.setScreen(new CommandBlockEditScreen(commandBlock));
    }

    public void openStructureBlock(StructureBlockEntity structureBlock) {
        this.minecraft.gui.setScreen(new StructureBlockEditScreen(structureBlock));
    }

    public void openTestBlock(TestBlockEntity testBlock) {
        this.minecraft.gui.setScreen(new TestBlockEditScreen(testBlock));
    }

    public void openTestInstanceBlock(TestInstanceBlockEntity testInstanceBlock) {
        this.minecraft.gui.setScreen(new TestInstanceBlockEditScreen(testInstanceBlock));
    }

    public void openJigsawBlock(JigsawBlockEntity jigsawBlock) {
        this.minecraft.gui.setScreen(new JigsawBlockEditScreen(jigsawBlock));
    }

    public void openDialog(Holder<Dialog> dialog) {
        this.connection.showDialog(dialog, this.minecraft.gui.screen());
    }

    public void openItemGui(ItemStack itemStack, InteractionHand hand) {
        WritableBookContent content = (WritableBookContent)itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (content != null) {
            this.minecraft.gui.setScreen(new BookEditScreen(this, itemStack, hand, content));
        }
    }

    public void crit(Entity entity) {
        this.minecraft.particleEngine.createTrackingEmitter(entity, (ParticleOptions)ParticleTypes.CRIT);
    }

    public void magicCrit(Entity entity) {
        this.minecraft.particleEngine.createTrackingEmitter(entity, (ParticleOptions)ParticleTypes.ENCHANTED_HIT);
    }

    public boolean isShiftKeyDown() {
        return this.input.keyPresses.shift();
    }

    public boolean isCrouching() {
        return this.crouching;
    }

    public boolean isMovingSlowly() {
        return this.isCrouching() || this.isVisuallyCrawling();
    }

    public void applyInput() {
        if (this.isControlledCamera()) {
            Vec2 modifiedInput = this.modifyInput(this.input.getMoveVector());
            this.xxa = modifiedInput.x;
            this.zza = modifiedInput.y;
            this.jumping = this.input.keyPresses.jump();
            this.yBobO = this.yBob;
            this.xBobO = this.xBob;
            this.xBob += (this.getXRot() - this.xBob) * 0.5f;
            this.yBob += (this.getYRot() - this.yBob) * 0.5f;
        } else {
            super.applyInput();
        }
    }

    private Vec2 modifyInput(Vec2 input) {
        if (input.lengthSquared() == 0.0f) {
            return input;
        }
        Vec2 newInput = input.scale(0.98f);
        if (this.isUsingItem() && !this.isPassenger()) {
            newInput = newInput.scale(this.itemUseSpeedMultiplier());
        }
        if (this.isMovingSlowly()) {
            float sneakingMovementFactor = (float)this.getAttributeValue(Attributes.SNEAKING_SPEED);
            newInput = newInput.scale(sneakingMovementFactor);
        }
        return LocalPlayer.modifyInputSpeedForSquareMovement(newInput);
    }

    private static Vec2 modifyInputSpeedForSquareMovement(Vec2 input) {
        float length = input.length();
        if (length <= 0.0f) {
            return input;
        }
        Vec2 direction = input.scale(1.0f / length);
        float distanceToUnitSquare = LocalPlayer.distanceToUnitSquare(direction);
        float modifiedLength = Math.min(length * distanceToUnitSquare, 1.0f);
        return direction.scale(modifiedLength);
    }

    private static float distanceToUnitSquare(Vec2 direction) {
        float directionX = Math.abs(direction.x);
        float directionY = Math.abs(direction.y);
        float tan = directionY > directionX ? directionX / directionY : directionY / directionX;
        return Mth.sqrt((float)(1.0f + Mth.square((float)tan)));
    }

    protected boolean isControlledCamera() {
        return this.minecraft.getCameraEntity() == this;
    }

    public void resetPos() {
        this.setPose(Pose.STANDING);
        if (this.level() != null) {
            for (double testY = this.getY(); testY > (double)this.level().getMinY() && testY <= (double)this.level().getMaxY(); testY += 1.0) {
                this.setPos(this.getX(), testY, this.getZ());
                if (this.level().noCollision((Entity)this)) break;
            }
            this.setDeltaMovement(Vec3.ZERO);
            this.setXRot(0.0f);
        }
        this.setHealth(this.getMaxHealth());
        this.deathTime = 0;
    }

    @Override
    public void aiStep() {
        PlayerRideableJumping jumpableVehicle;
        if (this.sprintTriggerTime > 0) {
            --this.sprintTriggerTime;
        }
        if (!(this.minecraft.gui.screen() instanceof LevelLoadingScreen)) {
            this.handlePortalTransitionEffect(this.getActivePortalLocalTransition() == Portal.Transition.CONFUSION);
            this.processPortalCooldown();
        }
        boolean wasJumping = this.input.keyPresses.jump();
        boolean wasShiftKeyDown = this.input.keyPresses.shift();
        boolean hasForwardImpulse = this.input.hasForwardImpulse();
        Abilities abilities = this.getAbilities();
        this.crouching = !abilities.flying && !this.isSwimming() && !this.isPassenger() && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING) && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
        this.input.tick();
        this.minecraft.getTutorial().onInput(this.input);
        boolean wasAutoJump = false;
        if (this.autoJumpTime > 0) {
            --this.autoJumpTime;
            wasAutoJump = true;
            this.input.makeJump();
        }
        if (!this.noPhysics) {
            this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35, this.getZ() + (double)this.getBbWidth() * 0.35);
            this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35, this.getZ() - (double)this.getBbWidth() * 0.35);
            this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35, this.getZ() - (double)this.getBbWidth() * 0.35);
            this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35, this.getZ() + (double)this.getBbWidth() * 0.35);
        }
        if (wasShiftKeyDown || this.isSlowDueToUsingItem() && !this.isPassenger() || this.input.keyPresses.backward()) {
            this.sprintTriggerTime = 0;
        }
        if (this.canStartSprinting()) {
            if (!hasForwardImpulse) {
                if (this.sprintTriggerTime > 0) {
                    this.setSprinting(true);
                } else {
                    this.sprintTriggerTime = this.minecraft.options.sprintWindow().get();
                }
            }
            if (this.input.keyPresses.sprint()) {
                this.setSprinting(true);
            }
        }
        if (this.isSprinting()) {
            if (this.isSwimming()) {
                if (this.shouldStopSwimSprinting()) {
                    this.setSprinting(false);
                }
            } else if (this.shouldStopRunSprinting()) {
                this.setSprinting(false);
            }
        }
        boolean justToggledCreativeFlight = false;
        if (abilities.mayfly) {
            if (this.minecraft.gameMode.isSpectator()) {
                if (!abilities.flying) {
                    abilities.flying = true;
                    justToggledCreativeFlight = true;
                    this.onUpdateAbilities();
                }
            } else if (!wasJumping && this.input.keyPresses.jump() && !wasAutoJump) {
                if (this.jumpTriggerTime == 0) {
                    this.jumpTriggerTime = 7;
                } else if (!(this.isSwimming() || this.getVehicle() != null && this.jumpableVehicle() == null)) {
                    boolean bl = abilities.flying = !abilities.flying;
                    if (abilities.flying && this.onGround()) {
                        this.jumpFromGround();
                    }
                    justToggledCreativeFlight = true;
                    this.onUpdateAbilities();
                    this.jumpTriggerTime = 0;
                }
            }
        }
        if (this.input.keyPresses.jump() && !justToggledCreativeFlight && !wasJumping && !this.onClimbable() && this.tryToStartFallFlying()) {
            this.connection.send((Packet<?>)new ServerboundPlayerCommandPacket((Entity)this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
        this.wasFallFlying = this.isFallFlying();
        if (this.isInWater() && this.input.keyPresses.shift() && this.isAffectedByFluids()) {
            this.goDownInWater();
        }
        if (this.isEyeInFluid(FluidTags.WATER)) {
            int speed = this.isSpectator() ? 10 : 1;
            this.waterVisionTime = Mth.clamp((int)(this.waterVisionTime + speed), (int)0, (int)600);
        } else if (this.waterVisionTime > 0) {
            this.isEyeInFluid(FluidTags.WATER);
            this.waterVisionTime = Mth.clamp((int)(this.waterVisionTime - 10), (int)0, (int)600);
        }
        if (abilities.flying && this.isControlledCamera()) {
            int inputYa = 0;
            if (this.input.keyPresses.shift()) {
                --inputYa;
            }
            if (this.input.keyPresses.jump()) {
                ++inputYa;
            }
            if (inputYa != 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, (double)((float)inputYa * abilities.getFlyingSpeed() * 3.0f), 0.0));
            }
        }
        if ((jumpableVehicle = this.jumpableVehicle()) != null && jumpableVehicle.getJumpCooldown() == 0) {
            if (this.jumpRidingTicks < 0) {
                ++this.jumpRidingTicks;
                if (this.jumpRidingTicks == 0) {
                    this.jumpRidingScale = 0.0f;
                }
            }
            if (wasJumping && !this.input.keyPresses.jump()) {
                this.jumpRidingTicks = -10;
                jumpableVehicle.onPlayerJump(Mth.floor((float)(this.getJumpRidingScale() * 100.0f)));
                this.sendRidingJump();
            } else if (!wasJumping && this.input.keyPresses.jump()) {
                this.jumpRidingTicks = 0;
                this.jumpRidingScale = 0.0f;
            } else if (wasJumping) {
                ++this.jumpRidingTicks;
                this.jumpRidingScale = this.jumpRidingTicks < 10 ? (float)this.jumpRidingTicks * 0.1f : 0.8f + 2.0f / (float)(this.jumpRidingTicks - 9) * 0.1f;
            }
        } else {
            this.jumpRidingScale = 0.0f;
        }
        super.aiStep();
        if (this.onGround() && abilities.flying && !this.minecraft.gameMode.isSpectator()) {
            abilities.flying = false;
            this.onUpdateAbilities();
        }
    }

    private boolean shouldStopRunSprinting() {
        return !this.isSprintingPossible(this.getAbilities().flying) || !this.input.hasForwardImpulse() || this.horizontalCollision && !this.minorHorizontalCollision;
    }

    private boolean shouldStopSwimSprinting() {
        return !this.isSprintingPossible(true) || !this.isInWater() || !this.input.hasForwardImpulse() && !this.onGround() && !this.input.keyPresses.shift();
    }

    public Portal.Transition getActivePortalLocalTransition() {
        return this.portalProcess == null ? Portal.Transition.NONE : this.portalProcess.getPortalLocalTransition();
    }

    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    private void handlePortalTransitionEffect(boolean active) {
        this.oPortalEffectIntensity = this.portalEffectIntensity;
        float step = 0.0f;
        if (active && this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
            if (this.minecraft.gui.screen() != null && !this.minecraft.gui.screen().isAllowedInPortal()) {
                if (this.minecraft.gui.screen() instanceof AbstractContainerScreen) {
                    this.closeContainer();
                }
                this.minecraft.gui.setScreen(null);
            }
            if (this.portalEffectIntensity == 0.0f) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4f + 0.8f, 0.25f));
            }
            step = 0.0125f;
            this.portalProcess.setAsInsidePortalThisTick(false);
        } else if (this.portalEffectIntensity > 0.0f) {
            step = -0.05f;
        }
        this.portalEffectIntensity = Mth.clamp((float)(this.portalEffectIntensity + step), (float)0.0f, (float)1.0f);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.handsBusy = false;
        Entity entity = this.getControlledVehicle();
        if (entity instanceof AbstractBoat) {
            AbstractBoat boat = (AbstractBoat)entity;
            boat.setInput(this.input.keyPresses.left(), this.input.keyPresses.right(), this.input.keyPresses.forward(), this.input.keyPresses.backward());
            this.handsBusy |= this.input.keyPresses.left() || this.input.keyPresses.right() || this.input.keyPresses.forward() || this.input.keyPresses.backward();
        }
    }

    public boolean isHandsBusy() {
        return this.handsBusy;
    }

    public void move(MoverType moverType, Vec3 delta) {
        double prevX = this.getX();
        double prevZ = this.getZ();
        super.move(moverType, delta);
        float deltaX = (float)(this.getX() - prevX);
        float deltaZ = (float)(this.getZ() - prevZ);
        this.updateAutoJump(deltaX, deltaZ);
        this.addWalkedDistance(Mth.length((float)deltaX, (float)deltaZ) * 0.6f);
    }

    public boolean isAutoJumpEnabled() {
        return this.autoJumpEnabled;
    }

    public boolean shouldRotateWithMinecart() {
        return this.minecraft.options.rotateWithMinecart().get();
    }

    protected void updateAutoJump(float xa, float za) {
        if (!this.canAutoJump()) {
            return;
        }
        Vec3 moveBegin = this.position();
        Vec3 moveEnd = moveBegin.add((double)xa, 0.0, (double)za);
        Vec3 moveDiff = new Vec3((double)xa, 0.0, (double)za);
        float currentSpeed = this.getSpeed();
        float moveDistSq = (float)moveDiff.lengthSqr();
        if (moveDistSq <= 0.001f) {
            Vec2 move = this.input.getMoveVector();
            float inputXa = currentSpeed * move.x;
            float inputZa = currentSpeed * move.y;
            float sin = Mth.sin((double)(this.getYRot() * ((float)Math.PI / 180)));
            float cos = Mth.cos((double)(this.getYRot() * ((float)Math.PI / 180)));
            moveDiff = new Vec3((double)(inputXa * cos - inputZa * sin), moveDiff.y, (double)(inputZa * cos + inputXa * sin));
            moveDistSq = (float)moveDiff.lengthSqr();
            if (moveDistSq <= 0.001f) {
                return;
            }
        }
        float moveDistInverted = Mth.invSqrt((float)moveDistSq);
        Vec3 moveDir = moveDiff.scale((double)moveDistInverted);
        Vec3 facingDir3 = this.getForward();
        float facingVsMovingDotProduct2 = (float)(facingDir3.x * moveDir.x + facingDir3.z * moveDir.z);
        if (facingVsMovingDotProduct2 < -0.15f) {
            return;
        }
        CollisionContext context = CollisionContext.of((Entity)this);
        BlockPos ceilingPos = BlockPos.containing((double)this.getX(), (double)this.getBoundingBox().maxY, (double)this.getZ());
        BlockState aboveBlock1 = this.level().getBlockState(ceilingPos);
        if (!aboveBlock1.getCollisionShape((BlockGetter)this.level(), ceilingPos, context).isEmpty()) {
            return;
        }
        ceilingPos = ceilingPos.above();
        BlockState aboveBlock2 = this.level().getBlockState(ceilingPos);
        if (!aboveBlock2.getCollisionShape((BlockGetter)this.level(), ceilingPos, context).isEmpty()) {
            return;
        }
        float lookAheadSteps = 7.0f;
        float jumpHeight = 1.2f;
        if (this.hasEffect(MobEffects.JUMP_BOOST)) {
            jumpHeight += (float)(this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75f;
        }
        float lookAheadDist = Math.max(currentSpeed * 7.0f, 1.0f / moveDistInverted);
        Vec3 segBegin = moveBegin;
        Vec3 segEnd = moveEnd.add(moveDir.scale((double)lookAheadDist));
        float playerWidth = this.getBbWidth();
        float playerHeight = this.getBbHeight();
        AABB testBox = new AABB(segBegin, segEnd.add(0.0, (double)playerHeight, 0.0)).inflate((double)playerWidth, 0.0, (double)playerWidth);
        segBegin = segBegin.add(0.0, (double)0.51f, 0.0);
        segEnd = segEnd.add(0.0, (double)0.51f, 0.0);
        Vec3 rightDir = moveDir.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 rightOffset = rightDir.scale((double)(playerWidth * 0.5f));
        Vec3 leftSegBegin = segBegin.subtract(rightOffset);
        Vec3 leftSegEnd = segEnd.subtract(rightOffset);
        Vec3 rightSegBegin = segBegin.add(rightOffset);
        Vec3 rightSegEnd = segEnd.add(rightOffset);
        Iterable collisions = this.level().getCollisions((Entity)this, testBox);
        Iterator shape = StreamSupport.stream(collisions.spliterator(), false).flatMap(s -> s.toAabbs().stream()).iterator();
        float obstacleHeight = Float.MIN_VALUE;
        while (shape.hasNext()) {
            AABB box = (AABB)shape.next();
            if (!box.intersects(leftSegBegin, leftSegEnd) && !box.intersects(rightSegBegin, rightSegEnd)) continue;
            obstacleHeight = (float)box.maxY;
            Vec3 obstacleShapeCenter = box.getCenter();
            BlockPos obstacleBlockPos = BlockPos.containing((Position)obstacleShapeCenter);
            int steps = 1;
            while ((float)steps < jumpHeight) {
                BlockPos abovePos1 = obstacleBlockPos.above(steps);
                BlockState aboveBlock = this.level().getBlockState(abovePos1);
                VoxelShape blockShape = aboveBlock.getCollisionShape((BlockGetter)this.level(), abovePos1, context);
                if (!blockShape.isEmpty() && (double)(obstacleHeight = (float)blockShape.max(Direction.Axis.Y) + (float)abovePos1.getY()) - this.getY() > (double)jumpHeight) {
                    return;
                }
                if (steps > 1) {
                    ceilingPos = ceilingPos.above();
                    BlockState aboveBlock3 = this.level().getBlockState(ceilingPos);
                    if (!aboveBlock3.getCollisionShape((BlockGetter)this.level(), ceilingPos, context).isEmpty()) {
                        return;
                    }
                }
                ++steps;
            }
            break block0;
        }
        if (obstacleHeight == Float.MIN_VALUE) {
            return;
        }
        float ydelta = (float)((double)obstacleHeight - this.getY());
        if (ydelta <= 0.5f || ydelta > jumpHeight) {
            return;
        }
        this.autoJumpTime = 1;
    }

    protected boolean isHorizontalCollisionMinor(Vec3 movement) {
        float yRotInRadians = this.getYRot() * ((float)Math.PI / 180);
        double yRotSin = Mth.sin((double)yRotInRadians);
        double yRotCos = Mth.cos((double)yRotInRadians);
        double globalXA = (double)this.xxa * yRotCos - (double)this.zza * yRotSin;
        double globalZA = (double)this.zza * yRotCos + (double)this.xxa * yRotSin;
        double aLengthSquared = Mth.square((double)globalXA) + Mth.square((double)globalZA);
        double movementLengthSquared = Mth.square((double)movement.x) + Mth.square((double)movement.z);
        if (aLengthSquared < (double)1.0E-5f || movementLengthSquared < (double)1.0E-5f) {
            return false;
        }
        double dotProduct = globalXA * movement.x + globalZA * movement.z;
        double angleBetweenDesiredAndActualMovement = Math.acos(dotProduct / Math.sqrt(aLengthSquared * movementLengthSquared));
        return angleBetweenDesiredAndActualMovement < 0.13962633907794952;
    }

    private boolean canAutoJump() {
        return this.isAutoJumpEnabled() && this.autoJumpTime <= 0 && this.onGround() && !this.isStayingOnGroundSurface() && !this.isPassenger() && this.isMoving() && (double)this.getBlockJumpFactor() >= 1.0;
    }

    private boolean isMoving() {
        return this.input.getMoveVector().lengthSquared() > 0.0f;
    }

    private boolean isSprintingPossible(boolean allowedInShallowWater) {
        return !this.isMobilityRestricted() && (this.isPassenger() ? this.vehicleCanSprint(this.getVehicle()) : this.hasEnoughFoodToDoExhaustiveManoeuvres()) && (allowedInShallowWater || !this.isInShallowWater());
    }

    private boolean canStartSprinting() {
        return !(this.isSprinting() || !this.input.hasForwardImpulse() || !this.isSprintingPossible(this.getAbilities().flying) || this.isSlowDueToUsingItem() || this.isFallFlying() && !this.isUnderWater() || this.isMovingSlowly() && !this.isUnderWater());
    }

    private boolean vehicleCanSprint(Entity vehicle) {
        return vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative();
    }

    public float getWaterVision() {
        if (!this.isEyeInFluid(FluidTags.WATER)) {
            return 0.0f;
        }
        float max = 600.0f;
        float mid = 100.0f;
        if ((float)this.waterVisionTime >= 600.0f) {
            return 1.0f;
        }
        float a = Mth.clamp((float)((float)this.waterVisionTime / 100.0f), (float)0.0f, (float)1.0f);
        float b = (float)this.waterVisionTime < 100.0f ? 0.0f : Mth.clamp((float)(((float)this.waterVisionTime - 100.0f) / 500.0f), (float)0.0f, (float)1.0f);
        return a * 0.6f + b * 0.39999998f;
    }

    public void onGameModeChanged(GameType gameType) {
        if (gameType == GameType.SPECTATOR) {
            this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, 0.0));
        }
    }

    public boolean isUnderWater() {
        return this.wasUnderwater;
    }

    protected boolean updateIsUnderwater() {
        boolean oldIsUnderwater = this.wasUnderwater;
        boolean newIsUnderwater = super.updateIsUnderwater();
        if (this.isSpectator()) {
            return this.wasUnderwater;
        }
        if (!oldIsUnderwater && newIsUnderwater) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0f, 1.0f, false);
            this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
        }
        if (oldIsUnderwater && !newIsUnderwater) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0f, 1.0f, false);
        }
        return this.wasUnderwater;
    }

    public Vec3 getRopeHoldPosition(float partialTickTime) {
        if (this.minecraft.options.getCameraType().isFirstPerson()) {
            float yRot = Mth.lerp((float)(partialTickTime * 0.5f), (float)this.getYRot(), (float)this.yRotO) * ((float)Math.PI / 180);
            float xRot = Mth.lerp((float)(partialTickTime * 0.5f), (float)this.getXRot(), (float)this.xRotO) * ((float)Math.PI / 180);
            double handDir = this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
            Vec3 offset = new Vec3(0.39 * handDir, -0.6, 0.3);
            return offset.xRot(-xRot).yRot(-yRot).add(this.getEyePosition(partialTickTime));
        }
        return super.getRopeHoldPosition(partialTickTime);
    }

    public void updateTutorialInventoryAction(ItemStack itemCarried, ItemStack itemInSlot, ClickAction clickAction) {
        this.minecraft.getTutorial().onInventoryAction(itemCarried, itemInSlot, clickAction);
    }

    public float getVisualRotationYInDegrees() {
        return this.getYRot();
    }

    public void handleCreativeModeItemDrop(ItemStack stack) {
        this.minecraft.gameMode.handleCreativeModeItemDrop(stack);
    }

    public boolean canDropItems() {
        return this.dropSpamThrottler.isUnderThreshold();
    }

    public TickThrottler getDropSpamThrottler() {
        return this.dropSpamThrottler;
    }

    public Input getLastSentInput() {
        return this.lastSentInput;
    }

    public HitResult raycastHitResult(float a, Entity cameraEntity) {
        ItemStack itemStack = this.getActiveItem();
        AttackRange itemAttackRange = (AttackRange)itemStack.get(DataComponents.ATTACK_RANGE);
        double blockInteractionRange = this.blockInteractionRange();
        HitResult hitResult = null;
        if (itemAttackRange != null && (hitResult = itemAttackRange.getClosesetHit(cameraEntity, a, EntitySelector.CAN_BE_PICKED)) instanceof BlockHitResult) {
            hitResult = LocalPlayer.filterHitResult(hitResult, cameraEntity.getEyePosition(a), blockInteractionRange);
        }
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            double entityInteractionRange = this.entityInteractionRange();
            hitResult = LocalPlayer.pick(cameraEntity, blockInteractionRange, entityInteractionRange, a);
        }
        return hitResult;
    }

    private static HitResult pick(Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float partialTicks) {
        double maxDistance = Math.max(blockInteractionRange, entityInteractionRange);
        double maxDistanceSq = Mth.square((double)maxDistance);
        Vec3 from = cameraEntity.getEyePosition(partialTicks);
        HitResult blockHitResult = cameraEntity.pick(maxDistance, partialTicks, false);
        double blockDistanceSq = blockHitResult.getLocation().distanceToSqr(from);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            maxDistanceSq = blockDistanceSq;
            maxDistance = Math.sqrt(maxDistanceSq);
        }
        Vec3 direction = cameraEntity.getViewVector(partialTicks);
        Vec3 to = from.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance);
        float overlap = 1.0f;
        AABB box = cameraEntity.getBoundingBox().expandTowards(direction.scale(maxDistance)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult((Entity)cameraEntity, (Vec3)from, (Vec3)to, (AABB)box, (Predicate)EntitySelector.CAN_BE_PICKED, (double)maxDistanceSq);
        if (entityHitResult != null && entityHitResult.getLocation().distanceToSqr(from) < blockDistanceSq) {
            return LocalPlayer.filterHitResult((HitResult)entityHitResult, from, entityInteractionRange);
        }
        return LocalPlayer.filterHitResult(blockHitResult, from, blockInteractionRange);
    }

    private static HitResult filterHitResult(HitResult hitResult, Vec3 from, double maxRange) {
        Vec3 hitLocation = hitResult.getLocation();
        if (!hitLocation.closerThan((Position)from, maxRange)) {
            Vec3 location = hitResult.getLocation();
            Direction direction = Direction.getApproximateNearest((double)(location.x - from.x), (double)(location.y - from.y), (double)(location.z - from.z));
            return BlockHitResult.miss((Vec3)location, (Direction)direction, (BlockPos)BlockPos.containing((Position)location));
        }
        return hitResult;
    }
}

