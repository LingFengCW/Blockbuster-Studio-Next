package mchorse.bbs_mod.ui.dashboard.topbar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.context.DisabledContextAction;
import mchorse.bbs_mod.ui.windows.WindowRegistry;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.undo.EditState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * The persistent tool bar of the whole editor.
 *
 * It is <em>not</em> a dashboard widget: {@link #attach(UIBaseMenu)} mounts it
 * on any top level window (projects, scenes, backpack, animation, the
 * dashboard itself), so the bar is in the exact same place with the exact
 * same menus no matter where the user is.
 *
 * Entries that need the editor are still listed on the other windows, just
 * greyed out through {@link DisabledContextAction}. That keeps the bar
 * visually stable instead of having items appear and vanish.
 *
 * Nothing here is hardcoded - the menus come from {@link TopBarRegistry} and
 * the Window menu comes from {@link WindowRegistry}, both of which third
 * party code extends through events.
 */
public class UITopBar extends UIElement
{
    public static final int HEIGHT = 12;

    private static final int PADDING = 8;
    private static final int GAP = 4;

    private final UIBaseMenu menu;
    private final UIDashboard dashboard;

    private final List<UIButton> menuButtons = new ArrayList<>();

    private boolean laidOut;

    /**
     * Mount the bar on a window.
     *
     * The bar is added to the window's root, pinned to the top. Unlike the
     * dashboard - which lays its own panels out below the bar - these menu
     * windows centre their content on screen, so the top {@value #HEIGHT}
     * pixels are free space anyway and the bar simply overlays them without
     * disturbing the window's own layout. We deliberately leave
     * {@code menu.main}'s flex untouched: re-sizing it (as an early draft
     * did with {@code h(1F, -HEIGHT)}) is what made the project menu render
     * as an empty background.
     *
     * Returns the bar for further tweaking.
     */
    public static UITopBar attach(UIBaseMenu menu)
    {
        UITopBar bar = new UITopBar(menu);

        bar.relative(menu.getRoot());
        bar.x(0, 0).y(0, 0).w(1, 0).h(0, HEIGHT);

        menu.getRoot().add(bar);

        return bar;
    }

    public UITopBar(UIBaseMenu menu)
    {
        super();

        this.menu = menu;
        this.dashboard = menu instanceof UIDashboard editor ? editor : null;

        this.markContainer();

        /* Build (and let everyone extend) both models before reading them. */
        TopBarRegistry.setup();
        WindowRegistry.setup();

        this.rebuild();
    }

    public UIDashboard getDashboard()
    {
        return this.dashboard;
    }

    public UIBaseMenu getMenu()
    {
        return this.menu;
    }

    /**
     * Re-create the menu buttons from the registry. Called once at startup
     * and available to modules that register menus late.
     */
    public void rebuild()
    {
        for (UIButton button : this.menuButtons)
        {
            this.remove(button);
        }

        this.menuButtons.clear();

        for (TopBarMenu menu : TopBarRegistry.getMenus())
        {
            UIButton button = new UIButton(menu.label, (b) -> this.openMenu(menu));

            button.y(0, 1).h(0, 10);

            this.menuButtons.add(button);
            this.add(button);
        }

        this.laidOut = false;
    }

    /** Snapshot of "where the user is" handed to entries on every click. */
    public TopBarContext buildContext()
    {
        return new TopBarContext(this.menu, this.dashboard);
    }

    private void openMenu(TopBarMenu menu)
    {
        TopBarContext ctx = this.buildContext();
        List<TopBarItem> items = menu.getVisibleItems(ctx);

        if (items.isEmpty())
        {
            return;
        }

        ContextMenuManager manager = new ContextMenuManager();

        for (TopBarItem item : items)
        {
            if (item.isEnabled(ctx))
            {
                manager.action(item.icon, item.label, () -> item.run(ctx));
            }
            else
            {
                manager.action(new DisabledContextAction(item.icon, item.label));
            }
        }

        this.getContext().replaceContextMenu(manager.create());
    }

    @Override
    public void render(UIContext context)
    {
        try
        {
            this.renderBar(context);
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("UITopBar.render failed: main=" + this.menu.main.area + " bar=" + this.area, e);
        }
    }

    private void renderBar(UIContext context)
    {
        if (!this.laidOut)
        {
            this.layout(context);

            this.laidOut = true;
        }

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A100 | BBSSettings.primaryColor.get());

        super.render(context);
    }

    /**
     * Lay the bar out from measured text, so translations of any length (and
     * long project names) never overlap.
     */
    private void layout(UIContext context)
    {
        var font = context.batcher.getFont();

        int x = PADDING;

        for (UIButton button : this.menuButtons)
        {
            int width = font.getWidth(button.label.get()) + 12;

            button.x(0, x).w(0, width);

            x += width + GAP;
        }

        this.resize();
    }

    private String buildTitle()
    {
        BBSProject project = ProjectManager.get().getCurrent();
        String name = project != null ? project.name : UIKeys.MENUBAR_TITLE_DEFAULT.get();

        SceneManager scenes = SceneManager.get();
        Scene scene = scenes == null ? null : scenes.getCurrent();

        if (scene != null)
        {
            name = name + "  /  " + scene.name;
        }

        return name + (EditState.isDirty() ? " *" : "");
    }
}
