/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.animation;

import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record AnimationChannel(Target target, Keyframe[] keyframes) {

    public static interface Target {
        public void apply(ModelPart var1, Vector3f var2);
    }

    public static class Interpolations {
        public static final Interpolation LINEAR = (vector, alpha, keyframes, prev, next, targetScale) -> {
            Vector3fc point0 = keyframes[prev].postTarget();
            Vector3fc point1 = keyframes[next].preTarget();
            return point0.lerp(point1, alpha, vector).mul(targetScale);
        };
        public static final Interpolation CATMULLROM = (vector, alpha, keyframes, prev, next, targetScale) -> {
            Vector3fc point0 = keyframes[Math.max(0, prev - 1)].postTarget();
            Vector3fc point1 = keyframes[prev].postTarget();
            Vector3fc point2 = keyframes[next].postTarget();
            Vector3fc point3 = keyframes[Math.min(keyframes.length - 1, next + 1)].postTarget();
            vector.set(Mth.catmullrom((float)alpha, (float)point0.x(), (float)point1.x(), (float)point2.x(), (float)point3.x()) * targetScale, Mth.catmullrom((float)alpha, (float)point0.y(), (float)point1.y(), (float)point2.y(), (float)point3.y()) * targetScale, Mth.catmullrom((float)alpha, (float)point0.z(), (float)point1.z(), (float)point2.z(), (float)point3.z()) * targetScale);
            return vector;
        };
    }

    public static class Targets {
        public static final Target POSITION = ModelPart::offsetPos;
        public static final Target ROTATION = ModelPart::offsetRotation;
        public static final Target SCALE = ModelPart::offsetScale;
    }

    public static interface Interpolation {
        public Vector3f apply(Vector3f var1, float var2, Keyframe[] var3, int var4, int var5, float var6);
    }
}

