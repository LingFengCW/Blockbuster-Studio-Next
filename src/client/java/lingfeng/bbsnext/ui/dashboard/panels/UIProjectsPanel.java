package lingfeng.bbsnext.ui.dashboard.panels;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.scenes.UISceneMenu;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;

/**
 * Workspace picker: the first thing the dashboard shows. Lists every
 * project as a work; clicking one activates it and jumps straight into
 * the film editor panel (asset bin + clips). The dashboard itself stays a
 * light browser - projects, supporters, graph - while the editor is a
 * separate panel opened from here.
 */
public class UIProjectsPanel extends UIDashboardPanel
{
    public UIProjectsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        /* Plain label + project buttons laid out directly on the panel.
         * Earlier attempts with UIScrollView + ColumnResizer made the
         * buttons invisible: the resizer's stretch left the area blank
         * or off-screen, even with a correct panel area. Direct
         * relative() layout is the only thing that actually draws them. */
        UILabel label = UI.label(UIKeys.PROJECTS_LIBRARY, 18, Colors.LIGHTER_GRAY);

        label.relative(this).x(0).y(0).w(1, 0).h(20);

        this.add(label);

        this.refresh();

        mchorse.bbs_mod.BBSMod.LOGGER.info("UIProjectsPanel ready: area={} visible={} children={}", this.area, this.isVisible(), this.getChildren().size());
    }

    @Override
    public void appear()
    {
        mchorse.bbs_mod.BBSMod.LOGGER.info("UIProjectsPanel appear: area={} visible={}", this.area, this.isVisible());

        this.refresh();
    }

    @Override
    public void resize()
    {
        super.resize();

        mchorse.bbs_mod.BBSMod.LOGGER.info("UIProjectsPanel resize: area={} parentArea={}", this.area, this.parent == null ? "null" : this.parent.area);
    }

    private void refresh()
    {
        /* Strip out the previously rendered rows (everything but the
         * label, which sits at index 0). */
        while (this.getChildren().size() > 1)
        {
            this.remove(this.getChildren().get(1));
        }

        int y = 22;
        int rowHeight = 24;

        UIButton create = new UIButton(UIKeys.PROJECTS_NEW, (b) -> this.createProject());

        create.relative(this).x(0).y(y).w(1, 0).h(rowHeight);
        this.add(create);
        y += rowHeight + 2;

        List<BBSProject> projects = ProjectManager.get().getProjects();

        if (projects.isEmpty())
        {
            UILabel hint = UI.label(UIKeys.PROJECTS_EMPTY, 20, Colors.GRAY);

            hint.relative(this).x(0).y(y).w(1, 0).h(20);
            this.add(hint);
        }

        for (BBSProject project : projects)
        {
            UIButton row = new UIButton(IKey.raw(project.name), (b) -> this.openProject(project));

            row.relative(this).x(0).y(y).w(1, 0).h(rowHeight);
            this.add(row);
            y += rowHeight + 2;
        }
    }

    /** New-work dialog: name + optional background world, then straight
     * into the editor. Same flow as the legacy project menu. */
    private void createProject()
    {
        UIOverlay.addOverlay(this.getContext(), new mchorse.bbs_mod.ui.projects.UINewProjectOverlayPanel((name, world) ->
        {
            if (name == null || name.trim().isEmpty())
            {
                return;
            }

            BBSProject created = ProjectManager.get().create(name.trim(), world);

            this.refresh();

            if (created != null)
            {
                this.openProject(created);
            }
        }));
    }

    /** Activate the picked work and open the editor panel on top of it. */
    private void openProject(BBSProject project)
    {
        ProjectManager.get().setCurrent(project);

        UIFilmPanel film = this.dashboard.getPanel(UIFilmPanel.class);

        if (film == null)
        {
            return;
        }

        this.dashboard.setPanel(film);

        SceneManager scenes = SceneManager.get();

        if (scenes != null && scenes.getCurrent() != null)
        {
            UISceneMenu.openScene(this.dashboard, scenes.getCurrent());
        }
    }
}
