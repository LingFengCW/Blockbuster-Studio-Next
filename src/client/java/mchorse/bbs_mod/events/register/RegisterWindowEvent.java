package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.windows.WindowEntry;
import mchorse.bbs_mod.ui.windows.WindowRegistry;

/**
 * Fired once, right after BBS registered its own windows. This is the hook
 * for adding a brand new top level screen to the editor:
 *
 * <pre>
 * BBSMod.events.register(RegisterWindowEvent.class, (event) ->
 * {
 *     event.window("mymod.tools", MyKeys.TOOLS, () -> UIScreen.open(new UIMyToolsMenu()))
 *         .order(100)
 *         .enabled(() -> ProjectManager.get().getCurrent() != null);
 * });
 * </pre>
 */
public class RegisterWindowEvent
{
    public WindowEntry window(String id, IKey label, Runnable open)
    {
        return WindowRegistry.register(new WindowEntry(id, label, open));
    }

    /** Look up an already registered window, or null. */
    public WindowEntry window(String id)
    {
        return WindowRegistry.get(id);
    }

    /** Drop a built-in window, e.g. to replace it with your own. */
    public void remove(String id)
    {
        WindowRegistry.remove(id);
    }
}
