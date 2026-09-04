package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Session library of composed action groups, surfaced in the action editor's
 * dropdown so a once-composed group can be bound (added as a clip) onto any
 * character's action timeline. Lazily created so it never runs before the
 * action-clip factory is registered during mod init.
 *
 * The library survives game restarts by persisting to
 * &lt;configDir&gt;/bbs/action_groups.json. Serialization reuses the project's
 * {@link Clips#toData()} / {@link Clips#fromData(BaseType)} pipeline (the same
 * one AnimationManager uses) so sub-actions round-trip exactly like inside a
 * film. Writes are atomic (temp file + ATOMIC_MOVE) so a crash mid-write never
 * leaves a half-written file; a missing or corrupt file is logged and ignored
 * rather than throwing into the caller.
 */
public class ActionGroupLibrary
{
    private static final String DIR = "bbs";
    private static final String FILE = "action_groups.json";

    private static Clips groups;

    public static Clips get()
    {
        if (groups == null)
        {
            groups = new Clips("actionGroups", BBSMod.getFactoryActionClips());
            loadGroups();
        }

        return groups;
    }

    public static ActionGroup find(String id)
    {
        if (id == null)
        {
            return null;
        }

        for (Clip clip : get().get())
        {
            if (clip instanceof ActionGroup group && id.equals(group.id.get()))
            {
                return group;
            }
        }

        return null;
    }

    public static void addGroup(Clip clip)
    {
        if (!(clip instanceof ActionGroup group))
        {
            return;
        }

        get().addClip(group);
        saveGroups();
    }

    public static void removeGroup(String id)
    {
        ActionGroup group = find(id);

        if (group != null)
        {
            get().remove(group);
            saveGroups();
        }
    }

    private static Path configFile()
    {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR).resolve(FILE);
    }

    public static void saveGroups()
    {
        if (groups == null)
        {
            return;
        }

        Path file = configFile();

        try
        {
            Files.createDirectories(file.getParent());

            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");

            Files.write(tmp, DataStorageUtils.writeToBytes(groups.toData()));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException e)
        {
            BBSMod.LOGGER.error("[ActionGroupLibrary] failed to save action groups", e);
        }
    }

    public static void loadGroups()
    {
        if (groups == null)
        {
            return;
        }

        Path file = configFile();

        if (!Files.exists(file))
        {
            return;
        }

        try
        {
            byte[] bytes = Files.readAllBytes(file);

            if (bytes.length == 0)
            {
                return;
            }

            BaseType data = DataStorageUtils.readFromBytes(bytes);

            if (data != null)
            {
                groups.fromData(data);
            }
        }
        catch (IOException e)
        {
            BBSMod.LOGGER.error("[ActionGroupLibrary] failed to load action groups", e);
        }
    }
}
