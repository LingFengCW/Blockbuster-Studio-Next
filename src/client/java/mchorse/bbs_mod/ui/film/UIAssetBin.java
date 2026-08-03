package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.Backpack;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;

/**
 * Asset bin - the PR-style editor's left panel (design doc 3.1).
 *
 * Two stacked lists: the current project's private assets on top (scene
 * entries plus import controls) and the global cross-project backpack
 * below. Assets are referenced by id on the timeline, never copied.
 */
public class UIAssetBin extends UIElement
{
    public final UIFilmPanel panel;

    private final UIScrollView projectList;
    private final UIScrollView backpackList;

    public UIAssetBin(UIFilmPanel panel)
    {
        this.panel = panel;

        this.markContainer();

        /* ---- Project assets (no header buttons - everything lives in
         * the right-click menu: New -> Scene / Character, Import, Export,
         * To backpack). ---- */
        UILabel projectLabel = UI.label(UIKeys.ASSETS_PROJECT, 18, Colors.LIGHTER_GRAY);

        projectLabel.h(18);

        this.projectList = UI.scrollView(5, 5);
        this.projectList.h(0.45F);

        /* ---- Bottom: global backpack ---- */
        UILabel globalLabel = UI.label(UIKeys.ASSETS_GLOBAL, 18, Colors.LIGHTER_GRAY);

        globalLabel.h(18);

        this.backpackList = UI.scrollView(5, 5);
        this.backpackList.h(1F);

        UIElement column = UI.column(4);

        column.relative(this).w(1F).h(1F);
        column.add(projectLabel, this.projectList, globalLabel, this.backpackList);
        this.add(column);

        /* Right click anywhere on the asset bin: New (sub-menu) plus the
         * import / export / backpack actions. */
        this.context(m ->
        {
            mchorse.bbs_mod.ui.utils.context.ContextAction create = new mchorse.bbs_mod.ui.utils.context.ContextAction(
                mchorse.bbs_mod.ui.utils.icons.Icons.ADD, UIKeys.ASSETS_NEW, null);

            create.subActions = new java.util.ArrayList<>();
            create.subActions.add(new mchorse.bbs_mod.ui.utils.context.ContextAction(
                mchorse.bbs_mod.ui.utils.icons.Icons.SCENE, UIKeys.ASSETS_NEW_SCENE, this::newScene));
            create.subActions.add(new mchorse.bbs_mod.ui.utils.context.ContextAction(
                mchorse.bbs_mod.ui.utils.icons.Icons.PLAYER, UIKeys.ASSETS_NEW_CHARACTER, this::newCharacter));

            m.action(create);
            m.action(mchorse.bbs_mod.ui.utils.icons.Icons.DOWNLOAD, UIKeys.ASSETS_IMPORT, this::importAsset);
            m.action(mchorse.bbs_mod.ui.utils.icons.Icons.FILM, UIKeys.ASSETS_EXPORT, this::exportSequence);
            m.action(mchorse.bbs_mod.ui.utils.icons.Icons.USER, UIKeys.ASSETS_TO_BACKPACK, this::toBackpack);
        });

        this.refresh();
    }

    @Override
    public void render(UIContext context)
    {
        int x = this.area.x;
        int y = this.area.y;
        int ex = this.area.ex();
        int ey = this.area.ey();

        if (ex > x && ey > y)
        {
            /* Panel background - make the asset bin read as a distinct
             * dark panel against the dashboard background. */
            context.batcher.box(x, y, ex, ey, Colors.A90);

            /* Right separator border against the viewport / timeline. */
            context.batcher.box(ex - 1, y, ex, ey, Colors.ACTIVE);
        }

        super.render(context);
    }

    /** Reload both lists from the managers. */
    public void refresh()
    {
        this.projectList.removeAll();
        this.backpackList.removeAll();

        SceneManager scenes = SceneManager.get();

        if (scenes != null)
        {
            List<Scene> list = scenes.getScenes();

            if (list.isEmpty())
            {
                this.projectList.add(this.hint(UIKeys.ASSETS_EMPTY));
            }

        for (Scene scene : list)
        {
            UIButton row = new UIButton(IKey.raw("\uD83D\uDDFA\uFE0F " + scene.name), (b) -> this.panel.openScene(scene));

            row.context((menu) -> this.sceneContextMenu(menu, scenes, scene));
            row.h(20);
            this.projectList.add(row);
        }

        /* Reusable sequences of this project (nestable clip documents). */
        mchorse.bbs_mod.projects.SequenceManager sequences = mchorse.bbs_mod.projects.SequenceManager.get();

        if (sequences != null)
        {
            for (mchorse.bbs_mod.projects.Sequence sequence : sequences.getSequences())
            {
                UIButton row = new UIButton(IKey.raw("\uD83E\uDDE9 " + sequence.name), (b) ->
                {
                    this.panel.openSequence(sequence);
                });

                row.context((menu) -> this.sequenceContextMenu(menu, sequences, sequence));
                row.h(20);
                this.projectList.add(row);
            }
        }
        }

        List<String> items = Backpack.getItems();

        if (items.isEmpty())
        {
            this.backpackList.add(this.hint(UIKeys.ASSETS_EMPTY));
        }

        for (String item : items)
        {
            UIButton row = new UIButton(IKey.raw(item), (b) -> {});

            row.h(20);
            this.backpackList.add(row);
        }

        this.projectList.resize();
        this.backpackList.resize();
    }

