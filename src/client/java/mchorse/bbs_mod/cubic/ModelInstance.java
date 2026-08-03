package mchorse.bbs_mod.cubic;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.View;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.render.CubicCubeRenderer;
import mchorse.bbs_mod.cubic.render.CubicMatrixRenderer;
import mchorse.bbs_mod.cubic.render.CubicRenderer;
import mchorse.bbs_mod.cubic.render.CubicVAOBuilderRenderer;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.resources.LinkUtils;
import mchorse.bbs_mod.client.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModelInstance implements IModelInstance
{
    public final String id;
    public IModel model;
    public Animations animations;
    public Link texture;

    /* Model's additional properties */
    public String poseGroup;
    public boolean procedural;
    public boolean culling = true;
    public boolean onCpu;
    public String anchorGroup = "";

    public View view;

    public Vector3f scale = new Vector3f(1F);
    public float uiScale = 1F;
    public Pose sneakingPose = new Pose();

    /* Temp vectors for CPU-side vertex transforms (BOBJ rendering). */
    private final Vector4f tmpVec = new Vector4f();
    private final Vector3f tmpNormal = new Vector3f();

    public List<ArmorSlot> itemsMain = new ArrayList<>();
    public List<ArmorSlot> itemsOff = new ArrayList<>();
    public Map<String, String> flippedParts = new HashMap<>();
    public Map<ArmorType, ArmorSlot> armorSlots = new HashMap<>();

    public ArmorSlot fpMain;
    public ArmorSlot fpOffhand;

    private Map<ModelGroup, ModelVAO> vaos = new HashMap<>();

    public ModelInstance(String id, IModel model, Animations animations, Link texture)
    {
        this.id = id;
        this.model = model;
        this.animations = animations;
        this.texture = texture;

        this.poseGroup = id;
    }

    @Override
    public IModel getModel()
    {
        return this.model;
    }

    @Override
    public Pose getSneakingPose()
    {
        return this.sneakingPose;
    }

    @Override
    public Animations getAnimations()
    {
        return this.animations;
    }

    public Map<ModelGroup, ModelVAO> getVaos()
    {
        return this.vaos;
    }

    public String getAnchor()
    {
        String anchor = this.model.getAnchor();

        if (this.anchorGroup.isEmpty() && !anchor.isEmpty())
        {
            return anchor;
        }

        return this.anchorGroup;
    }

    public void applyConfig(MapType config)
    {
        if (config == null)
        {
            return;
        }

        this.procedural = config.getBool("procedural", this.procedural);
        this.culling = config.getBool("culling", this.culling);
        this.onCpu = config.getBool("on_cpu", this.onCpu);
        this.poseGroup = config.getString("pose_group", this.poseGroup);

        if (config.has("texture"))
        {
            this.texture = LinkUtils.create(config.get("texture"));
        }
        if (config.has("items_main"))
        {
            ListType list = config.get("items_main").asList();

            for (BaseType type : list)
            {
                ArmorSlot slot = new ArmorSlot();

                slot.fromData(type);
                this.itemsMain.add(slot);
            }
        }
        if (config.has("items_off"))
        {
            ListType list = config.get("items_off").asList();

            for (BaseType type : list)
            {
                ArmorSlot slot = new ArmorSlot();

                slot.fromData(type);
                this.itemsOff.add(slot);
            }
        }
        if (config.has("ui_scale")) this.uiScale = config.getFloat("ui_scale");
        if (config.has("scale")) this.scale = DataStorageUtils.vector3fFromData(config.getList("scale"), new Vector3f(1F));
        if (config.has("sneaking_pose", BaseType.TYPE_MAP))
        {
            this.sneakingPose = new Pose();
            this.sneakingPose.fromData(config.getMap("sneaking_pose"));
        }
        if (config.has("anchor")) this.anchorGroup = config.getString("anchor");
        if (config.has("flipped_parts"))
        {
            MapType map = config.getMap("flipped_parts");

            for (String key : map.keys())
            {
                String string = map.getString(key);

                if (!string.trim().isEmpty())
                {
                    this.flippedParts.put(key, string);
                }
            }
        }
        if (config.has("armor_slots"))
        {
            MapType map = config.getMap("armor_slots");

            for (String key : map.keys())
            {
                try
                {
                    ArmorType type = ArmorType.valueOf(key.toUpperCase());
                    ArmorSlot slot = new ArmorSlot();

                    slot.fromData(map.getMap(key));
                    this.armorSlots.put(type, slot);
                }
                catch (Exception e)
                {}
            }
        }
        if (config.has("fp_main"))
        {
            this.fpMain = new ArmorSlot();
            this.fpMain.fromData(config.get("fp_main"));
        }
        if (config.has("fp_offhand"))
        {
            this.fpOffhand = new ArmorSlot();
            this.fpOffhand.fromData(config.get("fp_offhand"));
        }

        /* Optional look-at configuration */
        if (config.has("look_at", BaseType.TYPE_MAP))
        {
            this.view = new View();

            this.view.fromData(config.getMap("look_at"));
        }
    }

    public void setup()
    {
        if (this.model instanceof BOBJModel model)
        {
            Minecraft.getInstance().execute(model::setup);
        }

        /* VAOs should be only generated if there are no shape keys */
        if (!this.model.getShapeKeys().isEmpty())
        {
            return;
        }

        if (this.model instanceof Model model && !this.onCpu)
        {
            Minecraft.getInstance().execute(() ->
            {
                CubicRenderer.processRenderModel(new CubicVAOBuilderRenderer(this.vaos), null, new PoseStack(), model);
            });
        }
    }

    public boolean isVAORendered()
    {
        return !this.vaos.isEmpty() || this.model instanceof BOBJModel;
    }

    public void delete()
    {
        for (ModelVAO value : this.vaos.values())
        {
            value.delete();
        }

        this.vaos.clear();
    }

    /* Rendering */

    public void fillStencilMap(StencilMap stencilMap, ModelForm form)
    {
        if (this.model instanceof Model model)
        {
            for (ModelGroup group : model.getOrderedGroups())
            {
                stencilMap.addPicking(form, group.id);
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            for (BOBJBone orderedBone : model.getArmature().orderedBones)
            {
                stencilMap.addPicking(form, orderedBone.name);
            }
        }
    }

    public void captureMatrices(MatrixCache bones)
    {
        if (this.model instanceof Model model)
        {
            PoseStack stack = new PoseStack();
            CubicMatrixRenderer renderer = new CubicMatrixRenderer(model);

            CubicRenderer.processRenderModel(renderer, null, stack, model);

            for (ModelGroup group : model.getAllGroups())
            {
                Matrix4f matrix = new Matrix4f(renderer.matrices.get(group.index));
                Matrix4f origin = new Matrix4f(renderer.origins.get(group.index));

                matrix.translate(
                    group.initial.translate.x / 16,
                    group.initial.translate.y / 16,
                    group.initial.translate.z / 16
                );
                matrix.rotateY(MathUtils.PI);
                origin.translate(
                    group.initial.translate.x / 16,
                    group.initial.translate.y / 16,
                    group.initial.translate.z / 16
                );
                origin.rotateY(MathUtils.PI);
                bones.put(group.id, matrix, origin);
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            model.getArmature().setupMatrices();

            for (BOBJBone orderedBone : model.getArmature().orderedBones)
            {
                Matrix4f matrix = new Matrix4f();
                Matrix4f origin = new Matrix4f();

                matrix.rotateY(MathUtils.PI).mul(orderedBone.mat);
                origin.rotateY(MathUtils.PI).mul(orderedBone.originMat);
                bones.put(orderedBone.name, matrix, origin);
            }
        }
    }

    public void render(PoseStack stack, Supplier<ShaderProgram> program, Color color, int light, int overlay, StencilMap stencilMap, ShapeKeys keys)
    {
        /* The 7-arg overload defaults to the CPU/BufferBuilder path (gui=true),
         * which is what every current caller passes (program is always null).
         * The explicit gui overload is used when the caller knows the mode. */
        this.render(stack, program, color, light, overlay, stencilMap, keys, true);
    }

    public void render(PoseStack stack, Supplier<ShaderProgram> program, Color color, int light, int overlay, StencilMap stencilMap, ShapeKeys keys, boolean gui)
    {
        if (this.model instanceof Model model)
        {
            CubicCubeRenderer renderProcessor = new CubicCubeRenderer(light, overlay, stencilMap, keys);
            renderProcessor.setColor(color.r, color.g, color.b, color.a);
            renderProcessor.setGuiMode(gui);

            if (gui)
            {
                /* [MC 26.2] UI rendering: the GUI extraction phase must not open
                 * render passes, so geometry is submitted through the engine's
                 * picture-in-picture SubmitNodeCollector instead */
                net.minecraft.client.renderer.SubmitNodeCollector collector = mchorse.bbs_mod.client.PipGeometry.getCollector();

                if (collector == null)
                {
                    mchorse.bbs_mod.client.PipGeometry.debug("noCollector", "ModelInstance.render(gui): no collector, skipping " + this.id);

                    return;
                }

                renderProcessor.setGuiMode(false);

                /* Snapshot the pose, because the geometry callback is executed
                 * later, when the caller's stack may have been popped already */
                PoseStack snapshot = new PoseStack();

                snapshot.last().pose().set(stack.last().pose());
                snapshot.last().normal().set(stack.last().normal());

                Link tex = mchorse.bbs_mod.client.PipGeometry.getLastTexture();

                mchorse.bbs_mod.client.PipGeometry.debug("submit", "ModelInstance.render(gui): submitting " + this.id
                    + " tex=" + (tex == null ? this.texture : tex)
                    + " m00=" + snapshot.last().pose().m00()
                    + " m30=" + snapshot.last().pose().m30()
                    + " m31=" + snapshot.last().pose().m31());

                collector.submitCustomGeometry(stack, mchorse.bbs_mod.client.PipGeometry.getModelRenderType(tex == null ? this.texture : tex), (pose, consumer) ->
                {
                    mchorse.bbs_mod.client.PipGeometry.debug("lambda", "ModelInstance.render(gui): geometry callback invoked for " + this.id);
                    CubicRenderer.processRenderModel(renderProcessor, consumer, snapshot, model);
                });
            }
            else
            {
                /* World/entity rendering: use ENTITY_TRANSLUCENT pipeline */
                ByteBufferBuilder byteBuf = new ByteBufferBuilder(65536);
                BufferBuilder builder = new BufferBuilder(byteBuf, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
                CubicRenderer.processRenderModel(renderProcessor, builder, stack, model);
                Draw.drawBuffer(builder, RenderPipelines.ENTITY_TRANSLUCENT,
                    mchorse.bbs_mod.client.PipGeometry.bridgeView(this.texture),
                    mchorse.bbs_mod.client.PipGeometry.bridgeSampler());
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            /* [MC 26.2] BOBJ meshes are now rendered through the same native
             * pipelines as cubic models: picture-in-picture SubmitNodeCollector
             * for UI, BufferBuilder + Draw for the world. The vertex data is
             * transformed on the CPU by updateMesh() (skeletal animation). */
            BOBJModelVAO vao = model.getVao();

            if (vao == null)
            {
                return;
            }

            if (gui)
            {
                net.minecraft.client.renderer.SubmitNodeCollector collector = mchorse.bbs_mod.client.PipGeometry.getCollector();

                if (collector == null)
                {
                    mchorse.bbs_mod.client.PipGeometry.debug("noCollector", "ModelInstance.render(gui): BOBJ no collector, skipping " + this.id);

                    return;
                }

                PoseStack snapshot = new PoseStack();

                snapshot.last().pose().set(stack.last().pose());
                snapshot.last().normal().set(stack.last().normal());

                Link tex = mchorse.bbs_mod.client.PipGeometry.getLastTexture();

                /* updateMesh() runs here (extraction phase, CPU) instead of
                 * inside the geometry callback. The callback may be invoked
                 * multiple times per frame by the engine's multi-pass PiP
                 * pipeline; doing the full skeletal transform every pass
                 * ground the render thread to a halt. The callback now only
                 * uploads the already-transformed vertices. */
                vao.updateMesh(null);

                collector.submitCustomGeometry(stack, mchorse.bbs_mod.client.PipGeometry.getModelRenderType(tex == null ? this.texture : tex), (pose, consumer) ->
                {
                    this.renderBOBJ(vao, consumer, snapshot, color, light, overlay, true);
                });
            }
            else
            {
                vao.updateMesh(null);

                ByteBufferBuilder byteBuf = new ByteBufferBuilder(65536);
                BufferBuilder builder = new BufferBuilder(byteBuf, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

                this.renderBOBJ(vao, builder, stack, color, light, overlay, false);

                Draw.drawBuffer(builder, RenderPipelines.ENTITY_TRANSLUCENT,
                    mchorse.bbs_mod.client.PipGeometry.bridgeView(this.texture),
                    mchorse.bbs_mod.client.PipGeometry.bridgeSampler());
            }
        }
    }

    /**
     * Write BOBJ mesh vertices into a native VertexConsumer. The skeletal
     * transform was already baked into {@link BOBJModelVAO#tmpVertices} by
     * updateMesh(); the remaining pose transform is applied here.
     */
    private void renderBOBJ(BOBJModelVAO vao, VertexConsumer builder, PoseStack stack, Color color, int light, int overlay, boolean gui)
    {
        BOBJLoader.CompiledData data = vao.data;
        float[] verts = vao.getTmpVertices();
        float[] norms = vao.getTmpNormals();
        float[] uvs = data.texData;
        int count = vao.getCount();

        if (verts == null || norms == null || uvs == null)
        {
            return;
        }

        Matrix4f pose = stack.last().pose();

        for (int i = 0; i < count; i++)
        {
            float vx = verts[i * 3];
            float vy = verts[i * 3 + 1];
            float vz = verts[i * 3 + 2];
            float nx = norms[i * 3];
            float ny = norms[i * 3 + 1];
            float nz = norms[i * 3 + 2];
            float u = uvs[i * 2];
            float v = uvs[i * 2 + 1];

            this.tmpVec.set(vx, vy, vz, 1F);
            pose.transform(this.tmpVec);

            /* Transform the normal with the pose's normal matrix so lighting
             * follows the model rotation. Without this the mesh renders as a
             * flat, faceted mess ("triangle soup") from every angle. */
            this.tmpNormal.set(nx, ny, nz);
            stack.last().normal().transform(this.tmpNormal);

            /* Both the GUI (PiP) and world paths use the ENTITY_TRANSLUCENT
             * pipeline (POSITION_COLOR_TEX_LIGHTMAP). The gui render type is
             * entityTranslucent, so all vertex elements must be written or
             * BufferBuilder throws "Missing elements in vertex". */
            builder.addVertex(this.tmpVec.x, this.tmpVec.y, this.tmpVec.z)
                .setColor(color.r, color.g, color.b, color.a)
                .setUv(u, v)
                .setUv1(overlay & 0xFFFF, overlay >> 16 & 0xFFFF)
                .setUv2(light & 0xFFFF, light >> 16 & 0xFFFF)
                .setNormal(this.tmpNormal.x, this.tmpNormal.y, this.tmpNormal.z);
        }
    }
}




