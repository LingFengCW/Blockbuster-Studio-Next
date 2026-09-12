package mchorse.bbs_mod.mixin.client;

import lingfeng.bbsnext.mcef.EditorBridge;
import lingfeng.bbsnext.mcef.MCEFUI;
import mchorse.bbs_mod.BBSMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Task (用户明确方案): 编辑器拥有预览世界时——
 *   1) 把原生 {@link LoadingOverlay} 收集到的绘制（"正在转变资源/正在加载"文字+进度条）丢弃（reset），
 *      即「渲染加载动画的那段删掉」；
 *   2) 立刻把 MCEF 浏览器画在最上层，让编辑器一直挂在上面、全程可见；
 *   3) 不取消 extractRenderState（否则世界卡死），只删「原生渲染那部分」+ 画浏览器；
 *   4) 不换场景、屏面交换对玩家不可见（浏览器始终覆盖）。
 *
 * <p>原生绘制先 reset 掉，再画浏览器——所以不是「用浏览器去盖一个还在闪的原生屏」，
 * 而是原生屏压根没画出来、浏览器直接接管整个画面。方法本体（驱动世界加载收尾的逻辑）
 * 照常执行，世界不会卡死。
 *
 * <p>Guarded by {@link EditorBridge#isBrowserOverlayActive()}：正常单人进世界（编辑器不拥有）
 * 完全不动，原生 LoadingOverlay 照常显示。
 *
 * <p>独立配置 {@code required: true}：织入失败直接报错指明本 mixin，而非静默跳过。
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayRenderMixin
{
    /* One-shot flag so the log shows, on first invocation, that this mixin was
     * actually woven into LoadingOverlay.extractRenderState at runtime. If the
     * log never contains this line while the editor owns a preview world,
     * the mixin silently failed to apply and the vanilla overlay would show. */
    private static boolean bbs$loggedInjected = false;
    private static boolean bbs$loggedGuardFalse = false;

    /* 静态初始化日志：只要 Fabric 解析到本 mixin 配置并把类加载进来，就会打印。
     * 配合 TAIL 里的 woven marker，可一次性区分：
     *   - 本行出现 + marker 出现  => 配置已加载且织入成功（修复生效）
     *   - 本行出现 + 无 marker    => 配置加载了，但 extractRenderState 在本次进世界没被调用（hook 点错）
     *   - 本行不出现              => 配置根本没被 Fabric 读取 */
    static
    {
        try
        {
            BBSMod.LOGGER.info("[EditorBridge] LoadingOverlayRenderMixin CLASS INIT (config parsed & class loaded)");
        }
        catch (Throwable t)
        {
            System.out.println("[EditorBridge] LoadingOverlayRenderMixin CLASS INIT (fallback println)");
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void bbs$discardNativeRender(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        if (!EditorBridge.isBrowserOverlayActive())
        {
            if (!bbs$loggedGuardFalse)
            {
                bbs$loggedGuardFalse = true;
                boolean ready = MCEFUI.isReady();
                BBSMod.LOGGER.warn("[EditorBridge] LoadingOverlay mixin: guard false during overlay render (browserReady=" + ready + "; true=>owningPreviewWorld not set, false=>browser null)");
            }
            return;
        }

        if (!bbs$loggedInjected)
        {
            bbs$loggedInjected = true;
            BBSMod.LOGGER.info("[EditorBridge] LoadingOverlayRenderMixin woven into LoadingOverlay.extractRenderState (native render discarded + editor browser kept on top)");
        }

        /* 1) 26.2 的 GuiRenderState 清屏方法是 reset()（无 clear()）。
         *    先丢弃本次收集到的原生绘制（"正在转变资源"/"正在加载" 文字与进度条），
         *    让原生加载动画压根不画出来。方法本体（驱动世界加载收尾的逻辑）照常执行，世界不会卡死。 */
        extractor.guiRenderState.reset();

        /* 2) 立刻把 MCEF 浏览器画在最上层——编辑器全程挂着、可见，屏面交换对玩家不可见。
         *    因为原生绘制已被 reset，这不是「盖住一个还在闪的原生屏」，而是原生屏没画 + 浏览器直接接管。 */
        int w = extractor.guiWidth();
        int h = extractor.guiHeight();
        MCEFUI.renderBrowserOnTop(extractor, w, h);
    }
}
