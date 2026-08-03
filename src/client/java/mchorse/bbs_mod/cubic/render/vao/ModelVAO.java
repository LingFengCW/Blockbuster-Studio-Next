package mchorse.bbs_mod.cubic.render.vao;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * ModelVAO - a CPU-side extruded mesh (replaces the removed GL VAO path).
 *
 * upload() keeps the vertex/normal/tangent/UV data; render() writes it into
 * a native BufferBuilder and draws through the 26.2 render pipeline with the
 * BBS texture bound to Sampler0, exactly like the cubic/BOBJ world path.
 */
public class ModelVAO implements IModelVAO
{
    private float[] vertices;
    private float[] normals;
    private float[] texCoords;
    private int count;

    public ModelVAO(ModelVAOData data)
    {
        this.upload(data);
    }

    public void delete()
    {
        this.vertices = null;
        this.normals = null;
        this.texCoords = null;
        this.count = 0;
    }

    public void upload(ModelVAOData data)
    {
        this.vertices = data.vertices();
        this.normals = data.normals();
        this.texCoords = data.texCoords();
        this.count = data.vertices() == null ? 0 : data.vertices().length / 3;
    }

    @Override
    public void render(VertexFormat format, float r, float g, float b, float a, int light, int overlay)
    {
        this.render(format, new PoseStack(), null, r, g, b, a, light, overlay);
    }

    /**
     * Render the mesh into a native BufferBuilder and draw it with the given
     * BBS texture bound. {@code texture} may be null, in which case the
     * default (missing) texture is used.
     */
    public void render(VertexFormat format, PoseStack stack, Link texture, float r, float g, float b, float a, int light, int overlay)
    {
        if (this.vertices == null || this.count == 0)
        {
            return;
        }

        ByteBufferBuilder byteBuf = new ByteBufferBuilder(65536);
        BufferBuilder builder = new BufferBuilder(byteBuf, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        var pose = stack.last().pose();

        for (int i = 0; i < this.count; i++)
        {
            float vx = this.vertices[i * 3];
            float vy = this.vertices[i * 3 + 1];
            float vz = this.vertices[i * 3 + 2];
            float nx = this.normals == null ? 0F : this.normals[i * 3];
            float ny = this.normals == null ? 0F : this.normals[i * 3 + 1];
            float nz = this.normals == null ? 0F : this.normals[i * 3 + 2];
            float u = this.texCoords == null ? 0F : this.texCoords[i * 2];
            float v = this.texCoords == null ? 0F : this.texCoords[i * 2 + 1];

            builder.addVertex(pose, vx, vy, vz)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setUv1(overlay & 0xFFFF, overlay >> 16 & 0xFFFF)
                .setUv2(light & 0xFFFF, light >> 16 & 0xFFFF)
                .setNormal(nx, ny, nz);
        }

        Draw.drawBuffer(builder, RenderPipelines.ENTITY_TRANSLUCENT,
            texture == null ? null : mchorse.bbs_mod.client.PipGeometry.bridgeView(texture),
            texture == null ? null : mchorse.bbs_mod.client.PipGeometry.bridgeSampler());
    }

    public static boolean isShadersEnabled()
    {
        return BBSRendering.isIrisShadersEnabled();
    }
}
