package mchorse.bbs_mod.utils.keyframes;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.factories.DoubleKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

/**
 * Keyframe segment.
 *
 * This structure contains all the necessary data to create an interpolated
 * value.
 */
public class KeyframeSegment <T>
{
    public Keyframe<T> a;
    public Keyframe<T> b;

    public Keyframe<T> preA;
    public Keyframe<T> postB;
    public float duration;
    public float offset;
    public float x;

    public KeyframeSegment()
    {}

    public KeyframeSegment(Keyframe<T> a, Keyframe<T> b)
    {
        this.fill(a, b);
    }

    public void setup(Keyframe<T> a, Keyframe<T> b, float ticks)
    {
        this.fill(a, b);
        this.setup(ticks);
    }

    public void fill(Keyframe<T> a, Keyframe<T> b)
    {
        this.a = a;
        this.b = b;

        KeyframeChannel<T> channel = (KeyframeChannel<T>) a.getParent();
        int index = channel.getKeyframes().indexOf(a);

        if (index >= 0)
        {
            Keyframe<T> preA = channel.get(index - 1);
            Keyframe<T> postB = channel.get(index + 2);

            this.preA = preA == null ? a : preA;
            this.postB = postB == null ? b : postB;
        }
        else
        {
            this.preA = a;
            this.postB = b;
        }
    }

    public void fill(Keyframe<T> a, Keyframe<T> b, int bIndex)
    {
        this.a = a;
        this.b = b;

        KeyframeChannel<T> channel = (KeyframeChannel<T>) a.getParent();
        Keyframe<T> preA = channel.get(bIndex - 2);
        Keyframe<T> postB = channel.get(bIndex + 1);

        this.preA = preA == null ? a : preA;
        this.postB = postB == null ? b : postB;
    }

    public void setup(float ticks)
    {
        float forcedDuration = this.a.getDuration();

        this.duration = forcedDuration > 0 ? forcedDuration : this.b.getTick() - this.a.getTick();
        this.offset = ticks - this.a.getTick();
        this.x = MathUtils.clamp(this.duration == 0 ? 0F : this.offset / this.duration, 0F, 1F);
    }

    public T createInterpolated()
    {
        IKeyframeFactory<T> factory = this.a.getFactory();

        if (this.isSame())
        {
            return factory.copy(this.a.getValue());
        }

        KeyframeChannel<T> channel = (KeyframeChannel<T>) this.a.getParent();

        /* Angular (cyclic) channels interpolate along the shortest arc so values
         * such as yaw 350° → 10° don't spin the long way around. */
        if (channel != null && channel.isCyclic() && factory instanceof DoubleKeyframeFactory)
        {
            DoubleKeyframeFactory df = (DoubleKeyframeFactory) factory;
            double av = (Double) this.a.getValue();
            double preA = av + shortestAngleDelta(av, (Double) this.preA.getValue());
            double bv = av + shortestAngleDelta(av, (Double) this.b.getValue());
            double postB = av + shortestAngleDelta(av, (Double) this.postB.getValue());

            return (T) (Double) df.interpolate(preA, av, bv, postB, this.a.getInterpolation(), this.x);
        }

        return factory.copy(factory.interpolate(this.preA, this.a, this.b, this.postB, this.a.getInterpolation(), this.x));
    }

    private static double shortestAngleDelta(double from, double to)
    {
        double d = (to - from) % 360D;

        if (d > 180D) d -= 360D;
        else if (d < -180D) d += 360D;

        return d;
    }

    public boolean isSame()
    {
        return this.a == this.b;
    }

    public Keyframe<T> getClosest()
    {
        return this.x > 0.5F ? this.b : this.a;
    }
}