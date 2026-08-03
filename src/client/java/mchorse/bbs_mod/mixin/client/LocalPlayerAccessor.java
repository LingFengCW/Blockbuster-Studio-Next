package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor
{
    /* Mojmap field name is "crouching" ("inSneakingPose" was the old Yarn name).
       Verified against the 26.2 clientOnly jar constant pool. */
    @Accessor("crouching")
    public void bbs$setIsSneakingPose(boolean sneaking);
}


