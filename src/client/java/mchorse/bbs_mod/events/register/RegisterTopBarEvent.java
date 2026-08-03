package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarMenu;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarRegistry;

/**
 * Fired once, right before the dashboard's persistent top bar is built for
 * the first time. This is the injection point for the window tool bar: any
 * module may add its own menu or extend / override a built-in one.
 *
 * <pre>
 * BBSMod.events.register(RegisterTopBarEvent.class, (event) ->
 * {
 *     event.menu(TopBarRegistry.SCENE)
 *         .action("mymod.bake", MyKeys.BAKE, (dashboard) -> bake(dashboard));
 * });
 * </pre>
 */
public class RegisterTopBarEvent
{
    /** Extend an existing menu (File, Edit, Scene, Tools, Window, Help). */
    public TopBarMenu menu(String id)
    {
        TopBarMenu menu = TopBarRegistry.menu(id);

        if (menu == null)
        {
            throw new IllegalArgumentException("Top bar menu \"" + id + "\" doesn't exist, use menu(id, label, order) to create it!");
        }

        return menu;
    }

    /** Get or create a menu. Order decides the left-to-right placement. */
    public TopBarMenu menu(String id, IKey label, int order)
    {
        return TopBarRegistry.menu(id, label, order);
    }
}
