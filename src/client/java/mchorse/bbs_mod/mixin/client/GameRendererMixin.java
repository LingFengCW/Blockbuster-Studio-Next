package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin
{
    /**
     * This injection cancels bobbing when camera controller takes over
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBob(CallbackInfo ci)
    {
        if (BBSModClient.getCameraController().getCurrent() != null)
        {
            ci.cancel();
        }
    }

    /**
     * This injection replaces the camera roll when camera controller takes
     * over. MC 26.2: tiltViewWhenHurt was renamed to bobHurt.
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onTiltViewWhenHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo info)
    {
        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() != null && !BBSRendering.isIrisShadowPass())
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(controller.getRoll()));

            info.cancel();
        }
    }

    /* MC 26.2: renderHand was renamed to renderItemInHand. */
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    public void onRenderHand(CameraRenderState cameraState, float deltaPartialTick, Matrix4fc modelViewMatrix, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof PlayCameraController)
        {
            info.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "renderLevel")
    private void onWorldRenderBegin(CallbackInfo callbackInfo)
    {
        BBSRendering.onLevelRenderBegin();
    }

    @Inject(at = @At("HEAD"), method = "renderLevel")
    private void onWorldRenderEnd(CallbackInfo callbackInfo)
    {
        BBSRendering.onLevelRenderEnd();
    }
}
