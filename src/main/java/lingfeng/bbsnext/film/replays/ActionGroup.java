package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;

import net.minecraft.world.entity.LivingEntity;

/**
 * An ordered composition of sub-actions played as a single bindable unit.
 *
 * Each sub-action is a normal {@link ActionClip} (e.g. a locomotion step or a
 * script action) placed at a relative start tick inside the group via its own
 * {@code tick}/{@code duration}. Reusing {@link Clips} + the existing action
 * factory means groups nest inside {@link Replay#actions} exactly like any
 * other action clip and serialize through the same pipeline.
 *
 * Playback delegates every tick (frequency forced to 1) to each sub-action at
 * its relative position, so a group such as "leg forward / leg back / leg
 * forward / leg back" reproduces the whole sequence, and a separate "jump"
 * group can be bound onto any character's action timeline independently.
 */
public class ActionGroup extends ActionClip
{
    public final Clips subActions = new Clips("subActions", BBSMod.getFactoryActionClips());

    public ActionGroup()
    {
        super();

        this.frequency.set(1);
        this.add(this.subActions);
    }

    @Override
    protected Clip create()
    {
        return new ActionGroup();
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        super.copy(group);

        if (group instanceof ActionGroup source)
        {
            for (Clip sub : source.subActions.get())
            {
                this.subActions.addClip(sub.copy());
            }
        }
    }

    @Override
    public boolean isClient()
    {
        for (Clip sub : this.subActions.get())
        {
            if (sub instanceof ActionClip action && !action.isClient())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        int relative = tick - this.tick.get();
        int duration = this.duration.get();

        if (relative < 0 || relative >= duration)
        {
            return;
        }

        for (Clip sub : this.subActions.get())
        {
            if (sub instanceof ActionClip action)
            {
                action.apply(actor, player, film, replay, relative);
            }
        }
    }

    @Override
    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {
        int relative = tick - this.tick.get();
        int duration = this.duration.get();

        if (relative < 0 || relative >= duration)
        {
            return;
        }

        for (Clip sub : this.subActions.get())
        {
            if (sub instanceof ActionClip action && action.isClient())
            {
                action.applyClient(entity, film, replay, relative);
            }
        }
    }
}
