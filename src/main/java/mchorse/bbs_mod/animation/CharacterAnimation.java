package mchorse.bbs_mod.animation;

import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * A complete character animation: named keyframe data for every bone of a
 * model. Each bone has its own independent keyframe channels (position,
 * rotation, scale with bezier curves). Applying the animation at a tick
 * writes the interpolated values into a Pose, which the model renderer
 * consumes.
 */
public class CharacterAnimation
{
    public String id;
    public String name;
    public final List<BoneAnimation> bones = new ArrayList<>();

    public CharacterAnimation(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public BoneAnimation getBone(String bone)
    {
        for (BoneAnimation animation : this.bones)
        {
            if (animation.bone.equals(bone))
            {
                return animation;
            }
        }

        return null;
    }

    public BoneAnimation getOrCreateBone(String bone)
    {
        BoneAnimation animation = this.getBone(bone);

        if (animation == null)
        {
            animation = new BoneAnimation(bone);

            this.bones.add(animation);
        }

        return animation;
    }

    public void removeBone(String bone)
    {
        this.bones.removeIf(animation -> animation.bone.equals(bone));
    }

    public float getLength()
    {
        float length = 0F;

        for (BoneAnimation animation : this.bones)
        {
            length = Math.max(length, animation.getLength());
        }

        return length;
    }

    /**
     * Apply the animation at the given tick into a pose. Bones without any
     * keyframe data are left untouched.
     */
    public void apply(Pose pose, float tick)
    {
        for (BoneAnimation animation : this.bones)
        {
            if (animation.isEmpty())
            {
                continue;
            }

            PoseTransform transform = pose.get(animation.bone);

            transform.translate.set(
                animation.interpolate(BoneAnimation.POS_X, tick),
                animation.interpolate(BoneAnimation.POS_Y, tick),
                animation.interpolate(BoneAnimation.POS_Z, tick)
            );
            transform.rotate.set(
                animation.interpolate(BoneAnimation.ROT_X, tick),
                animation.interpolate(BoneAnimation.ROT_Y, tick),
                animation.interpolate(BoneAnimation.ROT_Z, tick)
            );
            transform.scale.set(
                animation.interpolate(BoneAnimation.SCALE_X, tick),
                animation.interpolate(BoneAnimation.SCALE_Y, tick),
                animation.interpolate(BoneAnimation.SCALE_Z, tick)
            );
        }
    }

    public boolean isEmpty()
    {
        return this.bones.isEmpty() || this.bones.stream().allMatch(BoneAnimation::isEmpty);
    }
}
