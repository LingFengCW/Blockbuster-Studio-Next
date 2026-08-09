package mchorse.bbs_mod.actions.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.world.entity.LivingEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

/**
 * 脚本动作 (Script Action).
 *
 * <p>Carries a user-defined list of numbers ({@link #params}) that other
 * tooling or the community can read and drive. Each script action can be
 * exported to a standalone {@code .json} file under
 * {@code <gameDir>/bbs_scripts/} so creators can share parameter presets with
 * each other.</p>
 */
public class ScriptActionClip extends ActionClip
{
    public final ValueList<ValueFloat> params = new ValueList<ValueFloat>("params")
    {
        @Override
        protected ValueFloat create(String id)
        {
            return new ValueFloat(id, 0F);
        }
    };

    public ScriptActionClip()
    {
        this.add(this.params);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        /* Script actions are parameter carriers; the runtime effect is defined
         * by external tooling reading params. No default in-engine behavior. */
    }

    @Override
    protected Clip create()
    {
        return new ScriptActionClip();
    }

    /** Build a shareable JSON representation of this script action. */
    public JsonObject toScriptJson()
    {
        JsonObject root = new JsonObject();
        root.addProperty("type", "bbs:script");
        root.addProperty("title", this.title.get());
        root.addProperty("frequency", this.frequency.get());
        root.addProperty("duration", this.duration.get());

        JsonArray arr = new JsonArray();
        for (ValueFloat v : this.params.getList())
        {
            arr.add(v.get());
        }
        root.add("params", arr);
        return root;
    }

    /** Export this script action to {@code <gameDir>/bbs_scripts/<title>.json}.
     *  Returns the written file, or {@code null} on failure. */
    public File exportScript(File gameDir)
    {
        File dir = new File(gameDir, "bbs_scripts");
        if (!dir.exists() && !dir.mkdirs())
        {
            return null;
        }

        String name = this.title.get();
        if (name == null || name.isEmpty())
        {
            name = "script_action";
        }
        name = name.replaceAll("[^\\w\\-一-龥]", "_");

        File out = new File(dir, name + ".json");
        try (Writer w = new FileWriter(out))
        {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            w.write(gson.toJson(this.toScriptJson()));
            return out;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** Re-import params from a JSON object produced by {@link #toScriptJson()}. */
    public void fromScriptJson(JsonObject root)
    {
        if (root == null)
        {
            return;
        }

        if (root.has("title"))
        {
            this.title.set(root.get("title").getAsString());
        }
        if (root.has("frequency"))
        {
            this.frequency.set(root.get("frequency").getAsInt());
        }
        if (root.has("duration"))
        {
            this.duration.set(root.get("duration").getAsInt());
        }
        if (root.has("params") && root.get("params").isJsonArray())
        {
            this.params.getAllTyped().clear();
            for (JsonElement e : root.getAsJsonArray("params"))
            {
                ValueFloat v = new ValueFloat(String.valueOf(this.params.getAllTyped().size()), 0F);
                v.set(e.getAsFloat());
                this.params.add(v);
            }
        }
    }
}
