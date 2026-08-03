/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.gizmos.TextGizmo$Style
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NeighborsUpdateRenderer
implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        LastUpdate lastUpdate;
        BlockPos pos;
        int shrinkTime = DebugSubscriptions.NEIGHBOR_UPDATES.expireAfterTicks();
        double shrinkSpeed = 1.0 / (double)(shrinkTime * 2);
        HashMap lastUpdates = new HashMap();
        debugValues.forEachEvent(DebugSubscriptions.NEIGHBOR_UPDATES, (blockPos, remainingTicks, totalLifetime) -> {
            long age = totalLifetime - remainingTicks;
            LastUpdate lastUpdate = lastUpdates.getOrDefault(blockPos, LastUpdate.NONE);
            lastUpdates.put(blockPos, lastUpdate.tryCount((int)age));
        });
        for (Map.Entry entry : lastUpdates.entrySet()) {
            pos = (BlockPos)entry.getKey();
            lastUpdate = (LastUpdate)entry.getValue();
            AABB aabb = new AABB(pos).inflate(0.002).deflate(shrinkSpeed * (double)lastUpdate.age);
            Gizmos.cuboid((AABB)aabb, (GizmoStyle)GizmoStyle.stroke((int)-1));
        }
        for (Map.Entry entry : lastUpdates.entrySet()) {
            pos = (BlockPos)entry.getKey();
            lastUpdate = (LastUpdate)entry.getValue();
            Gizmos.billboardText((String)String.valueOf(lastUpdate.count), (Vec3)Vec3.atCenterOf((Vec3i)pos), (TextGizmo.Style)TextGizmo.Style.whiteAndCentered());
        }
    }

    private record LastUpdate(int count, int age) {
        private static final LastUpdate NONE = new LastUpdate(0, Integer.MAX_VALUE);

        public LastUpdate tryCount(int age) {
            if (age == this.age) {
                return new LastUpdate(this.count + 1, age);
            }
            if (age < this.age) {
                return new LastUpdate(1, age);
            }
            return this;
        }
    }
}

