/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;

public class RedstoneWireOrientationsRenderer
implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        debugValues.forEachBlock(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, (wirePos, orientation) -> {
            Vec3 center = Vec3.atBottomCenterOf((Vec3i)wirePos).subtract(0.0, 0.1, 0.0);
            Gizmos.arrow((Vec3)center, (Vec3)center.add(orientation.getFront().getUnitVec3().scale(0.5)), (int)-16776961);
            Gizmos.arrow((Vec3)center, (Vec3)center.add(orientation.getUp().getUnitVec3().scale(0.4)), (int)-65536);
            Gizmos.arrow((Vec3)center, (Vec3)center.add(orientation.getSide().getUnitVec3().scale(0.3)), (int)-256);
        });
    }
}

