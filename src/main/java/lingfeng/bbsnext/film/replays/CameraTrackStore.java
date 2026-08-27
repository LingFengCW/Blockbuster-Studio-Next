package lingfeng.bbsnext.film.replays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Disk-backed side-table for the "placed on timeline" ordering of cameras.
 *
 * <p>In BBS a {@link mchorse.bbs_mod.film.replays.Clip} camera always lives
 * inside {@code film.camera} (serialised by the original mod). This store adds
 * the concept of a <em>timeline track</em> without touching the mchorse
 * {@code Film} class: a camera is "on the timeline" iff its stable clip id
 * appears in this ordered list for the film. Dragging a library camera onto
 * the timeline inserts its id here; removing a track drops the id (the camera
 * stays in {@code film.camera}, so it returns to the library).</p>
 *
 * <p>Keyed by film id. Persisted to {@code config/bbs/cameratrack/cameratrack.json}
 * under the Minecraft game folder, written on every mutation so it survives
 * film reloads regardless of how the film itself is saved.</p>
 */
public class CameraTrackStore
{
    private static File file()
    {
        return new File(BBSMod.getGameFolder(), "config/bbs/cameratrack/cameratrack.json");
    }

    private static final Map<String, List<String>> MAP = new HashMap<>();

    static
    {
        load();
    }

    private static String key(String filmId)
    {
        return filmId;
    }

    /** Ordered list of placed camera clip ids for a film (may be empty). */
    public static List<String> get(String filmId)
    {
        List<String> list = MAP.get(key(filmId));

        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    /** True once this film has an explicit (possibly empty) order stored. */
    public static boolean has(String filmId)
    {
        return MAP.containsKey(key(filmId));
    }

    public static void set(String filmId, List<String> order)
    {
        MAP.put(key(filmId), new ArrayList<>(order));
        save();
    }

    /** Insert a camera clip id at a clamped position; no-op if already present. */
    public static void insert(String filmId, String cameraId, int targetIndex)
    {
        List<String> list = get(filmId);

        if (list.contains(cameraId))
        {
            return;
        }

        int t = targetIndex;

        if (t < 0 || t > list.size())
        {
            t = list.size();
        }

        list.add(t, cameraId);
        set(filmId, list);
    }

    public static void remove(String filmId, String cameraId)
    {
        List<String> list = get(filmId);

        if (list.remove(cameraId))
        {
            set(filmId, list);
        }
    }

    public static void removeForFilm(String filmId)
    {
        if (MAP.remove(key(filmId)) != null)
        {
            save();
        }
    }

    public static synchronized void load()
    {
        MAP.clear();

        File f = file();

        if (!f.exists())
        {
            return;
        }

        try
        {
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(text);

            if (root.isJsonObject())
            {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet())
                {
                    List<String> ids = new ArrayList<>();

                    if (e.getValue().isJsonArray())
                    {
                        for (JsonElement idEl : e.getValue().getAsJsonArray())
                        {
                            ids.add(idEl.getAsString());
                        }
                    }

                    MAP.put(e.getKey(), ids);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    public static synchronized void save()
    {
        try
        {
            File f = file();

            f.getParentFile().mkdirs();

            JsonObject root = new JsonObject();

            for (Map.Entry<String, List<String>> e : MAP.entrySet())
            {
                JsonArray arr = new JsonArray();

                for (String id : e.getValue())
                {
                    arr.add(id);
                }

                root.add(e.getKey(), arr);
            }

            Files.write(f.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception ignored)
        {
        }
    }
}
