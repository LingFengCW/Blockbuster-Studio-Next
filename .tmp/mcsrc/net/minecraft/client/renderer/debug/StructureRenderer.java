/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.debug.DebugStructureInfo
 *  net.minecraft.util.debug.DebugStructureInfo$Piece
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 *  net.minecraft.world.phys.AABB
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class StructureRenderer
implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        debugValues.forEachChunk(DebugSubscriptions.STRUCTURES, (chunkPos, structures) -> {
            for (DebugStructureInfo structure : structures) {
                Gizmos.cuboid((AABB)AABB.of((BoundingBox)structure.boundingBox()), (GizmoStyle)GizmoStyle.stroke((int)ARGB.colorFromFloat((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f)));
                for (DebugStructureInfo.Piece piece : structure.pieces()) {
                    if (piece.isStart()) {
                        Gizmos.cuboid((AABB)AABB.of((BoundingBox)piece.boundingBox()), (GizmoStyle)GizmoStyle.stroke((int)ARGB.colorFromFloat((float)1.0f, (float)0.0f, (float)1.0f, (float)0.0f)));
                        continue;
                    }
                    Gizmos.cuboid((AABB)AABB.of((BoundingBox)piece.boundingBox()), (GizmoStyle)GizmoStyle.stroke((int)ARGB.colorFromFloat((float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f)));
                }
            }
        });
    }
}

