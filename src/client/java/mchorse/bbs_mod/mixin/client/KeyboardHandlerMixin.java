package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin
{
    /* MC 26.2: onKey was renamed to keyPress with a KeyEvent payload. The
     * TAIL injection that triggered morph animation states was removed with
     * the morphing feature. */
    @Inject(method = "keyPress", at = @At("HEAD"))
    public void onOnKey(long window, int action, KeyEvent event, CallbackInfo info)
    {
        BBSRendering.lastAction = action;
    }
}
