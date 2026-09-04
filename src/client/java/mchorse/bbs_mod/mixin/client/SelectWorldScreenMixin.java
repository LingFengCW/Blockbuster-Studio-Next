package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hide BBS's reserved preview world (bbs_preview) from the single-player
 * "Select World" list, so players can neither see nor enter it there.
 * The BBS editor's own world picker already filters it on the Java side
 * (EditorBridge.isReservedWorld), this covers the vanilla single-player UI.
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin
{
    @Shadow
    protected WorldSelectionList list;

    private static final String RESERVED = "bbs_preview";

    @Inject(method = "init", at = @At("TAIL"))
    private void bbsHideReservedWorld(CallbackInfo ci)
    {
        if (this.list == null)
        {
            return;
        }

        /* children() is typed as List<Entry> (the abstract base); the
         * concrete WorldListEntry carries getLevelName(), so filter by type. */
        List entries = this.list.children();

        if (entries == null)
        {
            return;
        }

        entries.removeIf(entry ->
        {
            if (!(entry instanceof WorldSelectionList.WorldListEntry))
            {
                return false;
            }

            String name = ((WorldSelectionList.WorldListEntry) entry).getLevelName();
            return name != null && RESERVED.equals(name);
        });
    }
}
