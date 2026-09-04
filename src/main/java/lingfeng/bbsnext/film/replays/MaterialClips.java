package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.settings.values.core.ValueList;

/**
 * Ordered list of {@link MaterialClip}s owned by a {@link Replay}. Backed by
 * {@link ValueList} so it serialises/deserialises like the rest of the value
 * tree (add / remove / reorder + auto id re-index via {@code sync()}).
 *
 * <p>Lives in the {@code lingfeng.bbsnext} namespace (personal code folder) but
 * in the {@code main} source set, because {@code Replay} (compiled for client
 * and server) holds and applies it.</p>
 */
public class MaterialClips extends ValueList<MaterialClip>
{
    public MaterialClips(String id)
    {
        super(id);
    }

    @Override
    protected MaterialClip create(String id)
    {
        return new MaterialClip(id);
    }

    /** Offset every material clip's start tick by {@code tick} (mirrors
     *  {@code Clips.shift} so the whole replay can be shifted as one unit). */
    public void shift(float tick)
    {
        for (MaterialClip mc : this.getAllTyped())
        {
            mc.tick.set(Math.round(mc.tick.get() + tick));
        }
    }
}
