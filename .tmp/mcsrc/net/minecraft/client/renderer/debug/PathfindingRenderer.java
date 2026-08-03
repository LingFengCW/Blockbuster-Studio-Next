/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.gizmos.TextGizmo$Style
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.Mth
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.level.pathfinder.Path$DebugData
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import java.util.Locale;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PathfindingRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final float MAX_RENDER_DIST = 80.0f;
    private static final int MAX_TARGETING_DIST = 8;
    private static final boolean SHOW_ONLY_SELECTED = false;
    private static final boolean SHOW_OPEN_CLOSED = true;
    private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
    private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
    private static final boolean SHOW_GROUND_LABELS = true;
    private static final float TEXT_SCALE = 0.32f;

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        debugValues.forEachEntity(DebugSubscriptions.ENTITY_PATHS, (entity, info) -> PathfindingRenderer.renderPath(camX, camY, camZ, info.path(), info.maxNodeDistance()));
    }

    private static void renderPath(double camX, double camY, double camZ, Path path, float maxNodeDistance) {
        PathfindingRenderer.renderPath(path, maxNodeDistance, true, true, camX, camY, camZ);
    }

    public static void renderPath(Path path, float maxNodeDistance, boolean renderOpenAndClosedSets, boolean renderGroundLabels, double camX, double camY, double camZ) {
        PathfindingRenderer.renderPathLine(path, camX, camY, camZ);
        BlockPos pos = path.getTarget();
        if (PathfindingRenderer.distanceToCamera(pos, camX, camY, camZ) <= 80.0f) {
            Gizmos.cuboid((AABB)new AABB((double)((float)pos.getX() + 0.25f), (double)((float)pos.getY() + 0.25f), (double)pos.getZ() + 0.25, (double)((float)pos.getX() + 0.75f), (double)((float)pos.getY() + 0.75f), (double)((float)pos.getZ() + 0.75f)), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.5f, (float)0.0f, (float)1.0f, (float)0.0f)));
            for (int i = 0; i < path.getNodeCount(); ++i) {
                Node n = path.getNode(i);
                if (!(PathfindingRenderer.distanceToCamera(n.asBlockPos(), camX, camY, camZ) <= 80.0f)) continue;
                float r = i == path.getNextNodeIndex() ? 1.0f : 0.0f;
                float b = i == path.getNextNodeIndex() ? 0.0f : 1.0f;
                AABB aabb = new AABB((double)((float)n.x + 0.5f - maxNodeDistance), (double)((float)n.y + 0.01f * (float)i), (double)((float)n.z + 0.5f - maxNodeDistance), (double)((float)n.x + 0.5f + maxNodeDistance), (double)((float)n.y + 0.25f + 0.01f * (float)i), (double)((float)n.z + 0.5f + maxNodeDistance));
                Gizmos.cuboid((AABB)aabb, (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.5f, (float)r, (float)0.0f, (float)b)));
            }
        }
        Path.DebugData debugData = path.debugData();
        if (renderOpenAndClosedSets && debugData != null) {
            for (Node node : debugData.closedSet()) {
                if (!(PathfindingRenderer.distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= 80.0f)) continue;
                Gizmos.cuboid((AABB)new AABB((double)((float)node.x + 0.5f - maxNodeDistance / 2.0f), (double)((float)node.y + 0.01f), (double)((float)node.z + 0.5f - maxNodeDistance / 2.0f), (double)((float)node.x + 0.5f + maxNodeDistance / 2.0f), (double)node.y + 0.1, (double)((float)node.z + 0.5f + maxNodeDistance / 2.0f)), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.5f, (float)1.0f, (float)0.8f, (float)0.8f)));
            }
            for (Node node : debugData.openSet()) {
                if (!(PathfindingRenderer.distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= 80.0f)) continue;
                Gizmos.cuboid((AABB)new AABB((double)((float)node.x + 0.5f - maxNodeDistance / 2.0f), (double)((float)node.y + 0.01f), (double)((float)node.z + 0.5f - maxNodeDistance / 2.0f), (double)((float)node.x + 0.5f + maxNodeDistance / 2.0f), (double)node.y + 0.1, (double)((float)node.z + 0.5f + maxNodeDistance / 2.0f)), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.5f, (float)0.8f, (float)1.0f, (float)1.0f)));
            }
        }
        if (renderGroundLabels) {
            for (int i = 0; i < path.getNodeCount(); ++i) {
                Node n = path.getNode(i);
                if (!(PathfindingRenderer.distanceToCamera(n.asBlockPos(), camX, camY, camZ) <= 80.0f)) continue;
                Gizmos.billboardText((String)String.valueOf(n.type), (Vec3)new Vec3((double)n.x + 0.5, (double)n.y + 0.75, (double)n.z + 0.5), (TextGizmo.Style)TextGizmo.Style.whiteAndCentered().withScale(0.32f)).setAlwaysOnTop();
                Gizmos.billboardText((String)String.format(Locale.ROOT, "%.2f", Float.valueOf(n.costMalus)), (Vec3)new Vec3((double)n.x + 0.5, (double)n.y + 0.25, (double)n.z + 0.5), (TextGizmo.Style)TextGizmo.Style.whiteAndCentered().withScale(0.32f)).setAlwaysOnTop();
            }
        }
    }

    public static void renderPathLine(Path path, double camX, double camY, double camZ) {
        if (path.getNodeCount() < 2) {
            return;
        }
        Vec3 last = path.getNode(0).asVec3();
        for (int i = 1; i < path.getNodeCount(); ++i) {
            Node n = path.getNode(i);
            if (PathfindingRenderer.distanceToCamera(n.asBlockPos(), camX, camY, camZ) > 80.0f) {
                last = n.asVec3();
                continue;
            }
            float hue = (float)i / (float)path.getNodeCount() * 0.33f;
            int color = ARGB.opaque((int)Mth.hsvToRgb((float)hue, (float)0.9f, (float)0.9f));
            Gizmos.arrow((Vec3)last.add(0.5, 0.5, 0.5), (Vec3)n.asVec3().add(0.5, 0.5, 0.5), (int)color);
            last = n.asVec3();
        }
    }

    private static float distanceToCamera(BlockPos n, double camX, double camY, double camZ) {
        return (float)(Math.abs((double)n.getX() - camX) + Math.abs((double)n.getY() - camY) + Math.abs((double)n.getZ() - camZ));
    }
}

