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
 * Task: keep the MCEF editor browser painted on top of the vanilla
 * {@link LoadingOverlay} while the editor is entering (or switching into) its
 * preview world.
 *
 * <p>The editor browser is normally composited only from
 * {@code UIScreen.extractRenderState} (the dashboard screen). During
 * {@code openWorld(...)} MC swaps that screen away and shows the native
 * LoadingOverlay, leaving the browser un-drawn for the load duration - which is
 * exactly the window where the user sees the vanilla spinner "push the editor
 * off". We draw the live browser texture again here, on top of the overlay, so
 * the editor (and its own in-page loading spinner) stays visible the whole time.
 *
 * <p>We deliberately do NOT cancel the overlay's own render: the overlay drives
 * world-load finalisation, and cancelling it freezes the world (project iron
 * rule). Drawing on top is safe.
 *
 * <p>Guarded by {@link EditorBridge#isBrowserOverlayActive()} so a normal
 * single-player world join (which the editor does not own) is completely
 * untouched - its native LoadingOverlay shows as usual.
 *
 * <p>Registered as {@code required: true} in its own config
 * ({@code bbs.loadingoverlay.mixins.json}) so a weaving failure fails loudly
 * (naming this mixin) instead of silently skipping and leaving the browser
 * swallowed with no error. The native LoadingOverlay is only suppressed while
 * the editor owns a preview world (guarded by
 * {@link EditorBridge#isBrowserOverlayActive()}).
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayRenderMixin
{
    /* One-shot flag so the log shows, on first invocation, that this mixin was
     * actually woven into LoadingOverlay.extractRenderState at runtime. If the
     * log never contains this line while the editor owns a preview world,
     * the mixin silently failed to apply (e.g. refmap drift) and the browser
     * would be "swallowed" by the vanilla loading screen with no error - this
     * marker makes that failure observable. */
    private static boolean bbs$loggedInjected = false;
    private static boolean bbs$loggedGuardFalse = false;
    private static boolean bbs$loggedViewNull = false;

    /* 静态初始化日志：只要 Fabric 解析到本 mixin 配置并把类加载进来，就会打印。
     * 配合 TAIL 里的 woven marker，可一次性区分：
     *   - 本行出现 + marker 出现  => 配置已加载且织入成功（修复生效）
     *   - 本行出现 + 无 marker    => 配置加载了，但 extractRenderState 在本次进世界没被调用（hook 点错）
     *   - 本行不出现              => 配置根本没被 Fabric 读取（required:false 静默跳过已改为 true） */
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
    private void bbs$drawBrowserOnTop(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
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
            BBSMod.LOGGER.info("[EditorBridge] LoadingOverlayRenderMixin woven into LoadingOverlay.extractRenderState (native render discarded, browser shown instead)");
        }

        /* 先确认浏览器当前有可用纹理帧，再丢弃原生渲染、只画浏览器。
         * 否则若浏览器在 loading 期间暂停（getTextureView() 返回 null），
         * reset() 清空原生后又没东西可画会留空白屏——此时保留原生更稳妥。
         * 注：26.2 的 GuiRenderState 清屏方法是 reset()（无 clear()）。 */
        if (MCEFUI.getTextureView() == null)
        {
            if (!bbs$loggedViewNull)
            {
                bbs$loggedViewNull = true;
                BBSMod.LOGGER.warn("[EditorBridge] LoadingOverlay mixin: isBrowserOverlayActive() true but getTextureView() null (browser paused during load?)");
            }
            return;
        }

        extractor.guiRenderState.reset();

        int w = extractor.guiWidth();
        int h = extractor.guiHeight();

        MCEFUI.renderBrowserOnTop(extractor, w, h);
    }
}
