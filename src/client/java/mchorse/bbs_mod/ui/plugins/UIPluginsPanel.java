package mchorse.bbs_mod.ui.plugins;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.plugins.PluginManifest;
import mchorse.bbs_mod.plugins.PluginManager;
import mchorse.bbs_mod.plugins.ScriptPlugin;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
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
                UIPluginCard card = new UIPluginCard(plugin.manifest, () ->
                {
                    if (!plugin.manifest.description.isEmpty())
                    {
                        this.getContext().notifyInfo(IKey.raw(plugin.manifest.description));
                    }
                });

                this.list.add(card);
            }
        }

        this.list.resize();
    }

    /**
     * A prominent, card-style row for a single plugin: a large glyph badge
     * (from the manifest's {@code icon}, falling back to a plug emoji) on the
     * left, then the plugin name, a meta line (version · author · [id]) and the
     * description. Hovering brightens the card; clicking shows the description.
     */
    public static class UIPluginCard extends UIClickable<UIElement>
    {
        private static final String FALLBACK_ICON = "🔌";

        private final PluginManifest manifest;

        public UIPluginCard(PluginManifest manifest, Runnable onClick)
        {
            super((e) -> onClick.run());

            this.manifest = manifest;

            boolean hasDesc = !manifest.description.isEmpty();

            this.h(hasDesc ? 56 : 44);

            if (hasDesc)
            {
                this.tooltip(IKey.raw(manifest.description));
            }
        }

        @Override
        protected UIElement get()
        {
            return this;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            int bg = this.hover ? (Colors.A100 | 0x2a3340) : (Colors.A100 | 0x1c2230);

            this.area.render(context.batcher, bg);

            FontRenderer font = context.batcher.getFont();
            int x = this.area.x + 10;
            int y = this.area.y + 8;

            /* Glyph badge: drawn as text so any emoji/character works without
             * touching the texture pipeline (MC 26.2 safe). */
            String icon = this.manifest.icon.isEmpty() ? FALLBACK_ICON : this.manifest.icon;

            context.batcher.text(icon, x, y, Colors.WHITE, true);

            int textX = x + 34;
            int textW = this.area.ex() - textX - 10;

            context.batcher.text(this.manifest.name, textX, y, Colors.WHITE, true);

            StringBuilder meta = new StringBuilder();

            if (!this.manifest.version.isEmpty())
            {
                meta.append("v").append(this.manifest.version);
            }

            if (!this.manifest.author.isEmpty())
            {
                if (meta.length() > 0)
                {
                    meta.append("  ·  ");
                }

                meta.append(this.manifest.author);
            }

            meta.append("  [").append(this.manifest.id).append("]");

            context.batcher.text(meta.toString(), textX, y + 18, Colors.GRAY, true);

            if (!this.manifest.description.isEmpty())
            {
                String desc = font.limitToWidth(this.manifest.description, textW);

                context.batcher.text(desc, textX, y + 32, Colors.LIGHTER_GRAY, true);
            }
        }
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
