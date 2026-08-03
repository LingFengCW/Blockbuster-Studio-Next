package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.ShaderProgram;
import com.mojang.blaze3d.vertex.PoseStack;

public class ModelVAORenderer
{
    public static void render(ShaderProgram shader, IModelVAO modelVAO, PoseStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        if (shader == null) return;

        setupUniforms(stack, shader);

        shader.bind();
        modelVAO.render(shader.getVertexFormat(), r, g, b, a, light, overlay);
        shader.unbind();
    }

    public static void setupUniforms(PoseStack stack, ShaderProgram shader)
    {
    }
}
