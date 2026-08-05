package lingfeng.bbsnext.mcef;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;

/**
 * Java <-> JavaScript bridge for the HTML editor.
 *
 * The page reads its state from `window.bbsState` (pushed by
 * {@link MCEFUI#pushState()}, which calls {@link #getStateJson(EditorBridge)})
 * and sends actions back through the CEF JSQuery channel
 * ({@link #handle(String, EditorBridge)}).
 *
 * Action request format (JSON string): {"a":"openScene","id":"..."}
 */
public class EditorBridge
{
    private final UIFilmPanel panel;
    private static final Gson GSON = new Gson();

    public EditorBridge(UIFilmPanel panel)
    {
        this.panel = panel;
    }

    /* -------- state (JSON) -------- */

    /** Full editor state as a JSON string, pushed to the page as window.bbsState. */
    public static String getStateJson(EditorBridge bridge)
    {
        JsonObject root = new JsonObject();

        UIFilmPanel panel = bridge.panel;

        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();
        SceneManager scenes = SceneManager.get();
        Film film = panel.getData();

        root.addProperty("project", project == null ? "" : project.name);
        root.addProperty("film", film == null ? "" : film.getId());

        Scene current = scenes == null ? null : scenes.getCurrent();
        root.addProperty("scene", current == null ? "" : current.name);

        root.addProperty("cursor", panel.getCursor());
        root.addProperty("running", panel.isRunning());
        root.addProperty("dirty", panel.isDirty());

        /* Scenes */
        JsonArray sceneArr = new JsonArray();

        if (scenes != null)
        {
            for (Scene scene : scenes.getScenes())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", scene.id);
                obj.addProperty("name", scene.name);
                sceneArr.add(obj);
            }
        }

        root.add("scenes", sceneArr);

        /* Replays (characters) */
        JsonArray replayArr = new JsonArray();

