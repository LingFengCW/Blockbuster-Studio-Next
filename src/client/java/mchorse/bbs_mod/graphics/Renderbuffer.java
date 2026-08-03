package mchorse.bbs_mod.graphics;

import org.lwjgl.opengl.GL30;

public class Renderbuffer
{
    public int id;
    public final int target;
    public final int storage;

    public Renderbuffer()
    {
        this(0, 0);
    }

    public Renderbuffer(int target, int storage)
    {
        this.id = -1;
        this.target = target;
        this.storage = storage;
    }

    public void bind()
    {
    }

    public void unbind()
    {
    }

    public void delete()
    {
        if (this.id >= 0)
        {
            this.id = -1;
        }
    }

    public void resize(int width, int height)
    {
        this.bind();
    }
}
