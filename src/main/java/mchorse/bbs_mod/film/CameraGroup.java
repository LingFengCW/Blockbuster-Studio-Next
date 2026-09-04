package mchorse.bbs_mod.film;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.core.ValueString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A camera group bundles several camera clips (by their stable {@link Clip#id})
 * into a single reusable asset that shows up in the asset bin, can be exported
 * to a file or the backpack, and referenced by a sequence.
 *
 * The group only stores references - the actual camera clips live in
 * {@link Film#camera}. That keeps a group cheap to build on the timeline and
 * lets the same camera appear in several groups.
 */
public class CameraGroup extends ValueGroup
{
    /** Stable asset id (8 hex chars), see {@link Clip#id}. */
    public final ValueString id = new ValueString("id", newId());
    public final ValueString name = new ValueString("name", "");
    public final ValueList<ValueString> refs = new ValueList<ValueString>("refs")
    {
        @Override
        protected ValueString create(String id)
        {
            return new ValueString(id, "");
        }
    };

    public CameraGroup(String id)
    {
        super(id);

        this.add(this.id);
        this.add(this.name);
        this.add(this.refs);
    }

    private static String newId()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** Stable camera clip ids referenced by this group. */
    public List<String> cameraIds()
    {
        List<String> out = new ArrayList<>();

        for (ValueString v : this.refs.getList())
        {
            String cid = v.get();

            if (cid != null && !cid.isEmpty())
            {
                out.add(cid);
            }
        }

        return out;
    }

    /** Append a camera clip id (de-duplicated). */
    public void addCamera(String clipId)
    {
        if (clipId == null || clipId.isEmpty())
        {
            return;
        }

        for (ValueString v : this.refs.getList())
        {
            if (clipId.equals(v.get()))
            {
                return;
            }
        }

        this.refs.add(new ValueString(String.valueOf(this.refs.getList().size()), clipId));
    }
}
