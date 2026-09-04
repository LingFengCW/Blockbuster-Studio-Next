package mchorse.bbs_mod.ui.scenes;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.projects.Sequence;
import mchorse.bbs_mod.projects.SequenceManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.projects.UIProjectMenu;
import net.minecraft.client.Minecraft;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sequence / scene browser of the active project. Opening a project lands
 * here first: the editor requires an editing target, so the user must pick
 * an existing sequence or scene (or create one via the buttons or the right
 * click menu) before the dashboard opens with its content loaded.
 *
 * Flow: main menu → {@link UIProjectMenu} (pick a project) → this menu (pick
 * a sequence or scene) → dashboard with that target's content loaded.
 */
public class UISceneMenu extends UIBaseMenu
{
    /** Tab index: 0 = sequences, 1 = scenes. */
    private int tab;

    private final UIScrollView list;
    private final UILabel subtitle;
    private final UIButton sequencesTab;
    private final UIButton scenesTab;
    private final UIButton newBtn;
    private final UIButton renameBtn;
    private final UIButton deleteBtn;
    private final UIButton openBtn;
    private final UIElement column;

    private Scene selectedScene;
    private Sequence selectedSequence;
    private final Map<Scene, UIButton> sceneButtons = new HashMap<>();
    private final Map<Sequence, UIButton> sequenceButtons = new HashMap<>();

    public UISceneMenu()
    {
        UIElement column = UI.column(10);

        column.relative(this.main).xy(0.5F, 0.5F).w(440).h(500).anchor(0.5F, 0.5F);
        this.column = column;

        column.add(UI.label(UIKeys.SCENES_TITLE, 28));

        BBSProject project = ProjectManager.get().getCurrent();

        this.subtitle = UI.label(project == null ? UIKeys.SCENES_NO_PROJECT : IKey.constant(project.name), 20, Colors.LIGHTER_GRAY);

        column.add(this.subtitle);

        /* Tab switch: sequences / scenes. */
        UIElement tabs = UI.row(5);

        tabs.h(24);

        this.sequencesTab = new UIButton(UIKeys.SEQUENCES_TITLE, (b) -> this.switchTab(0));
        this.scenesTab = new UIButton(UIKeys.SCENES_TITLE, (b) -> this.switchTab(1));
        tabs.add(this.sequencesTab, this.scenesTab);

        column.add(tabs);

        this.list = UI.scrollView(5, 5);
        this.list.h(320);
        column.add(this.list);

        this.newBtn = new UIButton(UIKeys.SCENES_NEW_SEQUENCE, (b) -> this.createCurrent());
        this.renameBtn = new UIButton(UIKeys.SCENES_RENAME, (b) -> this.renameCurrent());
        this.deleteBtn = new UIButton(UIKeys.SCENES_DELETE, (b) -> this.deleteCurrent());
        this.openBtn = new UIButton(UIKeys.SCENES_OPEN, (b) -> this.openSelected());

        UIButton backBtn = new UIButton(UIKeys.SCENES_BACK, (b) -> UIScreen.open(new UIProjectMenu()));

        this.renameBtn.setEnabled(false);
        this.deleteBtn.setEnabled(false);
        this.openBtn.setEnabled(false);

        UIElement buttons = UI.row(5);

        buttons.h(24);
        buttons.add(this.newBtn, this.renameBtn, this.deleteBtn, this.openBtn, backBtn);

        column.add(buttons);

        /* Right click anywhere in this menu: create a new actor sequence or
         * a new scene. UIBaseMenu is not a UIElement, so the menu is wired
         * to the root element and the list itself. */
        this.main.context(m ->
        {
            m.action(UIKeys.SCENES_NEW_SEQUENCE, this::createSequence);
            m.action(UIKeys.SCENES_CREATE, this::createScene);
        });

        this.list.context(m ->
        {
            m.action(UIKeys.SCENES_NEW_SEQUENCE, this::createSequence);
            m.action(UIKeys.SCENES_CREATE, this::createScene);
        });

        this.main.add(column);

        this.switchTab(0);
    }

