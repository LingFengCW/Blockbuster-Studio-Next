/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.primitives.Shorts
 *  com.google.common.primitives.SignedBytes
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  net.minecraft.SharedConstants
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.NonNullList
 *  net.minecraft.network.HashedPatchMap$HashGenerator
 *  net.minecraft.network.HashedStack
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerGamePacketListener
 *  net.minecraft.network.protocol.game.ServerboundAttackPacket
 *  net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
 *  net.minecraft.network.protocol.game.ServerboundContainerClickPacket
 *  net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket
 *  net.minecraft.network.protocol.game.ServerboundInteractPacket
 *  net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket
 *  net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket
 *  net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
 *  net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
 *  net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket
 *  net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
 *  net.minecraft.network.protocol.game.ServerboundUseItemPacket
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.stats.StatsCounter
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.InteractionResult$Success
 *  net.minecraft.world.InteractionResult$TryEmptyHandInteraction
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.HasCustomInventoryScreen
 *  net.minecraft.world.entity.player.Input
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ContainerInput
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.PiercingWeapon
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.item.crafting.display.RecipeDisplayId
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.GameMasterBlock
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.material.FluidState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MultiPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final ClientPacketListener connection;
    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
    private ItemStack destroyingItem = ItemStack.EMPTY;
    private float destroyProgress;
    private float destroyTicks;
    private int destroyDelay;
    private boolean isDestroying;
    private GameType localPlayerMode = GameType.DEFAULT_MODE;
    private @Nullable GameType previousLocalPlayerMode;
    private int carriedIndex;

    public MultiPlayerGameMode(Minecraft minecraft, ClientPacketListener connection) {
        this.minecraft = minecraft;
        this.connection = connection;
    }

    public void adjustPlayer(Player player) {
        this.localPlayerMode.updatePlayerAbilities(player.getAbilities());
    }

    public void setLocalMode(GameType mode, @Nullable GameType previousMode) {
        this.localPlayerMode = mode;
        this.previousLocalPlayerMode = previousMode;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public void setLocalMode(GameType mode) {
        if (mode != this.localPlayerMode) {
            this.previousLocalPlayerMode = this.localPlayerMode;
        }
        this.localPlayerMode = mode;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public boolean canHurtPlayer() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean destroyBlock(BlockPos pos) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pos, this.localPlayerMode)) {
            return false;
        }
        ClientLevel level = this.minecraft.level;
        BlockState oldState = level.getBlockState(pos);
        if (!this.minecraft.player.getMainHandItem().canDestroyBlock(oldState, (Level)level, pos, (Player)this.minecraft.player)) {
            return false;
        }
        Block oldBlock = oldState.getBlock();
        if (oldBlock instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
            return false;
        }
        if (oldState.isAir()) {
            return false;
        }
        oldBlock.playerWillDestroy((Level)level, pos, oldState, (Player)this.minecraft.player);
        FluidState fluidState = level.getFluidState(pos);
        boolean changed = level.setBlock(pos, fluidState.createLegacyBlock(), 11);
        if (changed) {
            oldBlock.destroy((LevelAccessor)level, pos, oldState);
        }
        if (SharedConstants.DEBUG_BLOCK_BREAK) {
            LOGGER.error("client broke {} {} -> {}", new Object[]{pos, oldState, level.getBlockState(pos)});
        }
        return changed;
    }

    public boolean startDestroyBlock(BlockPos pos, Direction direction) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pos, this.localPlayerMode)) {
            return false;
        }
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (this.minecraft.player.getAbilities().instabuild) {
            BlockState state = this.minecraft.level.getBlockState(pos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, 1.0f);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative start {} {}", (Object)pos, (Object)state);
            }
            this.startPrediction(this.minecraft.level, sequence -> {
                this.destroyBlock(pos);
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            this.destroyDelay = 5;
        } else if (!this.isDestroying || !this.sameDestroyTarget(pos)) {
            if (this.isDestroying) {
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Abort old break {} {}", (Object)pos, (Object)this.minecraft.level.getBlockState(pos));
                }
                this.connection.send((Packet<?>)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, direction));
            }
            BlockState state = this.minecraft.level.getBlockState(pos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, 0.0f);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Start break {} {}", (Object)pos, (Object)state);
            }
            this.startPrediction(this.minecraft.level, sequence -> {
                boolean notAir;
                boolean bl = notAir = !state.isAir();
                if (notAir && this.destroyProgress == 0.0f) {
                    state.attack((Level)this.minecraft.level, pos, (Player)this.minecraft.player);
                }
                if (notAir && state.getDestroyProgress((Player)this.minecraft.player, (BlockGetter)this.minecraft.player.level(), pos) >= 1.0f) {
                    this.destroyBlock(pos);
                } else {
                    this.isDestroying = true;
                    this.destroyBlockPos = pos;
                    this.destroyingItem = this.minecraft.player.getMainHandItem();
                    this.destroyProgress = 0.0f;
                    this.destroyTicks = 0.0f;
                    this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                }
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
        }
        return true;
    }

    public void stopDestroyBlock() {
        if (this.isDestroying) {
            BlockState state = this.minecraft.level.getBlockState(this.destroyBlockPos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, state, -1.0f);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Stop dest {} {}", (Object)this.destroyBlockPos, (Object)state);
            }
            this.connection.send((Packet<?>)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN));
            this.isDestroying = false;
            this.destroyProgress = 0.0f;
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
            this.minecraft.player.resetAttackStrengthTicker();
        }
    }

    public boolean continueDestroyBlock(BlockPos pos, Direction direction) {
        this.ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            return true;
        }
        if (this.minecraft.player.getAbilities().instabuild && this.minecraft.level.getWorldBorder().isWithinBounds(pos)) {
            this.destroyDelay = 5;
            BlockState state = this.minecraft.level.getBlockState(pos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, 1.0f);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative cont {} {}", (Object)pos, (Object)state);
            }
            this.startPrediction(this.minecraft.level, sequence -> {
                this.destroyBlock(pos);
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            return true;
        }
        if (this.sameDestroyTarget(pos)) {
            BlockState state = this.minecraft.level.getBlockState(pos);
            if (state.isAir()) {
                this.isDestroying = false;
                return false;
            }
            this.destroyProgress += state.getDestroyProgress((Player)this.minecraft.player, (BlockGetter)this.minecraft.player.level(), pos);
            if (this.destroyTicks % 4.0f == 0.0f) {
                SoundType soundType = state.getSoundType();
                this.minecraft.getSoundManager().play(new SimpleSoundInstance(soundType.getHitSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 8.0f, soundType.getPitch() * 0.5f, SoundInstance.createUnseededRandom(), pos));
            }
            this.destroyTicks += 1.0f;
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, Mth.clamp((float)this.destroyProgress, (float)0.0f, (float)1.0f));
            if (this.destroyProgress >= 1.0f) {
                this.isDestroying = false;
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Finished breaking {} {}", (Object)pos, (Object)state);
                }
                this.startPrediction(this.minecraft.level, sequence -> {
                    this.destroyBlock(pos);
                    return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                });
                this.destroyProgress = 0.0f;
                this.destroyTicks = 0.0f;
                this.destroyDelay = 5;
            }
        } else {
            return this.startDestroyBlock(pos, direction);
        }
        this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
        return true;
    }

    private void startPrediction(ClientLevel level, PredictiveAction predictiveAction) {
        try (BlockStatePredictionHandler prediction = level.getBlockStatePredictionHandler().startPredicting();){
            int sequence = prediction.currentSequence();
            Packet<ServerGamePacketListener> packetConcludingPrediction = predictiveAction.predict(sequence);
            this.connection.send(packetConcludingPrediction);
        }
    }

    public void tick() {
        this.ensureHasSentCarriedItem();
        if (this.connection.getConnection().isConnected()) {
            this.connection.getConnection().tick();
        } else {
            this.connection.getConnection().handleDisconnection();
        }
    }

    private boolean sameDestroyTarget(BlockPos pos) {
        ItemStack selected = this.minecraft.player.getMainHandItem();
        return pos.equals((Object)this.destroyBlockPos) && ItemStack.isSameItemSameComponents((ItemStack)selected, (ItemStack)this.destroyingItem);
    }

    private void ensureHasSentCarriedItem() {
        int index = this.minecraft.player.getInventory().getSelectedSlot();
        if (index != this.carriedIndex) {
            this.carriedIndex = index;
            this.connection.send((Packet<?>)new ServerboundSetCarriedItemPacket(this.carriedIndex));
        }
    }

    public InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit) {
        this.ensureHasSentCarriedItem();
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(blockHit.getBlockPos())) {
            return InteractionResult.FAIL;
        }
        MutableObject result = new MutableObject();
        this.startPrediction(this.minecraft.level, sequence -> {
            result.setValue((Object)this.performUseItemOn(player, hand, blockHit));
            return new ServerboundUseItemOnPacket(hand, blockHit, sequence);
        });
        return (InteractionResult)result.get();
    }

    private InteractionResult performUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit) {
        InteractionResult success;
        boolean suppressUsingBlock;
        BlockPos pos = blockHit.getBlockPos();
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.CONSUME;
        }
        boolean haveSomethingInOurHands = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        boolean bl = suppressUsingBlock = player.isSecondaryUseActive() && haveSomethingInOurHands;
        if (!suppressUsingBlock) {
            InteractionResult use;
            BlockState blockState = this.minecraft.level.getBlockState(pos);
            if (!this.connection.isFeatureEnabled(blockState.getBlock().requiredFeatures())) {
                return InteractionResult.FAIL;
            }
            InteractionResult itemUse = blockState.useItemOn(player.getItemInHand(hand), (Level)this.minecraft.level, (Player)player, hand, blockHit);
            if (itemUse.consumesAction()) {
                return itemUse;
            }
            if (itemUse instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND && (use = blockState.useWithoutItem((Level)this.minecraft.level, (Player)player, blockHit)).consumesAction()) {
                return use;
            }
        }
        if (itemStack.isEmpty() || player.getCooldowns().isOnCooldown(itemStack)) {
            return InteractionResult.PASS;
        }
        UseOnContext context = new UseOnContext((Player)player, hand, blockHit);
        if (player.hasInfiniteMaterials()) {
            int count = itemStack.getCount();
            success = itemStack.useOn(context);
            itemStack.setCount(count);
        } else {
            success = itemStack.useOn(context);
        }
        return success;
    }

    public InteractionResult useItem(Player player, InteractionHand hand) {
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        }
        this.ensureHasSentCarriedItem();
        MutableObject interactionResult = new MutableObject();
        this.startPrediction(this.minecraft.level, sequence -> {
            ItemStack result;
            ServerboundUseItemPacket packet = new ServerboundUseItemPacket(hand, sequence, player.getYRot(), player.getXRot());
            ItemStack itemStack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(itemStack)) {
                interactionResult.setValue((Object)InteractionResult.PASS);
                return packet;
            }
            InteractionResult resultHolder = itemStack.use((Level)this.minecraft.level, player, hand);
            if (resultHolder instanceof InteractionResult.Success) {
                InteractionResult.Success success = (InteractionResult.Success)resultHolder;
                result = Objects.requireNonNullElseGet(success.heldItemTransformedTo(), () -> player.getItemInHand(hand));
            } else {
                result = player.getItemInHand(hand);
            }
            if (result != itemStack) {
                player.setItemInHand(hand, result);
            }
            interactionResult.setValue((Object)resultHolder);
            return packet;
        });
        return (InteractionResult)interactionResult.get();
    }

    public LocalPlayer createPlayer(ClientLevel level, StatsCounter stats, ClientRecipeBook recipeBook) {
        return this.createPlayer(level, stats, recipeBook, Input.EMPTY, false);
    }

    public LocalPlayer createPlayer(ClientLevel level, StatsCounter stats, ClientRecipeBook recipeBook, Input lastSentInput, boolean wasSprinting) {
        return new LocalPlayer(this.minecraft, level, this.connection, stats, recipeBook, lastSentInput, wasSprinting, this.minecraft.computeChatAbilities());
    }

    public void attack(Player player, Entity entity) {
        this.ensureHasSentCarriedItem();
        this.connection.send((Packet<?>)new ServerboundAttackPacket(entity.getId()));
        player.attack(entity);
        player.resetAttackStrengthTicker();
    }

    public void spectate(Entity entity) {
        this.connection.send((Packet<?>)new ServerboundSpectatorActionPacket(OptionalInt.of(entity.getId())));
    }

    public void spectatorNoAction() {
        this.connection.send((Packet<?>)new ServerboundSpectatorActionPacket(OptionalInt.empty()));
    }

    public InteractionResult interact(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand) {
        this.ensureHasSentCarriedItem();
        Vec3 location = hitResult.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
        this.connection.send((Packet<?>)new ServerboundInteractPacket(entity.getId(), hand, location, player.isShiftKeyDown()));
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        }
        return player.interactOn(entity, hand, location);
    }

    public void handleContainerInput(int containerId, int slotNum, int buttonNum, ContainerInput containerInput, Player player) {
        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerId != containerMenu.containerId) {
            LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", (Object)containerId, (Object)containerMenu.containerId);
            return;
        }
        NonNullList slots = containerMenu.slots;
        int slotCount = slots.size();
        ArrayList itemsBeforeClick = Lists.newArrayListWithCapacity((int)slotCount);
        for (Slot slot : slots) {
            itemsBeforeClick.add(slot.getItem().copy());
        }
        containerMenu.clicked(slotNum, buttonNum, containerInput, player);
        Int2ObjectOpenHashMap changedSlots = new Int2ObjectOpenHashMap();
        for (int i = 0; i < slotCount; ++i) {
            ItemStack after;
            ItemStack before = (ItemStack)itemsBeforeClick.get(i);
            if (ItemStack.matches((ItemStack)before, (ItemStack)(after = ((Slot)slots.get(i)).getItem()))) continue;
            changedSlots.put(i, (Object)HashedStack.create((ItemStack)after, (HashedPatchMap.HashGenerator)this.connection.decoratedHashOpsGenenerator()));
        }
        HashedStack carriedItem = HashedStack.create((ItemStack)containerMenu.getCarried(), (HashedPatchMap.HashGenerator)this.connection.decoratedHashOpsGenenerator());
        this.connection.send((Packet<?>)new ServerboundContainerClickPacket(containerId, containerMenu.getStateId(), Shorts.checkedCast((long)slotNum), SignedBytes.checkedCast((long)buttonNum), containerInput, (Int2ObjectMap)changedSlots, carriedItem));
    }

    public void handlePlaceRecipe(int containerId, RecipeDisplayId recipe, boolean useMaxItems) {
        this.connection.send((Packet<?>)new ServerboundPlaceRecipePacket(containerId, recipe, useMaxItems));
    }

    public void handleInventoryButtonClick(int containerId, int buttonId) {
        this.connection.send((Packet<?>)new ServerboundContainerButtonClickPacket(containerId, buttonId));
    }

    public void handleCreativeModeItemAdd(ItemStack clicked, int slot) {
        if (this.minecraft.player.hasInfiniteMaterials() && this.connection.isFeatureEnabled(clicked.getItem().requiredFeatures())) {
            this.connection.send((Packet<?>)new ServerboundSetCreativeModeSlotPacket(slot, clicked));
        }
    }

    public void handleCreativeModeItemDrop(ItemStack clicked) {
        boolean hasOtherInventoryOpen;
        boolean bl = hasOtherInventoryOpen = this.minecraft.gui.screen() instanceof AbstractContainerScreen && !(this.minecraft.gui.screen() instanceof CreativeModeInventoryScreen);
        if (this.minecraft.player.hasInfiniteMaterials() && !hasOtherInventoryOpen && !clicked.isEmpty() && this.connection.isFeatureEnabled(clicked.getItem().requiredFeatures())) {
            this.connection.send((Packet<?>)new ServerboundSetCreativeModeSlotPacket(-1, clicked));
            this.minecraft.player.getDropSpamThrottler().increment();
        }
    }

    public void releaseUsingItem(Player player) {
        this.ensureHasSentCarriedItem();
        this.connection.send((Packet<?>)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        player.releaseUsingItem();
    }

    public void piercingAttack(PiercingWeapon weapon) {
        this.ensureHasSentCarriedItem();
        this.connection.send((Packet<?>)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN));
        this.minecraft.player.onAttack();
        this.minecraft.player.postPiercingAttack();
        weapon.makeSound((Entity)this.minecraft.player);
    }

    public boolean hasExperience() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean hasMissTime() {
        return !this.localPlayerMode.isCreative();
    }

    public boolean isServerControlledInventory() {
        return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
    }

    public boolean isSpectator() {
        return this.localPlayerMode == GameType.SPECTATOR;
    }

    public @Nullable GameType getPreviousPlayerMode() {
        return this.previousLocalPlayerMode;
    }

    public GameType getPlayerMode() {
        return this.localPlayerMode;
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public int getDestroyStage() {
        return this.destroyProgress > 0.0f ? (int)(this.destroyProgress * 10.0f) : -1;
    }

    public void handlePickItemFromBlock(BlockPos pos, boolean includeData) {
        this.connection.send((Packet<?>)new ServerboundPickItemFromBlockPacket(pos, includeData));
    }

    public void handlePickItemFromEntity(Entity entity, boolean includeData) {
        this.connection.send((Packet<?>)new ServerboundPickItemFromEntityPacket(entity.getId(), includeData));
    }

    public void handleSlotStateChanged(int slotId, int containerId, boolean newState) {
        this.connection.send((Packet<?>)new ServerboundContainerSlotStateChangedPacket(slotId, containerId, newState));
    }
}

