/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BreezeDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final int JUMP_TARGET_LINE_COLOR = ARGB.color((int)255, (int)255, (int)100, (int)255);
    private static final int TARGET_LINE_COLOR = ARGB.color((int)255, (int)100, (int)255, (int)255);
    private static final int INNER_CIRCLE_COLOR = ARGB.color((int)255, (int)0, (int)255, (int)0);
    private static final int MIDDLE_CIRCLE_COLOR = ARGB.color((int)255, (int)255, (int)165, (int)0);
    private static final int OUTER_CIRCLE_COLOR = ARGB.color((int)255, (int)255, (int)0, (int)0);
    private final Minecraft minecraft;

    public BreezeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        ClientLevel level = this.minecraft.level;
        debugValues.forEachEntity(DebugSubscriptions.BREEZES, (entity, info) -> {
            info.attackTarget().map(level::getEntity).map(targetEntity -> targetEntity.getPosition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true))).ifPresent(attackTargetPosition -> {
                Gizmos.arrow((Vec3)entity.position(), (Vec3)attackTargetPosition, (int)TARGET_LINE_COLOR);
                Vec3 drawCenter = attackTargetPosition.add(0.0, (double)0.01f, 0.0);
                Gizmos.circle((Vec3)drawCenter, (float)4.0f, (GizmoStyle)GizmoStyle.stroke((int)INNER_CIRCLE_COLOR));
                Gizmos.circle((Vec3)drawCenter, (float)8.0f, (GizmoStyle)GizmoStyle.stroke((int)MIDDLE_CIRCLE_COLOR));
                Gizmos.circle((Vec3)drawCenter, (float)24.0f, (GizmoStyle)GizmoStyle.stroke((int)OUTER_CIRCLE_COLOR));
            });
            info.jumpTarget().ifPresent(blockPos -> {
                Gizmos.arrow((Vec3)entity.position(), (Vec3)Vec3.atCenterOf((Vec3i)blockPos), (int)JUMP_TARGET_LINE_COLOR);
                Gizmos.cuboid((AABB)AABB.unitCubeFromLowerCorner((Vec3)Vec3.atLowerCornerOf((Vec3i)blockPos)), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)1.0f, (float)1.0f, (float)0.0f, (float)0.0f)));
            });
        });
    }
}

