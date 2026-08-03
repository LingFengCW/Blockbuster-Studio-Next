package mchorse.bbs_mod.animation;

import mchorse.bbs_mod.utils.pose.Pose;

/**
 * Client-side playback state for character animations. Holds the currently
 * active animation and the playhead tick. ModelFormRenderer.getPose() calls
 * {@link #apply(Pose)} so the keyframe timeline drives the model's bones.
 */
public class AnimationPlayer
{
    public static CharacterAnimation current;
    public static float tick;
    public static boolean playing;
    public static float playbackSpeed = 1F;

    public static void apply(Pose pose)
    {
        if (current != null)
        {
            current.apply(pose, tick);
        }
    }

    /**
     * Advance the playhead; called every render tick while playing.
     */
    public static void update(float delta)
    {
        if (current == null || !playing)
        {
            return;
        }

        tick += delta * 20F * playbackSpeed;

        float length = current.getLength();

        if (length > 0F && tick > length)
        {
            tick = 0F;
        }
    }

    public static void stop()
    {
        playing = false;
        tick = 0F;
    }
}
