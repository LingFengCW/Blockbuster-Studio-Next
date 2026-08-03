/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.IndexType;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface SectionMesh
extends AutoCloseable {
    default public boolean isDifferentPointOfView(TranslucencyPointOfView pointOfView) {
        return false;
    }

    default public boolean hasRenderableLayers() {
        return false;
    }

    default public boolean hasTranslucentGeometry() {
        return false;
    }

    default public boolean isEmpty(ChunkSectionLayer layer) {
        return true;
    }

    default public List<BlockEntity> getRenderableBlockEntities() {
        return Collections.emptyList();
    }

    public boolean facesCanSeeEachother(Direction var1, Direction var2);

    default public @Nullable SectionDraw getSectionDraw(ChunkSectionLayer layer) {
        return null;
    }

    @Override
    default public void close() {
    }

    public record SectionDraw(int indexCount, IndexType indexType, boolean hasCustomIndexBuffer) {
    }
}

