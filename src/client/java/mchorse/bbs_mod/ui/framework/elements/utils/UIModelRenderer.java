package mchorse.bbs_mod.ui.framework.elements.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.Factor;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PoseStackUtils;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Intersectiond;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;

/**
 * Model renderer GUI element
 *
 * This base class can be used for full screen model viewer.
 */
public abstract class UIModelRenderer extends UIElement
{
    private static Vector3d vec = new Vector3d();
    private static Matrix3d mat = new Matrix3d();

    protected IEntity entity = new StubEntity();

    protected int timer;
    protected int dragging;

    public Camera camera = new Camera();

    public Vector3f pos = new Vector3f();
    public Factor distance = new Factor(0, 0, 100, (x) -> Math.pow(x, 2) / 100D);
    public boolean grid = true;

    private Vector3d cachedPlaneIntersection = new Vector3d();
    private Vector3f cachedPos = new Vector3f();
    private Camera cachedCamera = new Camera();
    private Vector3d plane = new Vector3d();
    private float lastX;
    private float lastY;

    private long tick;
    private Matrix4f transform = new Matrix4f();

    /**
     * MC 26.2: pose stack with camera transformations applied, valid only
     * during the picture-in-picture draw callback (renderInPip).
     */
    protected PoseStack pipStack;

    public UIModelRenderer()
    {
        super();

        this.reset();
    }

    public void setTransform(Matrix4f transform)
    {
        this.transform = transform;
    }

    public void setRotation(float yaw, float pitch)
    {
        this.camera.rotation.y = MathUtils.toRad(yaw);
        this.camera.rotation.x = MathUtils.toRad(pitch);
    }

    public void setPosition(float x, float y, float z)
    {
        this.pos.set(x, y, z);
    }

    public void setDistance(int distanceX)
    {
        this.distance.setX(distanceX);
    }

    public void setEntity(IEntity entity)
    {
        this.entity = entity;
    }

    public IEntity getEntity()
    {
        return this.entity;
    }

    public void reset()
    {
        this.setDistance(15);
        this.setPosition(0, 1, 0);
        this.setRotation(0, 0);
    }

    public boolean isDragging()
    {
        return this.dragging != 0;
    }

    public boolean isDraggingPosition()
    {
        return this.dragging == 2;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.isDragging() && this.area.isInside(context) && (context.mouseButton == 0 || context.mouseButton == 2))
        {
            this.dragging = Window.isShiftPressed() || context.mouseButton == 2 ? 2 : 1;
            this.lastX = context.mouseX;
            this.lastY = context.mouseY;

            this.cachedPos.set(this.pos);
            this.cachedCamera.copy(this.camera);
            this.plane.set(0, 0, 1);
            this.rotateVector(this.plane);

            this.cachedPlaneIntersection = this.calculateOnPlane(context);
        }

        return false;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (this.area.isInside(context) && !this.isDragging())
        {
            int x = Integer.compare(-(int) context.mouseWheel, 0);

            if (Window.isCtrlPressed())
            {
                x *= 8;
            }

            this.distance.setX(this.distance.getX() + x);
        }

        return super.subMouseScrolled(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.dragging = 0;

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.updateLogic(context);

        context.batcher.clip(this.area, context);
        this.renderModel(context);
        context.batcher.unclip(context);

        super.render(context);
    }

    private void updateLogic(UIContext context)
    {
        long tick = context.getTick();
        long i = tick - this.tick;

        if (i > 10)
        {
            i = 10;
        }

        while (i > 0)
        {
            this.update();
            i --;
        }

        this.tick = tick;
    }

    /**
     * Update logic
     */
    protected void update()
    {
        this.timer += 1;
        this.entity.setAge(this.timer);
    }

    /**
     * Submit currently edited model as a picture-in-picture render state.
     *
     * MC 26.2: GUI extraction phase must not open render passes. Instead,
     * the actual 3D rendering is deferred into UIModelPipRenderer, which the
     * engine invokes legally during the draw phase (GuiRenderer#prepare).
     */
    private void renderModel(UIContext context)
    {
        this.setupPosition();
        this.camera.updatePerspectiveProjection(this.area.w, this.area.h);

        /* processInputs must run before updateView: it modifies camera.rotation
         * (from mouse drag) and camera.distance (from scroll). If we called
         * updateView first, the PiP callback would capture a stale view matrix
         * that doesn't reflect the user's interaction. */
        this.processInputs(context);
        this.camera.updateView();

        net.minecraft.client.gui.GuiGraphicsExtractor extractor = context.batcher.getContext();

        if (extractor != null)
        {
            mchorse.bbs_mod.client.PipGeometry.debug("addPipState", "Adding PiP state: area=" + this.area.w + "x" + this.area.h + " x=" + this.area.x + " y=" + this.area.y);

            extractor.guiRenderState.addPicturesInPictureState(new UIModelPipRenderState(
                (poseStack, collector) -> this.renderInPip(context, poseStack, collector),
                this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                null
            ));
        }

        this.processInputs(context);
    }

