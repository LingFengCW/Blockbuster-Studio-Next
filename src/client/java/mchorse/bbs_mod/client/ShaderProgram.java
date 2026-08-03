package mchorse.bbs_mod.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

/**
 * ShaderProgram - wrapper for MC 26.2 shader pipeline compatibility.
 * Replaces the old net.minecraft.client.gl.ShaderProgram which no longer exists.
 */
public class ShaderProgram
{
    private final Identifier name;
    private final VertexFormat format;
    private int glProgramId;

    public ShaderProgram(Identifier name, VertexFormat format)
    {
        this.name = name;
        this.format = format;
    }

    public VertexFormat getVertexFormat()
    {
        return this.format;
    }

    public Identifier getName()
    {
        return this.name;
    }

    public void bind()
    {
    }

    public void unbind()
    {
    }

    public int getUniformLocation(String name)
    {
        return -1;
    }

    public void setUniform(int location, Matrix4f matrix)
    {
    }

    public void setUniform(int location, float v0, float v1, float v2, float v3)
    {
    }

    public void setUniform(int location, int v0, int v1, int v2, int v3)
    {
    }

    public int getGlProgramId()
    {
        return glProgramId;
    }

    public void setGlProgramId(int id)
    {
        this.glProgramId = id;
    }
}
