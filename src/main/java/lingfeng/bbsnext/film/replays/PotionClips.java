package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.settings.values.core.ValueList;

/**
 * Ordered list of {@link PotionClip}s owned by a {@link Replay}. Backed by
 * {@link ValueList} so it serialises/deserialises like the rest of the value
 * tree (add / remove / reorder + auto id re-index via {@code sync()}).
 *
 * <p>Lives in the {@code lingfeng.bbsnext} namespace (personal code folder) but
 * in the {@code main} source set, because {@code Replay} (compiled for client
 * and server) holds and applies it.</p>
 */
public class PotionClips extends ValueList<PotionClip>
{
    public PotionClips(String id)
    {
        super(id);
    }

    @Override
    protected PotionClip create(String id)
    {
        return new PotionClip(id);
    }

    /** Offset every potion clip's start tick by {@code tick} (mirrors
     *  {@code MaterialClips.shift} so the whole replay can be shifted as one unit). */
    public void shift(float tick)
    {
        for (PotionClip pc : this.getAllTyped())
        {
            pc.tick.set(Math.round(pc.tick.get() + tick));
        }
    }
}
