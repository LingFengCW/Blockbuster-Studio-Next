package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.blocks.InteractBlockActionClip;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin
{
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChatCommand", at = @At("HEAD"))
    public void onHandleChatCommand(ServerboundChatCommandPacket packet, CallbackInfo info)
    {
        BBSMod.getActions().addAction(this.player, () ->
        {
            CommandActionClip clip = new CommandActionClip();

            clip.command.set(packet.command());

            return clip;
        });
    }

    // [MC 26.2 REMOVED] onPlayerInteractBlock was removed from ServerGamePacketListenerImpl
    // Block interaction handling needs to be re-implemented using the new handleUseItem/handleInteract methods
    // @Redirect(method = "onPlayerInteractBlock", ...
    // private InteractionResult redirectOnBlockInteract(ServerPlayerGameMode manager, ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult)
    // {
    //     BBSMod.getActions().addAction(this.player, () ->
    //     {
    //         InteractBlockActionClip clip = new InteractBlockActionClip();
    //
    //         clip.hit.setHitResult(hitResult);
    //         clip.hand.set(hand == InteractionHand.MAIN_HAND);
    //
    //         return clip;
    //     });
    //
    //     return manager.useItemOn(player, world, stack, hand, hitResult);
    // }
}