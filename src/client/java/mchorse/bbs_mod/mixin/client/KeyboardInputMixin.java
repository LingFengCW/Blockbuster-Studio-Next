package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 26.2: moveVector lives on the {@link ClientInput} superclass, so a
 * @Shadow on KeyboardInput cannot locate it (the field is only referenced,
 * not declared there). Extending ClientInput makes the field reachable
 * through normal inheritance instead of @Shadow.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput
{
    @Inject(method = "tick", at = @At("RETURN"))
    public void onTick(CallbackInfo info)
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (
            menu instanceof UIDashboard dashboard &&
            dashboard.getPanels().panel instanceof UIFilmPanel filmPanel &&
            filmPanel.getController().isControlling()
        ) {
            KeyboardInput input = (KeyboardInput) (Object) this;

            boolean forward = Window.isKeyPressed(GLFW.GLFW_KEY_W);
            boolean backward = Window.isKeyPressed(GLFW.GLFW_KEY_S);
            boolean left = Window.isKeyPressed(GLFW.GLFW_KEY_A);
            boolean right = Window.isKeyPressed(GLFW.GLFW_KEY_D);
            boolean jump = Window.isKeyPressed(GLFW.GLFW_KEY_SPACE);
            boolean shift = Window.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);

            input.keyPresses = new Input(forward, backward, left, right, jump, shift, false);

            float forwardImpulse = getMovementMultiplier(forward, backward);
            float leftImpulse = getMovementMultiplier(left, right);
            this.moveVector = new Vec2(leftImpulse, forwardImpulse);
        }
    }

    private static float getMovementMultiplier(boolean positive, boolean negative)
    {
        return positive == negative ? 0F : (positive ? 1F : -1F);
    }
}
