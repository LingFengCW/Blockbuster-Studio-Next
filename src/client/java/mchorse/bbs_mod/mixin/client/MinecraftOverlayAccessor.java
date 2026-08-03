package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* MC 26.2 removed the public Minecraft.getOverlay() accessor; only the private
   "overlay" field and setOverlay(...) remain. We expose a read accessor so we can
   detect when the LoadingOverlay has finished (overlay == null), which is the point
   at which the item registry's Holder components are bound. Field name verified
   against the 26.2 clientOnly jar constant pool. */
@Mixin(Minecraft.class)
public interface MinecraftOverlayAccessor
{
    @Accessor("overlay")
    Overlay bbs$getOverlay();
}
