package lingfeng.bbsnext.film.replays;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Disk-backed side-table for replay-level leash relationships.
 *
 * <p>Keyed by {@code "<filmId>/<leashedReplayId>"} (each leashed replay has at most
 * one holder) so a link survives replay reordering. Persisted to
 * {@code config/bbs/leashes/leashes.json} under the Minecraft game folder. Never
 * touches the original BBS {@code Replay}; the vanilla leash rope is driven by
 * {@code ActorEntity} leash data, this store only records the editing relationship
 * (which replay holds which, and the bones the rope connects to).</p>
 */
public class LeashStore
{
    private static File file()
    {
        return new File(BBSMod.getGameFolder(), "config/bbs/leashes/leashes.json");
    }

    private static final Map<String, LeashLink> MAP = new HashMap<>();

    static
    {
        load();
    }

    private static String key(String filmId, String leashedReplayId)
    {
        return filmId + "/" + leashedReplayId;
    }

    public static LeashLink get(String filmId, String leashedReplayId)
    {
        return MAP.get(key(filmId, leashedReplayId));
    }

    public static LeashLink getIfPresent(String filmId, String leashedReplayId)
    {
        return MAP.get(key(filmId, leashedReplayId));
    }

    public static List<LeashLink> getAll(String filmId)
    {
        List<LeashLink> out = new ArrayList<>();
        String prefix = filmId + "/";

        for (Map.Entry<String, LeashLink> e : MAP.entrySet())
        {
            if (e.getKey().startsWith(prefix))
            {
                out.add(e.getValue());
            }
        }

        return out;
    }

    public static void set(String filmId, String holderReplayId, String leashedReplayId, String holderBone, String leashedBone)
    {
        MAP.put(key(filmId, leashedReplayId), new LeashLink(holderReplayId, leashedReplayId, holderBone, leashedBone));
        save();
    }

    /* Proxy holder (empty-world-point endpoint): materialised at runtime as a
       vanilla BlockDisplay entity. Position is persisted so it can be rebuilt on
       level reload; the entity itself is never written to the project save. */
    public static void setProxy(String filmId, String leashedReplayId, String leashedBone, double x, double y, double z)
    {
        LeashLink link = new LeashLink("", leashedReplayId, "", leashedBone);
        link.holderType = "proxy";
        link.holderX = x;
        link.holderY = y;
        link.holderZ = z;
        MAP.put(key(filmId, leashedReplayId), link);
        save();
    }

    public static void set(String filmId, LeashLink link)
    {
        if (link == null)
        {
            return;
        }

        MAP.put(key(filmId, link.leashedReplayId), link);
        save();
    }

    public static void removeLeash(String filmId, String leashedReplayId)
    {
        MAP.remove(key(filmId, leashedReplayId));
        save();
    }

    /* Cascade delete: drop any link that references the replay as holder OR leashed
     * so deleting a replay never leaves a phantom leash pointing at a ghost. */
    public static synchronized void removeForReplay(String filmId, String replayId)
    {
        if (replayId == null || replayId.isEmpty())
        {
            return;
        }

        String prefix = filmId + "/";
        boolean changed = false;

        for (Map.Entry<String, LeashLink> e : new HashMap<>(MAP).entrySet())
        {
            if (!e.getKey().startsWith(prefix))
            {
                continue;
            }

            LeashLink link = e.getValue();

            if (replayId.equals(link.holderReplayId) || replayId.equals(link.leashedReplayId))
            {
                MAP.remove(e.getKey());
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
                    MAP.put(e.getKey(), LeashLink.fromJson(e.getValue().getAsJsonObject()));
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

            for (Map.Entry<String, LeashLink> e : MAP.entrySet())
            {
                root.add(e.getKey(), e.getValue().toJson());
            }

            Files.write(f.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException ignored)
        {
        }
    }

    public static class LeashLink
    {
        public String holderReplayId = "";
        public String leashedReplayId = "";
        public String holderBone = "";
        public String leashedBone = "";
        public String holderType = "replay";   // "replay" | "proxy"
        public double holderX, holderY, holderZ;
        public int startTick = -1;   // timeline clip window; -1 = always active (compat)
        public int endTick = -1;

        public LeashLink()
        {}

        public LeashLink(String holderReplayId, String leashedReplayId, String holderBone, String leashedBone)
        {
            this.holderReplayId = holderReplayId == null ? "" : holderReplayId;
            this.leashedReplayId = leashedReplayId == null ? "" : leashedReplayId;
            this.holderBone = holderBone == null ? "" : holderBone;
            this.leashedBone = leashedBone == null ? "" : leashedBone;
            this.holderType = "replay";
            this.startTick = -1;
            this.endTick = -1;
        }

        public boolean isProxy()
        {
            return "proxy".equals(holderType);
        }

        /* A link is windowed when both bounds are set (>=0). Windowed links are
           only leashed while the playback tick is inside [startTick, endTick]. */
        public boolean isWindowed()
        {
            return startTick >= 0 && endTick >= 0;
        }

        public boolean inWindow(int tick)
        {
            if (!isWindowed())
            {
                return true;
            }

            int lo = Math.min(startTick, endTick);
            int hi = Math.max(startTick, endTick);

            return tick >= lo && tick <= hi;
        }

        public JsonObject toJson()
        {
            JsonObject o = new JsonObject();

            o.addProperty("holderReplayId", holderReplayId);
            o.addProperty("leashedReplayId", leashedReplayId);
            o.addProperty("holderBone", holderBone);
            o.addProperty("leashedBone", leashedBone);
            o.addProperty("holderType", holderType);
            o.addProperty("holderX", holderX);
            o.addProperty("holderY", holderY);
            o.addProperty("holderZ", holderZ);
            o.addProperty("startTick", startTick);
            o.addProperty("endTick", endTick);

            return o;
        }

        public static LeashLink fromJson(JsonObject o)
        {
            LeashLink link = new LeashLink();

            if (o == null)
            {
                return link;
            }

            if (o.has("holderReplayId")) link.holderReplayId = o.get("holderReplayId").getAsString();
            if (o.has("leashedReplayId")) link.leashedReplayId = o.get("leashedReplayId").getAsString();
            if (o.has("holderBone")) link.holderBone = o.get("holderBone").getAsString();
            if (o.has("leashedBone")) link.leashedBone = o.get("leashedBone").getAsString();
            if (o.has("holderType")) link.holderType = o.get("holderType").getAsString();
            if (o.has("holderX")) link.holderX = o.get("holderX").getAsDouble();
            if (o.has("holderY")) link.holderY = o.get("holderY").getAsDouble();
            if (o.has("holderZ")) link.holderZ = o.get("holderZ").getAsDouble();
            if (o.has("startTick")) link.startTick = o.get("startTick").getAsInt();
            if (o.has("endTick")) link.endTick = o.get("endTick").getAsInt();

            return link;
        }
    }

    /* Update only the timeline clip window of an existing link (used by drag /
       resize from the timeline UI) without touching holder / bone. No-op if the
       link does not exist. */
    public static void setWindow(String filmId, String leashedReplayId, int startTick, int endTick)
    {
        LeashLink link = MAP.get(key(filmId, leashedReplayId));

        if (link == null)
        {
            return;
        }

        link.startTick = startTick;
        link.endTick = endTick;
        save();
    }
}