    /**
     * Executed by the engine during the draw phase, rendering into the
     * picture-in-picture texture.
     */
    private void renderInPip(UIContext context, PoseStack stack, net.minecraft.client.renderer.SubmitNodeCollector collector)
    {
        /* Rendering begins... */
        stack.pushPose();

        /* The engine prepared this PiP texture with an orthographic
         * projection and pre-multiplied the pose stack with:
         * translate(width / 2, height, 0); scale(16 * guiScale, 16 * guiScale, -16 * guiScale).
         * So: origin = bottom-center of the preview, 1 unit = 16 GUI px,
         * +y points down, z flipped.
         *
         * True perspective is impossible in PiP: vertices are transformed
         * on the CPU via addVertex(Matrix4f, ...) which never performs a
         * perspective divide (w is dropped). Baking a perspective matrix
         * into the pose therefore produces garbage geometry. Instead the
         * BBS perspective camera is emulated orthographically: the zoom
         * factor matches the visible height a perspective camera would
         * capture at the orbit distance (2 * d * tan(fov / 2)). Camera
         * rotation, panning and zooming all keep working. */
        float unitsHeight = this.area.h / 16F;
        float dist = Math.max((float) this.distance.getValue(), 0.1F);
        float zoom = unitsHeight / (2F * dist * (float) Math.tan(this.camera.fov / 2D));

        mchorse.bbs_mod.client.PipGeometry.debug("renderInPip", "renderInPip: area=" + this.area.w + "x" + this.area.h
            + " dist=" + dist + " zoom=" + zoom + " fov=" + this.camera.fov);

        /* Move origin from bottom-center to the viewport center */
        stack.translate(0F, -unitsHeight / 2F, 0F);
        /* World units -> PiP units, flip Y back up (engine's -z flip plus
         * this -y flip keeps the overall winding positive) */
        stack.scale(zoom, -zoom, zoom);

        PoseStackUtils.multiply(stack, this.camera.view);
        /* Shift the world vertically so the model (standing at y=0..1.8)
         * is centered within the viewport. The camera looks at y=1 (eye
         * level); the default -camera.position.y would put the eyes at
         * the viewport top, clipping the head. */
        stack.translate(-this.camera.position.x, -this.camera.position.y - 0.5F, -this.camera.position.z);
        PoseStackUtils.multiply(stack, this.transform);

        if (this.grid)
        {
            this.renderGrid(stack, collector);
        }

        mchorse.bbs_mod.client.PipGeometry.setCollector(collector);
        this.pipStack = stack;

        try
        {
            this.renderUserModel(context);
        }
        finally
        {
            mchorse.bbs_mod.client.PipGeometry.setCollector(null);
            this.pipStack = null;
        }

        stack.popPose();
    }

    protected void processInputs(UIContext context)
    {
        int mouseX = context.mouseX;
        int mouseY = context.mouseY;

        if (this.isDragging())
        {
            if (this.isDraggingPosition())
            {
                if (this.lastX != context.mouseX || this.lastY != context.mouseY)
                {
                    Vector3d newPoint = this.calculateOnPlane(context);

                    this.pos.set(this.cachedPos);
                    this.pos.sub((float) newPoint.x, (float) newPoint.y, (float) newPoint.z);
                    this.pos.add((float) this.cachedPlaneIntersection.x, (float) this.cachedPlaneIntersection.y, (float) this.cachedPlaneIntersection.z);

                    this.lastX = mouseX;
                    this.lastY = mouseY;
                }
            }
            else
            {
                this.camera.rotation.y -= MathUtils.toRad(this.lastX - mouseX);
                this.camera.rotation.x -= MathUtils.toRad(this.lastY - mouseY);

                this.lastX = mouseX;
                this.lastY = mouseY;
            }
        }
    }

    public void setupPosition()
    {
        this.camera.position.set(this.pos);

        vec.set(0, 0, -this.distance.getValue());
        this.rotateVector(vec);

        this.camera.position.x += vec.x;
        this.camera.position.y += vec.y;
        this.camera.position.z += vec.z;
    }

