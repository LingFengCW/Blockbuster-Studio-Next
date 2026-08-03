package mchorse.bbs_mod.ui.windows;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.function.BooleanSupplier;

/**
 * A registered window of the editor.
 *
 * A "window" here is not a widget - it is a top level screen (projects,
 * scenes, the dashboard, the backpack, ...) that the user can be sent to.
 * Registering one makes it appear in the persistent top bar's Window menu
 * and makes it openable by id from anywhere, without hardcoding
 * {@code UIScreen.open(new UIWhatever())} all over the code base.
 */
public class WindowEntry
{
    public final String id;
    public final IKey label;
    public final Runnable open;

    public int order;
    public Icon icon = Icons.NONE;

    /** When false the entry is still listed, but greyed out and inert. */
    public BooleanSupplier enabled = () -> true;

    public WindowEntry(String id, IKey label, Runnable open)
    {
        this.id = id;
        this.label = label;
        this.open = open;
    }

    public WindowEntry order(int order)
    {
        this.order = order;

        return this;
    }

    public WindowEntry icon(Icon icon)
    {
        this.icon = icon == null ? Icons.NONE : icon;

        return this;
    }

    public WindowEntry enabled(BooleanSupplier enabled)
    {
        this.enabled = enabled == null ? () -> true : enabled;

        return this;
    }

    public boolean isEnabled()
    {
        try
        {
            return this.enabled.getAsBoolean();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public void open()
    {
        if (this.open != null)
        {
            this.open.run();
        }
    }
}
