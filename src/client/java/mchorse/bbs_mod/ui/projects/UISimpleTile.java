package mchorse.bbs_mod.ui.projects;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

/**
 * A self-drawn, clickable tile. It extends UIElement (not the vanilla
 * Button), so MC's render pipeline never draws its blue default box - the
 * painter callback owns every pixel. Click (left button inside the area)
 * runs the action.
 */
public class UISimpleTile extends UIElement
{
    private final Runnable onClick;

    public UISimpleTile(Consumer<UIContext> painter, Runnable onClick)
    {
        this.onClick = onClick;

        this.markContainer();
        this.add(new UIRenderable(painter));
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.onClick != null && this.area.isInside(context) && context.mouseButton == 0)
        {
            this.onClick.run();

            return true;
        }

        return super.subMouseClicked(context);
    }

    /** Small helpers used by painters. */
    public static void paintHoverFrame(UIContext context, int x, int y, int w, int h, boolean hover)
    {
        context.batcher.box(x, y, x + w, y + h, Colors.A50 | 0xFF1E242C);
        context.batcher.outline(x + 1, y + 1, x + w - 1, y + h - 1, hover ? Colors.A75 | 0xFF4FA8FF : Colors.A25 | 0xFF3A4450);
    }
}
