package mchorse.bbs_mod.graphics.texture;

import mchorse.bbs_mod.utils.resources.Pixels;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Texture class
 * 
 * MC 26.2 with Vulkan: all GL calls removed.
 * This class stores texture metadata only.
 */
public class Texture
{
    public int id = -1;
    public int target;

    public int width;
    public int height;

    private boolean mipmap;
    private boolean clearable;

    private AnimatedTexture parent;
    private TextureFormat format = TextureFormat.RGBA_U8;
    private int filter;

    public static Pixels pixelsFromTexture(Texture texture)
    {
        if (!texture.isValid())
        {
            return null;
        }
        // GL readback removed for Vulkan compatibility
        return null;
    }

    public static Texture textureFromPixels(Pixels pixels, int filter)
    {
        Texture texture = new Texture();
        texture.setFilter(filter);
        return texture;
    }

    public Texture()
    {
        this.target = 0x0DE1; // GL_TEXTURE_2D constant
    }

    public void setParent(AnimatedTexture parent)
    {
        this.parent = parent;
    }

    public AnimatedTexture getParent()
    {
        return this.parent;
    }

    public void setClearable(boolean clearable)
    {
        this.clearable = clearable;
    }

    public boolean isClearable()
    {
        return this.clearable;
    }

    public TextureFormat getFormat()
    {
        return this.format;
    }

    public boolean isMipmap()
    {
        return this.mipmap;
    }

    public boolean isReallyMipmap()
    {
        return this.mipmap;
    }

    public boolean isValid()
    {
        return this.id >= 0;
    }

    public void bind() {}
    public void bind(int texture) {}
    public void unbind() {}
    public void unbind(int texture) {}

    public void setFormat(TextureFormat format)
    {
        this.format = format;
    }

    public int getFilter()
    {
        return this.filter;
    }

    public boolean isLinear()
    {
        return this.filter == 0x2601 || this.filter == 0x2703; // GL_LINEAR values
    }

    public int getParameter(int parameter)
    {
        return 0;
    }

    public void setFilterMipmap(boolean linear, boolean mipmap) {}

    public void setFilter(int filter)
    {
        this.filter = filter;
    }

    public void setWrap(int mode) {}
    public void setParameter(int param, int value) {}

    public void delete()
    {
        this.id = -1;
    }

    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public void updateTexture(Pixels pixels)
    {
        this.updateTexture(this.target, pixels);
    }

    public void updateTexture(int target, Pixels pixels) {}

    public void uploadTexture(Pixels pixels) {}
    public void uploadTexture(int target, Pixels pixels) {}
    public void uploadTexture(int target, int level, Pixels pixels) {}

    public void uploadTexture(int target, int level, int w, int h, ByteBuffer buffer)
    {
        if (level == 0)
        {
            this.width = w;
            this.height = h;
        }
    }

    public void generateMipmap()
    {
        this.mipmap = true;
    }
}
