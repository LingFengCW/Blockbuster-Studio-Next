/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.math.Axis
 *  com.mojang.math.Transformation
 *  net.minecraft.core.Direction
 *  net.minecraft.util.Unit
 *  net.minecraft.util.Util
 *  net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.CopperGolemStatueBlock
 *  net.minecraft.world.level.block.CopperGolemStatueBlock$Pose
 *  net.minecraft.world.level.block.WeatheringCopper$WeatherState
 *  net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.CopperGolemStatueRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class CopperGolemStatueBlockRenderer
implements BlockEntityRenderer<CopperGolemStatueBlockEntity, CopperGolemStatueRenderState> {
    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, CopperGolemStatueBlockRenderer::createModelTransformation);
    private final Map<CopperGolemStatueBlock.Pose, CopperGolemStatueModel> models = new HashMap<CopperGolemStatueBlock.Pose, CopperGolemStatueModel>();

    public CopperGolemStatueBlockRenderer(BlockEntityRendererProvider.Context context) {
        EntityModelSet modelSet = context.entityModelSet();
        this.models.put(CopperGolemStatueBlock.Pose.STANDING, new CopperGolemStatueModel(modelSet.bakeLayer(ModelLayers.COPPER_GOLEM)));
        this.models.put(CopperGolemStatueBlock.Pose.RUNNING, new CopperGolemStatueModel(modelSet.bakeLayer(ModelLayers.COPPER_GOLEM_RUNNING)));
        this.models.put(CopperGolemStatueBlock.Pose.SITTING, new CopperGolemStatueModel(modelSet.bakeLayer(ModelLayers.COPPER_GOLEM_SITTING)));
        this.models.put(CopperGolemStatueBlock.Pose.STAR, new CopperGolemStatueModel(modelSet.bakeLayer(ModelLayers.COPPER_GOLEM_STAR)));
    }

    @Override
    public CopperGolemStatueRenderState createRenderState() {
        return new CopperGolemStatueRenderState();
    }

    @Override
    public void extractRenderState(CopperGolemStatueBlockEntity blockEntity, CopperGolemStatueRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        WeatheringCopper.WeatherState weatherState;
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.direction = (Direction)blockState.getValue((Property)CopperGolemStatueBlock.FACING);
        state.pose = (CopperGolemStatueBlock.Pose)blockState.getValue((Property)CopperGolemStatueBlock.POSE);
        Block block = blockState.getBlock();
        if (block instanceof CopperGolemStatueBlock) {
            CopperGolemStatueBlock copperGolemStatueBlock = (CopperGolemStatueBlock)block;
            weatherState = copperGolemStatueBlock.getWeatheringState();
        } else {
            weatherState = WeatheringCopper.WeatherState.UNAFFECTED;
        }
        state.oxidationState = weatherState;
    }

    @Override
    public void submit(CopperGolemStatueRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(CopperGolemStatueBlockRenderer.modelTransformation(state.direction));
        CopperGolemStatueModel model = this.models.get(state.pose);
        submitNodeCollector.submitModel(model, Unit.INSTANCE, poseStack, CopperGolemOxidationLevels.getOxidationLevel((WeatheringCopper.WeatherState)state.oxidationState).texture(), state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
        poseStack.popPose();
    }

    public static Transformation modelTransformation(Direction facing) {
        return TRANSFORMATIONS.get(facing);
    }

    private static Transformation createModelTransformation(Direction entityDirection) {
        return new Transformation((Matrix4fc)new Matrix4f().translation(0.5f, 0.0f, 0.5f).rotate((Quaternionfc)Axis.YP.rotationDegrees(-entityDirection.getOpposite().toYRot())));
    }
}

