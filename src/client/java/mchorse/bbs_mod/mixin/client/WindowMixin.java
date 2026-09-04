package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import com.mojang.blaze3d.platform.Window;
import lingfeng.bbsnext.mcef.EditorBridge;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin
{
    @Shadow
    private long handle;

    @Shadow
    private int width;

    @Shadow
    private int height;

    /**
     * Intercept every call to Window.setTitle and replace the title with the
     * live BBS project + save-status title, so MC (loading screen, world name,
     * server name etc.) can never overwrite it.
     */
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    public void onSetTitle(CallbackInfo ci)
    {
        GLFW.glfwSetWindowTitle(this.handle, EditorBridge.getWindowTitle());
        ci.cancel();
    }

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    public void onGetWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue(BBSRendering.getVideoWidth());
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void onGetHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue(BBSRendering.getVideoHeight());
        }
    }
}
