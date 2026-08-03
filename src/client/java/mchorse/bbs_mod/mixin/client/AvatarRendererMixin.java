package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.interps.Lerps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MC 26.2: players are rendered by AvatarRenderer (PlayerRenderer was
 * removed). The old PlayerRendererMixin is dead code, so morphing never
 * replaced the player model. extractRenderState() is the only hook that
 * still receives the actual entity (render states carry no entity
 * reference), so the entity is stashed and consumed by submit(), where
 * the morphed mob is submitted to the collector and the vanilla player
 * render is cancelled.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin
{
    @Unique
    private static final ThreadLocal<Player> bbs$currentPlayer = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Float> bbs$currentTick = new ThreadLocal<>();

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void bbs$onExtract(Entity entity, AvatarRenderState state, float partialTick, CallbackInfo ci)
    {
        if (entity instanceof Player player)
        {
            bbs$currentPlayer.set(player);
            bbs$currentTick.set(partialTick);
        }
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void bbs$onSubmit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci)
    {
        Player player = bbs$currentPlayer.get();
        float partialTick = bbs$currentTick.get() == null ? 0F : bbs$currentTick.get();

        bbs$currentPlayer.remove();
        bbs$currentTick.remove();

        if (player == null)
        {
            return;
        }

        if (MorphRenderer.hidePlayer)
        {
            if (FormUtilsClient.getCurrentForm() instanceof MobForm form && !form.isPlayer())
            {
                ci.cancel();

                return;
            }
        }

        Morph morph = Morph.getMorph(player);

        System.out.println("[BBS MORPH] AvatarRendererMixin.submit: player=" + player.getName().getString()
            + " morph=" + (morph == null ? "null" : (morph.getForm() == null ? "empty" : morph.getForm().getFormId())));

        if (morph == null || !(morph.getForm() instanceof MobForm mobForm))
        {
            return;
        }

        MobFormRenderer renderer = (MobFormRenderer) FormUtilsClient.getRenderer(mobForm);

        renderer.ensureEntity();

        Entity mobEntity = renderer.getEntity();

        if (mobEntity == null)
        {
            return;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        @SuppressWarnings("rawtypes")
        EntityRenderer mobRenderer = dispatcher.getRenderer(mobEntity);

        if (mobRenderer == null)
        {
            return;
        }

        /* Keep the mob's own render state (animation, pose) and submit it
         * through the collector, replacing the vanilla player render. */
        EntityRenderState mobState = mobRenderer.createRenderState();
        mobRenderer.extractRenderState(mobEntity, mobState, partialTick);

        float bodyYaw = Lerps.lerp(player.yBodyRotO, player.yBodyRot, partialTick);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

        mobRenderer.submit(mobState, poseStack, collector, cameraState);

        poseStack.popPose();

        ci.cancel();
    }
}
