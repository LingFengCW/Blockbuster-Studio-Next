package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.client.renderer.PlayerMorphCapture;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.interps.Lerps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the inherited LivingEntityRenderer.submit — the real body-draw entry point
 * for the player, since AvatarRenderer (which renders the player in 26.2) does not
 * override it. The active morph (MobForm / ModelForm) replaces the vanilla player
 * mesh here. The player entity is captured upstream by AvatarRendererMixin and read
 * from PlayerMorphCapture.
 *
 * Note: this replaces the old 1.12-era LivingEntityRendererMixin (which hooked the
 * removed render()/setAngles() API) — that code was dead on 26.2.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void bbs$onSubmit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci)
    {
        Player player = PlayerMorphCapture.PLAYER.get();
        float partialTick = PlayerMorphCapture.TICK.get() == null ? 0F : PlayerMorphCapture.TICK.get();

        PlayerMorphCapture.PLAYER.remove();
        PlayerMorphCapture.TICK.remove();

        if (player == null)
        {
            return;
        }

        if (MorphRenderer.hidePlayer)
        {
            Form current = FormUtilsClient.getCurrentForm();

            if (current instanceof MobForm mob && !mob.isPlayer())
            {
                ci.cancel();

                return;
            }

            if (current instanceof ModelForm)
            {
                ci.cancel();

                return;
            }
        }

        Morph morph = Morph.getMorph(player);

        if (morph == null)
        {
            return;
        }

        Form form = morph.getForm();

        if (form instanceof MobForm mobForm)
        {
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

            EntityRenderState mobState = mobRenderer.createRenderState();
            mobRenderer.extractRenderState(mobEntity, mobState, partialTick);

            float bodyYaw = Lerps.lerp(player.yBodyRotO, player.yBodyRot, partialTick);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));

            mobRenderer.submit(mobState, poseStack, collector, cameraState);

            poseStack.popPose();

            ci.cancel();

            return;
        }

        if (form instanceof ModelForm modelForm)
        {
            poseStack.pushPose();

            FormUtilsClient.render(modelForm, new FormRenderingContext()
                .set(FormRenderType.ENTITY, new MCEntity(player), poseStack, state.lightCoords, 0, partialTick)
                .camera(Minecraft.getInstance().gameRenderer.mainCamera()));

            poseStack.popPose();

            ci.cancel();

            return;
        }
    }
}
