package lingfeng.bbsnext.mcef;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lingfeng.bbsnext.update.UpdateChecker;
import lingfeng.bbsnext.update.UpdateConfig;
import lingfeng.bbsnext.update.LiveUi;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.supporters.Supporter;
import mchorse.bbs_mod.ui.supporters.Supporters;
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

        return GSON.toJson(root);
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
