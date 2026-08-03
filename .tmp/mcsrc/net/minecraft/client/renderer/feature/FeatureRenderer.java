/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.client.renderer.feature;

import java.util.List;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

public interface FeatureRenderer<Submit extends SubmitNode>
extends AutoCloseable {
    default public void beginPrepare(FeatureFrameContext context) {
    }

    public void prepareGroup(FeatureFrameContext var1, List<Submit> var2, boolean var3);

    default public void finishPrepare(FeatureFrameContext context) {
    }

    public void executeGroup(FeatureFrameContext var1, int var2, List<Submit> var3, boolean var4);

    default public void finishExecute(FeatureFrameContext context) {
    }

    @Override
    default public void close() {
    }
}

