package mchorse.bbs_mod.mixin.client;

import lingfeng.bbsnext.mcef.EditorBridge;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Task #19 / HUD 强移游玩区.
 *
 * MC 26.2 把整层 vanilla HUD 的绘制收敛到 {@link Hud#extractRenderState(GuiGraphicsExtractor, DeltaTracker)}
 * 这一个漏斗，所有 hotbar/health/crosshair/chat/boss/title/vignette 等元素都经
 * {@link GuiGraphicsExtractor} 的 pose 矩阵落点。因此只要在调用 HUD 抽取之前对
 * extractor.pose() 施加 translate+scale，就能把整层 HUD 从「铺满整窗」变换进编辑器
 * 中央游玩区矩形，无需逐元素改。
 *
 * 仅当工具栏模式（EditorBridge.isHudRelocationActive()）激活时生效，正常游戏 HUD 完全不受影响。
 * 变换用 pushMatrix 入栈、方法返回前 popMatrix 出栈，与 HUD 内部自身的 push/pop 互不干扰。
 */
@Mixin(Hud.class)
public abstract class HudRelocateMixin
{
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void bbs$relocateHead(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci)
    {
        if (!EditorBridge.isHudRelocationActive()) return;

        float[] r = EditorBridge.getHudRect();
        int gw = extractor.guiWidth();
        int gh = extractor.guiHeight();

        /* 把整屏 HUD（0..gw, 0..gh）映射进游玩区矩形 {l,t,w,h}（占窗口比例）。 */
        float sx = r[2];
        float sy = r[3];
        float tx = r[0] * gw;
        float ty = r[1] * gh;

        Matrix3x2fStack pose = extractor.pose();
        pose.pushMatrix();
        pose.translate(tx, ty);
        pose.scale(sx, sy);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void bbs$relocateTail(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci)
    {
        if (!EditorBridge.isHudRelocationActive()) return;
        extractor.pose().popMatrix();
    }
}
