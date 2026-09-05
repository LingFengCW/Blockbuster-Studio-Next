package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.Draw;
import net.minecraft.client.renderer.RenderPipelines;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.Inventory;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class Recorder extends WorldFilmController
{
    public ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public FormProperties properties = new FormProperties("properties");
    public Inventory inventory = new Inventory("inventory");
    public float hp;
    public float hunger;
    public int xpLevel;
    public float xpProgress;

    private static Matrix4f perspective = new Matrix4f();

    public Form lastForm;
    public Vector3d lastPosition;
    public Vector4f lastRotation;

    public int countdown;
    public final int initialTick;

    public static void renderCameraPreview(Position position, Camera camera, PoseStack stack)
    {
        if (!BBSSettings.recordingOverlays.get())
        {
            return;
        }

        float x = (float) (position.point.x - camera.position().x);
        float y = (float) (position.point.y - camera.position().y);
        float z = (float) (position.point.z - camera.position().z);
        float fov = MathUtils.toRad(position.angle.fov);
        float aspect = BBSRendering.getVideoWidth() / (float) BBSRendering.getVideoHeight();

        perspective.identity().perspective(fov, aspect, 0.001F, 100F).invert();

        Matrix4f matrix = Matrices.TEMP_4F;
        matrix.identity()
            .rotateY(MathUtils.toRad(position.angle.yaw + 180))
            .rotateX(MathUtils.toRad(-position.angle.pitch));

        /* Frustum: four corner directions projected from the apex. */
        Vector4f[] corners = new Vector4f[] {
            frustumCorner(matrix, 1F, 1F),
            frustumCorner(matrix, -1F, 1F),
            frustumCorner(matrix, -1F, -1F),
            frustumCorner(matrix, 1F, -1F)
        };

        ByteBufferBuilder byteBuf = new ByteBufferBuilder(4096);
        BufferBuilder builder = new BufferBuilder(byteBuf, PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = stack.last().pose();

        /* Apex to each corner. */
        for (Vector4f c : corners)
        {
            builder.addVertex(m, x, y, z).setColor(1F, 1F, 1F, 1F);
            builder.addVertex(m, x + c.x, y + c.y, z + c.z).setColor(1F, 1F, 1F, 1F);
        }

        /* Far rectangle connecting the four corners. */
        for (int i = 0; i < corners.length; i++)
        {
            Vector4f a = corners[i];
            Vector4f b = corners[(i + 1) % corners.length];

            builder.addVertex(m, x + a.x, y + a.y, z + a.z).setColor(1F, 1F, 1F, 1F);
            builder.addVertex(m, x + b.x, y + b.y, z + b.z).setColor(1F, 1F, 1F, 1F);
        }

        Draw.drawBuffer(builder, RenderPipelines.LINES_TRANSLUCENT);
    }

    private static Vector4f frustumCorner(Matrix4f matrix, float nx, float ny)
    {
        Vector4f vector = Vectors.TEMP_4F;
        vector.set(nx, ny, 0F, 1F);
        vector.mul(perspective);
        vector.w = 1F;
        vector.normalize().mul(100F);
        vector.w = 1F;
        vector.mul(matrix);

        /* Copy out so the shared TEMP_4F can be reused for the next corner. */
        return new Vector4f(vector);
    }

    public Recorder(Film film, Form form, int replayId, int tick)
    {
        super(film);

        this.lastForm = FormUtils.copy(form);
        this.exception = replayId;
        this.tick = tick;
        this.countdown = TimeUtils.toTick(BBSSettings.recordingCountdown.get());
        this.initialTick = tick;
    }

    public boolean hasNotStarted()
    {
        return this.countdown > 0;
    }

    public void update()
    {
        if (this.hasNotStarted())
        {
            this.countdown -= 1;

            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (this.lastPosition == null)
        {
            this.lastPosition = new Vector3d(player.getX(), player.getY(), player.getZ());
            this.lastRotation = new Vector4f(player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRotO);
            this.inventory.fromPlayer(player);

            this.hp = player.getHealth();
            this.hunger = player.getFoodData().getSaturationLevel();
            this.xpLevel = player.experienceLevel;
            this.xpProgress = player.experienceProgress;
        }

        if (this.tick >= 0)
        {
            Morph morph = Morph.getMorph(player);

            this.keyframes.record(this.tick, morph.entity, null);
        }

        super.update();
    }

    public void render(LevelRenderContext context)
    {
        super.render(context);

        renderCameraPreview(this.position, Minecraft.getInstance().gameRenderer.mainCamera(), context.poseStack());
    }

    @Override
    public void shutdown()
    {
        Vector3d pos = this.lastPosition;

        if (pos != null)
        {
            Vector4f rot = this.lastRotation;

            PlayerUtils.teleport(pos.x, pos.y, pos.z, rot.z, rot.y);
            ClientNetwork.sendPlayerForm(this.lastForm);
        }

        super.shutdown();
    }
}
