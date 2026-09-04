package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 录制路径动作（Phase D「使用录制创建动作」）。
 *
 * <p>把世界内录制的行走轨迹（x/y/z/yaw 关键帧）打包成一个可复用、可拖入时间轴
 * 的动作。播放时按 tick 采样轨迹并把演员移动到对应位置，实现"跟着录制的路走"。
 * 轨迹数据直接内嵌在本 clip 里，随作品 JSON 一起保存，可跨作品复制。</p>
 */
public class RecordedPathActionClip extends ActionClip
{
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> z = new KeyframeChannel<>("z", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> yaw = new KeyframeChannel<>("yaw", KeyframeFactories.DOUBLE);

    public RecordedPathActionClip()
    {
        this.add(this.x);
        this.add(this.y);
        this.add(this.z);
        this.add(this.yaw);
    }

    /** 从角色已录制的 keyframes 拷贝轨迹（录制坐标写回角色后调用）。 */
    public void copyFrom(ReplayKeyframes keyframes)
    {
        this.x.copyKeyframes(keyframes.x);
        this.y.copyKeyframes(keyframes.y);
        this.z.copyKeyframes(keyframes.z);
        this.yaw.copyKeyframes(keyframes.yaw);
    }

    /** 轨迹覆盖到的最大 tick（用作动作默认时长）。 */
    public int pathDuration()
    {
        int duration = 0;

        for (KeyframeChannel<Double> channel : new KeyframeChannel[]{ this.x, this.y, this.z, this.yaw })
        {
            List<Keyframe<Double>> keyframes = channel.getKeyframes();

            if (!keyframes.isEmpty())
            {
                duration = (int) Math.max(duration, Math.ceil(keyframes.get(keyframes.size() - 1).getTick()));
            }
        }

        return duration;
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        if (this.x.isEmpty())
        {
            return;
        }

        double px = this.x.sample(tick);
        double py = this.y.sample(tick);
        double pz = this.z.sample(tick);

        actor.setPos(px, py, pz);

        if (!this.yaw.isEmpty())
        {
            float yawDeg = this.yaw.sample(tick).floatValue();

            actor.setYRot(yawDeg);
            actor.setYHeadRot(yawDeg);
            actor.setYBodyRot(yawDeg);
        }
    }

    @Override
    protected Clip create()
    {
        return new RecordedPathActionClip();
    }
}
