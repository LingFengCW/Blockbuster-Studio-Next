/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.math.Axis
 *  com.mojang.math.Transformation
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.block.PlainSignBlock
 *  net.minecraft.world.level.block.PlainSignBlock$Attachment
 *  net.minecraft.world.level.block.StandingSignBlock
 *  net.minecraft.world.level.block.WallSignBlock
 *  net.minecraft.world.level.block.entity.SignBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.RotationSegment
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.WallAndGroundTransformations;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.blockentity.state.StandingSignRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PlainSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class StandingSignRenderer
extends AbstractSignRenderer<StandingSignRenderState> {
    private static final float RENDER_SCALE = 0.6666667f;
    private static final Vector3fc TEXT_OFFSET = new Vector3f(0.0f, 0.33333334f, 0.046666667f);
    public static final WallAndGroundTransformations<SignRenderState.SignTransformations> TRANSFORMATIONS = new WallAndGroundTransformations<SignRenderState.SignTransformations>(StandingSignRenderer::createWallTransformation, StandingSignRenderer::createGroundTransformation, 16);

    public StandingSignRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public StandingSignRenderState createRenderState() {
        return new StandingSignRenderState();
    }

    @Override
    public void extractRenderState(SignBlockEntity blockEntity, StandingSignRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.attachmentType = PlainSignBlock.getAttachmentPoint((BlockState)blockState);
        state.transformations = blockState.getBlock() instanceof WallSignBlock ? TRANSFORMATIONS.wallTransformation((Direction)blockState.getValue((Property)WallSignBlock.FACING)) : TRANSFORMATIONS.freeTransformations((Integer)blockState.getValue((Property)StandingSignBlock.ROTATION));
    }

    private static Transformation textTransformation(PlainSignBlock.Attachment attachmentType, float angle, boolean isFrontText) {
        Matrix4f result = new Matrix4f().translate(0.5f, 0.5f, 0.5f).rotate((Quaternionfc)Axis.YP.rotationDegrees(-angle));
        if (attachmentType == PlainSignBlock.Attachment.WALL) {
            result.translate(0.0f, -0.3125f, -0.4375f);
        }
        if (!isFrontText) {
            result.rotate((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        }
        float s = 0.010416667f;
        return new Transformation((Matrix4fc)result.translate(TEXT_OFFSET).scale(0.010416667f, -0.010416667f, 0.010416667f));
    }

    private static SignRenderState.SignTransformations createTransformations(PlainSignBlock.Attachment attachmentType, float angle) {
        return new SignRenderState.SignTransformations(StandingSignRenderer.textTransformation(attachmentType, angle, true), StandingSignRenderer.textTransformation(attachmentType, angle, false));
    }

    private static SignRenderState.SignTransformations createGroundTransformation(int segment) {
        return StandingSignRenderer.createTransformations(PlainSignBlock.Attachment.GROUND, RotationSegment.convertToDegrees((int)segment));
    }

    private static SignRenderState.SignTransformations createWallTransformation(Direction direction) {
        return StandingSignRenderer.createTransformations(PlainSignBlock.Attachment.WALL, direction.toYRot());
    }
}

