/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.Mth
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector4f
 */
package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

public class ChunkCullingDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;

    public ChunkCullingDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Frustum capturedFrustum;
        LevelRenderer levelRenderer = this.minecraft.levelRenderer;
        boolean sectionPath = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_PATHS);
        boolean sectionVisibility = this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_VISIBILITY);
        if (sectionPath || sectionVisibility) {
            SectionOcclusionGraph sectionOcclusionGraph = levelRenderer.sectionOcclusionGraph();
            for (SectionRenderDispatcher.RenderSection section : levelRenderer.visibleSections()) {
                SectionOcclusionGraph.Node node = sectionOcclusionGraph.getNode(section);
                if (node == null) continue;
                BlockPos renderOffset = section.getRenderOrigin();
                if (sectionPath) {
                    int color = node.step == 0 ? 0 : Mth.hsvToRgb((float)((float)node.step / 50.0f), (float)0.9f, (float)0.9f);
                    for (int i = 0; i < DIRECTIONS.length; ++i) {
                        if (!node.hasSourceDirection(i)) continue;
                        Direction direction = DIRECTIONS[i];
                        Gizmos.line((Vec3)Vec3.atLowerCornerWithOffset((Vec3i)renderOffset, (double)8.0, (double)8.0, (double)8.0), (Vec3)Vec3.atLowerCornerWithOffset((Vec3i)renderOffset, (double)(8 - 16 * direction.getStepX()), (double)(8 - 16 * direction.getStepY()), (double)(8 - 16 * direction.getStepZ())), (int)ARGB.opaque((int)color));
                    }
                }
                if (!sectionVisibility || !section.getSectionMesh().hasRenderableLayers()) continue;
                int c = 0;
                for (Direction direction1 : DIRECTIONS) {
                    for (Direction direction2 : DIRECTIONS) {
                        boolean b = section.getSectionMesh().facesCanSeeEachother(direction1, direction2);
                        if (b) continue;
                        ++c;
                        Gizmos.line((Vec3)Vec3.atLowerCornerWithOffset((Vec3i)renderOffset, (double)(8 + 8 * direction1.getStepX()), (double)(8 + 8 * direction1.getStepY()), (double)(8 + 8 * direction1.getStepZ())), (Vec3)Vec3.atLowerCornerWithOffset((Vec3i)renderOffset, (double)(8 + 8 * direction2.getStepX()), (double)(8 + 8 * direction2.getStepY()), (double)(8 + 8 * direction2.getStepZ())), (int)ARGB.color((int)255, (int)255, (int)0, (int)0));
                    }
                }
                if (c <= 0) continue;
                float delta = 0.5f;
                float a = 0.2f;
                Gizmos.cuboid((AABB)section.getBoundingBox().deflate(0.5), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.2f, (float)0.9f, (float)0.9f, (float)0.0f)));
            }
        }
        if ((capturedFrustum = this.minecraft.gameRenderer.mainCamera().getCapturedFrustum()) != null) {
            Vec3 offset = new Vec3(capturedFrustum.getCamX(), capturedFrustum.getCamY(), capturedFrustum.getCamZ());
            Vector4f[] frustumPoints = capturedFrustum.getFrustumPoints();
            this.addFrustumQuad(offset, frustumPoints, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(offset, frustumPoints, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(offset, frustumPoints, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(offset, frustumPoints, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(offset, frustumPoints, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(offset, frustumPoints, 1, 5, 6, 2, 1, 0, 1);
            this.addFrustumLine(offset, frustumPoints[0], frustumPoints[1]);
            this.addFrustumLine(offset, frustumPoints[1], frustumPoints[2]);
            this.addFrustumLine(offset, frustumPoints[2], frustumPoints[3]);
            this.addFrustumLine(offset, frustumPoints[3], frustumPoints[0]);
            this.addFrustumLine(offset, frustumPoints[4], frustumPoints[5]);
            this.addFrustumLine(offset, frustumPoints[5], frustumPoints[6]);
            this.addFrustumLine(offset, frustumPoints[6], frustumPoints[7]);
            this.addFrustumLine(offset, frustumPoints[7], frustumPoints[4]);
            this.addFrustumLine(offset, frustumPoints[0], frustumPoints[4]);
            this.addFrustumLine(offset, frustumPoints[1], frustumPoints[5]);
            this.addFrustumLine(offset, frustumPoints[2], frustumPoints[6]);
            this.addFrustumLine(offset, frustumPoints[3], frustumPoints[7]);
        }
    }

    private void addFrustumLine(Vec3 offset, Vector4f a, Vector4f b) {
        Gizmos.line((Vec3)new Vec3(offset.x + (double)a.x, offset.y + (double)a.y, offset.z + (double)a.z), (Vec3)new Vec3(offset.x + (double)b.x, offset.y + (double)b.y, offset.z + (double)b.z), (int)-16777216);
    }

    private void addFrustumQuad(Vec3 offset, Vector4f[] frustumPoints, int i0, int i1, int i2, int i3, int r, int g, int b) {
        float a = 0.25f;
        Gizmos.rect((Vec3)new Vec3((double)frustumPoints[i0].x(), (double)frustumPoints[i0].y(), (double)frustumPoints[i0].z()).add(offset), (Vec3)new Vec3((double)frustumPoints[i1].x(), (double)frustumPoints[i1].y(), (double)frustumPoints[i1].z()).add(offset), (Vec3)new Vec3((double)frustumPoints[i2].x(), (double)frustumPoints[i2].y(), (double)frustumPoints[i2].z()).add(offset), (Vec3)new Vec3((double)frustumPoints[i3].x(), (double)frustumPoints[i3].y(), (double)frustumPoints[i3].z()).add(offset), (GizmoStyle)GizmoStyle.fill((int)ARGB.colorFromFloat((float)0.25f, (float)r, (float)g, (float)b)));
    }
}

