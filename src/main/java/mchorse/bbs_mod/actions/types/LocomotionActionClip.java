package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import net.minecraft.world.entity.LivingEntity;

/**
 * Locomotion action (走路 / 奔跑 / 空闲) for "纯动作角色".
 *
 * <p>When this clip is active it advances the actor forward along its facing
 * direction by {@link #step} blocks every {@link #frequency} ticks (the
 * "每组帧数" the user sets in the action editor). {@code mode = idle} keeps the
 * actor in place so the procedural limb animation can play without drifting.</p>
 *
 * <p>The endpoint / trajectory the user drags in the in-game viewport is the
 * actor's own keyframed position; this clip only adds the per-group step on
 * top of it.</p>
 */
public class LocomotionActionClip extends ActionClip
{
    public final ValueString mode = new ValueString("mode", "walk"); /* walk | run | idle */
    public final ValueFloat step = new ValueFloat("step", 0.5F, 0F, 16F);

    public LocomotionActionClip()
    {
        this.add(this.mode);
        this.add(this.step);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        String m = this.mode.get();

        if ("idle".equals(m))
        {
            return;
        }

        float s = this.step.get();

        if ("run".equals(m))
        {
            s *= 1.9F;
        }

        if (s <= 0F)
        {
            return;
        }

        float yaw = (float) Math.toRadians(actor.getYRot());
        double dx = -Math.sin(yaw) * s;
        double dz = Math.cos(yaw) * s;

        actor.setPos(actor.getX() + dx, actor.getY(), actor.getZ() + dz);
    }

    @Override
    protected Clip create()
    {
        return new LocomotionActionClip();
    }
}