    /** Clamp the column to the window so a small window never hides the menu. */
    @Override
    protected void viewportSet()
    {
        int avail = this.viewport.h - 60;
        int colH = Math.max(160, Math.min(500, avail));

        this.column.h(colH);
        this.list.h(Math.max(80, colH - 28 - 20 - 24 - 24 - 30));

        this.column.xy(0.5F, 0.5F).anchor(0.5F, 0.5F);
    }

    private SceneManager scenes()
    {
        return SceneManager.get();
    }

    private SequenceManager sequences()
    {
        return SequenceManager.get();
    }

    private void switchTab(int tab)
    {
        this.tab = tab;

        this.sequencesTab.color(tab == 0 ? Colors.A50 | Colors.ACTIVE : 0);
        this.scenesTab.color(tab == 1 ? Colors.A50 | Colors.ACTIVE : 0);

        this.newBtn.label = tab == 0 ? UIKeys.SCENES_NEW_SEQUENCE : UIKeys.SCENES_CREATE;

        this.refresh();
    }

    private void refresh()
    {
        this.sceneButtons.clear();
        this.sequenceButtons.clear();
        this.list.removeAll();

        if (this.tab == 0)
        {
            this.refreshSequences();
        }
        else
        {
            this.refreshScenes();
        }

        this.list.resize();
        this.selectClear();
    }

    private void refreshSequences()
    {
        SequenceManager sequences = this.sequences();

        if (sequences == null)
        {
            this.addInfoLabel(UIKeys.SCENES_NO_PROJECT);

            return;
        }

        List<Sequence> list = sequences.getSequences();

        if (list.isEmpty())
        {
            this.addInfoLabel(UIKeys.SEQUENCES_EMPTY);
        }

        for (Sequence sequence : list)
        {
            final long[] lastClick = {0L};

            UIButton button = new UIButton(IKey.constant(sequence.name), (b) ->
            {
                long now = System.currentTimeMillis();

                /* Double-click (within 350ms) opens the sequence in the
                 * editor; a single click just selects/highlights it. */
                if (now - lastClick[0] < 350)
                {
                    lastClick[0] = 0;
                    this.openSequence(sequence);
                }
                else
                {
                    lastClick[0] = now;
                    this.select(sequence);
                }
            });

            button.h(24);

            UIButton remove = new UIButton(IKey.raw("\u2716"), (b) ->
            {
                this.select(sequence);
                this.deleteCurrent();
            });

            remove.h(24).w(24);

            UIElement entry = UI.row(2);

            entry.h(24);
            entry.add(button, remove);

            this.sequenceButtons.put(sequence, button);
            this.list.add(entry);
        }
    }

    private void refreshScenes()
    {
        SceneManager scenes = this.scenes();

        if (scenes == null)
        {
            this.addInfoLabel(UIKeys.SCENES_NO_PROJECT);

            return;
        }

        List<Scene> list = scenes.getScenes();

        if (list.isEmpty())
        {
            this.addInfoLabel(UIKeys.SCENES_EMPTY);
        }

        for (Scene scene : list)
        {
            final long[] lastClick = {0L};

            UIButton button = new UIButton(IKey.constant(scene.name), (b) ->
            {
                long now = System.currentTimeMillis();

                /* Double-click (within 350ms) opens the scene in the editor;
                 * a single click just selects/highlights it. */
                if (now - lastClick[0] < 350)
                {
                    lastClick[0] = 0;
                    this.openScene(scene);
                }
                else
                {
                    lastClick[0] = now;
                    this.select(scene);
                }
            });

            button.h(24);

            UIButton remove = new UIButton(IKey.raw("\u2716"), (b) ->
            {
                this.select(scene);
                this.deleteCurrent();
            });

            remove.h(24).w(24);

            UIElement entry = UI.row(2);

            entry.h(24);
            entry.add(button, remove);

            this.sceneButtons.put(scene, button);
            this.list.add(entry);
        }
    }

