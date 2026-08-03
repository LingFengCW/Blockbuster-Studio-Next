package mchorse.bbs_mod.ui.plugins;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.plugins.PluginManifest;
import mchorse.bbs_mod.plugins.PluginManager;
import mchorse.bbs_mod.plugins.ScriptPlugin;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/**
 * Plugin manager panel: lists every loaded script plugin (from the
 * <gameDir>/bbsnplugin/ folder) with its name, version and author, plus
 * buttons to open the plugins folder and reload plugins.
 */
public class UIPluginsPanel extends UIDashboardPanel
{
    private UIScrollView list;

    public UIPluginsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        UIButton openFolder = new UIButton(UIKeys.PLUGINS_OPEN_FOLDER, (b) -> this.openFolder());
        UIButton reload = new UIButton(UIKeys.PLUGINS_RELOAD, (b) -> this.reload());

        openFolder.relative(this).x(10).y(10).w(140).h(20);
        reload.relative(openFolder).x(1F).y(0).w(140).h(20);

        this.add(openFolder, reload);

        this.list = UI.scrollView(5, 5);
        this.list.relative(this).x(0).y(40).w(1F).hTo(this.area, 1F);

        this.add(this.list);

        this.refresh();
    }

    private void refresh()
    {
        this.list.removeAll();

        PluginManager manager = BBSMod.getPluginManager();
        List<ScriptPlugin> plugins = manager == null ? List.of() : manager.getPlugins();

        if (plugins.isEmpty())
        {
            UILabel hint = UI.label(UIKeys.PLUGINS_EMPTY, 18, Colors.GRAY);

            hint.h(18);
            this.list.add(hint);
        }
        else
        {
            for (ScriptPlugin plugin : plugins)
            {
                PluginManifest manifest = plugin.manifest;

                String line = manifest.name
                    + (manifest.version.isEmpty() ? "" : " v" + manifest.version)
                    + (manifest.author.isEmpty() ? "" : " - " + manifest.author)
                    + "  [" + manifest.id + "]";

                UIButton row = new UIButton(IKey.raw(line), (b) ->
                {
                    if (!manifest.description.isEmpty())
                    {
                        this.getContext().notifyInfo(IKey.raw(manifest.description));
                    }
                });

                row.h(20);
                this.list.add(row);
            }
        }

        this.list.resize();
    }

    private void openFolder()
    {
        File folder = BBSMod.getGamePath(PluginManager.FOLDER_NAME);

        try
        {
            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().open(folder);
            }
        }
        catch (Exception e)
        {
            this.getContext().notifyError(IKey.raw(folder.getAbsolutePath()));
        }
    }

    private void reload()
    {
        /* Reload = drop the loaded plugins and re-scan the folder. */
        PluginManager manager = BBSMod.getPluginManager();

        if (manager != null)
        {
            manager.getPlugins().clear();
            manager.load();
        }

        this.refresh();
        this.getContext().notifySuccess(UIKeys.PLUGINS_RELOADED);
    }
}
