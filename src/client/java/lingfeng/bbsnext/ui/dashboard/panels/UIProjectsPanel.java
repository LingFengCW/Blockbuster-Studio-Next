package lingfeng.bbsnext.ui.dashboard.panels;

import lingfeng.bbsnext.mcef.DashboardBridge;
import lingfeng.bbsnext.mcef.UIOverlay;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.scenes.UISceneMenu;

/**
 * Workspace picker, rendered entirely in HTML (dashboard_ui.html) through the
 * shared MCEF browser overlay. The page builds the project cards, the
 * supporters view and the settings view (as in-page tabs) and talks back to
 * {@link DashboardBridge} over the same BBS_ACTION: console channel the editor
 * uses. The native dashboard chrome has been removed entirely (HTML owns the
 * view), so the overlay simply fills the whole panel.
 */
public class UIProjectsPanel extends UIDashboardPanel
{
    public UIOverlay dashboardOverlay;

    public UIProjectsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.dashboardOverlay = new UIOverlay(this, new DashboardBridge(this));
        this.dashboardOverlay.relative(this).x(0).y(0).w(1F).h(1F);
        this.dashboardOverlay.setVisible(false);
        this.add(this.dashboardOverlay);
    }

    @Override
    public void appear()
    {
        super.appear();

        this.dashboardOverlay.setVisible(true);
    }

    @Override
    public void disappear()
    {
        this.dashboardOverlay.setVisible(false);

        super.disappear();
    }

    /** Activate the picked work and open the editor straight into the HTML
     *  editor (作品 / 场景 / 序列). The old native "影片" panel is hidden,
     *  so the user is never forced to pick a film. Public so the dashboard
     *  bridge can call it. */
    public void openProject(BBSProject project)
    {
        ProjectManager.get().setCurrent(project);

        UIFilmPanel film = this.dashboard.getPanel(UIFilmPanel.class);

        if (film == null)
        {
            return;
        }

        film.prepareHtmlEditor();
        this.dashboard.setPanel(film);

        SceneManager scenes = SceneManager.get();

        if (scenes != null && scenes.getCurrent() != null)
        {
            UISceneMenu.openScene(this.dashboard, scenes.getCurrent());
        }

        /* Always land in the HTML editor, even with no current scene yet
         * (openScene already opened it when a scene was loaded). */
        if (!film.ultralightOverlay.isVisible())
        {
            film.openHtmlEditor();
        }
    }
}
