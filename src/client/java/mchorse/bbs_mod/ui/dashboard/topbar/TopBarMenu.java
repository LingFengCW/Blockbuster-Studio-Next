package mchorse.bbs_mod.ui.dashboard.topbar;

import mchorse.bbs_mod.l10n.keys.IKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A drop down of the persistent top bar (File, Edit, Tools, ...).
 *
 * Menus are addressed by a stable string id so that other modules can extend
 * an existing menu instead of piling up new buttons:
 *
 * <pre>
 * TopBarRegistry.menu(TopBarRegistry.TOOLS)
 *     .action("my_mod.thing", MyKeys.THING, (context) -> ...);
 * </pre>
 */
public class TopBarMenu
{
    public final String id;

    public IKey label;
    public int order;

    private final List<TopBarItem> items = new ArrayList<>();
    private final List<Function<TopBarContext, List<TopBarItem>>> suppliers = new ArrayList<>();

    public TopBarMenu(String id, IKey label, int order)
    {
        this.id = id;
        this.label = label;
        this.order = order;
    }

    public TopBarMenu label(IKey label)
    {
        this.label = label;

        return this;
    }

    public TopBarMenu order(int order)
    {
        this.order = order;

        return this;
    }

    /**
     * Register (or replace) an entry. Re-registering the same id overwrites
     * the previous entry, which lets a module override a built-in action.
     */
    public TopBarItem action(String id, IKey label, Consumer<TopBarContext> action)
    {
        TopBarItem item = new TopBarItem(id, label, action);

        item.order(this.items.size() * 10);

        this.add(item);

        return item;
    }

    public TopBarMenu add(TopBarItem item)
    {
        this.remove(item.id);
        this.items.add(item);

        return this;
    }

    public TopBarMenu remove(String id)
    {
        this.items.removeIf((item) -> item.id.equals(id));

        return this;
    }

    public TopBarItem get(String id)
    {
        for (TopBarItem item : this.items)
        {
            if (item.id.equals(id))
            {
                return item;
            }
        }

        return null;
    }

    /**
     * Feed entries into this menu that are computed every time it is opened.
     *
     * Static registration takes a snapshot, which is wrong for a menu that
     * mirrors another live registry (the Window menu mirrors
     * {@link mchorse.bbs_mod.ui.windows.WindowRegistry}): a window registered
     * after the bar was built would never show up. A supplier is re-evaluated
     * on every click, so late registrations are picked up.
     */
    public TopBarMenu dynamic(Function<TopBarContext, List<TopBarItem>> supplier)
    {
        this.suppliers.add(supplier);

        return this;
    }

    public List<TopBarItem> getItems(TopBarContext context)
    {
        List<TopBarItem> sorted = new ArrayList<>(this.items);

        for (Function<TopBarContext, List<TopBarItem>> supplier : this.suppliers)
        {
            try
            {
                List<TopBarItem> supplied = supplier.apply(context);

                if (supplied != null)
                {
                    sorted.addAll(supplied);
                }
            }
            catch (Exception e)
            {
                /* A broken third party supplier must not take the bar down. */
            }
        }

        sorted.sort(Comparator.comparingInt((TopBarItem item) -> item.order));

        return sorted;
    }

    public List<TopBarItem> getVisibleItems(TopBarContext context)
    {
        List<TopBarItem> result = new ArrayList<>();

        for (TopBarItem item : this.getItems(context))
        {
            if (item.isVisible(context))
            {
                result.add(item);
            }
        }

        return result;
    }

    public boolean isEmpty(TopBarContext context)
    {
        return this.getVisibleItems(context).isEmpty();
    }

    /**
     * Convenience for entries that only make sense with an open panel. Used
     * as an {@code enabled} predicate so the entry stays listed, greyed out.
     */
    public static Predicate<TopBarContext> requiresPanel()
    {
        return (context) -> context != null && context.panel() != null;
    }

    /**
     * Convenience for entries that need the editor itself. Used as an
     * {@code enabled} predicate so the entry stays listed (greyed out) on
     * windows that are not the dashboard.
     */
    public static Predicate<TopBarContext> requiresDashboard()
    {
        return (context) -> context != null && context.hasDashboard();
    }
}