    private void addInfoLabel(IKey key)
    {
        UILabel label = UI.label(key, 20);

        label.h(20);
        this.list.add(label);
    }

    private void select(Scene scene)
    {
        this.selectedScene = scene;
        this.selectedSequence = null;

        for (Map.Entry<Scene, UIButton> entry : this.sceneButtons.entrySet())
        {
            entry.getValue().color(entry.getKey() == scene ? Colors.A50 | Colors.ACTIVE : 0);
        }

        for (UIButton button : this.sequenceButtons.values())
        {
            button.color(0);
        }

        this.renameBtn.setEnabled(scene != null);
        this.deleteBtn.setEnabled(scene != null);
        this.openBtn.setEnabled(scene != null);
    }

    private void select(Sequence sequence)
    {
        this.selectedSequence = sequence;
        this.selectedScene = null;

        for (Map.Entry<Sequence, UIButton> entry : this.sequenceButtons.entrySet())
        {
            entry.getValue().color(entry.getKey() == sequence ? Colors.A50 | Colors.ACTIVE : 0);
        }

        for (UIButton button : this.sceneButtons.values())
        {
            button.color(0);
        }

        this.renameBtn.setEnabled(sequence != null);
        this.deleteBtn.setEnabled(sequence != null);
        this.openBtn.setEnabled(sequence != null);
    }

    private void selectClear()
    {
        this.selectedScene = null;
        this.selectedSequence = null;

        for (UIButton button : this.sceneButtons.values())
        {
            button.color(0);
        }

        for (UIButton button : this.sequenceButtons.values())
        {
            button.color(0);
        }

        this.renameBtn.setEnabled(false);
        this.deleteBtn.setEnabled(false);
        this.openBtn.setEnabled(false);
    }

    /* Creation */

    private void createCurrent()
    {
        if (this.tab == 0)
        {
            this.createSequence();
        }
        else
        {
            this.createScene();
        }
    }

    private void createSequence()
    {
        SequenceManager sequences = this.sequences();

        if (sequences == null)
        {
            UIScreen.open(new UIProjectMenu());

            return;
        }

        UIOverlay.addOverlay(this.context, new UIPromptOverlayPanel(
            UIKeys.SCENES_NEW_SEQUENCE,
            UIKeys.SCENES_NAME,
            (str) ->
            {
                String name = str == null ? "" : str.trim();

                if (name.isEmpty())
                {
                    return;
                }

                Sequence sequence = sequences.create(name);

                this.refresh();
                this.select(sequence);

                /* Open the new sequence in the editor right away, matching the
                 * scene-create behaviour (land on a clean editor, not the
                 * previously-open work). */
                this.openSequence(sequence);
            }
        ));
    }

    private void createScene()
    {
        SceneManager scenes = this.scenes();

        if (scenes == null)
        {
            UIScreen.open(new UIProjectMenu());

            return;
        }

        lingfeng.bbsnext.mcef.NativeDialog.sceneDialog((name, world) ->
        {
            if (name == null)
            {
                return;
            }

            /* NativeDialog callback runs on the Swing EDT; hop back to the
             * Minecraft main thread before mutating project state. */
            Minecraft.getInstance().execute(() ->
            {
                String sceneName = name.trim();

                if (sceneName.isEmpty())
                {
                    sceneName = UIKeys.SCENES_DEFAULT_NAME.format(scenes.getScenes().size() + 1).get();
                }

                Scene scene = scenes.create(sceneName, world);

                this.refresh();
                this.select(scene);

                /* Open the freshly created scene in the editor right away, so
                 * the user lands on a clean (empty) scene instead of the
                 * previously-open one - otherwise the editor would still show
                 * the old scene's cameras/clips and look "stale". */
                this.openScene(scene);
            });
        });
    }

    /* Rename / delete */

