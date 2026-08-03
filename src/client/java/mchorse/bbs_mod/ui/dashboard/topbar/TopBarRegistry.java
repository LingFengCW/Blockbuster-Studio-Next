package mchorse.bbs_mod.ui.dashboard.topbar;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.events.register.RegisterTopBarEvent;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.ui.particles.UIParticleSchemePanel;
import mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.windows.WindowEntry;
import mchorse.bbs_mod.ui.windows.WindowRegistry;
import mchorse.bbs_mod.utils.undo.EditState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The extension point of the persistent top bar.
 *
 * The bar itself renders nothing that is hardcoded - it walks this registry.
 * BBS fills in its own menus in {@link #registerDefaults()} and then fires
 * {@link RegisterTopBarEvent} exactly once, which is the hook every other
 * module (including script plugins) uses to add or override entries:
 *
 * <pre>
 * BBSMod.events.register(RegisterTopBarEvent.class, (event) -&gt;
 * {
 *     event.menu(TopBarRegistry.TOOLS)
 *         .action("mymod.tool", MyKeys.TOOL, (context) -&gt; ...);
 *
 *     event.menu("mymod", MyKeys.MY_MENU, 55)
 *         .action("mymod.about", MyKeys.ABOUT, (context) -&gt; ...);
 * });
 * </pre>
 *
 * Entries declare what they need through {@code enabled(...)} rather than
 * {@code visible(...)}: the bar is mounted on every window, and an entry that
 * cannot run right now is greyed out instead of vanishing.
 */
public class TopBarRegistry
{
    public static final String FILE = "file";
    public static final String EDIT = "edit";
    public static final String WINDOW = "window";
    public static final String HELP = "help";

    private static final Map<String, TopBarMenu> MENUS = new LinkedHashMap<>();

    private static boolean setup;

    /**
     * Build the default menus and let everybody else extend them. Safe to
     * call repeatedly - only the first call does anything.
     */
    public static void setup()
    {
        if (setup)
        {
            return;
        }

        setup = true;

        registerDefaults();

        BBSMod.events.post(new RegisterTopBarEvent());
    }

    /** Wipe everything, mostly useful for reloads and tests. */
    public static void clear()
    {
        MENUS.clear();

        setup = false;
    }

    /** Get an existing menu or create it with a fallback label. */
    public static TopBarMenu menu(String id, IKey label, int order)
    {
        TopBarMenu menu = MENUS.get(id);

        if (menu == null)
        {
            menu = new TopBarMenu(id, label, order);

            MENUS.put(id, menu);
        }

        return menu;
    }

    /** Get an already registered menu, or null. */
    public static TopBarMenu menu(String id)
    {
        return MENUS.get(id);
    }

    public static List<TopBarMenu> getMenus()
    {
        List<TopBarMenu> menus = new ArrayList<>(MENUS.values());

        menus.sort(Comparator.comparingInt((TopBarMenu menu) -> menu.order));

        return menus;
    }

    /* Built-in menus */

    private static void registerDefaults()
    {
        /* File: project-level operations, following the layout of a classic
         * NLE (new / open / save / export / close). Works from any window. */
        TopBarMenu file = menu(FILE, UIKeys.MENUBAR_FILE, 0);

        file.action("file.new", UIKeys.MENUBAR_FILE_NEW, (context) -> UIScreen.open(new mchorse.bbs_mod.ui.projects.UIProjectMenu()));
        file.action("file.open", UIKeys.MENUBAR_FILE_OPEN, (context) -> UIScreen.open(new mchorse.bbs_mod.ui.projects.UIProjectMenu()));
        file.action("file.save", UIKeys.MENUBAR_FILE_SAVE, TopBarRegistry::save).enabled(TopBarRegistry::hasProject);
        file.action("file.export", UIKeys.MENUBAR_FILE_EXPORT, TopBarRegistry::exportScene).enabled(TopBarRegistry::hasProject);
        file.action("file.close", UIKeys.MENUBAR_FILE_CLOSE, (context) -> closeEditor());

        /* Edit: strictly editor state, greyed out elsewhere. */
        TopBarMenu edit = menu(EDIT, UIKeys.MENUBAR_EDIT, 10);

        edit.action("edit.undo", UIKeys.MENUBAR_EDIT_UNDO, (context) -> context.panel().undo()).enabled(TopBarMenu.requiresPanel());
        edit.action("edit.redo", UIKeys.MENUBAR_EDIT_REDO, (context) -> context.panel().redo()).enabled(TopBarMenu.requiresPanel());

        /* No Scene menu: scenes are created inside the editor from the asset
         * bin (right-click -> New -> Scene), the single canonical entry. A
         * duplicate toolbar entry just opens another prompt screen. */

        /* No Tools menu: the graph panel was removed from the dashboard
         * along with the other info panels. */

        /*
         * Window: mirrors the window registry instead of listing screens by
         * hand, so a window registered by another module shows up here for
         * free - and stays greyed out while its precondition is unmet.
         */
        TopBarMenu window = menu(WINDOW, UIKeys.MENUBAR_WINDOW, 40);

        window.dynamic((context) ->
        {
            List<TopBarItem> items = new ArrayList<>();
            int order = 0;

            for (WindowEntry entry : WindowRegistry.getWindows())
            {
                TopBarItem item = new TopBarItem("window.open." + entry.id, entry.label, (c) -> WindowRegistry.open(entry.id));

                item.order(order).icon(entry.icon).enabled((c) -> entry.isEnabled());
                items.add(item);

                order += 10;
            }

            return items;
        });

        window.action("window.toggle", UIKeys.MENUBAR_WINDOW_TOGGLE, (context) -> context.dashboard.main.toggleVisible())
            .order(1000)
            .enabled(TopBarMenu.requiresDashboard());
        window.action("window.cycle", UIKeys.MENUBAR_WINDOW_CYCLE, (context) -> context.dashboard.cyclePanels())
            .order(1010)
            .enabled(TopBarMenu.requiresDashboard());
        window.action("window.reset_camera", UIKeys.MENUBAR_WINDOW_RESET_CAM, (context) -> context.dashboard.copyCurrentEntityCamera())
            .order(1020)
            .enabled(TopBarMenu.requiresDashboard());

        /* Help: only needs a window to hang the overlay on - always usable. */
        TopBarMenu help = menu(HELP, UIKeys.MENUBAR_HELP, 50);

        help.action("help.shortcuts", UIKeys.MENUBAR_HELP_SHORTCUTS, (context) ->
            UIOverlay.addOverlay(context.context(), new UIMessageOverlayPanel(UIKeys.MENUBAR_HELP_SHORTCUTS, UIKeys.MENUBAR_HELP_SHORTCUTS_DESC), 340, 220));
        help.action("help.about", UIKeys.MENUBAR_HELP_ABOUT, (context) ->
            UIOverlay.addOverlay(context.context(), new UIMessageOverlayPanel(UIKeys.MENUBAR_HELP_ABOUT, UIKeys.MENUBAR_HELP_ABOUT_DESC), 340, 180));
    }

    /* Shared actions */

    private static boolean hasProject(TopBarContext context)
    {
        return ProjectManager.get().getCurrent() != null;
    }

    /**
     * Leaving the editor must not blank the game: with no world loaded there
     * is nothing behind the UI, so a null screen renders black.
     */
    private static void closeEditor()
    {
        Minecraft mc = Minecraft.getInstance();

        mc.gui.setScreen(mc.level == null ? new TitleScreen() : null);
    }

    /**
     * Export the current scene as a sequence - the project's publish unit.
     * Mirrors the asset bin's export action so the File menu behaves the
     * same way as right-clicking the asset bin.
     */
    private static void exportScene(TopBarContext context)
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || scenes.getCurrent() == null)
        {
            return;
        }

        mchorse.bbs_mod.projects.SequenceManager sequences = mchorse.bbs_mod.projects.SequenceManager.get();

        if (sequences == null)
        {
            return;
        }

        mchorse.bbs_mod.projects.Sequence sequence = sequences.create(scenes.getCurrent().name + " seq");

        sequences.addRef(sequence, mchorse.bbs_mod.projects.Sequence.SequenceRef.SCENE, scenes.getCurrent().id);

        UIContext ui = context.context();

        if (ui != null)
        {
            ui.notifySuccess(UIKeys.MENUBAR_SAVED);
        }
    }

    /** Send a scene to the editor, opening the editor first if needed. */
    /**
     * Entity part control: pick an entity of the current scene, then pick one
     * of its bones. The bone is handed over to the replay editor, which is
     * what actually drives per-limb transforms.
     */
    private static void openParts(TopBarContext context)
    {
        UIDashboard dashboard = context.dashboard;
        UIFilmPanel panel = context.panel(UIFilmPanel.class);

        if (panel == null || panel.getData() == null)
        {
            context.context().notifyError(UIKeys.MENUBAR_SCENE_PARTS_NO_SCENE);

            return;
        }

        dashboard.setPanel(panel);

        List<Replay> replays = panel.getData().replays.getList();

        if (replays.isEmpty())
        {
            context.context().notifyError(UIKeys.MENUBAR_SCENE_PARTS_NO_ENTITY);

            return;
        }

        context.context().replaceContextMenu((menu) ->
        {
            for (Replay replay : replays)
            {
                Form form = replay.form.get();
                String name = form == null ? "-" : form.getDisplayName();

                menu.action(Icons.LIMB, IKey.constant(name), () -> openBones(context, panel, replay));
            }
        });
    }

    private static void openBones(TopBarContext context, UIFilmPanel panel, Replay replay)
    {
        Form form = replay.form.get();
        Collection<String> bones = Collections.emptyList();

        if (form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model != null && model.model != null)
            {
                bones = model.model.getAllGroupKeys();
            }
        }

        if (bones.isEmpty())
        {
            /* Not a rigged form - just focus the entity itself */
            panel.replayEditor.setReplay(replay);
            panel.replayEditor.pickForm(form, "");

            return;
        }

        List<String> sorted = new ArrayList<>(bones);

        Collections.sort(sorted);

        context.context().replaceContextMenu((menu) ->
        {
            for (String bone : sorted)
            {
                menu.action(Icons.LIMB, IKey.constant(bone), () ->
                {
                    panel.replayEditor.setReplay(replay);
                    panel.replayEditor.pickForm(form, bone);
                });
            }

            menu.autoKeys();
        });
    }

    /**
     * Save everything that is currently open: project metadata, the active
     * panel and - when a scene is open - the scene's own payload. Works
     * without the editor, in which case only the project is written.
     */
    private static void save(TopBarContext context)
    {
        BBSProject project = ProjectManager.get().getCurrent();

        if (project != null)
        {
            ProjectManager.get().save(project);
        }

        UIDashboardPanel panel = context.panel();

        if (panel != null)
        {
            panel.save();
        }

        SceneManager scenes = SceneManager.get();

        if (scenes != null && scenes.getCurrent() != null && panel instanceof UIFilmPanel film && film.getData() != null)
        {
            scenes.saveFilm(scenes.getCurrent(), film.getData());
        }

        EditState.markSaved();

        UIContext ui = context.context();

        if (ui != null)
        {
            ui.notifySuccess(UIKeys.MENUBAR_SAVED);
        }
    }
}
