package mchorse.bbs_mod.ui.utils.context;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * A context menu entry that is listed but cannot be used.
 *
 * The tool bar is present on every window of the editor, which means a lot
 * of its entries refer to things that only exist inside the dashboard. Rather
 * than hiding those entries - which would make the bar look different
 * depending on where the user is - they are rendered greyed out.
 *
 * The runnable is deliberately {@code null}: {@link mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu}
 * only ever arms an action whose runnable is non-null, so a disabled entry is
 * inert by construction instead of relying on a click guard.
 */
public class DisabledContextAction extends ContextAction
{
    public DisabledContextAction(Icon icon, IKey label)
    {
        super(icon, label, null);
    }

    @Override
    public void render(UIContext context, FontRenderer font, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        /* No hover highlight on purpose - the entry must read as inert. */
        context.batcher.icon(this.icon, Colors.setA(Colors.WHITE, 0.35F), x + 2, y + h / 2, 0, 0.5F);
        context.batcher.text(this.label.get(), x + 22, y + (h - font.getHeight()) / 2 + 1, Colors.GRAY, false);
    }
}
