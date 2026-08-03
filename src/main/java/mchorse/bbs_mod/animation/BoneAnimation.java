package mchorse.bbs_mod.animation;

import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * One bone's keyframe channels. Nine Float channels cover the three axes of
 * position, rotation and scale. Every channel supports bezier keyframes
 * (the KeyframeShape control points), so each body part can have its own
 * custom curve.
 */
public class BoneAnimation
{
    public static final int POS_X = 0;
    public static final int POS_Y = 1;
    public static final int POS_Z = 2;
    public static final int ROT_X = 3;
    public static final int ROT_Y = 4;
    public static final int ROT_Z = 5;
    public static final int SCALE_X = 6;
    public static final int SCALE_Y = 7;
    public static final int SCALE_Z = 8;
    public static final int CHANNELS = 9;

    public static final String[] AXIS_NAMES = { "x", "y", "z", "x", "y", "z", "x", "y", "z" };
    public static final String[] GROUP_NAMES = { "position", "position", "position", "rotation", "rotation", "rotation", "scale", "scale", "scale" };

    public String bone;
    public final KeyframeChannel<Float>[] channels;

    @SuppressWarnings("unchecked")
    public BoneAnimation(String bone)
    {
        this.bone = bone;
        this.channels = new KeyframeChannel[CHANNELS];

        for (int i = 0; i < CHANNELS; i++)
        {
            this.channels[i] = new KeyframeChannel<>("", KeyframeFactories.FLOAT);
        }
    }

    public float interpolate(int axis, float tick)
    {
        KeyframeChannel<Float> channel = this.channels[axis];

        return channel.isEmpty() ? 0F : channel.interpolate(tick);
    }

    public boolean isEmpty()
    {
        for (KeyframeChannel<Float> channel : this.channels)
        {
            if (!channel.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public float getLength()
    {
        float length = 0F;

        for (KeyframeChannel<Float> channel : this.channels)
        {
            length = Math.max(length, (float) channel.getLength());
        }

        return length;
    }
}
