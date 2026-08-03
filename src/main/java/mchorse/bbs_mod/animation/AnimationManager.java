package mchorse.bbs_mod.animation;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Loads/saves character animations. Files live under
 * settings/animations/<id>.json, which BBSMod.getSettingsPath() scopes to
 * the currently active project, so animations are isolated per project and
 * can be migrated between projects through the backpack.
 */
public class AnimationManager
{
    public static File getAnimationsFolder()
    {
        return BBSMod.getSettingsPath("animations");
    }

    public static List<CharacterAnimation> loadAll()
    {
        List<CharacterAnimation> animations = new ArrayList<>();
        File folder = getAnimationsFolder();

        if (folder.isDirectory())
        {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

            if (files != null)
            {
                for (File file : files)
                {
                    try
                    {
                        BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(file.toPath()));

                        if (data instanceof MapType map)
                        {
                            animations.add(fromData(map));
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        animations.sort(Comparator.comparing((CharacterAnimation a) -> a.name));

        return animations;
    }

    public static CharacterAnimation create(String name)
    {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        return new CharacterAnimation(id, name);
    }

    public static void save(CharacterAnimation animation)
    {
        try
        {
            File folder = getAnimationsFolder();

            folder.mkdirs();

            File file = new File(folder, animation.id + ".json");

            Files.write(file.toPath(), DataStorageUtils.writeToBytes(toData(animation)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void delete(CharacterAnimation animation)
    {
        File file = new File(getAnimationsFolder(), animation.id + ".json");

        if (file.isFile())
        {
            file.delete();
        }
    }

    public static MapType toData(CharacterAnimation animation)
    {
        MapType data = new MapType();
        ListType bones = new ListType();

        data.putString("id", animation.id);
        data.putString("name", animation.name);

        for (BoneAnimation bone : animation.bones)
        {
            MapType boneData = new MapType();

            boneData.putString("bone", bone.bone);

            for (int i = 0; i < BoneAnimation.CHANNELS; i++)
            {
                boneData.put("ch" + i, bone.channels[i].toData());
            }

            bones.add(boneData);
        }

        data.put("bones", bones);

        return data;
    }

    public static CharacterAnimation fromData(MapType data)
    {
        CharacterAnimation animation = new CharacterAnimation(
            data.getString("id"),
            data.getString("name")
        );

        BaseType bonesData = data.get("bones");

        if (bonesData instanceof ListType list)
        {
            for (BaseType item : list)
            {
                if (item instanceof MapType boneData)
                {
                    BoneAnimation bone = new BoneAnimation(boneData.getString("bone"));

                    for (int i = 0; i < BoneAnimation.CHANNELS; i++)
                    {
                        BaseType channel = boneData.get("ch" + i);

                        if (channel != null)
                        {
                            bone.channels[i].fromData(channel);
                        }
                    }

                    animation.bones.add(bone);
                }
            }
        }

        return animation;
    }
}
