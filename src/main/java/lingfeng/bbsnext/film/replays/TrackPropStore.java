package lingfeng.bbsnext.film.replays;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Disk-backed side-table for per-track visual properties.
 *
 * <p>Keyed by {@code "<filmId>/<stableReplayId>"} so the data survives replay
 * reordering and deletion. Persisted to {@code config/bbs/trackprops/trackprops.json}
 * under the Minecraft game folder. Never touches the original BBS {@code Replay}.</p>
 */
public class TrackPropStore
{
    private static File file()
    {
        return new File(BBSMod.getGameFolder(), "config/bbs/trackprops/trackprops.json");
    }

    private static final Map<String, TrackProp> MAP = new HashMap<>();

    static
    {
        load();
    }

    private static String key(String filmId, String replayId)
    {
        return filmId + "/" + replayId;
    }

    public static TrackProp get(String filmId, String replayId)
    {
        return MAP.computeIfAbsent(key(filmId, replayId), k -> new TrackProp());
    }

    /** Read a side-table entry without creating a default (used by D2 snapshots
     *  so capturing a replay's cascade never materialises phantom entries). */
    public static TrackProp getIfPresent(String filmId, String replayId)
    {
        return MAP.get(key(filmId, replayId));
    }

    public static void set(String filmId, String replayId, String prop, String value)
    {
        TrackProp p = get(filmId, replayId);

        p.apply(prop, value);
        save();
    }

    public static void removeForReplay(String filmId, String replayId)
    {
        MAP.remove(key(filmId, replayId));
        save();
    }

    /* A2/S15: when a track is unplaced or its replay deleted, every other track
     * whose matte source pointed at that replay id must drop the reference so it
     * degrades to "no mask" instead of pointing at a ghost. Scans all entries of
     * the film; safe to call even when the id is already gone. */
    public static synchronized void clearMatteSource(String filmId, String replayId)
    {
        if (replayId == null || replayId.isEmpty())
        {
            return;
        }

        String prefix = filmId + "/";
        boolean changed = false;

        for (Map.Entry<String, TrackProp> e : MAP.entrySet())
        {
            if (!e.getKey().startsWith(prefix))
            {
                continue;
            }

            TrackProp p = e.getValue();

            if (replayId.equals(p.matteSource))
            {
                p.matteSource = "";
                changed = true;
            }
        }

        if (changed)
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
                    MAP.put(e.getKey(), TrackProp.fromJson(e.getValue().getAsJsonObject()));
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

            for (Map.Entry<String, TrackProp> e : MAP.entrySet())
            {
                root.add(e.getKey(), e.getValue().toJson());
            }

            Files.write(f.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException ignored)
        {
        }
    }
}
