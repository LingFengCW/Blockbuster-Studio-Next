package mchorse.bbs_mod.ui.titlebar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarContext;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarItem;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarMenu;
import mchorse.bbs_mod.ui.dashboard.topbar.TopBarRegistry;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The editor's tool bar, rendered on the main menu (the title screen).
 *
 * The title screen is a vanilla screen, so it cannot host the BBS
 * {@code UITopBar} (that one needs a {@code UIBaseMenu} to render into).
 * This widget draws the same registry-driven bar - the same menus from
 * {@link TopBarRegistry} - with the vanilla rendering pipeline instead.
 *
 * Everything that needs the editor is greyed out here, exactly like on any
 * other non-dashboard window, while the entries that only need a window
 * (File -&gt; Projects / Scenes, Scene -&gt; Manage, ...) work straight away.
 */
public class TitleToolbarWidget extends AbstractButton
{
    private static final int BAR_H = 18;
    private static final int ITEM_H = 16;
    private static final int PADDING = 8;
    private static final int GAP = 4;

    private static final int COLOR_BAR = Colors.A100 | 0xFF1E2430;
    private static final int COLOR_TEXT = Colors.WHITE;
    private static final int COLOR_TEXT_DISABLED = Colors.GRAY;
    private static final int COLOR_HOVER = Colors.A50 | 0xFFFFFF;

    /** Always null context: the title screen has neither a window nor the editor. */
    private static final TopBarContext CTX = new TopBarContext(null, null);

    private final List<int[]> menuRects = new ArrayList<>();      // x, y, w, h
    private final List<int[]> itemRects = new ArrayList<>();      // x, y, w, h + item index
    private final List<TopBarItem> itemRefs = new ArrayList<>();
    private int[] closeRect;

    private String openMenuId;
    private int screenWidth;

    public TitleToolbarWidget()
    {
        super(0, 0, 1, BAR_H, Component.empty());
    }

    @Override
    public int getWidth()
    {
        return Math.max(this.screenWidth, 1);
    }

    @Override
    public int getHeight()
    {
        return this.openMenuId == null ? BAR_H : BAR_H + this.visibleItems().size() * ITEM_H + 2;
    }

    /* Render */

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a)
    {
        this.screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        this.menuRects.clear();
        this.itemRects.clear();
        this.itemRefs.clear();

        /* Bar background. */
        g.fill(0, 0, this.getWidth(), BAR_H, COLOR_BAR);

        /* Self-drawn window frame around the whole screen (hidden in
         * fullscreen). With the system decoration removed, the window edge
         * would otherwise be bare desktop. */
        if (!isFullscreen())
        {
            int w = this.getWidth();
            int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int primary = BBSSettings.primaryColor.get();
            int b = 2;

            g.fill(0, 0, w, b, primary);
            g.fill(0, h - b, w, h, primary);
            g.fill(0, 0, b, h, primary);
            g.fill(w - b, 0, w, h, primary);
        }

        /* Menu buttons only - the window chrome (icon, title, close) belongs
         * to the native OS title bar now, so nothing doubles up. */
        List<TopBarMenu> menus = TopBarRegistry.getMenus();
        int x = PADDING;

        for (TopBarMenu menu : menus)
        {
            String label = menu.label.get();
            int w = Minecraft.getInstance().font.width(label) + 12;
            boolean hover = this.inside(mouseX, mouseY, x, 0, w, BAR_H);

            if (hover)
            {
                g.fill(x, 0, x + w, BAR_H, COLOR_HOVER);
            }

            g.text(Minecraft.getInstance().font, label, x + 6, (BAR_H - Minecraft.getInstance().font.lineHeight) / 2,
                this.openMenuId != null && this.openMenuId.equals(menu.id) ? BBSSettings.primaryColor.get() : COLOR_TEXT);

            this.menuRects.add(new int[] { x, 0, w, BAR_H });
            x += w + GAP;
        }

        /* Dropdown of the open menu. */
        if (this.openMenuId == null)
        {
            return;
        }

        List<TopBarItem> items = this.visibleItems();
        int dropY = BAR_H;
        int dropW = 220;

        g.fill(0, dropY, dropW, dropY + items.size() * ITEM_H + 2, Colors.A100 | 0xFF14181F);
        g.fill(0, dropY, dropW, dropY + 1, BBSSettings.primaryColor.get());

        for (int i = 0; i < items.size(); i++)
        {
            TopBarItem item = items.get(i);
            boolean enabled = item.isEnabled(CTX);
            int y = dropY + 1 + i * ITEM_H;

            if (enabled && this.inside(mouseX, mouseY, 0, y, dropW, ITEM_H))
            {
                g.fill(0, y, dropW, y + ITEM_H, COLOR_HOVER);
            }

            g.text(Minecraft.getInstance().font, item.label.get(), 8, y + (ITEM_H - Minecraft.getInstance().font.lineHeight) / 2,
                enabled ? COLOR_TEXT : COLOR_TEXT_DISABLED);

            this.itemRects.add(new int[] { 0, y, dropW, ITEM_H });
            this.itemRefs.add(item);
        }
    }

    /* Interaction */

    /** The dropdown + button logic lives in {@link #onClick(MouseButtonEvent, boolean)}. */
    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input)
    {}

    @Override
    public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output)
    {
        this.defaultButtonNarrationText(output);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        int mx = (int) event.x();
        int my = (int) event.y();

        /* Dropdown item first - it takes precedence. */
        for (int i = 0; i < this.itemRects.size(); i++)
        {
            int[] r = this.itemRects.get(i);

            if (this.inside(mx, my, r[0], r[1], r[2], r[3]))
            {
                TopBarItem item = this.itemRefs.get(i);

                if (item.isEnabled(CTX))
                {
                    this.openMenuId = null;
                    item.run(CTX);
                }

                return;
            }
        }

        /* Menu buttons - open / close. Each rect corresponds to the menu at
         * the same index in TopBarRegistry.getMenus(). */
        List<TopBarMenu> menus = TopBarRegistry.getMenus();

        for (int i = 0; i < this.menuRects.size() && i < menus.size(); i++)
        {
            int[] r = this.menuRects.get(i);

            if (this.inside(mx, my, r[0], r[1], r[2], r[3]))
            {
                String target = menus.get(i).id;

                this.openMenuId = this.openMenuId != null && this.openMenuId.equals(target) ? null : target;

                return;
            }
        }

        /* Clicked outside - close the dropdown. */
        this.openMenuId = null;
    }

    private List<TopBarItem> visibleItems()
    {
        TopBarMenu menu = TopBarRegistry.menu(this.openMenuId);

        if (menu == null)
        {
            return new ArrayList<>();
        }

        return menu.getVisibleItems(CTX);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean isFullscreen()
    {
        long handle = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();

        return handle != 0L && org.lwjgl.glfw.GLFW.glfwGetWindowMonitor(handle) != 0L;
    }
}
