/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.gizmos.TextGizmo$Style
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GameEventListenerRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final float BOX_HEIGHT = 1.0f;

    private void forEachListener(DebugValueAccess debugValues, ListenerVisitor visitor) {
        debugValues.forEachBlock(DebugSubscriptions.GAME_EVENT_LISTENERS, (blockPos, listener) -> visitor.accept(Vec3.atCenterOf((Vec3i)blockPos), listener.listenerRadius()));
        debugValues.forEachEntity(DebugSubscriptions.GAME_EVENT_LISTENERS, (entity, listener) -> visitor.accept(entity.position(), listener.listenerRadius()));
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        this.forEachListener(debugValues, (origin, radius) -> {
            double size = (double)radius * 2.0;
            Gizmos.cuboid((AABB)AABB.ofSize((Vec3)origin, (double)size, (double)size, (double)size), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.35f, (float)1.0f, (float)1.0f, (float)0.0f)));
        });
        this.forEachListener(debugValues, (origin, radius) -> Gizmos.cuboid((AABB)AABB.ofSize((Vec3)origin, (double)0.5, (double)1.0, (double)0.5).move(0.0, 0.5, 0.0), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.35f, (float)1.0f, (float)1.0f, (float)0.0f))));
        this.forEachListener(debugValues, (origin, radius) -> {
            Gizmos.billboardText((String)"Listener Origin", (Vec3)origin.add(0.0, 1.8, 0.0), (TextGizmo.Style)TextGizmo.Style.whiteAndCentered().withScale(0.4f));
            Gizmos.billboardText((String)BlockPos.containing((Position)origin).toString(), (Vec3)origin.add(0.0, 1.5, 0.0), (TextGizmo.Style)TextGizmo.Style.forColorAndCentered((int)-6959665).withScale(0.4f));
        });
        debugValues.forEachEvent(DebugSubscriptions.GAME_EVENTS, (event, remainingTicks, totalLifetime) -> {
            Vec3 origin = event.pos();
            double size = 0.4;
            AABB box = AABB.ofSize((Vec3)origin.add(0.0, 0.5, 0.0), (double)0.4, (double)0.9, (double)0.4);
            Gizmos.cuboid((AABB)box, (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.2f, (float)1.0f, (float)1.0f, (float)1.0f)));
            Gizmos.billboardText((String)event.event().getRegisteredName(), (Vec3)origin.add(0.0, 0.85, 0.0), (TextGizmo.Style)TextGizmo.Style.forColorAndCentered((int)-7564911).withScale(0.12f));
        });
    }

    @FunctionalInterface
    private static interface ListenerVisitor {
        public void accept(Vec3 var1, int var2);
    }
}

