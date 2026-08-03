package mchorse.bbs_mod.projects;

/**
 * A scene is a single stage within a {@link BBSProject}. One project owns
 * many scenes, and every scene carries its own {@link mchorse.bbs_mod.film.Film}
 * payload (camera clips, actors and inventory state) which is stored inside
 * the owning project's directory.
 *
 * The class itself is intentionally a lightweight descriptor: the heavy Film
 * payload is loaded lazily through {@link SceneManager#loadFilm(Scene)} so
 * that listing scenes never has to decompress every stage on disk.
 */
public class Scene
{
    public final String id;
    public String name;
    public final long createdAt;

    /** Background world (singleplayer save folder name, "" = blank). */
    public String background = "";

    public Scene(String id, String name, long createdAt)
    {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    @Override
    public String toString()
    {
        return "Scene{" + this.id + ", " + this.name + "}";
    }
}
