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
 * Task: make the vanilla {@link LoadingOverlay} not paint anything while the
 * editor owns its preview world.
 *
 * <p>The editor's browser is composited from {@code UIScreen.extractRenderState}
 * (the dashboard screen). During {@code openWorld(...)} MC swaps that screen away
 * and shows the native LoadingOverlay. The user does NOT want the vanilla
 * "Converting world / Loading" overlay to appear at all - and explicitly does
 * NOT want the browser painted on top of it to hide it ("不能盖"). So the fix is
 * simply: discard the overlay's collected render output (reset the
 * GuiRenderState) when the editor owns the preview world, and draw nothing.
 *
 * <p>We deliberately do NOT cancel the overlay's own render method: the overlay
 * drives world-load finalisation, and cancelling it freezes the world (project
 * iron rule). The method still runs (its tick/finalisation logic lives in the
 * render body, before this TAIL), we only throw away the pixels it collected.
 * This is exactly "delete the rendering part, keep the method".
 *
 * <p>Guarded by {@link EditorBridge#isBrowserOverlayActive()} so a normal
 * single-player world join (which the editor does not own) is completely
 * untouched - its native LoadingOverlay shows as usual.
 *
 * <p>Registered as {@code required: true} in its own config
 * ({@code bbs.loadingoverlay.mixins.json}) so a weaving failure fails loudly
 * (naming this mixin) instead of silently skipping. The native LoadingOverlay is
 * only suppressed while the editor owns a preview world.
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
            BBSMod.LOGGER.info("[EditorBridge] LoadingOverlayRenderMixin woven into LoadingOverlay.extractRenderState (native render discarded, overlay hidden)");
        }

        /* 26.2 的 GuiRenderState 清屏方法是 reset()（无 clear()）。丢弃本次收集到的
         * 原生绘制（"正在转变资源"/"正在加载" 文字与进度条），不画任何东西、也不画浏览器
         * （绝不用浏览器去盖）。方法本体（驱动世界加载收尾的逻辑）照常执行，仅丢弃其像素。 */
        extractor.guiRenderState.reset();
    }
}
