package mchorse.bbs_mod.ui.dashboard.panels;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

public class UIDashboardPanel extends UIElement
{
    public final UIDashboard dashboard;

    public UIDashboardPanel(UIDashboard dashboard)
    {
        super();

        this.dashboard = dashboard;
        this.markContainer();
    }

    public boolean needsBackground()
    {
        return true;
    }

    public boolean canToggleVisibility()
    {
        return true;
    }

    public boolean canPause()
    {
        return true;
    }

    public boolean canRefresh()
    {
        return true;
    }

    public void appear()
    {}

    public void disappear()
    {}

    public void open()
    {}

    public void close()
    {}

    public void update()
    {}

    public void startRenderFrame(float tickDelta)
    {}

    public void renderInWorld(LevelRenderContext context)
    {}

    public void renderPanelBackground(UIContext context)
    {}

    /**
     * Undo the last edit in this panel. Panels that own an undo manager
     * (e.g. the film editor) override this; the default is a no-op because
     * not every panel holds editable state.
     */
    public void undo()
    {}

    public void redo()
    {}

    /**
     * Persist this panel's data. Overridden by panels that own savable state.
     */
    public void save()
    {}
}
