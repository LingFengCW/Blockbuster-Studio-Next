/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.gizmos.TextGizmo$Style
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.debug.DebugSubscriptions
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;

public class RaidDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private static final float TEXT_SCALE = 0.64f;
    private final Minecraft minecraft;

    public RaidDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        BlockPos playerPos = this.getCamera().blockPosition();
        debugValues.forEachChunk(DebugSubscriptions.RAIDS, (chunkPos, raidCenters) -> {
            for (BlockPos raidCenter : raidCenters) {
                if (!playerPos.closerThan((Vec3i)raidCenter, 160.0)) continue;
                RaidDebugRenderer.highlightRaidCenter(raidCenter);
            }
        });
    }

    private static void highlightRaidCenter(BlockPos raidCenter) {
        Gizmos.cuboid((BlockPos)raidCenter, (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.15f, (float)1.0f, (float)0.0f, (float)0.0f)));
        RaidDebugRenderer.renderTextOverBlock("Raid center", raidCenter, -65536);
    }

    private static void renderTextOverBlock(String text, BlockPos pos, int color) {
        Gizmos.billboardText((String)text, (Vec3)Vec3.atLowerCornerWithOffset((Vec3i)pos, (double)0.5, (double)1.3, (double)0.5), (TextGizmo.Style)TextGizmo.Style.forColor((int)color).withScale(0.64f)).setAlwaysOnTop();
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.mainCamera();
    }
}

