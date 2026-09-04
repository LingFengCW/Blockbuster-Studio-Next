package mchorse.bbs_mod.ui.projects;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.HashMap;
import java.util.Map;

/**
 * Project library - the PR-style "clip project" manager.
 *
 * A card grid (4 per row) with a preview tile, an editable name and a
 * trash button per project; a top toolbar (back / refresh / title / new
 * project / backpack) and a bottom view switcher (cards vs compact list).
 * The vanilla title screen panorama stays visible under a dark overlay.
 */
public class UIProjectMenu extends UIBaseMenu
{
    private static final java.text.SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");

    private UIScrollView grid;
    private UIScrollView listView;
    private final Map<BBSProject, UIButton> buttons = new HashMap<>();
    private BBSProject selected;
    private boolean gridMode = true;

    public UIProjectMenu()
    {
        /* Top toolbar: back / refresh / title / new project / backpack.
         * Sits below the 18px window-level menu bar. */
        UIElement toolbar = UI.row(4);

        toolbar.relative(this.main).x(0).y(20).w(1, 0).h(26);

        UIButton back = new UIButton(UIKeys.PROJECTS_BACK, (b) -> this.backToTitle());
        UIButton refresh = new UIButton(UIKeys.PROJECTS_REFRESH, (b) -> this.refresh());
        UIElement title = UI.label(UIKeys.PROJECTS_LIBRARY, 20);
        UIButton create = new UIButton(UIKeys.PROJECTS_CREATE, (b) -> this.createProject());
        UIButton backpack = new UIButton(UIKeys.PROJECTS_BACKPACK, (b) -> UIBackpackMenu.open());

        toolbar.add(back, refresh, title, create, backpack);
        this.main.add(toolbar);

        /* Card grid (default view). Rows of up to 4 cards are stacked in the
         * scroll view - GridResizer would replace the scroll layout. */
        this.grid = UI.scrollView(10, 10);
        this.grid.relative(this.main).x(0).y(50).w(1, 0).h(1, -74);
        this.main.add(this.grid);

        /* Compact list view. */
        this.listView = UI.scrollView(5, 5);
        this.listView.relative(this.main).x(0).y(50).w(1, 0).h(1, -74);
        this.listView.setVisible(false);
        this.main.add(this.listView);

        /* Bottom view switcher. */
        UIElement bottom = UI.row(4);

        bottom.relative(this.main).x(0).y(1, -22).w(1, 0).h(18);

        UIButton gridBtn = new UIButton(UIKeys.PROJECTS_VIEW_GRID, (b) -> this.setGridMode(true));
        UIButton listBtn = new UIButton(UIKeys.PROJECTS_VIEW_LIST, (b) -> this.setGridMode(false));

        bottom.add(gridBtn, listBtn);
        this.main.add(bottom);

        this.refresh();
    }

    /* Layout */

    private void setGridMode(boolean grid)
    {
        this.gridMode = grid;

        this.grid.setVisible(grid);
        this.listView.setVisible(!grid);
    }

    private void backToTitle()
    {
        Minecraft.getInstance().gui.setScreen(new TitleScreen());
    }

    /* Content */

    private void refresh()
    {
        this.buttons.clear();
        this.grid.removeAll();
        this.listView.removeAll();

        java.util.List<BBSProject> projects = ProjectManager.get().getProjects();

        if (projects.isEmpty())
        {
            UIElement hint = UI.label(UIKeys.PROJECTS_EMPTY, 20, Colors.GRAY);

            hint.h(20);
            this.grid.add(hint);
        }

        /* Rows of up to 4 cards. */
        for (int i = 0; i < projects.size(); i += 4)
        {
            UIElement row = UI.row(20);

            row.h(180);

            for (int j = i; j < Math.min(i + 4, projects.size()); j++)
            {
                row.add(this.createCard(projects.get(j)));
            }

            this.grid.add(row);
        }

        for (BBSProject project : projects)
        {
            this.listView.add(this.createRow(project));
        }

        this.grid.resize();
        this.listView.resize();

        this.selected = null;
    }

