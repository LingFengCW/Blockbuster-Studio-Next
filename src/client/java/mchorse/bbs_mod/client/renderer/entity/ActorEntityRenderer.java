package mchorse.bbs_mod.client.renderer.entity;

import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class ActorEntityRenderer extends EntityRenderer<ActorEntity, EntityRenderState>
{
    public static ArmorRenderer armorRenderer;
    private ActorEntity currentEntity;

    public ActorEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);

        armorRenderer = new ArmorRenderer(ctx.getModelSet(), false);

        this.shadowRadius = 0.5F;
    }

    @Override
    public EntityRenderState createRenderState()
    {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(ActorEntity entity, EntityRenderState state, float tickDelta)
    {
        super.extractRenderState(entity, state, tickDelta);
        this.currentEntity = entity;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera)
    {
        ActorEntity livingEntity = this.currentEntity;

        if (livingEntity == null)
        {
            super.submit(state, matrices, collector, camera);
            return;
        }

        matrices.pushPose();

        float bodyYaw = Mth.rotLerp(state.ageInTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
        int overlay = livingEntity.hurtTime > 0 ? 0 : 0;

        this.setupTransforms(livingEntity, matrices, bodyYaw, state.ageInTicks);

        // enableBlend/disableBlend removed in MC 26.2
        // enableDepthTest removed;
        FormUtilsClient.render(livingEntity.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, livingEntity.getEntity(), matrices, state.lightCoords, overlay, state.ageInTicks)
            .camera(Minecraft.getInstance().gameRenderer.mainCamera()));
        // disableDepthTest removed;

        matrices.popPose();

        /* Explicit leash rope. The preview world is rendered through the custom
         * ActorEntityRenderer pipeline, so Minecraft's automatic per-entity leash
         * pass does not draw it for ActorEntity; render the rope here between the
         * leashed entity and its holder. */
        Entity holder = livingEntity.getLeashHolder();

        if (holder != null && holder != livingEntity && livingEntity.level().isClientSide())
        {
            this.renderLeash(collector, livingEntity, holder, state.ageInTicks);
        }

        super.submit(state, matrices, collector, camera);

        this.currentEntity = null;
    }

    private void renderLeash(SubmitNodeCollector collector, ActorEntity entity, Entity holder, float tickDelta)
    {
        Vec3 ep = entity.position().add(entity.getLeashOffset(tickDelta));
        Vec3 hp;

        if (holder instanceof Leashable hl)
        {
            hp = holder.position().add(hl.getLeashOffset(tickDelta));
        }
        else
        {
            hp = holder.position();
        }

        PoseStack stack = new PoseStack();

        collector.submitCustomGeometry(stack, net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()), (pose, consumer) -> {
            Matrix4f m = pose.pose();

            int N = 12;
            Vec3[] pts = new Vec3[N + 1];
            double dist = ep.distanceTo(hp);
            double sag = Math.min(0.6D, dist * 0.15D);

            for (int i = 0; i <= N; i++)
            {
                double t = (double) i / N;
                Vec3 base = ep.scale(1D - t).add(hp.scale(t));
                double s = Math.sin(Math.PI * t) * sag;

                pts[i] = new Vec3(base.x, base.y - s, base.z);
            }

            Vec3 up = new Vec3(0D, 1D, 0D);
            float w = 0.07F;

            for (int i = 0; i < N; i++)
            {
                Vec3 a = pts[i];
                Vec3 b = pts[i + 1];
                Vec3 dir = b.subtract(a);
                double len = dir.length();

                if (len < 1e-6D)
                {
                    continue;
                }

                dir = dir.scale(1D / len);

                Vec3 side = up.cross(dir);
                double sl = side.length();

                if (sl < 1e-6D)
                {
                    side = new Vec3(1D, 0D, 0D);
                }
                else
                {
                    side = side.scale(1D / sl);
                }

                float nx = (float) side.x, ny = (float) side.y, nz = (float) side.z;
                float r = 0.45F, g = 0.32F, bl = 0.18F, al = 0.9F;

                /* Rope ribbon: two triangles (a-side, a+side, b+side, b-side). */
                addLeashVertex(consumer, m, a.x - side.x * w, a.y - side.y * w, a.z - side.z * w, nx, ny, nz, r, g, bl, al);
                addLeashVertex(consumer, m, a.x + side.x * w, a.y + side.y * w, a.z + side.z * w, nx, ny, nz, r, g, bl, al);
                addLeashVertex(consumer, m, b.x + side.x * w, b.y + side.y * w, b.z + side.z * w, nx, ny, nz, r, g, bl, al);
                addLeashVertex(consumer, m, a.x - side.x * w, a.y - side.y * w, a.z - side.z * w, nx, ny, nz, r, g, bl, al);
                addLeashVertex(consumer, m, b.x + side.x * w, b.y + side.y * w, b.z + side.z * w, nx, ny, nz, r, g, bl, al);
                addLeashVertex(consumer, m, b.x - side.x * w, b.y - side.y * w, b.z - side.z * w, nx, ny, nz, r, g, bl, al);
            }
        });
    }

    private static void addLeashVertex(VertexConsumer consumer, Matrix4f m, double x, double y, double z, float nx, float ny, float nz, float r, float g, float b, float a)
    {
        consumer.addVertex(m, (float) x, (float) y, (float) z)
            .setColor(r, g, b, a)
            .setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(nx, ny, nz);
    }

    protected boolean isVisible(ActorEntity entity)
    {
        return !entity.isInvisible();
    }

    protected void setupTransforms(ActorEntity entity, PoseStack matrices, float bodyYaw, float tickDelta)
    {
        if (!entity.hasPose(Pose.SLEEPING))
        {
            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-bodyYaw));
        }

        if (entity.deathTime > 0)
        {
            float deathAngle = (entity.deathTime + tickDelta - 1F) / 20F * 1.6F;

            matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathAngle), 1F) * 90F));
        }
    }
}
