/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 */
package net.minecraft.client.renderer.feature.submit;

import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public interface TranslucentSubmit
extends SubmitNode {
    public float distanceToCameraSq();

    public FeatureRendererType<? extends TranslucentSubmit> featureType();

    public static float computeDistanceToCameraSq(Matrix4fc pose) {
        return Mth.lengthSquared((float)pose.m30(), (float)pose.m31(), (float)pose.m32());
    }

    public static float computeDistanceToCameraSq(Matrix4fc pose, float originX, float originY, float originZ) {
        return pose.transformPosition(originX, originY, originZ, new Vector3f()).lengthSquared();
    }
}

