package mchorse.bbs_mod.film;

import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

/**
 * Timeline markers for a {@link Film}. Each entry is a tick position that the
 * user dropped on the timeline (M / 标记 button). Stored as a {@link ValueList}
 * of {@link ValueInt} so it serialises with the film and survives editor
 * reloads and project file round-trips.
 */
public class MarkerList extends ValueList<ValueInt>
{
    public MarkerList(String id)
    {
        super(id);
    }

    @Override
    protected ValueInt create(String id)
    {
        return new ValueInt(id, 0);
    }
}
