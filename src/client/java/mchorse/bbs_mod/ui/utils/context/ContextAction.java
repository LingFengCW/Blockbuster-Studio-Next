package mchorse.bbs_mod.ui.utils.context;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;

public class ContextAction
{
    public Icon icon;
    public IKey label;
    public Runnable runnable;

    /** Nested sub-menu entries; when non-null the item opens a sub-menu
     * on hover instead of running its own runnable. */
    public List<ContextAction> subActions;

    public IKey keyCategory;
    public int[] keys;
    public int order = -1;

    public ContextAction(Icon icon, IKey label, Runnable runnable)
    {
        this.icon = icon;
        this.label = label;
        this.runnable = runnable;
    }

    public ContextAction key(IKey keyCategory, int... keys)
    {
        this.keyCategory = keyCategory;

        return this.key(keys);
    }

    public ContextAction key(int... keys)
    {
        this.keys = keys;

        return this;
    }

    public ContextAction order(int order)
    {
        this.order = order;

        return this;
    }

    public boolean hasSubMenu()
    {
        return this.subActions != null && !this.subActions.isEmpty();
    }

    public int getWidth(FontRenderer font)
    {
        return 28 + font.getWidth(this.label.get());
    }

    public void render(UIContext context, FontRenderer font, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        this.renderBackground(context, x, y, w, h, hover, selected);

        context.batcher.icon(this.icon, x + 2, y + h / 2, 0, 0.5F);
        context.batcher.text(this.label.get(), x + 22, y + (h - font.getHeight()) / 2 + 1, Colors.WHITE, false);

        if (this.hasSubMenu())
        {
            context.batcher.text("\u25B6", x + w - 14, y + (h - font.getHeight()) / 2 + 1, Colors.WHITE, false);
        }
    }

    protected void renderBackground(UIContext context, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        if (hover)
        {
            context.batcher.box(x, y, x + w, y + h, Colors.A50 | BBSSettings.primaryColor.get());
        }
    }
}

