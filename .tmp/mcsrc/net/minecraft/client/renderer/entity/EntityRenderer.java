/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.LightCoordsUtil
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityAttachment
 *  net.minecraft.world.entity.Leashable
 *  net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
 *  net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.ChunkAccess
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5f;
    private static final float MAX_SHADOW_RADIUS = 32.0f;
    public static final float NAMETAG_SCALE = 0.025f;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    protected float shadowRadius;
    protected float shadowStrength = 1.0f;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.font = context.getFont();
    }

    public final int getPackedLightCoords(T entity, float partialTickTime) {
        BlockPos blockPos = BlockPos.containing((Position)entity.getLightProbePosition(partialTickTime));
        return LightCoordsUtil.pack((int)this.getBlockLightLevel(entity, blockPos), (int)this.getSkyLightLevel(entity, blockPos));
    }

    protected int getSkyLightLevel(T entity, BlockPos blockPos) {
        return entity.level().getBrightness(LightLayer.SKY, blockPos);
    }

    protected int getBlockLightLevel(T entity, BlockPos blockPos) {
        if (entity.isOnFire()) {
            return 15;
        }
        return entity.level().getBrightness(LightLayer.BLOCK, blockPos);
    }

    public boolean shouldRender(T entity, Frustum culler, double camX, double camY, double camZ) {
        Leashable leashable;
        Entity leashHolder;
        if (!entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (!this.affectedByCulling(entity)) {
            return true;
        }
        AABB boundingBox = this.getBoundingBoxForCulling(entity).inflate(0.5);
        if (boundingBox.hasNaN() || boundingBox.getSize() == 0.0) {
            boundingBox = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
        }
        if (culler.isVisible(boundingBox)) {
            return true;
        }
        if (entity instanceof Leashable && (leashHolder = (leashable = (Leashable)entity).getLeashHolder()) != null) {
            AABB leasherBox = this.entityRenderDispatcher.getRenderer(leashHolder).getBoundingBoxForCulling(leashHolder);
            return culler.isVisible(leasherBox) || culler.isVisible(boundingBox.minmax(leasherBox));
        }
        return false;
    }

    protected AABB getBoundingBoxForCulling(T entity) {
        return entity.getBoundingBox();
    }

    protected boolean affectedByCulling(T entity) {
        return true;
    }

    public Vec3 getRenderOffset(S state) {
        if (((EntityRenderState)state).passengerOffset != null) {
            return ((EntityRenderState)state).passengerOffset;
        }
        return Vec3.ZERO;
    }

    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (((EntityRenderState)state).leashStates != null) {
            for (EntityRenderState.LeashState leashState : ((EntityRenderState)state).leashStates) {
                submitNodeCollector.submitLeash(poseStack, leashState);
            }
        }
        this.submitNameDisplay(state, poseStack, submitNodeCollector, camera);
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    public Font getFont() {
        return this.font;
    }

    protected void submitNameDisplay(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.submitNameDisplay(state, poseStack, submitNodeCollector, camera, 0);
    }

    protected final <S extends EntityRenderState> void submitNameDisplay(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, int offset) {
        poseStack.pushPose();
        if (state.scoreText != null) {
            submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.scoreText, !state.isDiscrete, state.lightCoords, camera);
            Objects.requireNonNull(this.getFont());
            poseStack.translate(0.0f, 9.0f * 1.15f * 0.025f, 0.0f);
        }
        if (state.nameTag != null) {
            submitNodeCollector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.nameTag, !state.isDiscrete, state.lightCoords, camera);
        }
        poseStack.popPose();
    }

    protected @Nullable Component getNameTag(T entity) {
        return entity.getDisplayName();
    }

    protected float getShadowRadius(S state) {
        return this.shadowRadius;
    }

    protected float getShadowStrength(S state) {
        return this.shadowStrength;
    }

    public abstract S createRenderState();

    public final S createRenderState(T entity, float partialTicks) {
        S state = this.createRenderState();
        this.extractRenderState(entity, state, partialTicks);
        this.finalizeRenderState(entity, state);
        return state;
    }

    public void extractRenderState(T entity, S state, float partialTicks) {
        Leashable leashable;
        NewMinecartBehavior behavior;
        AbstractMinecart minecart;
        Entity entity2;
        ((EntityRenderState)state).entityType = entity.getType();
        ((EntityRenderState)state).x = Mth.lerp((double)partialTicks, (double)((Entity)entity).xOld, (double)entity.getX());
        ((EntityRenderState)state).y = Mth.lerp((double)partialTicks, (double)((Entity)entity).yOld, (double)entity.getY());
        ((EntityRenderState)state).z = Mth.lerp((double)partialTicks, (double)((Entity)entity).zOld, (double)entity.getZ());
        ((EntityRenderState)state).isInvisible = entity.isInvisible();
        ((EntityRenderState)state).ageInTicks = (float)((Entity)entity).tickCount + partialTicks;
        ((EntityRenderState)state).boundingBoxWidth = entity.getBbWidth();
        ((EntityRenderState)state).boundingBoxHeight = entity.getBbHeight();
        ((EntityRenderState)state).eyeHeight = entity.getEyeHeight();
        if (entity.isPassenger() && (entity2 = entity.getVehicle()) instanceof AbstractMinecart && (entity2 = (minecart = (AbstractMinecart)entity2).getBehavior()) instanceof NewMinecartBehavior && (behavior = (NewMinecartBehavior)entity2).cartHasPosRotLerp()) {
            double cartLerpX = Mth.lerp((double)partialTicks, (double)minecart.xOld, (double)minecart.getX());
            double cartLerpY = Mth.lerp((double)partialTicks, (double)minecart.yOld, (double)minecart.getY());
            double cartLerpZ = Mth.lerp((double)partialTicks, (double)minecart.zOld, (double)minecart.getZ());
            ((EntityRenderState)state).passengerOffset = behavior.getCartLerpPosition(partialTicks).subtract(new Vec3(cartLerpX, cartLerpY, cartLerpZ));
        } else {
            ((EntityRenderState)state).passengerOffset = null;
        }
        this.extractNameTags(entity, state, partialTicks);
        ((EntityRenderState)state).isDiscrete = entity.isDiscrete();
        Level level = entity.level();
        if (entity instanceof Leashable && (leashable = (Leashable)entity).getLeashHolder() != null) {
            int leashCount;
            Entity roper = leashable.getLeashHolder();
            float entityYRot = entity.getPreciseBodyRotation(partialTicks) * ((float)Math.PI / 180);
            Vec3 attachOffset = leashable.getLeashOffset(partialTicks);
            BlockPos entityEyePos = BlockPos.containing((Position)entity.getEyePosition(partialTicks));
            BlockPos roperEyePos = BlockPos.containing((Position)roper.getEyePosition(partialTicks));
            int startBlockLight = this.getBlockLightLevel(entity, entityEyePos);
            int endBlockLight = this.entityRenderDispatcher.getRenderer(roper).getBlockLightLevel(roper, roperEyePos);
            int startSkyLight = level.getBrightness(LightLayer.SKY, entityEyePos);
            int endSkyLight = level.getBrightness(LightLayer.SKY, roperEyePos);
            boolean quadConnection = roper.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
            int n = leashCount = quadConnection ? 4 : 1;
            if (((EntityRenderState)state).leashStates == null || ((EntityRenderState)state).leashStates.size() != leashCount) {
                ((EntityRenderState)state).leashStates = new ArrayList<EntityRenderState.LeashState>(leashCount);
                for (int i = 0; i < leashCount; ++i) {
                    ((EntityRenderState)state).leashStates.add(new EntityRenderState.LeashState());
                }
            }
            if (quadConnection) {
                float roperYRot = roper.getPreciseBodyRotation(partialTicks) * ((float)Math.PI / 180);
                Vec3 holderPos = roper.getPosition(partialTicks);
                Vec3[] leashableAttachmentPoints = leashable.getQuadLeashOffsets();
                Vec3[] roperAttachmentPoints = roper.getQuadLeashHolderOffsets();
                for (int i = 0; i < leashCount; ++i) {
                    EntityRenderState.LeashState leashState = ((EntityRenderState)state).leashStates.get(i);
                    leashState.offset = leashableAttachmentPoints[i].yRot(-entityYRot);
                    leashState.start = entity.getPosition(partialTicks).add(leashState.offset);
                    leashState.end = holderPos.add(roperAttachmentPoints[i].yRot(-roperYRot));
                    leashState.startBlockLight = startBlockLight;
                    leashState.endBlockLight = endBlockLight;
                    leashState.startSkyLight = startSkyLight;
                    leashState.endSkyLight = endSkyLight;
                    leashState.slack = false;
                }
            } else {
                Vec3 rotatedAttachOffset = attachOffset.yRot(-entityYRot);
                EntityRenderState.LeashState leashState = ((EntityRenderState)state).leashStates.getFirst();
                leashState.offset = rotatedAttachOffset;
                leashState.start = entity.getPosition(partialTicks).add(rotatedAttachOffset);
                leashState.end = roper.getRopeHoldPosition(partialTicks);
                leashState.startBlockLight = startBlockLight;
                leashState.endBlockLight = endBlockLight;
                leashState.startSkyLight = startSkyLight;
                leashState.endSkyLight = endSkyLight;
            }
        } else {
            ((EntityRenderState)state).leashStates = null;
        }
        ((EntityRenderState)state).displayFireAnimation = entity.displayFireAnimation();
        Minecraft minecraft = Minecraft.getInstance();
        boolean appearsGlowing = minecraft.shouldEntityAppearGlowing((Entity)entity);
        ((EntityRenderState)state).outlineColor = appearsGlowing ? ARGB.opaque((int)entity.getTeamColor()) : 0;
        ((EntityRenderState)state).lightCoords = this.getPackedLightCoords(entity, partialTicks);
    }

    protected void extractNameTags(T entity, S state, float partialTicks) {
        this.extractNameTags(entity, state, partialTicks, 64.0, 10.0);
    }

    protected final void extractNameTags(T entity, S state, float partialTicks, double nameTagDistance, double belowNameDistance) {
        if (this.entityRenderDispatcher.camera != null) {
            boolean shouldShowName;
            ((EntityRenderState)state).distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr((Entity)entity);
            boolean bl = shouldShowName = ((EntityRenderState)state).distanceToCameraSq < Mth.square((double)nameTagDistance) && this.shouldShowName(entity, ((EntityRenderState)state).distanceToCameraSq);
            if (shouldShowName) {
                ((EntityRenderState)state).nameTag = this.getNameTag(entity);
                ((EntityRenderState)state).nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
            } else {
                ((EntityRenderState)state).nameTag = null;
            }
            ((EntityRenderState)state).scoreText = ((EntityRenderState)state).distanceToCameraSq < Mth.square((double)belowNameDistance) ? entity.belowNameDisplay() : null;
        }
    }

    protected void finalizeRenderState(T entity, S state) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = entity.level();
        this.extractShadow(state, minecraft, level);
    }

    private void extractShadow(S state, Minecraft minecraft, Level level) {
        ((EntityRenderState)state).shadowPieces.clear();
        if (minecraft.options.entityShadows().get().booleanValue() && !((EntityRenderState)state).isInvisible) {
            double distSq;
            float pow;
            float shadowRadius;
            ((EntityRenderState)state).shadowRadius = shadowRadius = Math.min(this.getShadowRadius(state), 32.0f);
            if (shadowRadius > 0.0f && (pow = (float)((1.0 - (distSq = ((EntityRenderState)state).distanceToCameraSq) / 256.0) * (double)this.getShadowStrength(state))) > 0.0f) {
                int x0 = Mth.floor((double)(((EntityRenderState)state).x - (double)shadowRadius));
                int x1 = Mth.floor((double)(((EntityRenderState)state).x + (double)shadowRadius));
                int z0 = Mth.floor((double)(((EntityRenderState)state).z - (double)shadowRadius));
                int z1 = Mth.floor((double)(((EntityRenderState)state).z + (double)shadowRadius));
                float depth = Math.min(pow / 0.5f - 1.0f, shadowRadius);
                int y0 = Mth.floor((double)(((EntityRenderState)state).y - (double)depth));
                int y1 = Mth.floor((double)((EntityRenderState)state).y);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int z = z0; z <= z1; ++z) {
                    for (int x = x0; x <= x1; ++x) {
                        pos.set(x, 0, z);
                        ChunkAccess chunk = level.getChunk((BlockPos)pos);
                        for (int y = y0; y <= y1; ++y) {
                            pos.setY(y);
                            this.extractShadowPiece(state, level, pow, pos, chunk);
                        }
                    }
                }
            }
        } else {
            ((EntityRenderState)state).shadowRadius = 0.0f;
        }
    }

    private void extractShadowPiece(S state, Level level, float pow, BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        float powerAtDepth = pow - (float)(((EntityRenderState)state).y - (double)pos.getY()) * 0.5f;
        BlockPos belowPos = pos.below();
        BlockState belowState = chunk.getBlockState(belowPos);
        if (belowState.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        int brightness = level.getMaxLocalRawBrightness((BlockPos)pos);
        if (brightness <= 3) {
            return;
        }
        if (!belowState.isCollisionShapeFullBlock((BlockGetter)chunk, belowPos)) {
            return;
        }
        VoxelShape belowShape = belowState.getShape((BlockGetter)chunk, belowPos);
        if (belowShape.isEmpty()) {
            return;
        }
        float alpha = Mth.clamp((float)(powerAtDepth * 0.5f * Lightmap.getBrightness(level.dimensionType(), brightness)), (float)0.0f, (float)1.0f);
        float relativeX = (float)((double)pos.getX() - ((EntityRenderState)state).x);
        float relativeY = (float)((double)pos.getY() - ((EntityRenderState)state).y);
        float relativeZ = (float)((double)pos.getZ() - ((EntityRenderState)state).z);
        ((EntityRenderState)state).shadowPieces.add(new EntityRenderState.ShadowPiece(relativeX, relativeY, relativeZ, belowShape, alpha));
    }

    private static @Nullable Entity getServerSideEntity(Entity entity) {
        ServerLevel level;
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null && (level = server.getLevel(entity.level().dimension())) != null) {
            return level.getEntity(entity.getId());
        }
        return null;
    }
}

