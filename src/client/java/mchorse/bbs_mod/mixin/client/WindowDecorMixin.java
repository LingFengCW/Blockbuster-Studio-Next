package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.GpuBackend;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects a "create the game window undecorated" hook into the window
 * creation path.
 *
 * Minecraft builds its OS window in {@link Window#createGlfwWindow}. GLFW
 * only honours {@code GLFW_DECORATED} as a creation hint, so this has to be
 * set before {@code glfwCreateWindow} runs - which is exactly where this
 * injection sits. The result is a borderless window with no system title
 * bar; the mod is then free to draw its own window chrome on top.
 *
 * This lives in its own tiny mixin config ({@code bbs.window.mixins.json})
 * because {@code bbs.client.mixins.json} is intentionally NOT registered -
 * it contains client mixins targeting methods that no longer exist in 26.2
 * and enabling it would crash the game. Keeping this one separate lets the
 * undecorated-window hook load without pulling any of those in.
 */
@Mixin(Window.class)
public class WindowDecorMixin
{
    @Inject(method = "createGlfwWindow", at = @At("HEAD"))
    private static void bbs_makeUndecorated(int width, int height, String title, long monitor, GpuBackend backend, CallbackInfoReturnable<Long> cir)
    {
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
    }
}