    private void renameCurrent()
    {
        if (this.tab == 0)
        {
            this.renameSequence();
        }
        else
        {
            this.renameScene();
        }
    }

    private void renameScene()
    {
        SceneManager scenes = this.scenes();
        Scene scene = this.selectedScene;

        if (scenes == null || scene == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.context, new UIPromptOverlayPanel(
            UIKeys.SCENES_RENAME,
            UIKeys.SCENES_NAME,
            (str) ->
            {
                String name = str == null ? "" : str.trim();

                if (!name.isEmpty())
                {
                    scenes.rename(scene, name);
                    this.refresh();
                    this.select(scene);
                }
            }
        ));
    }

    private void renameSequence()
    {
        SequenceManager sequences = this.sequences();
        Sequence sequence = this.selectedSequence;

        if (sequences == null || sequence == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.context, new UIPromptOverlayPanel(
            UIKeys.SCENES_RENAME,
            UIKeys.SCENES_NAME,
            (str) ->
            {
                String name = str == null ? "" : str.trim();

                if (!name.isEmpty())
                {
                    sequences.rename(sequence, name);
                    this.refresh();
                    this.select(sequence);
                }
            }
        ));
    }

    private void deleteCurrent()
    {
        if (this.tab == 0)
        {
            this.deleteSequence();
        }
        else
        {
            this.deleteScene();
        }
    }

    private void deleteScene()
    {
        SceneManager scenes = this.scenes();
        Scene scene = this.selectedScene;

        if (scenes == null || scene == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.context, new UIConfirmOverlayPanel(
            UIKeys.SCENES_DELETE,
            UIKeys.SCENES_CONFIRM_DELETE.format(scene.name),
            (result) ->
            {
                if (result)
                {
                    scenes.delete(scene);
                    this.refresh();
                }
            }
        ));
    }

    private void deleteSequence()
    {
        SequenceManager sequences = this.sequences();
        Sequence sequence = this.selectedSequence;

        if (sequences == null || sequence == null)
        {
            return;
        }

        UIOverlay.addOverlay(this.context, new UIConfirmOverlayPanel(
            UIKeys.SCENES_DELETE,
            UIKeys.SCENES_CONFIRM_DELETE.format(sequence.name),
            (result) ->
            {
                if (result)
                {
                    sequences.delete(sequence);
                    this.refresh();
                }
            }
        ));
    }

    /* Opening */

    private void openSelected()
    {
        if (this.tab == 0)
        {
            this.openSequence(this.selectedSequence);
        }
        else
        {
            this.openScene(this.selectedScene);
        }
    }

    private void openScene(Scene scene)
    {
        if (scene == null)
        {
            this.context.notifyError(UIKeys.SCENES_EMPTY);

            return;
        }

        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            UIScreen.open(dashboard);
            openScene(dashboard, scene);
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to open scene " + scene.name, e);
            this.context.notifyError(IKey.raw("Failed to open scene: " + e.getMessage()));
        }
    }

    private void openSequence(Sequence sequence)
    {
        if (sequence == null)
        {
            this.context.notifyError(UIKeys.SEQUENCES_EMPTY);

            return;
        }

        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();
            UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

            UIScreen.open(dashboard);

            if (panel != null)
            {
                dashboard.setPanel(panel);
                panel.openSequence(sequence);
            }
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to open sequence " + sequence.name, e);
            this.context.notifyError(IKey.raw("Failed to open sequence: " + e.getMessage()));
        }
    }

    /**
     * Make the given scene the active one and push its content into the
     * editor. Static so that the top bar can reuse it right after creating
     * a scene from the menu bar.
     */
    public static void openScene(UIDashboard dashboard, Scene scene)
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || scene == null || dashboard == null)
        {
            return;
        }

        scenes.setCurrent(scene);

        Film film = scenes.loadFilm(scene);

        film.setId(scene.id);

        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null)
        {
            dashboard.setPanel(panel);
            panel.fill(film);
        }
    }
}
