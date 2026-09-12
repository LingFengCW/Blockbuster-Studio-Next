package lingfeng.bbsnext.mcef;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lingfeng.bbsnext.update.UpdateChecker;
import lingfeng.bbsnext.update.UpdateConfig;
import lingfeng.bbsnext.update.LiveUi;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.ui.UIValueFactory;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.ui.ValueLanguage;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.supporters.Supporter;
import mchorse.bbs_mod.ui.supporters.Supporters;
import mchorse.bbs_mod.ui.utils.Label;
import mchorse.bbs_mod.ui.utils.UIUtils;
import lingfeng.bbsnext.ui.dashboard.panels.UIProjectsPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Java <-> JavaScript bridge for the HTML dashboard (作品库 / project picker).
 *
 * <p>The page reads {@code window.bbsState} ({@link #getStateJson()}) and sends
 * actions back through the same {@code BBS_ACTION:} console channel the editor
 * uses ({@link #handle(String)}). Unlike the editor, every action stays inside
 * the dashboard - creating / opening / deleting / renaming projects, switching
 * to another dashboard panel, or returning to the title screen.</p>
 */
public class DashboardBridge implements IHtmlBridge
{
    private final UIProjectsPanel panel;
    private static final Gson GSON = new Gson();

    public DashboardBridge(UIProjectsPanel panel)
    {
        this.panel = panel;
    }

    /* -------- state (JSON) -------- */

    @Override
    public String getStateJson()
    {
        JsonObject root = new JsonObject();

        JsonArray projects = new JsonArray();

        for (BBSProject project : ProjectManager.get().getProjects())
        {
            JsonObject o = new JsonObject();

            o.addProperty("id", project.id);
            o.addProperty("name", project.name);
            o.addProperty("createdAt", project.createdAt);
            projects.add(o);
        }

        root.add("projects", projects);

        /* World list for the "new project" dialog (singleplayer saves). */
        JsonArray worlds = new JsonArray();
        Path saves = Minecraft.getInstance().gameDirectory.toPath().resolve("saves");

        if (Files.isDirectory(saves))
        {
            try (var stream = Files.list(saves))
            {
                stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .forEach(worlds::add);
            }
            catch (Exception ignored)
            {
            }
        }

        root.add("worlds", worlds);

        /* Trigger (throttled) background checks for version + live UI the
         * first time the dashboard is shown; results surface via bbsState. */
        UpdateChecker.ensureChecked();
        LiveUi.ensureChecked();

        /* Update preferences + any available update. */
        UpdateConfig cfg = UpdateConfig.get();
        JsonObject settings = new JsonObject();

        settings.addProperty("autoUpdate", cfg.autoUpdate);
        settings.addProperty("updatePush", cfg.updatePush);
        settings.addProperty("liveUi", cfg.liveUi);
        root.add("settings", settings);
        root.addProperty("localVersion", UpdateChecker.localVersion());

        UpdateChecker.UpdateInfo info = UpdateChecker.current;

        if (info != null && (cfg.updatePush || cfg.autoUpdate))
        {
            JsonObject update = new JsonObject();

            update.addProperty("version", info.version);
            update.addProperty("releaseName", info.releaseName);
            update.addProperty("size", info.size);
            update.addProperty("staged", info.stagedJar != null && Files.exists(info.stagedJar));
            root.add("update", update);
        }

        /* Supporters (ported from the legacy native panel). */
        root.add("supporters", buildSupporters());

        /* The real BBS settings tree plus the shipped languages, so the settings
         * tab can render the actual options (including the language picker)
         * instead of only the updater switches. */
        root.add("settingsModules", this.buildSettingsModules());
        root.add("languages", this.buildLanguages());

        return GSON.toJson(root);
    }

    /** Every settings module (BBS and any mod that registered one) described as
     *  module -> category -> value, mirroring what the native settings overlay
     *  walks. Values that cannot be edited inline (texture links, keybinds,
     *  sub-panels) are exported too, so nothing silently disappears from the
     *  page: the HTML renders those rows read only. */
    private JsonArray buildSettingsModules()
    {
        JsonArray modules = new JsonArray();

        for (Settings settings : BBSMod.getSettings().modules.values())
        {
            JsonObject module = new JsonObject();

            module.addProperty("id", settings.getId());
            module.addProperty("title", L10n.lang(UIValueFactory.getTitleKey(settings)).get());

            JsonArray categories = new JsonArray();

            for (ValueGroup category : settings.categories.values())
            {
                if (!category.isVisible())
                {
                    continue;
                }

                JsonObject cat = new JsonObject();
                JsonArray values = new JsonArray();

                cat.addProperty("id", category.getId());
                cat.addProperty("title", L10n.lang(UIValueFactory.getCategoryTitleKey(category)).get());
                cat.addProperty("tooltip", L10n.lang(UIValueFactory.getCategoryTooltipKey(category)).get());

                for (BaseValue value : category.getAll())
                {
                    if (!value.isVisible())
                    {
                        continue;
                    }

                    values.add(this.serializeSetting(value));
                }

                cat.add("values", values);
                categories.add(cat);
            }

            module.add("categories", categories);
            modules.add(module);
        }

        return modules;
    }

    /** Describe one setting for the HTML controls: kind, localized label and
     *  tooltip, current value, and the bounds / mode labels the native widget
     *  would have offered. */
    private JsonObject serializeSetting(BaseValue value)
    {
        JsonObject o = new JsonObject();

        o.addProperty("id", value.getId());
        o.addProperty("title", L10n.lang(UIValueFactory.getValueLabelKey(value)).get());
        o.addProperty("tooltip", L10n.lang(UIValueFactory.getValueCommentKey(value)).get());

        /* ValueLanguage extends ValueString, so it has to be tested first. */
        if (value instanceof ValueLanguage language)
        {
            o.addProperty("type", "language");
            o.addProperty("value", language.get());
        }
        else if (value instanceof ValueBoolean bool)
        {
            o.addProperty("type", "bool");
            o.addProperty("value", bool.get());
        }
        else if (value instanceof ValueInt integer)
        {
            ValueInt.Subtype subtype = integer.getSubtype();
            int packed = integer.get();

            if (subtype == ValueInt.Subtype.COLOR || subtype == ValueInt.Subtype.COLOR_ALPHA)
            {
                o.addProperty("type", "color");
                o.addProperty("hasAlpha", subtype == ValueInt.Subtype.COLOR_ALPHA);
                /* Browser hex inputs are #rrggbb, while BBS packs ARGB, so the
                 * alpha channel travels as its own field. */
                o.addProperty("value", String.format("#%06X", packed & 0xFFFFFF));
                /* Named "alpha" and not "a": the request object already uses
                 * "a" for the action name. */
                o.addProperty("alpha", (packed >>> 24) & 0xFF);
            }
            else if (subtype == ValueInt.Subtype.MODES)
            {
                JsonArray labels = new JsonArray();

                for (IKey label : integer.getLabels())
                {
                    labels.add(label.get());
                }

                o.addProperty("type", "modes");
                o.addProperty("value", integer.get());
                o.add("labels", labels);
            }
            else
            {
                o.addProperty("type", "int");
                o.addProperty("value", integer.get());
                o.addProperty("min", integer.getMin());
                o.addProperty("max", integer.getMax());
            }
        }
        else if (value instanceof ValueFloat number)
        {
            o.addProperty("type", "number");
            o.addProperty("value", number.get());
            o.addProperty("min", number.getMin());
            o.addProperty("max", number.getMax());
        }
        else if (value instanceof ValueDouble number)
        {
            o.addProperty("type", "number");
            o.addProperty("value", number.get());
            o.addProperty("min", number.getMin());
            o.addProperty("max", number.getMax());
        }
        else if (value instanceof ValueString string)
        {
            o.addProperty("type", "string");
            o.addProperty("value", string.get());
        }
        else if (value instanceof ValueLink link)
        {
            o.addProperty("type", "readonly");
            o.addProperty("value", String.valueOf(link.get()));
        }
        else
        {
            o.addProperty("type", "readonly");
            o.addProperty("value", String.valueOf(value));
        }

        return o;
    }

    /** Languages the mod ships, for the language picker in the settings tab. */
    private JsonArray buildLanguages()
    {
        JsonArray languages = new JsonArray();

        try
        {
            for (Label<String> label : BBSModClient.getL10n().getSupportedLanguageLabels())
            {
                JsonObject o = new JsonObject();

                o.addProperty("value", label.value);
                o.addProperty("title", label.title.get());
                languages.add(o);
            }
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.warn("[Dashboard] could not list the supported languages", t);
        }

        return languages;
    }

    private JsonObject buildSupporters()
    {
        JsonObject groups = new JsonObject();

        Supporters supporters = new Supporters();

        supporters.setup();

        groups.add("developers", toArray(supporters.getDevelopers()));
        groups.add("cc", toArray(supporters.getCCSupporters()));
        groups.add("super", toArray(supporters.getSuperSupporters()));
        groups.add("early", toArray(supporters.getBBSEarlyAccessSupporters()));

        return groups;
    }

    private JsonArray toArray(List<Supporter> list)
    {
        JsonArray arr = new JsonArray();

        for (Supporter s : list)
        {
            JsonObject o = new JsonObject();

            o.addProperty("name", s.name);
            o.addProperty("link", s.link == null ? "" : s.link);

            /* Banner avatars are extracted next to the HTML (bbs_editor/banners/)
             * by MCEFUI.extractPages; expose the relative URL for <img>. The
             * legacy panel treats a "..." banner as "no banner" (gradient
             * placeholder), so skip those too. */
            if (s.banner != null && !s.banner.path.equals("..."))
            {
                String path = s.banner.path;
                String file = path.substring(path.lastIndexOf('/') + 1);

                if (!file.isEmpty())
                {
                    o.addProperty("banner", "banners/" + file);
                }
            }

            arr.add(o);
        }

        return arr;
    }

    /* -------- actions -------- */

    @Override
    public String handle(String request)
    {
        try
        {
            JsonObject req = GSON.fromJson(request, JsonObject.class);
            String action = req.has("a") ? req.get("a").getAsString() : "";

            switch (action)
            {
                case "createProject":
                {
                    String name = req.has("name") ? req.get("name").getAsString().trim() : "";
                    String world = req.has("world") ? req.get("world").getAsString() : "";

                    if (!name.isEmpty())
                    {
                        BBSProject created = ProjectManager.get().create(name, world);

                        if (created != null)
                        {
                            this.panel.openProject(created);
                        }
                    }

                    break;
                }
                case "openProject":
                {
                    BBSProject p = this.find(req.get("id").getAsString());

                    if (p != null)
                    {
                        this.panel.openProject(p);
                    }

                    break;
                }
                case "deleteProject":
                {
                    BBSProject p = this.find(req.get("id").getAsString());

                    if (p != null)
                    {
                        ProjectManager.get().delete(p);
                    }

                    break;
                }
                case "renameProject":
                {
                    BBSProject p = this.find(req.get("id").getAsString());

                    if (p != null && req.has("name"))
                    {
                        ProjectManager.get().rename(p, req.get("name").getAsString().trim());
                    }

                    break;
                }
                case "backToTitle":
                {
                    Minecraft.getInstance().gui.setScreen(new TitleScreen());

                    break;
                }
                case "setSetting":
                {
                    UpdateConfig cfg = UpdateConfig.get();
                    String key = req.has("key") ? req.get("key").getAsString() : "";
                    boolean val = req.has("value") && req.get("value").getAsBoolean();

                    if ("autoUpdate".equals(key))
                    {
                        cfg.autoUpdate = val;
                    }
                    else if ("updatePush".equals(key))
                    {
                        cfg.updatePush = val;
                    }
                    else if ("liveUi".equals(key))
                    {
                        cfg.liveUi = val;
                    }

                    cfg.save();

                    break;
                }
                case "setBbsValue":
                {
                    String module = req.has("module") ? req.get("module").getAsString() : "bbs";
                    String id = req.has("id") ? req.get("id").getAsString() : "";

                    this.setBbsValue(module, id, req);

                    break;
                }
                case "checkUpdate":
                {
                    UpdateChecker.checkAsync();

                    break;
                }
                case "updateNow":
                {
                    UpdateChecker.applyUpdate();

                    break;
                }
                case "dismissUpdate":
                {
                    UpdateChecker.dismiss();

                    break;
                }
                case "openLink":
                {
                    String url = req.has("url") ? req.get("url").getAsString() : "";

                    if (!url.isEmpty() && !url.equals("..."))
                    {
                        UIUtils.openWebLink(url);
                    }

                    break;
                }
                default:
                    BBSMod.LOGGER.warn("[Dashboard] unknown action: {}", action);
            }
        }
        catch (Exception e)
        {
            BBSMod.LOGGER.error("[Dashboard] handle failed: {}", request, e);
        }

        return "{\"ok\":true}";
    }

    @Override
    public String pageUrl()
    {
        return MCEFUI.pageFileUrl("dashboard_ui.html");
    }

    /* -------- settings writeback -------- */

    /** Write one setting coming from the HTML settings tab. The value is looked
     *  up by its global id inside the given module, written through the setter
     *  that matches its concrete type, and the module is saved straight away
     *  (the settings thread batches writes otherwise, which would drop the
     *  change if the game is closed right after). */
    private void setBbsValue(String moduleId, String id, JsonObject req)
    {
        Settings settings = BBSMod.getSettings().modules.get(moduleId);

        if (settings == null || !req.has("value"))
        {
            return;
        }

        for (ValueGroup category : settings.categories.values())
        {
            for (BaseValue value : category.getAll())
            {
                if (!value.getId().equals(id))
                {
                    continue;
                }

                try
                {
                    /* ValueLanguage extends ValueString, so it goes first. */
                    if (value instanceof ValueLanguage language)
                    {
                        language.set(req.get("value").getAsString());
                    }
                    else if (value instanceof ValueBoolean bool)
                    {
                        bool.set(req.get("value").getAsBoolean());
                    }
                    else if (value instanceof ValueInt integer)
                    {
                        ValueInt.Subtype subtype = integer.getSubtype();

                        if (subtype == ValueInt.Subtype.COLOR || subtype == ValueInt.Subtype.COLOR_ALPHA)
                        {
                            int alpha = req.has("alpha") ? req.get("alpha").getAsInt() : 255;

                            integer.set((alpha << 24) | (parseColor(req.get("value").getAsString()) & 0xFFFFFF));
                        }
                        else
                        {
                            integer.set(req.get("value").getAsInt());
                        }
                    }
                    else if (value instanceof ValueFloat number)
                    {
                        number.set(req.get("value").getAsFloat());
                    }
                    else if (value instanceof ValueDouble number)
                    {
                        number.set(req.get("value").getAsDouble());
                    }
                    else if (value instanceof ValueString string)
                    {
                        string.set(req.get("value").getAsString());
                    }
                    else
                    {
                        BBSMod.LOGGER.warn("[Dashboard] setting {} is not editable from the HTML page", id);
                    }

                    settings.save();
                }
                catch (Exception e)
                {
                    BBSMod.LOGGER.error("[Dashboard] could not write setting {}", id, e);
                }

                return;
            }
        }
    }

    /** Parse a "#rrggbb" hex colour into a packed rgb int. */
    private static int parseColor(String hex)
    {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;

        try
        {
            return (int) Long.parseLong(clean, 16);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private BBSProject find(String id)
    {
        if (id == null)
        {
            return null;
        }

        List<BBSProject> projects = ProjectManager.get().getProjects();

        for (BBSProject p : projects)
        {
            if (id.equals(p.id))
            {
                return p;
            }
        }

        return null;
    }
}
