package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin
{
    /* MC 26.2: setPos was renamed to setPosition; update now takes a
     * DeltaTracker. */
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "update", at = @At(value = "RETURN"))
    public void onUpdate(DeltaTracker deltaTracker, CallbackInfo ci)
    {
        CameraController controller = BBSModClient.getCameraController();
        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);

        controller.setup(controller.camera, tickDelta);

        if (controller.getCurrent() != null)
        {
            Vector3d position = controller.getPosition();
            float yaw = controller.getYaw();
            float pitch = controller.getPitch();

            this.setPosition(position.x, position.y, position.z);
            this.setRotation(yaw, pitch);
        }
    }
}
