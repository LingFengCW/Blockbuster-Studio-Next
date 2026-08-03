/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.gizmos.TextGizmo$Style
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.debug.DebugValueAccess
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.phys.Vec3
 */
package net.minecraft.client.renderer.debug;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class LightDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private final boolean showBlockLight;
    private final boolean showSkyLight;
    private static final int MAX_RENDER_DIST = 10;

    public LightDebugRenderer(Minecraft minecraft, boolean showBlockLight, boolean showSkyLight) {
        this.minecraft = minecraft;
        this.showBlockLight = showBlockLight;
        this.showSkyLight = showSkyLight;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        ClientLevel level = this.minecraft.level;
        BlockPos playerPos = BlockPos.containing((double)camX, (double)camY, (double)camZ);
        LongOpenHashSet set = new LongOpenHashSet();
        for (BlockPos blockPos : BlockPos.betweenClosed((BlockPos)playerPos.offset(-10, -10, -10), (BlockPos)playerPos.offset(10, 10, 10))) {
            int blockBrightness;
            int skyBrightness = level.getBrightness(LightLayer.SKY, blockPos);
            long sectionNode = SectionPos.blockToSection((long)blockPos.asLong());
            if (set.add(sectionNode)) {
                Gizmos.billboardText((String)level.getChunkSource().getLightEngine().getDebugData(LightLayer.SKY, SectionPos.of((long)sectionNode)), (Vec3)new Vec3((double)SectionPos.sectionToBlockCoord((int)SectionPos.x((long)sectionNode), (int)8), (double)SectionPos.sectionToBlockCoord((int)SectionPos.y((long)sectionNode), (int)8), (double)SectionPos.sectionToBlockCoord((int)SectionPos.z((long)sectionNode), (int)8)), (TextGizmo.Style)TextGizmo.Style.forColorAndCentered((int)-65536).withScale(4.8f));
            }
            if (skyBrightness != 15 && this.showSkyLight) {
                int color = ARGB.srgbLerp((float)((float)skyBrightness / 15.0f), (int)-16776961, (int)-16711681);
                Gizmos.billboardText((String)String.valueOf(skyBrightness), (Vec3)Vec3.atLowerCornerWithOffset((Vec3i)blockPos, (double)0.5, (double)0.25, (double)0.5), (TextGizmo.Style)TextGizmo.Style.forColorAndCentered((int)color));
            }
            if (!this.showBlockLight || (blockBrightness = level.getBrightness(LightLayer.BLOCK, blockPos)) == 0) continue;
            int color = ARGB.srgbLerp((float)((float)blockBrightness / 15.0f), (int)-5636096, (int)-256);
            Gizmos.billboardText((String)String.valueOf(level.getBrightness(LightLayer.BLOCK, blockPos)), (Vec3)Vec3.atCenterOf((Vec3i)blockPos), (TextGizmo.Style)TextGizmo.Style.forColorAndCentered((int)color));
        }
    }
}

