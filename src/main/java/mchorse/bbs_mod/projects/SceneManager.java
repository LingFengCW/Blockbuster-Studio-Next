package mchorse.bbs_mod.projects;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages the scenes of a single {@link BBSProject}.
 *
 * On disk a project looks like this:
 *
 * <pre>
 * &lt;gameDir&gt;/bbs/projects/&lt;projectId&gt;/
 *     project.json          project metadata
 *     scenes/
 *         index.json        scene list + last opened scene
 *         &lt;sceneId&gt;.dat     Film payload of that scene (compressed)
 * </pre>
 *
 * The Film payload is reused as the scene content container, because it
 * already models everything a stage needs (camera clips, actors/replays and
 * inventory state) and has a proven compressed storage implementation.
 *
 * Unlike {@link mchorse.bbs_mod.BBSMod#films}, which is bound to the world
 * folder, scenes live inside the project directory and therefore survive
 * switching or deleting worlds.
 */
public class SceneManager
{
    public static final String SCENES_DIR = "scenes";

    private static final String INDEX_FILE = "index.json";

    private static SceneManager instance;
    private static String boundProjectId;

    private final BBSProject project;
    private final FilmManager films;
    private final List<Scene> scenes = new ArrayList<>();
    private Scene current;

    /**
     * Get the scene manager of the currently active project. Returns null
     * when no project is open, so callers must null check (opening a scene
     * without a project makes no sense).
     *
     * The instance is rebuilt whenever the active project changes, so scenes
     * of different works can never leak into each other.
     */
    public static SceneManager get()
    {
        BBSProject project = ProjectManager.get().getCurrent();

        if (project == null)
        {
            instance = null;
            boundProjectId = null;

            return null;
        }

        if (instance == null || !project.id.equals(boundProjectId))
        {
            instance = new SceneManager(project);
            boundProjectId = project.id;

            instance.loadAll();
        }

        return instance;
    }

    /**
     * Drop the cached instance. Called when a project gets deleted or the
     * player disconnects, so stale scenes are never served.
     */
    public static void reset()
    {
        instance = null;
        boundProjectId = null;
    }

    private SceneManager(BBSProject project)
    {
        this.project = project;
        this.films = new FilmManager(() -> this.getDirectory().toFile());
    }

    public BBSProject getProject()
    {
        return this.project;
    }

    public Path getDirectory()
    {
        return this.project.getDirectory().resolve(SCENES_DIR);
    }

    public FilmManager getFilms()
    {
        return this.films;
    }

    public List<Scene> getScenes()
    {
        return this.scenes;
    }

    public Scene getCurrent()
    {
        return this.current;
    }

    public void setCurrent(Scene scene)
    {
        this.current = scene;

        this.saveIndex();
    }

    public Scene getById(String id)
    {
        for (Scene scene : this.scenes)
        {
            if (scene.id.equals(id))
            {
                return scene;
            }
        }

        return null;
    }

    /* Index I/O */

    public void loadAll()
    {
        this.scenes.clear();
        this.current = null;

        Path index = this.getDirectory().resolve(INDEX_FILE);

        if (!Files.isRegularFile(index))
        {
            return;
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(index));

            if (!(data instanceof MapType map))
            {
                return;
            }

            MapType list = map.getMap("scenes");

            for (String id : list.keys())
            {
                MapType entry = list.getMap(id);
                String name = entry.getString("name");
                Scene scene = new Scene(id, name.isEmpty() ? id : name, entry.getLong("createdAt", 0L));

                scene.background = entry.getString("background");
                this.scenes.add(scene);
            }

            this.sort();

            String currentId = map.getString("current");

            if (!currentId.isEmpty())
            {
                this.current = this.getById(currentId);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void sort()
    {
        this.scenes.sort(Comparator.comparingLong((Scene s) -> s.createdAt).reversed());
    }

    public void saveIndex()
    {
        try
        {
            MapType list = new MapType();

            for (Scene scene : this.scenes)
            {
                MapType entry = new MapType();

                entry.putString("name", scene.name);
                entry.putLong("createdAt", scene.createdAt);
                entry.putString("background", scene.background);

                list.put(scene.id, entry);
            }

            MapType data = new MapType();

            data.put("scenes", list);
            data.putString("current", this.current == null ? "" : this.current.id);

            Path index = this.getDirectory().resolve(INDEX_FILE);

            Files.createDirectories(index.getParent());
            Files.write(index, DataStorageUtils.writeToBytes(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /* Scene lifecycle */

    /**
     * Create a brand new empty scene and immediately persist an empty Film
     * for it, so that opening the scene right away never hits a missing file.
     */
    public Scene create(String name)
    {
        return this.create(name, "");
    }

    public Scene create(String name, String background)
    {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Scene scene = new Scene(id, name, System.currentTimeMillis());

        scene.background = background == null ? "" : background;

        this.scenes.add(scene);
        this.sort();

        this.saveFilm(scene, new Film());
        this.saveIndex();

        return scene;
    }

    public void delete(Scene scene)
    {
        File file = this.films.getFile(scene.id);

        if (file != null && file.exists())
        {
            file.delete();
        }

        this.scenes.remove(scene);

        if (this.current == scene)
        {
            this.current = null;
        }

        this.saveIndex();
    }

    public void rename(Scene scene, String name)
    {
        scene.name = name;

        this.saveIndex();
    }

    /* Film payload */

    /**
     * Load the Film payload of a scene. A fresh empty Film is returned when
     * the file is missing or unreadable, so a broken file never blocks the
     * editor from opening.
     */
    public Film loadFilm(Scene scene)
    {
        Film film = null;

        if (this.films.exists(scene.id))
        {
            film = this.films.load(scene.id);
        }

        if (film == null)
        {
            film = new Film();
            film.setId(scene.id);
        }

        return film;
    }

    public boolean saveFilm(Scene scene, Film film)
    {
        BaseType data = film.toData();

        if (data instanceof MapType map)
        {
            try
            {
                Files.createDirectories(this.getDirectory());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return this.films.save(scene.id, map);
        }

        return false;
    }

    /**
     * Export a scene as a standalone document into the export folder.
     *
     * The exported file is a plain JSON document containing the scene's
     * descriptor (id/name/background) plus its full Film payload (camera
     * clips, replays, inventory). External formats (.scenebbs) are not used:
     * this mod's own native representation is the single source of truth.
     *
     * @return the exported file, or null on failure
     */
    public File exportScene(Scene scene)
    {
        if (scene == null)
        {
            return null;
        }

        try
        {
            MapType data = this.exportSceneData(scene);

            if (data == null)
            {
                return null;
            }

            File folder = mchorse.bbs_mod.BBSMod.getExportFolder();
            File file = new File(folder, this.sanitize(scene.name) + ".scenebbs");

            Files.createDirectories(folder.toPath());
            Files.write(file.toPath(), DataStorageUtils.writeToBytes(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return file;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Serialize a scene (descriptor + full Film payload) into a document.
     * Shared by the file exporter and the backpack.
     */
    public MapType exportSceneData(Scene scene)
    {
        if (scene == null)
        {
            return null;
        }

        MapType data = new MapType();

        data.putString("type", "scene");
        data.putString("id", scene.id);
        data.putString("name", scene.name);
        data.putString("background", scene.background);
        data.putLong("createdAt", scene.createdAt);

        Film film = this.loadFilm(scene);

        data.put("film", film.toData());

        return data;
    }

    /** Import a previously exported scene document back into the project. */
    public Scene importScene(File file)
    {
        if (file == null || !file.isFile())
        {
            return null;
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(file.toPath()));

            if (!(data instanceof MapType map))
            {
                return null;
            }

            return this.importSceneData(map, file.getName().replace(".scenebbs", ""));
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Create a new scene in this project from a scene document. The scene
     * always gets a fresh id, so importing the same document twice yields
     * two independent scenes instead of overwriting one.
     */
    public Scene importSceneData(MapType map, String fallbackName)
    {
        if (map == null)
        {
            return null;
        }

        try
        {
            String name = map.getString("name");
            String background = map.getString("background", "");

            if (name.isEmpty())
            {
                name = fallbackName == null || fallbackName.isEmpty() ? "Scene" : fallbackName;
            }

            Scene scene = this.create(name, background);

            BaseType filmData = map.get("film");

            if (filmData instanceof MapType filmMap)
            {
                Film film = new Film();

                film.fromData(filmMap);
                this.saveFilm(scene, film);
            }

            return scene;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    /** Filesystem-safe name: strip path separators and control characters. */
    private String sanitize(String name)
    {
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }
}