    /** Right-click menu for a scene row: open / export / import / rename / delete. */
    private void sceneContextMenu(mchorse.bbs_mod.ui.utils.context.ContextMenuManager menu, SceneManager scenes, Scene scene)
    {
        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.SCENE, UIKeys.PROJECTS_OPEN, () ->
        {
            scenes.setCurrent(scene);
            this.refresh();
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.FILM, UIKeys.ASSETS_EXPORT, () ->
        {
            java.io.File exported = scenes.exportScene(scene);

            if (exported != null)
            {
                this.getContext().notifySuccess(UIKeys.ASSETS_EXPORTED.format(exported.getName()));
            }
            else
            {
                this.getContext().notifyError(UIKeys.ASSETS_EXPORT_FAILED);
            }
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.DOWNLOAD, UIKeys.ASSETS_IMPORT, () ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIImportOverlayPanel((file) ->
            {
                Scene imported = scenes.importScene(file);

                if (imported != null)
                {
                    this.refresh();
                }
                else
                {
                    this.getContext().notifyError(UIKeys.ASSETS_IMPORT_FAILED);
                }
            }));
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.EDIT, UIKeys.SCENES_RENAME, () ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIPromptOverlayPanel(
                UIKeys.SCENES_RENAME,
                UIKeys.PROJECTS_NAME,
                (name) ->
                {
                    if (name != null && !name.trim().isEmpty())
                    {
                        scenes.rename(scene, name.trim());
                        this.refresh();
                    }
                }
            ));
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.TRASH, UIKeys.PROJECTS_DELETE, () ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIConfirmOverlayPanel(
                UIKeys.PROJECTS_DELETE,
                UIKeys.PROJECTS_CONFIRM_DELETE.format(scene.name),
                (result) ->
                {
                    if (result)
                    {
                        scenes.delete(scene);
                        this.refresh();
                    }
                }
            ));
        });
    }

    /** Right-click menu for a sequence row: manage links / open / rename / delete. */
    private void sequenceContextMenu(mchorse.bbs_mod.ui.utils.context.ContextMenuManager menu, mchorse.bbs_mod.projects.SequenceManager sequences, mchorse.bbs_mod.projects.Sequence sequence)
    {
        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.LINK, UIKeys.SEQUENCE_LINKS, () ->
        {
            UIOverlay.addOverlay(this.getContext(), new UISequenceOverlayPanel(sequence));
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.SCENE, UIKeys.PROJECTS_OPEN, () -> this.panel.openSequence(sequence));

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.FILM, UIKeys.ASSETS_EXPORT, () ->
        {
            java.io.File exported = sequences.exportSequence(sequence);

            if (exported != null)
            {
                this.getContext().notifySuccess(UIKeys.ASSETS_EXPORTED.format(exported.getName()));
            }
            else
            {
                this.getContext().notifyError(UIKeys.ASSETS_EXPORT_FAILED);
            }
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.EDIT, UIKeys.SCENES_RENAME, () ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIPromptOverlayPanel(
                UIKeys.SCENES_RENAME,
                UIKeys.PROJECTS_NAME,
                (name) ->
                {
                    if (name != null && !name.trim().isEmpty())
                    {
                        sequences.rename(sequence, name.trim());
                        this.refresh();
                    }
                }
            ));
        });

        menu.action(mchorse.bbs_mod.ui.utils.icons.Icons.TRASH, UIKeys.PROJECTS_DELETE, () ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIConfirmOverlayPanel(
                UIKeys.PROJECTS_DELETE,
                UIKeys.PROJECTS_CONFIRM_DELETE.format(sequence.name),
                (result) ->
                {
                    if (result)
                    {
                        sequences.delete(sequence);
                        this.refresh();
                    }
                }
            ));
        });
    }

    private UIElement hint(IKey label)
    {
        UIElement hint = UI.label(label, 18, Colors.GRAY);

        hint.h(18);

        return hint;
    }

    private void newScene()
    {
        this.panel.newScene();
    }

    private void newCharacter()
    {
        this.panel.newCharacter();
    }

    private void exportSequence()
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || scenes.getCurrent() == null)
        {
            this.getContext().notifyError(UIKeys.ASSETS_EMPTY);

            return;
        }

        mchorse.bbs_mod.projects.SequenceManager sequences = mchorse.bbs_mod.projects.SequenceManager.get();

        if (sequences == null)
        {
            return;
        }

        mchorse.bbs_mod.projects.Sequence sequence = sequences.create(scenes.getCurrent().name + " seq");

        sequences.addRef(sequence, mchorse.bbs_mod.projects.Sequence.SequenceRef.SCENE, scenes.getCurrent().id);
        this.refresh();
    }

    private void importAsset()
    {
        /* Import = pick one of this project's existing scenes (each carries
         * its film with recorded replays) and reference it as an asset. No
         * external file format knowledge needed. */
        UIOverlay.addOverlay(this.getContext(), new UIScenePickOverlayPanel((scene) ->
        {
            mchorse.bbs_mod.projects.SequenceManager sequences = mchorse.bbs_mod.projects.SequenceManager.get();

            if (sequences == null)
            {
                return;
            }

            mchorse.bbs_mod.projects.Sequence sequence = sequences.create(scene.name + " seq");

            sequences.addRef(sequence, mchorse.bbs_mod.projects.Sequence.SequenceRef.SCENE, scene.id);
            this.refresh();
        }));
    }

    private void toBackpack()
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || scenes.getProject() == null)
        {
            this.getContext().notifyError(UIKeys.ASSETS_EMPTY);

            return;
        }

        /* Export the project's form categories (with their assets) into the
         * global backpack - the existing, real cross-project mechanism. */
        List<String> errors = Backpack.exportCategories(scenes.getProject());

        if (!errors.isEmpty())
        {
            this.getContext().notifyError(IKey.raw(errors.get(0)));
        }

        this.refresh();
    }
}
