package lingfeng.bbsnext.film.replays;

import lingfeng.bbsnext.film.replays.LeashStore.LeashLink;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.undo.IUndo;

/**
 * Undo entry for a leash relationship change on the replay-level side-tables.
 *
 * <p>Mirrors the {@code UnplaceActorUndo} pattern from {@code EditorBridge}: it
 * captures the before/after link in {@link LeashStore} (plus the matching bone
 * in {@link LeashBoneStore}) and is pushed onto the film undo manager. This
 * class is deliberately free of any UI references so it stays in the common
 * (main) source set; the live in-world rope is re-derived from these stores on
 * the next world (re)entry, so undo/redo never has to touch the renderer.</p>
 *
 * <p>Three shapes are encoded by which link is null:
 * <ul>
 *   <li>{@code oldLink == null, newLink != null} - a bind (create)</li>
 *   <li>{@code oldLink != null, newLink == null} - an unbind (remove)</li>
 *   <li>both non-null - a bone/holder modification</li>
 * </ul></p>
 */
public class LeashUndo implements IUndo<ValueGroup>
{
    private final String filmId;
    private final LeashLink oldLink;
    private final LeashLink newLink;

    public LeashUndo(String filmId, LeashLink oldLink, LeashLink newLink)
    {
        this.filmId = filmId;
        this.oldLink = oldLink;
        this.newLink = newLink;
    }

    @Override
    public IUndo<ValueGroup> noMerging()
    {
        return this;
    }

    @Override
    public boolean isMergeable(IUndo<ValueGroup> undo)
    {
        return false;
    }

    @Override
    public void merge(IUndo<ValueGroup> undo)
    {}

    @Override
    public void undo(ValueGroup context)
    {
        if (this.oldLink != null)
        {
            LeashStore.set(this.filmId, this.oldLink);
            LeashBoneStore.set(this.filmId, this.oldLink.leashedReplayId, this.oldLink.leashedBone, new double[] {0D, 0D, 0D});
        }
        else if (this.newLink != null)
        {
            LeashStore.removeLeash(this.filmId, this.newLink.leashedReplayId);
            LeashBoneStore.removeForReplay(this.filmId, this.newLink.leashedReplayId);
        }
    }

    @Override
    public void redo(ValueGroup context)
    {
        if (this.newLink != null)
        {
            LeashStore.set(this.filmId, this.newLink);
            LeashBoneStore.set(this.filmId, this.newLink.leashedReplayId, this.newLink.leashedBone, new double[] {0D, 0D, 0D});
        }
        else if (this.oldLink != null)
        {
            LeashStore.removeLeash(this.filmId, this.oldLink.leashedReplayId);
            LeashBoneStore.removeForReplay(this.filmId, this.oldLink.leashedReplayId);
        }
    }
}
