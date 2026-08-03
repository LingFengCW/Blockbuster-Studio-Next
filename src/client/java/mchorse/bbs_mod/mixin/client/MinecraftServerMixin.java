package mchorse.bbs_mod.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.utils.VideoRecorder;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
    /* MC 26.2: tick was renamed to tickServer and no longer re-invokes an
     * overload internally, so the whole method is wrapped instead of an
     * inner INVOKE. */
    @WrapMethod(method = "tickServer")
    private void onTick(BooleanSupplier supplier, Operation<Void> original)
    {
        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();

        if (videoRecorder.isRecording())
        {
            while (videoRecorder.lastServerTicks < videoRecorder.serverTicks)
            {
                original.call(supplier);

                videoRecorder.lastServerTicks += 1;
            }
        }
        else
        {
            original.call(supplier);
        }
    }
}