    private Vector3d calculateOnPlane(UIContext context)
    {
        Vector3d vector = new Vector3d();
        Vector3d origin = new Vector3d(this.cachedCamera.position).sub(this.cachedPos);
        Vector3d destination = new Vector3d(this.cachedCamera.getMouseDirection(context.mouseX, context.mouseY, this.area.x, this.area.y, this.area.w, this.area.h)).mul(this.distance.getValue() * 2).add(origin);
        Intersectiond.intersectLineSegmentPlane(origin.x, origin.y, origin.z, destination.x, destination.y, destination.z, this.plane.x, this.plane.y, this.plane.z, 0, vector);

        return vector;
    }

    private void rotateVector(Vector3d vec)
    {
        mat.identity().rotateX(this.camera.rotation.x);
        mat.transform(vec);
        mat.identity().rotateY(MathUtils.PI - this.camera.rotation.y);
        mat.transform(vec);
    }

    protected void setupViewport(UIContext context)
    {
        Minecraft mc = Minecraft.getInstance();

        float rx = (float) Math.round(mc.getWindow().getWidth() / (double) context.menu.width);
        float ry = (float) Math.round(mc.getWindow().getHeight() / (double) context.menu.height);
        float size = BBSModClient.getOriginalFramebufferScale();

        int vx = (int) (this.area.x * rx);
        int vy = (int) (mc.getWindow().getHeight() - (this.area.y + this.area.h) * ry);
        int vw = (int) (this.area.w * rx);
        int vh = (int) (this.area.h * ry);

        this.camera.updatePerspectiveProjection(vw, vh);
        this.camera.updateView();
    }

    /**
     * Draw your model here
     */
    protected abstract void renderUserModel(UIContext context);

    /**
     * Get a pose stack for model rendering. During the PiP draw callback,
     * this returns a copy of the camera-transformed stack so that submitted
     * geometry ends up in the correct view space (MC 26.2 has no global
     * view matrix uniform for custom geometry anymore).
     */
    protected PoseStack createModelStack()
    {
        PoseStack stack = new PoseStack();

        if (this.pipStack != null)
        {
            stack.last().pose().set(this.pipStack.last().pose());
            stack.last().normal().set(this.pipStack.last().normal());
        }

        return stack;
    }

    /**
     * Render grid lines under the model (which signify where
     * located the ground below the model)
     *
     * MC 26.2: submitted legally through the PiP submit node collector
     * instead of opening a render pass manually.
     */
    protected void renderGrid(PoseStack stack, net.minecraft.client.renderer.SubmitNodeCollector collector)
    {
        /* MC 26.2: RenderTypes.lines() uses POSITION_COLOR_NORMAL_LINE_WIDTH,
         * which requires a line-width element not exposed via VertexConsumer.
         * Draw thin quads with entityTranslucent instead. */
        collector.submitCustomGeometry(stack, net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()), (pose, consumer) ->
        {
            Matrix4f matrix4f = pose.pose();

            for (int x = 0; x <= 10; x ++)
            {
                float r = x == 0 ? 0F : 0.25F;
                float g = x == 0 ? 0F : 0.25F;
                float b = x == 0 ? 1F : 0.25F;
                float cx = x - 5;

                /* Thin quad: 0.02 units wide, from z=-5 to z=5 */
                consumer.addVertex(matrix4f, cx - 0.01F, 0, -5).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
                consumer.addVertex(matrix4f, cx + 0.01F, 0, -5).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
                consumer.addVertex(matrix4f, cx + 0.01F, 0, 5).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
                consumer.addVertex(matrix4f, cx - 0.01F, 0, 5).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
            }

            for (int x = 0; x <= 10; x ++)
            {
                float r = x == 0 ? 1F : 0.25F;
                float g = x == 0 ? 0F : 0.25F;
                float b = x == 0 ? 0F : 0.25F;
                float cz = x - 5;

                /* Thin quad: 0.02 units wide, from x=-5 to x=5 */
                consumer.addVertex(matrix4f, -5, 0, cz - 0.01F).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
                consumer.addVertex(matrix4f, -5, 0, cz + 0.01F).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
                consumer.addVertex(matrix4f, 5, 0, cz + 0.01F).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
                consumer.addVertex(matrix4f, -5, 0, cz - 0.01F).setColor(r, g, b, 1F).setUv(0, 0).setUv1(0, 0).setUv2(0, 240).setNormal(0F, 1F, 0F);
            }
        });
    }
}


