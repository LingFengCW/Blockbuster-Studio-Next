package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.ui.dashboard.topbar.UITopBar;

/**
 * The dashboard's instance of the editor-wide tool bar.
 *
 * All of the behaviour lives in {@link UITopBar}, which is mounted on every
 * other window as well - this subclass only exists so that the dashboard can
 * keep a typed field and so that future dashboard-only tweaks have a home.
 */
public class UIDashboardTopBar extends UITopBar
{
    public UIDashboardTopBar(UIDashboard dashboard)
    {
        super(dashboard);
    }
}
