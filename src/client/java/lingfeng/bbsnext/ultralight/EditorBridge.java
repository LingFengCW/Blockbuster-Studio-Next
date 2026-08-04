package lingfeng.bbsnext.ultralight;

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
 * Java <-> JavaScript bridge for the HTML editor. Bound into the page as the
 * global `bbs` object (see {@link UltralightUI#tryBind()}). Every method here
 * is directly callable from the page's JS, e.g. `bbs.getState()` returns a
 * JSON string describing the current project/scene/film, and
 * `bbs.openScene("...")` switches the editor to another scene.
 *
 * All methods run on the render thread (they are invoked from the page while
 * the engine advances inside {@link UltralightUI#renderFrame()}), so it is
 * safe to touch the BBS data model directly.
 */
public class EditorBridge
{
    private final UIFilmPanel panel;
    private final Gson gson = new Gson();

    public EditorBridge(UIFilmPanel panel)
    {
        this.panel = panel;
    }

    /* -------- state (JSON) -------- */

    /** Full editor state as a JSON string; the page polls this to stay in sync. */
    public String getState()
    {
        JsonObject root = new JsonObject();

        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();
        SceneManager scenes = SceneManager.get();
        Film film = this.panel.getData();

        root.addProperty("project", project == null ? "" : project.name);
        root.addProperty("film", film == null ? "" : film.getId());

        Scene current = scenes == null ? null : scenes.getCurrent();
        root.addProperty("scene", current == null ? "" : current.name);

        root.addProperty("cursor", this.panel.getCursor());
        root.addProperty("running", this.panel.isRunning());
        root.addProperty("dirty", this.panel.isDirty());

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

        return this.gson.toJson(root);
    }

    /* -------- actions -------- */

    /** Switch the editor to another scene (by scene id). */
    public void openScene(String sceneId)
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
                this.panel.save();
                this.panel.openScene(scene);

                return;
            }
        }
    }

    /** Play/pause the film. */
    public void togglePlay()
    {
        this.panel.togglePlayback();
    }

    /** Seek to a specific tick. */
    public void setCursor(int tick)
    {
        this.panel.setCursor(tick);
    }

    /** Save the current film. */
    public void save()
    {
        this.panel.save();
    }

    /** Undo / redo. */
    public void undo()
    {
        this.panel.undo();
    }

    public void redo()
    {
        this.panel.redo();
    }

    /** Close the editor and return to the project library. */
    public void closeEditor()
    {
        UIDashboard dashboard = this.panel.dashboard;

        this.panel.save();
        dashboard.setPanel(dashboard.getPanel(lingfeng.bbsnext.ui.dashboard.panels.UIProjectsPanel.class));
    }

    /** Open the "new scene" dialog. */
    public void newScene()
    {
        this.panel.newScene();
    }

    /** Open the "new character" dialog. */
    public void newCharacter()
    {
        this.panel.newCharacter();
    }

    /** Delete a replay (character) by index. */
    public void deleteReplay(int index)
    {
        Film film = this.panel.getData();

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

    /** Toggle a replay's enabled flag (character hidden in the film). */
    public void toggleReplay(int index)
    {
        Film film = this.panel.getData();

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
