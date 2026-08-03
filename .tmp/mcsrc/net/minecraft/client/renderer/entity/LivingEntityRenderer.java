/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.math.Axis
 *  net.minecraft.core.Direction
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.component.ResolvableProfile
 *  net.minecraft.world.level.block.AbstractSkullBlock
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraft.world.scores.Team
 *  net.minecraft.world.scores.Team$Visibility
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
extends EntityRenderer<T, S>
implements RenderLayerParent<S, M> {
    private static final float EYE_BED_OFFSET = 0.1f;
    protected M model;
    protected final ItemModelResolver itemModelResolver;
    protected final List<RenderLayer<S, M>> layers = Lists.newArrayList();

    public LivingEntityRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.model = model;
        this.shadowRadius = shadow;
    }

    protected final boolean addLayer(RenderLayer<S, M> layer) {
        return this.layers.add(layer);
    }

    @Override
    public M getModel() {
        return this.model;
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity) {
        AABB aabb = super.getBoundingBoxForCulling(entity);
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is((Object)Items.DRAGON_HEAD)) {
            float extraSize = 0.5f;
            return aabb.inflate(0.5, 0.5, 0.5);
        }
        return aabb;
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Direction bedOrientation;
        poseStack.pushPose();
        if (((LivingEntityRenderState)state).hasPose(Pose.SLEEPING) && (bedOrientation = ((LivingEntityRenderState)state).bedOrientation) != null) {
            float headOffset = ((LivingEntityRenderState)state).eyeHeight - 0.1f;
            poseStack.translate((float)(-bedOrientation.getStepX()) * headOffset, 0.0f, (float)(-bedOrientation.getStepZ()) * headOffset);
        }
        float scale = ((LivingEntityRenderState)state).scale;
        poseStack.scale(scale, scale, scale);
        this.setupRotations(state, poseStack, ((LivingEntityRenderState)state).bodyRot, scale);
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        this.scale(state, poseStack);
        poseStack.translate(0.0f, -1.501f, 0.0f);
        boolean isBodyVisible = this.isBodyVisible(state);
        boolean forceTransparent = !isBodyVisible && !((LivingEntityRenderState)state).isInvisibleToPlayer;
        RenderType renderType = this.getRenderType(state, isBodyVisible, forceTransparent, ((EntityRenderState)state).appearsGlowing());
        if (renderType != null) {
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, this.getWhiteOverlayProgress(state));
            int baseColor = forceTransparent ? 0x26FFFFFF : -1;
            int tintedColor = ARGB.multiply((int)baseColor, (int)this.getModelTint(state));
            submitNodeCollector.submitModel(this.model, state, poseStack, renderType, ((LivingEntityRenderState)state).lightCoords, overlayCoords, tintedColor, (TextureAtlasSprite)null, ((LivingEntityRenderState)state).outlineColor, (ModelFeatureRenderer.CrumblingOverlay)null);
        }
        if (this.shouldRenderLayers(state) && !this.layers.isEmpty()) {
            ((Model)this.model).setupAnim(state);
            for (RenderLayer<S, M> layer : this.layers) {
                layer.submit(poseStack, submitNodeCollector, ((LivingEntityRenderState)state).lightCoords, state, ((LivingEntityRenderState)state).yRot, ((LivingEntityRenderState)state).xRot);
            }
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    protected boolean shouldRenderLayers(S state) {
        return true;
    }

    protected int getModelTint(S state) {
        return -1;
    }

    public abstract Identifier getTextureLocation(S var1);

    protected @Nullable RenderType getRenderType(S state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        Identifier texture = this.getTextureLocation(state);
        if (forceTransparent) {
            return RenderTypes.entityTranslucentCullItemTarget(texture);
        }
        if (isBodyVisible) {
            return ((Model)this.model).renderType(texture);
        }
        if (appearGlowing) {
            return RenderTypes.outline(texture);
        }
        return null;
    }

    public static int getOverlayCoords(LivingEntityRenderState state, float whiteOverlayProgress) {
        return OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress), OverlayTexture.v(state.hasRedOverlay));
    }

    protected boolean isBodyVisible(S state) {
        return !((LivingEntityRenderState)state).isInvisible;
    }

    private static float sleepDirectionToRotation(Direction direction) {
        return switch (direction) {
            case Direction.SOUTH -> 90.0f;
            case Direction.WEST -> 0.0f;
            case Direction.NORTH -> 270.0f;
            case Direction.EAST -> 180.0f;
            default -> 0.0f;
        };
    }

    protected boolean isShaking(S state) {
        return ((LivingEntityRenderState)state).isFullyFrozen;
    }

    protected void setupRotations(S state, PoseStack poseStack, float bodyRot, float entityScale) {
        if (this.isShaking(state)) {
            bodyRot += (float)(Math.cos((float)Mth.floor((float)((LivingEntityRenderState)state).ageInTicks) * 3.25f) * Math.PI * (double)0.4f);
        }
        if (!((LivingEntityRenderState)state).hasPose(Pose.SLEEPING)) {
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f - bodyRot));
        }
        if (((LivingEntityRenderState)state).deathTime > 0.0f) {
            float fall = (((LivingEntityRenderState)state).deathTime - 1.0f) / 20.0f * 1.6f;
            if ((fall = Mth.sqrt((float)fall)) > 1.0f) {
                fall = 1.0f;
            }
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(fall * this.getFlipDegrees()));
        } else if (((LivingEntityRenderState)state).isAutoSpinAttack) {
            poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-90.0f - ((LivingEntityRenderState)state).xRot));
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(((LivingEntityRenderState)state).ageInTicks * -75.0f));
        } else if (((LivingEntityRenderState)state).hasPose(Pose.SLEEPING)) {
            Direction bedOrientation = ((LivingEntityRenderState)state).bedOrientation;
            float angle = bedOrientation != null ? LivingEntityRenderer.sleepDirectionToRotation(bedOrientation) : bodyRot;
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(angle));
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(this.getFlipDegrees()));
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(270.0f));
        } else if (((LivingEntityRenderState)state).isUpsideDown) {
            poseStack.translate(0.0f, (((LivingEntityRenderState)state).boundingBoxHeight + 0.1f) / entityScale, 0.0f);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(180.0f));
        }
    }

    protected float getFlipDegrees() {
        return 90.0f;
    }

    protected float getWhiteOverlayProgress(S state) {
        return 0.0f;
    }

    protected void scale(S state, PoseStack poseStack) {
    }

    @Override
    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        boolean isVisibleToPlayer;
        if (entity.isDiscrete()) {
            float maxDist = 32.0f;
            if (distanceToCameraSq >= 1024.0) {
                return false;
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean bl = isVisibleToPlayer = !entity.isInvisibleTo((Player)player);
        if (entity != player) {
            PlayerTeam team = entity.getTeam();
            PlayerTeam myTeam = player.getTeam();
            if (team != null) {
                Team.Visibility visibility = team.getNameTagVisibility();
                return switch (visibility) {
                    default -> throw new MatchException(null, null);
                    case Team.Visibility.ALWAYS -> isVisibleToPlayer;
                    case Team.Visibility.NEVER -> false;
                    case Team.Visibility.HIDE_FOR_OTHER_TEAMS -> {
                        if (myTeam == null) {
                            yield isVisibleToPlayer;
                        }
                        if (team.isAlliedTo((Team)myTeam) && (team.canSeeFriendlyInvisibles() || isVisibleToPlayer)) {
                            yield true;
                        }
                        yield false;
                    }
                    case Team.Visibility.HIDE_FOR_OWN_TEAM -> myTeam == null ? isVisibleToPlayer : !team.isAlliedTo((Team)myTeam) && isVisibleToPlayer;
                };
            }
        }
        return !Minecraft.getInstance().gui.hud.isHidden() && entity != minecraft.getCameraEntity() && isVisibleToPlayer && !entity.isVehicle();
    }

    public boolean isEntityUpsideDown(T mob) {
        Component customName = mob.getCustomName();
        return customName != null && LivingEntityRenderer.isUpsideDownName(customName.getString());
    }

    protected static boolean isUpsideDownName(String name) {
        return "Dinnerbone".equals(name) || "Grumm".equals(name);
    }

    @Override
    protected float getShadowRadius(S state) {
        return super.getShadowRadius(state) * ((LivingEntityRenderState)state).scale;
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        BlockItem blockItem;
        super.extractRenderState(entity, state, partialTicks);
        float headRot = Mth.rotLerp((float)partialTicks, (float)((LivingEntity)entity).yHeadRotO, (float)((LivingEntity)entity).yHeadRot);
        ((LivingEntityRenderState)state).bodyRot = LivingEntityRenderer.solveBodyRot(entity, headRot, partialTicks);
        ((LivingEntityRenderState)state).yRot = Mth.wrapDegrees((float)(headRot - ((LivingEntityRenderState)state).bodyRot));
        ((LivingEntityRenderState)state).xRot = entity.getXRot(partialTicks);
        ((LivingEntityRenderState)state).isUpsideDown = this.isEntityUpsideDown(entity);
        if (((LivingEntityRenderState)state).isUpsideDown) {
            ((LivingEntityRenderState)state).xRot *= -1.0f;
            ((LivingEntityRenderState)state).yRot *= -1.0f;
        }
        if (!entity.isPassenger() && entity.isAlive()) {
            ((LivingEntityRenderState)state).walkAnimationPos = ((LivingEntity)entity).walkAnimation.position(partialTicks);
            ((LivingEntityRenderState)state).walkAnimationSpeed = ((LivingEntity)entity).walkAnimation.speed(partialTicks);
        } else {
            ((LivingEntityRenderState)state).walkAnimationPos = 0.0f;
            ((LivingEntityRenderState)state).walkAnimationSpeed = 0.0f;
        }
        Entity entity2 = entity.getVehicle();
        if (entity2 instanceof LivingEntity) {
            LivingEntity vehicle = (LivingEntity)entity2;
            ((LivingEntityRenderState)state).wornHeadAnimationPos = vehicle.walkAnimation.position(partialTicks);
        } else {
            ((LivingEntityRenderState)state).wornHeadAnimationPos = ((LivingEntityRenderState)state).walkAnimationPos;
        }
        ((LivingEntityRenderState)state).scale = entity.getScale();
        ((LivingEntityRenderState)state).ageScale = entity.getAgeScale();
        ((LivingEntityRenderState)state).pose = entity.getPose();
        ((LivingEntityRenderState)state).bedOrientation = entity.getBedOrientation();
        if (((LivingEntityRenderState)state).bedOrientation != null) {
            ((LivingEntityRenderState)state).eyeHeight = entity.getEyeHeight(Pose.STANDING);
        }
        ((LivingEntityRenderState)state).isFullyFrozen = entity.isFullyFrozen();
        ((LivingEntityRenderState)state).isBaby = entity.isBaby();
        ((LivingEntityRenderState)state).isInWater = entity.isInWater();
        ((LivingEntityRenderState)state).isAutoSpinAttack = entity.isAutoSpinAttack();
        ((LivingEntityRenderState)state).ticksSinceKineticHitFeedback = entity.getTicksSinceLastKineticHitFeedback(partialTicks);
        ((LivingEntityRenderState)state).hasRedOverlay = ((LivingEntity)entity).hurtTime > 0 || ((LivingEntity)entity).deathTime > 0;
        ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
        Item item = headItem.getItem();
        if (item instanceof BlockItem && (item = (blockItem = (BlockItem)item).getBlock()) instanceof AbstractSkullBlock) {
            AbstractSkullBlock skullBlock = (AbstractSkullBlock)item;
            ((LivingEntityRenderState)state).wornHeadType = skullBlock.getType();
            ((LivingEntityRenderState)state).wornHeadProfile = (ResolvableProfile)headItem.get(DataComponents.PROFILE);
            ((LivingEntityRenderState)state).headItem.clear();
        } else {
            ((LivingEntityRenderState)state).wornHeadType = null;
            ((LivingEntityRenderState)state).wornHeadProfile = null;
            if (!HumanoidArmorLayer.shouldRender(headItem, EquipmentSlot.HEAD)) {
                this.itemModelResolver.updateForLiving(((LivingEntityRenderState)state).headItem, headItem, ItemDisplayContext.HEAD, (LivingEntity)entity);
            } else {
                ((LivingEntityRenderState)state).headItem.clear();
            }
        }
        ((LivingEntityRenderState)state).deathTime = ((LivingEntity)entity).deathTime > 0 ? (float)((LivingEntity)entity).deathTime + partialTicks : 0.0f;
        Minecraft minecraft = Minecraft.getInstance();
        ((LivingEntityRenderState)state).isInvisibleToPlayer = ((LivingEntityRenderState)state).isInvisible && entity.isInvisibleTo((Player)minecraft.player);
    }

    @Override
    protected void extractNameTags(T entity, S state, float partialTicks) {
        double nameTagDistance = entity.getAttribute(Attributes.NAME_TAG_DISTANCE).getValue();
        double belowNameDistance = entity.getAttribute(Attributes.BELOW_NAME_DISTANCE).getValue();
        super.extractNameTags(entity, state, partialTicks, nameTagDistance, belowNameDistance);
    }

    private static float solveBodyRot(LivingEntity entity, float headRot, float partialTicks) {
        Entity entity2 = entity.getVehicle();
        if (entity2 instanceof LivingEntity) {
            LivingEntity riding = (LivingEntity)entity2;
            float bodyRot = Mth.rotLerp((float)partialTicks, (float)riding.yBodyRotO, (float)riding.yBodyRot);
            float maxHeadDiff = 85.0f;
            float headDiff = Mth.clamp((float)Mth.wrapDegrees((float)(headRot - bodyRot)), (float)-85.0f, (float)85.0f);
            bodyRot = headRot - headDiff;
            if (Math.abs(headDiff) > 50.0f) {
                bodyRot += headDiff * 0.2f;
            }
            return bodyRot;
        }
        return Mth.rotLerp((float)partialTicks, (float)entity.yBodyRotO, (float)entity.yBodyRot);
    }
}

