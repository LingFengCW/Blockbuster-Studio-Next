package mchorse.bbs_mod.ui.windows;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.events.register.RegisterWindowEvent;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.projects.UIBackpackMenu;
import mchorse.bbs_mod.ui.projects.UIProjectMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The registry of every top level window of the editor.
 *
 * This is the non-graphical half of the tool bar: the bar only draws what is
 * in here. Windows are addressed by a stable id, so anything (a keybind, a
 * script, another mod, a top bar entry) can say "open the scenes window"
 * without importing the screen class.
 */
public class WindowRegistry
{
    public static final String PROJECTS = "projects";
    public static final String SCENES = "scenes";
    public static final String EDITOR = "editor";
    public static final String BACKPACK = "backpack";

    private static final Map<String, WindowEntry> WINDOWS = new LinkedHashMap<>();

    private static boolean setup;

    /** Build the defaults and let everybody else extend them. Idempotent. */
    public static void setup()
    {
        if (setup)
        {
            return;
        }

        setup = true;

        registerDefaults();

        BBSMod.events.post(new RegisterWindowEvent());
    }

    public static void clear()
    {
        WINDOWS.clear();

        setup = false;
    }

    public static WindowEntry register(WindowEntry entry)
    {
        if (entry.order == 0)
        {
            entry.order(WINDOWS.size() * 10);
        }

        WINDOWS.put(entry.id, entry);

        return entry;
    }

    public static WindowEntry window(String id, IKey label, Runnable open)
    {
        return register(new WindowEntry(id, label, open));
    }

    public static WindowEntry get(String id)
    {
        return WINDOWS.get(id);
    }

    public static void remove(String id)
    {
        WINDOWS.remove(id);
    }

    public static List<WindowEntry> getWindows()
    {
        setup();

        List<WindowEntry> windows = new ArrayList<>(WINDOWS.values());

        windows.sort(Comparator.comparingInt((WindowEntry entry) -> entry.order));

        return windows;
    }

    /** Open a registered window by id. Returns false if unknown or disabled. */
    public static boolean open(String id)
    {
        setup();

        WindowEntry entry = WINDOWS.get(id);

        if (entry == null || !entry.isEnabled())
        {
            return false;
        }

        entry.open();

        return true;
    }

    private static void registerDefaults()
    {
        /* The project picker and the scene manager were removed entirely:
         * picking a work happens from the title screen button (which skips
         * straight into the dashboard when a project is already active),
         * and scenes are managed from the asset bin. The window menu only
         * offers the dashboard workspace and the backpack. */
        window(EDITOR, UIKeys.WINDOWS_EDITOR, () -> UIScreen.open(BBSModClient.getDashboard()))
            .order(20)
            .enabled(() -> ProjectManager.get().getCurrent() != null);

        /* The backpack is the cross-project asset store: export the current
         * project's assets into named categories, then import a category
         * into any other project. The asset bin only *lists* backpack
         * categories, so this management window is the real entry. */
        window(BACKPACK, UIKeys.WINDOWS_BACKPACK, UIBackpackMenu::open)
            .order(30)
            .enabled(() -> ProjectManager.get().getCurrent() != null);
    }
}