        if (film != null)
        {
            int i = 0;

            for (Replay replay : film.replays.getList())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("index", i++);
                obj.addProperty("label", replay.getName());
                obj.addProperty("actor", replay.actor.get());
                obj.addProperty("fp", replay.fp.get());
                obj.addProperty("enabled", replay.enabled.get());
                obj.addProperty("shadow", replay.shadow.get());
                replayArr.add(obj);
            }
        }

        root.add("replays", replayArr);

        /* Camera timeline clips */
        JsonArray clipArr = new JsonArray();

        if (film != null)
        {
            for (Clip clip : film.camera.get())
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("title", clip.title.get());
                obj.addProperty("tick", clip.tick.get());
                obj.addProperty("layer", clip.layer.get());
                obj.addProperty("duration", clip.duration.get());
                obj.addProperty("enabled", clip.enabled.get());
                clipArr.add(obj);
            }
        }

        root.add("clips", clipArr);

        root.addProperty("duration", film == null ? 0 : film.camera.calculateDuration());

        return GSON.toJson(root);
    }

    /* -------- action dispatch (JSQuery) -------- */

    /**
     * Dispatch an action request from the page. Returns a JSON result string
     * ("ok" on success, or an error message).
     */
    public static String handle(String request, EditorBridge bridge)
    {
        JsonObject req;

        try
        {
            req = GSON.fromJson(request, JsonObject.class);
        }
        catch (Throwable t)
        {
            return "{\"ok\":false,\"error\":\"bad request\"}";
        }

        if (bridge == null)
        {
            return "{\"ok\":false,\"error\":\"no bridge\"}";
        }

        UIFilmPanel panel = bridge.panel;
        String action = req.has("a") ? req.get("a").getAsString() : "";

        switch (action)
        {
            case "openScene":
                openScene(panel, req.has("id") ? req.get("id").getAsString() : null);
                break;
            case "togglePlay":
                panel.togglePlayback();
                break;
            case "setCursor":
                panel.setCursor(req.get("tick").getAsInt());
                break;
            case "save":
                panel.save();
                break;
            case "undo":
                panel.undo();
                break;
            case "redo":
                panel.redo();
                break;
            case "closeEditor":
                closeEditor(panel);
                break;
            case "newScene":
                panel.newScene();
                break;
            case "newCharacter":
                panel.newCharacter();
                break;
            case "deleteReplay":
                deleteReplay(panel, req.get("index").getAsInt());
                break;
            case "toggleReplay":
                toggleReplay(panel, req.get("index").getAsInt());
                break;
            case "setTool":
                setTool(panel, req.has("tool") ? req.get("tool").getAsString() : "");
                break;
            case "setCameraMode":
                panel.getController().setPov(req.get("mode").getAsInt());
                break;
            case "toggleControl":
                panel.getController().toggleControl();
                break;
            case "toggleRecord":
                panel.getController().startRecording(null);
                break;
            case "toggleInstantKeys":
                panel.getController().toggleInstantKeyframes();
                break;
            case "newEntity":
                panel.getController().createEntities();
                break;
            case "clipOp":
                clipOp(panel, req.has("op") ? req.get("op").getAsString() : "",
                    req.has("index") ? req.get("index").getAsInt() : -1);
                break;
            default:
                return "{\"ok\":false,\"error\":\"unknown action " + action + "\"}";
        }

        return "{\"ok\":true}";
    }

    /* -------- tool / clip actions -------- */

    /** Map the reference UI's left toolbar buttons to real BBS functions. */
    private static void setTool(UIFilmPanel panel, String tool)
    {
        if (tool == null || tool.isEmpty())
        {
            return;
        }

        switch (tool.charAt(0))
        {
            case 'Q': /* 选择/跟随相机 */ panel.getController().setPov(0); break;
            case 'W': /* 平移/自由相机 */ panel.getController().setPov(1); break;
            case 'E': /* 旋转/轨道相机 */ panel.getController().setPov(2); break;
            case 'R': /* 缩放/第三人称 */ panel.getController().setPov(4); break;
            case 'C': /* 切片/第一人称 */ panel.getController().setPov(3); break;
            case 'S': /* 吸附/瞬间关键帧 */ panel.getController().toggleInstantKeyframes(); break;
            case 'F': /* 录制 */ panel.getController().startRecording(null); break;
            case 'T': /* 骨骼/演员控制 */ panel.getController().toggleControl(); break;
            case 'Y': /* 动作模板/创建实体 */ panel.getController().createEntities(); break;
            default: break;
        }
    }

    private static void clipOp(UIFilmPanel panel, String op, int index)
    {
        Film film = panel.getData();

        if (film == null || index < 0 || index >= film.camera.get().size())
        {
            return;
        }

        switch (op)
        {
            case "delete":
            {
                final int i = index;

                BaseValue.edit(film, f -> f.camera.get().remove(i));

                break;
            }
            case "split":
            {
                /* Split the clip at the playhead. */
                final int i = index;
                final int offset = panel.getCursor() - film.camera.get().get(i).tick.get();

                if (offset > 0 && offset < film.camera.get().get(i).duration.get())
                {
                    BaseValue.edit(film, f ->
                    {
                        Clip clip = f.camera.get().get(i);
                        int splitOffset = Math.max(1, Math.min(offset, clip.duration.get() - 1));
                        clip.duration.set(splitOffset);

                        Clip rest = clip.copy();
                        rest.tick.set(clip.tick.get() + splitOffset);
                        rest.duration.set(Math.max(1, clip.duration.get() - splitOffset));
                        f.camera.get().add(i + 1, rest);
                    });
                }

                break;
            }
            default:
                break;
        }
    }

    private static void openScene(UIFilmPanel panel, String sceneId)
    {
        SceneManager scenes = SceneManager.get();

        if (scenes == null || sceneId == null)
        {
            return;
        }

        for (Scene scene : scenes.getScenes())
        {
            if (scene.id.equals(sceneId))
            {
                panel.save();
                panel.openScene(scene);

                return;
            }
        }
    }

    private static void closeEditor(UIFilmPanel panel)
    {
        UIDashboard dashboard = panel.dashboard;

        panel.save();
        dashboard.setPanel(dashboard.getPanel(lingfeng.bbsnext.ui.dashboard.panels.UIProjectsPanel.class));
    }

    private static void deleteReplay(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        int i = 0;

        for (Replay replay : film.replays.getList())
        {
            if (i++ == index)
            {
                final Replay target = replay;

                BaseValue.edit(film, f -> f.replays.remove(target));

                return;
            }
        }
    }

    private static void toggleReplay(UIFilmPanel panel, int index)
    {
        Film film = panel.getData();

        if (film == null)
        {
            return;
        }

        int i = 0;

        for (Replay replay : film.replays.getList())
        {
            if (i++ == index)
            {
                BaseValue.edit(film, f ->
                {
                    Replay r = f.replays.getList().get(index);
                    r.enabled.set(!r.enabled.get());
                });

                return;
            }
        }
    }
}
