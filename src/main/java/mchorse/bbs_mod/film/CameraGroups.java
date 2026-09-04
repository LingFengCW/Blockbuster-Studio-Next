package mchorse.bbs_mod.film;

import mchorse.bbs_mod.settings.values.core.ValueList;

/**
 * Ordered container of {@link CameraGroup} assets for one work. Mirrors the
 * {@link mchorse.bbs_mod.film.replays.Replays} container pattern.
 */
public class CameraGroups extends ValueList<CameraGroup>
{
    public CameraGroups(String id)
    {
        super(id);
    }

    @Override
    protected CameraGroup create(String id)
    {
        return new CameraGroup(id);
    }

    /** Find a group by its stable asset id (not its list index). */
    public CameraGroup getByAssetId(String assetId)
    {
        if (assetId == null)
        {
            return null;
        }

        for (CameraGroup g : this.getList())
        {
            if (assetId.equals(g.id.get()))
            {
                return g;
            }
        }

        return null;
    }
}