    /** A 220x180 card: preview tile, editable name, trash button. */
    private UIElement createCard(BBSProject project)
    {
        UIElement card = new UIElement();

        card.w(220).h(180);

        /* Preview tile - self-drawn (no vanilla button chrome), click opens
         * the project. Shows a colour block derived from the project id and
         * the project name large - this is the "big picture" card. */
        final UIElement[] previewRef = new UIElement[1];
        UIElement preview = new UISimpleTile((context) ->
        {
            int w = previewRef[0].area.w;
            int h = previewRef[0].area.h;
            boolean hover = previewRef[0].area.isInside(context);

            /* Deterministic colour per project. */
            int base = 0x405060 + (project.id.hashCode() & 0x3FFFFF) % 0x804060;
            int top = 0xFF000000 | base;
            int bottom = Colors.mulRGB(top, 0.45F);

            context.batcher.gradientVBox(0, 0, w, h, top, bottom);

            var font = context.batcher.getFont();
            String name = project.name;

            if (font.getWidth(name) > w - 16)
            {
                while (font.getWidth(name + "...") > w - 16 && name.length() > 1)
                {
                    name = name.substring(0, name.length() - 1);
                }

                name += "...";
            }

            context.batcher.text(name, (w - font.getWidth(name)) / 2, (h - font.getHeight()) / 2 - 8, Colors.WHITE);
            String date = DATE_FORMAT.format(new java.util.Date(project.createdAt));

            context.batcher.text(date, (w - font.getWidth(date)) / 2, (h - font.getHeight()) / 2 + 10, Colors.A75 | 0xFFFFFF);

            if (hover)
            {
                context.batcher.outline(1, 1, w - 1, h - 1, Colors.A75 | 0xFF4FA8FF);
            }
        }, () ->
        {
            this.selected = project;
            this.openProject();
        });

        /* Right click a project card: open / delete. */
        preview.context(m ->
        {
            m.action(mchorse.bbs_mod.ui.utils.icons.Icons.PLAY, UIKeys.PROJECTS_OPEN, () ->
            {
                this.selected = project;
                this.openProject();
            });
            m.action(mchorse.bbs_mod.ui.utils.icons.Icons.REMOVE, UIKeys.PROJECTS_DELETE, () ->
            {
                this.selected = project;
                this.deleteProject();
            });
        });

        preview.relative(card).x(0).y(0).w(1, 0).h(130);

        previewRef[0] = preview;

        /* Editable name - enter saves the rename. */
        UITextbox name = new UITextbox(64, (str) ->
        {
            String trimmed = str.trim();

            if (!trimmed.isEmpty() && !trimmed.equals(project.name))
            {
                ProjectManager.get().rename(project, trimmed);
            }
        });

        name.setText(project.name);
        name.filename();
        name.relative(card).x(0).y(134).w(1, 0).h(20);

        /* Trash button - a self-drawn cross so it never picks up the vanilla
         * blue button chrome either. */
        final UIElement[] trashRef = new UIElement[1];
        UIElement trash = new UISimpleTile((context) ->
        {
            int w = trashRef[0].area.w;
            int h = trashRef[0].area.h;
            boolean hover = trashRef[0].area.isInside(context);

            context.batcher.box(0, 0, w, h, hover ? 0xCCE81123 : Colors.A25 | 0x881C22);

            int cx = w / 2;
            int cy = h / 2;

            context.batcher.box(cx - 3, cy - 1, cx + 3, cy + 1, Colors.WHITE);
            context.batcher.box(cx - 1, cy - 3, cx + 1, cy + 3, Colors.WHITE);
        }, () ->
        {
            this.selected = project;
            this.deleteProject();
        });

        trash.relative(card).x(1, -20).y(2).wh(16, 16);

        trashRef[0] = trash;

        card.add(preview, name, trash);

        return card;
    }

    /** Compact list row: name + date + delete, click opens. */
    private UIElement createRow(BBSProject project)
    {
        UIButton open = new UIButton(IKey.constant(this.label(project)), (b) ->
        {
            this.selected = project;
            this.openProject();
        });

        open.h(24);

        UIButton remove = new UIButton(IKey.raw("\u2716"), (b) ->
        {
            this.selected = project;
            this.deleteProject();
        });

        remove.h(24).w(24);

        UIElement entry = UI.row(2);

        entry.h(24);
        entry.add(open, remove);

        return entry;
    }

    private String cardPreviewLabel(BBSProject project)
    {
        return project.name;
    }

    private String label(BBSProject project)
    {
        return project.name + "   \u00A78" + DATE_FORMAT.format(new java.util.Date(project.createdAt));
    }

    /* Actions */

    private void createProject()
    {
        UIOverlay.addOverlay(this.context, new UINewProjectOverlayPanel((name) ->
        {
            if (name != null && !name.trim().isEmpty())
            {
                BBSProject created = ProjectManager.get().create(name.trim());

                this.refresh();

                if (created != null)
                {
                    this.selected = created;
                    this.openProject();
                }
            }
        }));
    }

    private void deleteProject()
    {
        BBSProject project = this.selected;

        if (project == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.context, new UIConfirmOverlayPanel(
            UIKeys.PROJECTS_DELETE,
            UIKeys.PROJECTS_CONFIRM_DELETE.format(project.name),
            (result) ->
            {
                if (result)
                {
                    ProjectManager.get().delete(project);
                    this.refresh();
                }
            }
        ));
    }

    private void openProject()
    {
        BBSProject project = this.selected;

        if (project == null)
        {
            this.context.notifyError(UIKeys.PROJECTS_EMPTY);

            return;
        }

        ProjectManager.get().setCurrent(project);

        /* Work picked: open the dashboard and land straight in the HTML
         * editor (作品 / 场景 / 序列). The native "影片" panel is never
         * shown - with no scene yet the editor simply opens empty and the
         * user creates one from the asset bin. */
        try
        {
            mchorse.bbs_mod.ui.dashboard.UIDashboard dashboard = mchorse.bbs_mod.BBSModClient.getDashboard();

            UIScreen.open(dashboard);

            mchorse.bbs_mod.ui.film.UIFilmPanel film = dashboard.getPanel(mchorse.bbs_mod.ui.film.UIFilmPanel.class);

            if (film == null)
            {
                return;
            }

            film.prepareHtmlEditor();
            dashboard.setPanel(film);

            mchorse.bbs_mod.projects.SceneManager scenes = mchorse.bbs_mod.projects.SceneManager.get();

            if (scenes != null && scenes.getCurrent() != null)
            {
                mchorse.bbs_mod.ui.scenes.UISceneMenu.openScene(dashboard, scenes.getCurrent());
            }

            /* Always end up in the HTML editor, even when the work has no
             * scene yet (openScene() already opened it when one existed). */
            if (film.ultralightOverlay != null && !film.ultralightOverlay.isVisible())
            {
                film.openHtmlEditor();
            }
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to open the editor", e);

            this.context.notifyError(mchorse.bbs_mod.l10n.keys.IKey.raw("Failed to open editor: " + e.getMessage()));
        }
    }
}
