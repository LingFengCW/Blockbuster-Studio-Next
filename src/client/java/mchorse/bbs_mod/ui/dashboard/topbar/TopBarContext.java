package mchorse.bbs_mod.ui.dashboard.topbar;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;

/**
 * What a top bar entry is handed when it runs or when its state is queried.
 *
 * The bar lives on every window, not just the dashboard, so an entry cannot
 * assume the editor is around. This carries the host window plus the editor
 * when there is one, which lets an injected entry open overlays on whatever
 * window the user happens to be on instead of silently doing nothing.
 */
public class TopBarContext
{
    /** The window the bar is mounted on. Never null. */
    public final UIBaseMenu menu;

    /** The editor, or null when the host window is not the dashboard. */
    public final UIDashboard dashboard;

    public TopBarContext(UIBaseMenu menu, UIDashboard dashboard)
    {
        this.menu = menu;
        this.dashboard = dashboard;
    }

    public boolean hasDashboard()
    {
        return this.dashboard != null;
    }

    /** The host window's UI context - use this for overlays and notices. */
    public UIContext context()
    {
        return this.menu == null ? null : this.menu.context;
    }

    /** The panel the editor currently shows, or null. */
    public UIDashboardPanel panel()
    {
        return this.dashboard == null ? null : this.dashboard.getPanels().panel;
    }

    /** Typed lookup of a registered panel, or null when there is no editor. */
    public <T extends UIDashboardPanel> T panel(Class<T> type)
    {
        return this.dashboard == null ? null : this.dashboard.getPanel(type);
    }
}
