package lingfeng.bbsnext.film.replays;

import com.google.gson.JsonArray;
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
 * Disk-backed side-table for the per-replay leash attachment point.
 *
 * <p>Keyed by {@code "<filmId>/<replayId>"} so the data survives replay reordering
 * and deletion. Persisted to {@code config/bbs/leashbones/leashbones.json} under
 * the Minecraft game folder. Never touches the original BBS {@code Replay}. The
 * attached bone plus offset is where the vanilla leash rope connects on that
 * replay's actor; the actual rope is still rendered by Minecraft.</p>
 */
public class LeashBoneStore
{
    private static File file()
    {
        return new File(BBSMod.getGameFolder(), "config/bbs/leashbones/leashbones.json");
    }

    private static final Map<String, LeashBone> MAP = new HashMap<>();

    static
    {
        load();
    }

    private static String key(String filmId, String replayId)
    {
        return filmId + "/" + replayId;
    }

    public static LeashBone get(String filmId, String replayId)
    {
        return MAP.get(key(filmId, replayId));
    }

    public static LeashBone getIfPresent(String filmId, String replayId)
    {
        return MAP.get(key(filmId, replayId));
    }

    public static void set(String filmId, String replayId, String bone, double[] offset)
    {
        LeashBone b = MAP.computeIfAbsent(key(filmId, replayId), k -> new LeashBone());

        b.bone = bone == null ? "" : bone;
        b.offset = offset == null ? new double[] {0D, 0D, 0D} : offset;
        save();
    }

    public static void removeForReplay(String filmId, String replayId)
    {
        MAP.remove(key(filmId, replayId));
        save();
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
                    MAP.put(e.getKey(), LeashBone.fromJson(e.getValue().getAsJsonObject()));
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

            for (Map.Entry<String, LeashBone> e : MAP.entrySet())
            {
                root.add(e.getKey(), e.getValue().toJson());
            }

            Files.write(f.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException ignored)
        {
        }
    }

    public static class LeashBone
    {
        public String bone = "";
        public double[] offset = new double[] {0D, 0D, 0D};

        public JsonObject toJson()
        {
            JsonObject o = new JsonObject();

            o.addProperty("bone", bone);

            JsonArray array = new JsonArray();

            array.add(offset[0]);
            array.add(offset[1]);
            array.add(offset[2]);
            o.add("offset", array);

            return o;
        }

        public static LeashBone fromJson(JsonObject o)
        {
            LeashBone b = new LeashBone();

            if (o == null)
            {
                return b;
            }

            if (o.has("bone")) b.bone = o.get("bone").getAsString();

            if (o.has("offset") && o.get("offset").isJsonArray())
            {
                JsonArray arr = o.get("offset").getAsJsonArray();
                double[] v = new double[] {0D, 0D, 0D};

                for (int i = 0; i < 3 && i < arr.size(); i++)
                {
                    v[i] = arr.get(i).getAsDouble();
                }

                b.offset = v;
            }

            return b;
        }
    }
}
