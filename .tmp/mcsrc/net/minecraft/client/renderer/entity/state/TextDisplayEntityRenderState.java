/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import org.jspecify.annotations.Nullable;

public class TextDisplayEntityRenderState
extends DisplayEntityRenderState {
    public // Could not load outer class - annotation placement on inner may be incorrect
     @Nullable Display.TextDisplay.TextRenderState textRenderState;
    public // Could not load outer class - annotation placement on inner may be incorrect
     @Nullable Display.TextDisplay.CachedInfo cachedInfo;

    @Override
    public boolean hasSubState() {
        return this.textRenderState != null;
    }
}

