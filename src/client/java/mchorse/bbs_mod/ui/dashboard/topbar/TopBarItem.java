package mchorse.bbs_mod.ui.dashboard.topbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A single entry of a {@link TopBarMenu}. Third party code creates these
 * through {@link TopBarMenu#action(String, IKey, Consumer)} and never has to
 * know how the top bar renders them.
 *
 * The action receives a live {@link TopBarContext}, so an injected entry can
 * reach the host window (any window - the bar is everywhere) and the editor
 * when there is one, without holding onto a stale reference.
 */
public class TopBarItem
{
    public final String id;
    public final IKey label;
    public final Consumer<TopBarContext> action;

    public int order;
    public Icon icon = Icons.NONE;

    /**
     * Decides whether the entry is offered at all. Prefer {@link #enabled}:
     * an entry that disappears makes the bar look different from window to
     * window, which is exactly what the persistent bar is meant to avoid.
     */
    public Predicate<TopBarContext> visible = (context) -> true;

    /**
     * Decides whether the entry can be clicked. A disabled entry is still
     * listed - greyed out - so the tool bar reads identically on every
     * window instead of items appearing and vanishing.
     */
    public Predicate<TopBarContext> enabled = (context) -> true;

    public TopBarItem(String id, IKey label, Consumer<TopBarContext> action)
    {
        this.id = id;
        this.label = label;
        this.action = action;
    }

    public TopBarItem order(int order)
    {
        this.order = order;

        return this;
    }

    public TopBarItem icon(Icon icon)
    {
        this.icon = icon == null ? Icons.NONE : icon;

        return this;
    }

    public TopBarItem visible(Predicate<TopBarContext> visible)
    {
        this.visible = visible == null ? (context) -> true : visible;

        return this;
    }

    public TopBarItem enabled(Predicate<TopBarContext> enabled)
    {
        this.enabled = enabled == null ? (context) -> true : enabled;

        return this;
    }

    public boolean isEnabled(TopBarContext context)
    {
        try
        {
            return this.enabled.test(context);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public boolean isVisible(TopBarContext context)
    {
        try
        {
            return this.visible.test(context);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public void run(TopBarContext context)
    {
        if (this.action != null)
        {
            this.action.accept(context);
        }
    }
}
