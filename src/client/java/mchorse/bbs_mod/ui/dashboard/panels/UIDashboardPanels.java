package mchorse.bbs_mod.ui.dashboard.panels;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.events.UIEvent;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

public class UIDashboardPanels extends UIElement
{
    public List<UIDashboardPanel> panels = new ArrayList<>();
    public UIDashboardPanel panel;

    public static void renderHighlight(Batcher2D batcher, Area area)
    {
        int color = BBSSettings.primaryColor.get();

        batcher.box(area.x, area.ey() - 2, area.ex(), area.ey(), Colors.A100 | color);
        batcher.gradientVBox(area.x, area.y, area.ex(), area.ey() - 2, color, Colors.A75 | color);
    }

    public static void renderHighlightHorizontal(Batcher2D batcher, Area area)
    {
        int color = BBSSettings.primaryColor.get();

        batcher.box(area.ex() - 2, area.y, area.ex(), area.ey(), Colors.A100 | color);
        batcher.gradientHBox(area.x, area.y, area.ex() - 2, area.ey(), color, Colors.A75 | color);
    }

    public UIDashboardPanels()
    {
    }

    public <T> T getPanel(Class<T> clazz)
    {
        for (UIDashboardPanel panel : this.panels)
        {
            if (panel.getClass() == clazz)
            {
                return (T) panel;
            }
        }

        return null;
    }

    public boolean isFlightSupported()
    {
        return this.panel instanceof IFlightSupported;
    }

    public void open()
    {
        for (UIDashboardPanel panel : this.panels)
        {
            panel.open();
        }
    }

    public void close()
    {
        for (UIDashboardPanel panel : this.panels)
        {
            panel.close();
        }
    }

    public void setPanel(UIDashboardPanel panel)
    {
        UIDashboardPanel lastPanel = this.panel;

        if (this.panel != null)
        {
            this.panel.disappear();
            this.panel.removeFromParent();
        }

        this.panel = panel;

        this.getEvents().emit(new PanelEvent(this, lastPanel, panel));

        if (this.panel != null)
        {
            this.setPanelPlacement(panel);

            this.prepend(this.panel);
            this.panel.appear();
            this.panel.resize();
        }
    }

    private void setPanelPlacement(UIDashboardPanel panel)
    {
        panel.resetFlex().relative(this).w(1F).h(1F);
    }

    public UIIcon registerPanel(UIDashboardPanel panel, IKey tooltip, Icon icon)
    {
        return this.registerPanel(panel, tooltip, icon, false);
    }

    /**
     * Register a panel. The bottom task-bar chrome that used to host the panel
     * tabs has been removed (everything is driven from the HTML overlay now),
     * but panels stay switchable via {@link #getPanel}/{@link #setPanel}.
     */
    public UIIcon registerPanel(UIDashboardPanel panel, IKey tooltip, Icon icon, boolean hidden)
    {
        UIIcon button = new UIIcon(icon, (b) -> this.setPanel(panel));

        button.tooltip(tooltip, Direction.TOP);

        this.panels.add(panel);

        return button;
    }

    public static class PanelEvent extends UIEvent<UIDashboardPanels>
    {
        public final UIDashboardPanel lastPanel;
        public final UIDashboardPanel panel;

        public PanelEvent(UIDashboardPanels element, UIDashboardPanel lastPanel, UIDashboardPanel panel)
        {
            super(element);

            this.lastPanel = lastPanel;
            this.panel = panel;
        }
    }
}
