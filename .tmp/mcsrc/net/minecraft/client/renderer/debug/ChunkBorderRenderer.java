/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChunkBorderRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final float THICK_WIDTH = 4.0f;
    private static final float THIN_WIDTH = 1.0f;
    private final Minecraft minecraft;
    private static final int CELL_BORDER = ARGB.color((int)255, (int)0, (int)155, (int)155);
    private static final int YELLOW = ARGB.color((int)255, (int)255, (int)255, (int)0);
    private static final int MAJOR_LINES = ARGB.colorFromFloat((float)1.0f, (float)0.25f, (float)0.25f, (float)1.0f);

    public ChunkBorderRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        int y;
        int color;
        int x;
        Entity cameraEntity = this.minecraft.getCameraEntity();
        float ymin = this.minecraft.level.getMinY();
        float ymax = this.minecraft.level.getMaxY() + 1;
        SectionPos cameraPos = SectionPos.of((BlockPos)cameraEntity.blockPosition());
        double xstart = cameraPos.minBlockX();
        double zstart = cameraPos.minBlockZ();
        for (x = -16; x <= 32; x += 16) {
            for (int z = -16; z <= 32; z += 16) {
                Gizmos.line((Vec3)new Vec3(xstart + (double)x, (double)ymin, zstart + (double)z), (Vec3)new Vec3(xstart + (double)x, (double)ymax, zstart + (double)z), (int)ARGB.colorFromFloat((float)0.5f, (float)1.0f, (float)0.0f, (float)0.0f), (float)4.0f);
            }
        }
        for (x = 2; x < 16; x += 2) {
            color = x % 4 == 0 ? CELL_BORDER : YELLOW;
            Gizmos.line((Vec3)new Vec3(xstart + (double)x, (double)ymin, zstart), (Vec3)new Vec3(xstart + (double)x, (double)ymax, zstart), (int)color, (float)1.0f);
            Gizmos.line((Vec3)new Vec3(xstart + (double)x, (double)ymin, zstart + 16.0), (Vec3)new Vec3(xstart + (double)x, (double)ymax, zstart + 16.0), (int)color, (float)1.0f);
        }
        for (int z = 2; z < 16; z += 2) {
            color = z % 4 == 0 ? CELL_BORDER : YELLOW;
            Gizmos.line((Vec3)new Vec3(xstart, (double)ymin, zstart + (double)z), (Vec3)new Vec3(xstart, (double)ymax, zstart + (double)z), (int)color, (float)1.0f);
            Gizmos.line((Vec3)new Vec3(xstart + 16.0, (double)ymin, zstart + (double)z), (Vec3)new Vec3(xstart + 16.0, (double)ymax, zstart + (double)z), (int)color, (float)1.0f);
        }
        for (y = this.minecraft.level.getMinY(); y <= this.minecraft.level.getMaxY() + 1; y += 2) {
            float yline = y;
            int color2 = y % 8 == 0 ? CELL_BORDER : YELLOW;
            Gizmos.line((Vec3)new Vec3(xstart, (double)yline, zstart), (Vec3)new Vec3(xstart, (double)yline, zstart + 16.0), (int)color2, (float)1.0f);
            Gizmos.line((Vec3)new Vec3(xstart, (double)yline, zstart + 16.0), (Vec3)new Vec3(xstart + 16.0, (double)yline, zstart + 16.0), (int)color2, (float)1.0f);
            Gizmos.line((Vec3)new Vec3(xstart + 16.0, (double)yline, zstart + 16.0), (Vec3)new Vec3(xstart + 16.0, (double)yline, zstart), (int)color2, (float)1.0f);
            Gizmos.line((Vec3)new Vec3(xstart + 16.0, (double)yline, zstart), (Vec3)new Vec3(xstart, (double)yline, zstart), (int)color2, (float)1.0f);
        }
        for (x = 0; x <= 16; x += 16) {
            for (int z = 0; z <= 16; z += 16) {
                Gizmos.line((Vec3)new Vec3(xstart + (double)x, (double)ymin, zstart + (double)z), (Vec3)new Vec3(xstart + (double)x, (double)ymax, zstart + (double)z), (int)MAJOR_LINES, (float)4.0f);
            }
        }
        Gizmos.cuboid((AABB)new AABB((double)cameraPos.minBlockX(), (double)cameraPos.minBlockY(), (double)cameraPos.minBlockZ(), (double)(cameraPos.maxBlockX() + 1), (double)(cameraPos.maxBlockY() + 1), (double)(cameraPos.maxBlockZ() + 1)), (GizmoStyle)GizmoStyle.stroke((int)MAJOR_LINES, (float)1.0f)).setAlwaysOnTop();
        for (y = this.minecraft.level.getMinY(); y <= this.minecraft.level.getMaxY() + 1; y += 16) {
            Gizmos.line((Vec3)new Vec3(xstart, (double)y, zstart), (Vec3)new Vec3(xstart, (double)y, zstart + 16.0), (int)MAJOR_LINES, (float)4.0f);
            Gizmos.line((Vec3)new Vec3(xstart, (double)y, zstart + 16.0), (Vec3)new Vec3(xstart + 16.0, (double)y, zstart + 16.0), (int)MAJOR_LINES, (float)4.0f);
            Gizmos.line((Vec3)new Vec3(xstart + 16.0, (double)y, zstart + 16.0), (Vec3)new Vec3(xstart + 16.0, (double)y, zstart), (int)MAJOR_LINES, (float)4.0f);
            Gizmos.line((Vec3)new Vec3(xstart + 16.0, (double)y, zstart), (Vec3)new Vec3(xstart, (double)y, zstart), (int)MAJOR_LINES, (float)4.0f);
        }
    }
}

