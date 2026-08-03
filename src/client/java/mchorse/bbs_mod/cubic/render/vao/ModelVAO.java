package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSRendering;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

public class ModelVAO implements IModelVAO
{
    private int vao;
    private int vao2;
    private int count;

    public ModelVAO(ModelVAOData data)
    {
        this.upload(data);
    }

    public void delete()
    {
    }

    public void upload(ModelVAOData data)
    {
        this.count = data.vertices().length / 3;
    }

    @Override
    public void render(VertexFormat format, float r, float g, float b, float a, int light, int overlay)
    {
    }

    public static boolean isShadersEnabled()
    {
        return BBSRendering.isIrisShadersEnabled();
    }
}
