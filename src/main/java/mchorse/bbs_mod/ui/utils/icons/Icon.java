package mchorse.bbs_mod.ui.utils.icons;

import mchorse.bbs_mod.resources.Link;

public class Icon
{
    public final Link texture;
    public final String id;
    public final int x;
    public final int y;
    public final int w;
    public final int h;
    /* The atlas is now 2048x2048 (128px cells); logical icon coordinates
     * (16-unit grid) are unchanged, so every icon samples a 128px region
     * while still drawing at its 16px UI size - crisp at any GUI scale. */
    public int textureW = 2048;
    public int textureH = 2048;

    public Icon(Link texture, String id, int x, int y)
    {
        this(texture, id, x, y, 16, 16);
    }

    public Icon(Link texture, String id, int x, int y, int w, int h)
    {
        this.texture = texture;
        this.id = id;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Icon(Link texture, String id, int x, int y, int w, int h, int textureW, int textureH)
    {
        this(texture, id, x, y, w, h);

        this.textureW = textureW;
        this.textureH = textureH;
    }
}