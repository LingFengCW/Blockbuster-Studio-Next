package lingfeng.bbsnext.ui.dashboard.panels;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.scenes.UISceneMenu;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;
import java.util.function.Consumer;

/**
 * Workspace picker: the first thing the dashboard shows. Lists every
 * project as a large preview card (gradient cover + name, PR style);
 * clicking one activates it and jumps straight into the film editor.
 */
public class UIProjectsPanel extends UIDashboardPanel
{
    public UIProjectsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        UILabel label = UI.label(UIKeys.PROJECTS_LIBRARY, 18, Colors.LIGHTER_GRAY);

        label.relative(this).x(0).y(0).w(1, 0).h(20);

        this.add(label);

        this.refresh();
    }

    @Override
    public void appear()
    {
        this.refresh();
    }

    @Override
    public void resize()
    {
        super.resize();
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

        UIButton create = this.card(UIKeys.PROJECTS_NEW, 0x22cc88, (b) -> this.createProject());

        create.relative(this).x(0).y(y).w(1, 0).h(48);
        this.add(create);
        y += 48 + 6;

        List<BBSProject> projects = ProjectManager.get().getProjects();

        if (projects.isEmpty())
        {
            UILabel hint = UI.label(UIKeys.PROJECTS_EMPTY, 20, Colors.GRAY);

            hint.relative(this).x(0).y(y).w(1, 0).h(20);
            this.add(hint);
        }

        for (BBSProject project : projects)
        {
            UIButton row = this.card(IKey.raw(project.name), project.name.hashCode(), (b) -> this.openProject(project));

            row.relative(this).x(0).y(y).w(1, 0).h(64);
            this.add(row);
            y += 64 + 6;
        }
    }

    /**
     * Large preview-style card: gradient cover derived from the project
     * name, name overlaid at the bottom, hover highlight border.
     */
    private UIButton card(IKey name, int seed, Consumer<UIButton> onClick)
    {
        return new UIButton(name, onClick)
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                int x = this.area.x;
                int y = this.area.y;
                int ex = this.area.ex();
                int ey = this.area.ey();

                int base = Colors.RGB & (0x445566 + (seed & 0x0FFFFFFF));

                if (base < 0x202020)
                {
                    base = 0x445566;
                }

                /* Gradient cover (dark bottom so the label stays readable). */
                context.batcher.gradientVBox(x, y, ex, ey, base, Colors.A75);

                /* Hover / active border. */
                int border = this.hover ? 0x88ffffff : 0x33000000;

                context.batcher.box(x, y, ex, y + 1, border);
                context.batcher.box(x, ey - 1, ex, ey, border);
                context.batcher.box(x, y, x + 1, ey, border);
                context.batcher.box(ex - 1, y, ex, ey, border);

                /* Project name overlaid at the bottom-left. */
                FontRenderer font = context.batcher.getFont();
                String label = font.limitToWidth(this.label.get(), this.area.w - 12);

                context.batcher.text(label, x + 8, ey - font.getHeight() - 8,
                    this.hover ? Colors.WHITE : Colors.LIGHTER_GRAY, true);

                this.renderLockedArea(context);
            }
        };
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

    /** Activate the picked work and open the editor straight into the HTML
     *  editor (作品 / 场景 / 序列). The old native "影片" panel is hidden,
     *  so the user is never forced to pick a film. */
    private void openProject(BBSProject project)
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
