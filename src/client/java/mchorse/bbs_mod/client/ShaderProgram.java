package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSModClient;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * ShaderProgram - BBS's own GLSL program built on the OpenGL backend.
 *
 * MC 26.2 removed the vanilla ShaderProgram wrapper, but the game still
 * renders through OpenGL by default (Vulkan is experimental). BBS shaders
 * (assets/bbs/shaders/core/*.vsh/.fsh) use Mojang's #moj_import directive;
 * we expand those includes ourselves by reading them from the resource
 * manager, then compile/link with GL20.
 */
public class ShaderProgram
{
    private final Identifier name;
    private final VertexFormat format;
    private int glProgramId = -1;

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

    /**
     * Compile and link this program from its .vsh / .fsh sources (lazy).
     * Returns true on success. Failures are logged; the program stays
     * invalid (-1) and callers skip rendering, never crashing.
     */
    public boolean ensureCompiled()
    {
        if (this.glProgramId != -1)
        {
            return true;
        }

        try
        {
            String vs = this.loadShader(this.name.getPath(), ".vsh");
            String fs = this.loadShader(this.name.getPath(), ".fsh");

            if (vs == null || fs == null)
            {
                return false;
            }

            int vertex = this.compile(GL20.GL_VERTEX_SHADER, vs);
            int fragment = this.compile(GL20.GL_FRAGMENT_SHADER, fs);

            if (vertex == 0 || fragment == 0)
            {
                if (vertex != 0) GL20.glDeleteShader(vertex);
                if (fragment != 0) GL20.glDeleteShader(fragment);

                return false;
            }

            int program = GL20.glCreateProgram();

            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);

            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE)
            {
                String log = GL20.glGetProgramInfoLog(program);

                BBSModClient.LOGGER.error("[BBS] Shader link failed for {}: {}", this.name, log);
                GL20.glDeleteProgram(program);

                return false;
            }

            this.glProgramId = program;

            return true;
        }
        catch (Exception e)
        {
            BBSModClient.LOGGER.error("[BBS] Failed to compile shader {}", this.name, e);

            return false;
        }
    }

    private String loadShader(String path, String extension)
    {
        Identifier id = Identifier.fromNamespaceAndPath(this.name.getNamespace(), "shaders/core/" + path + extension);

        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);

            if (resource == null)
            {
                BBSModClient.LOGGER.error("[BBS] Shader resource not found: {}", id);

                return null;
            }

            try (InputStream stream = resource.open())
            {
                StringBuilder source = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                String line;

                while ((line = reader.readLine()) != null)
                {
                    source.append(line).append('\n');
                }

                return this.expandImports(source.toString(), new HashSet<>(), 0);
            }
        }
        catch (IOException e)
        {
            BBSModClient.LOGGER.error("[BBS] Failed to read shader {}", id, e);

            return null;
        }
    }

    /** Recursively expand #moj_import <file.glsl> directives. */
    private String expandImports(String source, Set<String> visited, int depth)
    {
        if (depth > 8)
        {
            return source;
        }

        StringBuilder out = new StringBuilder();
        String[] lines = source.split("\n", -1);

        for (String line : lines)
        {
            String trimmed = line.trim();

            if (trimmed.startsWith("#moj_import"))
            {
                int lt = trimmed.indexOf('<');
                int gt = trimmed.indexOf('>', lt);

                if (lt >= 0 && gt > lt)
                {
                    String include = trimmed.substring(lt + 1, gt);
                    String key = include;

                    /* Strip the "minecraft:" prefix used by newer shaders. */
                    if (include.startsWith("minecraft:"))
                    {
                        include = include.substring("minecraft:".length());
                    }

                    if (visited.add(key) && !include.contains("/"))
                    {
                        String included = this.readInclude(include);

                        if (included != null)
                        {
                            out.append(this.expandImports(included, visited, depth + 1));
                            out.append('\n');
                        }
                    }
                }
            }
            else
            {
                out.append(line).append('\n');
            }
        }

        return out.toString();
    }

    private String readInclude(String include)
    {
        Identifier id = Identifier.fromNamespaceAndPath("minecraft", "shaders/include/" + include);

        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);

            if (resource == null)
            {
                return null;
            }

            try (InputStream stream = resource.open())
            {
                StringBuilder source = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                String line;

                while ((line = reader.readLine()) != null)
                {
                    source.append(line).append('\n');
                }

                return source.toString();
            }
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private int compile(int type, String source)
    {
        int shader = GL20.glCreateShader(type);

        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE)
        {
            String log = GL20.glGetShaderInfoLog(shader);
            String kind = type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment";

            BBSModClient.LOGGER.error("[BBS] {} shader compile failed for {}: {}", kind, this.name, log);
            GL20.glDeleteShader(shader);

            return 0;
        }

        return shader;
    }

    public void bind()
    {
        if (this.ensureCompiled())
        {
            GL20.glUseProgram(this.glProgramId);
        }
    }

    public void unbind()
    {
        GL20.glUseProgram(0);
    }

    public int getUniformLocation(String name)
    {
        if (!this.ensureCompiled())
        {
            return -1;
        }

        return GL20.glGetUniformLocation(this.glProgramId, name);
    }

    public void setUniform(int location, Matrix4f matrix)
    {
        if (location < 0 || matrix == null)
        {
            return;
        }

        float[] values = new float[16];
        matrix.get(values);

        GL20.glUniformMatrix4fv(location, false, values);
    }

    public void setUniform(int location, Matrix3f matrix)
    {
        if (location < 0 || matrix == null)
        {
            return;
        }

        float[] values = new float[9];
        matrix.get(values);

        GL20.glUniformMatrix3fv(location, false, values);
    }

    public void setUniform(int location, float v0, float v1, float v2, float v3)
    {
        if (location < 0)
        {
            return;
        }

        GL20.glUniform4f(location, v0, v1, v2, v3);
    }

    public void setUniform(int location, int v0, int v1, int v2, int v3)
    {
        if (location < 0)
        {
            return;
        }

        GL20.glUniform4i(location, v0, v1, v2, v3);
    }

    public void setUniform(int location, int value)
    {
        if (location < 0)
        {
            return;
        }

        GL20.glUniform1i(location, value);
    }

    public void setUniform(int location, float value)
    {
        if (location < 0)
        {
            return;
        }

        GL20.glUniform1f(location, value);
    }

    public int getGlProgramId()
    {
        return this.glProgramId;
    }

    public void setGlProgramId(int id)
    {
        this.glProgramId = id;
    }

    /** Close the program; a later bind() recompiles it. */
    public void close()
    {
        if (this.glProgramId != -1)
        {
            GL20.glDeleteProgram(this.glProgramId);
            this.glProgramId = -1;
        }
    }
}
