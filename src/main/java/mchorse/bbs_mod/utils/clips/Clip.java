package mchorse.bbs_mod.utils.clips;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueString;

import java.util.UUID;

public abstract class Clip extends ValueGroup
{
    /** Stable asset id (8 hex chars). Lets camera groups and sequences
     *  reference a clip even after it is reordered or other clips are
     *  removed — index alone is not stable. Generated in the constructor,
     *  so legacy saves (no "id" field) get one automatically on load. */
    public final ValueString id = new ValueString("id", newId());
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString title = new ValueString("title", "");
    public final ValueInt layer = new ValueInt("layer", 0, 0, Integer.MAX_VALUE);
    public final ValueInt tick = new ValueInt("tick", 0, 0, Integer.MAX_VALUE);
    public final ValueInt duration = new ValueInt("duration", 1, 1, Integer.MAX_VALUE);
    public final Envelope envelope = new Envelope("envelope");

    public Clip()
    {
        super("");

        this.add(this.id);
        this.add(this.enabled);
        this.add(this.title);
        this.add(this.layer);
        this.add(this.tick);
        this.add(this.duration);
        this.add(this.envelope);
    }

    private static String newId()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public boolean isGlobal()
    {
        return false;
    }

    public boolean isInside(int tick)
    {
        int offset = this.tick.get();

        return tick >= offset && tick < offset + this.duration.get();
    }

    public void shift(double dx, double dy, double dz)
    {}

    public void shiftLeft(int tick)
    {}

    public Clip copy()
    {
        Clip clip = this.create();

        clip.copy(this);

        /* Give the copy its own asset id so duplicated clips stay distinct
         * when referenced by camera groups / sequences. */
        clip.id.set(newId());

        return clip;
    }

    protected abstract Clip create();

    /**
     * Breakdown this fixture into another piece starting at given offset
     */
    public Clip breakDown(int offset)
    {
        int duration = this.duration.get();

        if (offset <= 0 || offset >= duration)
        {
            return null;
        }

        Clip clip = this.copy();

        clip.duration.set(duration - offset);
        clip.breakDownClip(this, offset);

        return clip;
    }

    protected void breakDownClip(Clip original, int offset)
    {
        this.envelope.breakDown(original, offset);
    }
}