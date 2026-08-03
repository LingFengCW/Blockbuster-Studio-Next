package mchorse.bbs_mod.ui.forms;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.PipGeometry;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/**
 * Renders a form's 3D model into its palette cell as a picture-in-picture
 * preview (MC 26.2 legal channel for 3D content inside a GUI).
 *
 * The engine invokes the PiP callback during the draw phase, where opening
 * render passes is allowed. Each visible cell gets one PiP state per frame;
 * the camera is a fixed orthographic-ish view that fits the model into the
 * cell, and the Y rotation follows the mouse like the original BBS UI did.
 */
public class FormThumbnailRenderer
{
    public static void render(Form form, PoseStack stack, SubmitNodeCollector collector,
        int x1, int y1, int x2, int y2, int mouseX)
    {
        PipGeometry.setCollector(collector);

        try
        {
            /* MobForm must be checked FIRST: MobForm is NOT a subclass of
             * ModelForm, so the ModelForm instanceof below would return early
             * and the vanilla-mob branch would never run. */
            if (FormUtilsClient.getRenderer(form) instanceof MobFormRenderer mobRenderer)
            {
                renderMobThumb(mobRenderer, stack, collector, x1, y1, x2, y2, mouseX);

                return;
            }

            if (!(form instanceof ModelForm modelForm))
            {
                return;
            }

            if (!(FormUtilsClient.getRenderer(form) instanceof ModelFormRenderer renderer))
            {
                return;
            }

            ModelInstance model = renderer.getModel();

            if (model == null || model.model == null)
            {
                return;
            }

            /* Apply the form's state, actions and pose so the thumbnail
             * matches what the player would actually see. Without this the
             * BOBJ skeleton stays in its bind pose and player morphs look
             * completely distorted (stretched/twisted limbs). */
            modelForm.applyStates(0F);
            renderer.ensureAnimator(0F);

            mchorse.bbs_mod.cubic.animation.IAnimator animator = renderer.getAnimator();

            if (animator != null)
            {
                animator.applyActions(null, model, 0F);
            }

            model.model.resetPose();
            model.model.applyPose(renderer.getPose());

            /* PiP coordinate space: origin at bottom-center of the cell,
             * 1 world unit = 16 GUI pixels, +y down. Mirror the original
             * BBS getUIMatrix(): feet at 15% above the cell bottom,
             * scale = height / 2.5 (model fills ~72% of the cell height),
             * 22.5deg top-down tilt, Y rotation follows the mouse. */
            float unitsHeight = (y2 - y1) / 16F;
            float scale = unitsHeight / 2.5F;

            stack.pushPose();
            stack.translate(0F, -unitsHeight * 0.15F, 0F);
            stack.scale(scale, -scale, scale);

            float angle = MathUtils.toRad(mouseX - (x1 + x2) / 2) + MathUtils.PI;

            if (BBSSettings.freezeModels.get())
            {
                angle = -MathUtils.PI + MathUtils.PI / 8;
            }

            stack.mulPose(Axis.XP.rotation(MathUtils.PI / 8));
            stack.mulPose(Axis.YP.rotation(angle));

            Link tex = modelForm.texture.get();
            Link texture = tex == null ? model.texture : tex;

            PipGeometry.setLastTexture(texture);

            Color color = modelForm.color.get();
            float uiScale = form.uiScale.get() * model.uiScale;

            stack.scale(uiScale, uiScale, uiScale);

            model.render(stack, () -> null, color, 0xF000F0, 0, null, modelForm.shapeKeys.get(), true);

            stack.popPose();
        }
        catch (Exception e)
        {
            /* A single broken thumbnail must never take down the whole GUI. */
            PipGeometry.debug("thumbError", "FormThumbnailRenderer error: " + e.getMessage());
        }
        finally
        {
            PipGeometry.setCollector(null);
        }
    }

    /**
     * Renders a vanilla mob (MobForm) thumbnail through the entity renderer's
     * submit() pipeline - the MC 26.2 replacement for the old
     * EntityRenderDispatcher.render() + MultiBufferSource approach.
     */
    private static void renderMobThumb(MobFormRenderer renderer, PoseStack stack,
        SubmitNodeCollector collector, int x1, int y1, int x2, int y2, int mouseX)
    {
        renderer.ensureEntity();

        Entity entity = renderer.getEntity();

        if (entity == null)
        {
            PipGeometry.debug("mobNull", "renderMobThumb: entity is null for " + renderer.getForm().getFormId());
            return;
        }

        /* Reset the entity to the origin before extracting its render state.
         * extractRenderState() copies the entity's world position/rotation
         * into the state, and submit() then renders the model at that world
         * position - which is far outside the thumbnail cell. Pinning the
         * entity to (0,0,0) makes submit() draw it at the PiP origin, where
         * the pose transforms below place it inside the cell. */
        entity.setPos(0D, 0D, 0D);
        entity.setYRot(0F);
        entity.setXRot(0F);
        entity.setYHeadRot(0F);

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        /* EntityRenderer<T extends Entity, S extends EntityRenderState> in
         * MC 26.2 - raw type keeps the submit() call simple. */
        @SuppressWarnings("rawtypes")
        EntityRenderer entityRenderer = dispatcher.getRenderer(entity);

        if (entityRenderer == null)
        {
            PipGeometry.debug("mobRenderer", "renderMobThumb: no EntityRenderer for " + renderer.getForm().getFormId());
            return;
        }

        EntityRenderState state = entityRenderer.createRenderState();
        entityRenderer.extractRenderState(entity, state, 0F);

        PipGeometry.debug("mobSubmit", "renderMobThumb: submitting " + renderer.getForm().getFormId()
            + " entity=" + entity.getType().toShortString() + " bb=" + entity.getBbWidth() + "x" + entity.getBbHeight());

        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float scale = renderer.getForm().uiScale.get() * Math.min(1.8F / Math.max(width, height), 1F);

        float unitsHeight = (y2 - y1) / 16F;
        float pipScale = unitsHeight / 2.5F;

        stack.pushPose();
        stack.translate(0F, -unitsHeight * 0.15F, 0F);
        stack.scale(pipScale, -pipScale, pipScale);

        /* Vanilla entity models face +z; rotate them so the front faces the
         * camera, with the Y angle following the mouse like the original BBS
         * UI. Same 22.5deg top-down tilt. */
        float angle = MathUtils.toRad(mouseX - (x1 + x2) / 2) + MathUtils.PI;

        if (BBSSettings.freezeModels.get())
        {
            angle = -MathUtils.PI + MathUtils.PI / 8;
        }

        stack.mulPose(Axis.XP.rotation(MathUtils.PI / 8));
        stack.mulPose(Axis.YP.rotation(angle));
        stack.scale(scale, scale, scale);

        CameraRenderState cameraState = new CameraRenderState();

        entityRenderer.submit(state, stack, collector, cameraState);

        stack.popPose();
    }
}
