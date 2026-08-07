package lingfeng.bbsnext.mcef;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * MCEF (Minecraft Chromium Embedded Framework) HTML editor integration.
 *
 * Replaces the Ultralight path: MCEF ships a maintained Chromium renderer
 * with a working GPU driver for MC 26.2, so the HTML editor no longer
 * crashes in native code.
 *
 * Architecture:
 *   - The browser is created lazily when the editor opens
 *     ({@link #createBrowser(int, int, EditorBridge)}).
 *   - Events flow in from {@link UIScreen} (which already receives the
 *     native MC 26.2 MouseButtonEvent/KeyEvent/CharacterEvent objects).
 *   - Rendering: CEF's onPaint pixels (BGRA) are captured and uploaded to a
 *     vanilla DynamicTexture, then blitted into the GUI via
 *     GuiGraphics.blit() on MC 26.2 (no GL texture readback).
 *   - Java -> JS: executeJavaScript pushes the editor state as
 *     `window.bbsState`.
 *   - JS -> Java: CEF JSQuery (`window.javaQuery`) routes actions to
 *     {@link EditorBridge}.
 */
public class MCEFUI
{
    private static MCEFApi api;
    private static MCEFBrowser browser;
    private static EditorBridge bridge;
    private static boolean initialized;

    /* The browser frame is drawn via MCEF's own native GpuTexture, exposed
     * through a vanilla AbstractTexture wrapper registered in the
     * TextureManager. MCEF keeps that texture updated every paint, so we just
     * blit it each frame - no GL readback, no pixel copying. */
    private static AbstractTexture browserTex;
    private static Identifier browserTexId;

    /* Browser viewport offset on screen, in GUI (scaled) units. The overlay
     * is positioned at (area.x, area.y); the browser's internal (0,0) maps to
     * that origin, so input must be made relative before scaling. Without
     * subtracting this offset, clicks land shifted by the panel's top/left
     * margin (visibly broken on Y when a toolbar sits above the editor). */
    private static double browserOffsetX;
    private static double browserOffsetY;

    /* Last known mouse position (screen GUI units), refreshed on every move.
     * Click/release reuse it so the three event sources can never disagree. */
    private static double lastMouseX;
    private static double lastMouseY;

    /* -------- lifecycle -------- */

    /** Start MCEF initialization (async). Safe to call multiple times. */
    public static void initialize()
    {
        if (initialized || api != null)
        {
            return;
        }

        try
        {
            MCEFApi.initialize();
            initialized = true;

            /* Non-blocking: complete asynchronously in the background. */
            MCEFApi.getInstanceFuture().thenAccept(a ->
            {
                api = a;
                BBSMod.LOGGER.info("[MCEF] Browser engine ready");
            }).exceptionally(e ->
            {
                BBSMod.LOGGER.error("[MCEF] Initialization failed", e);

                return null;
            });
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] initialize failed", t);
            initialized = false;
        }
    }

    /**
     * Non-blocking API accessor. Returns null while the CEF engine is still
     * initializing (Chromium binaries may be downloading on a background
     * thread). Never join() on the render thread - that freezes the game.
     */
    private static MCEFApi getApi()
    {
        if (api == null)
        {
            MCEFApi.Initialization init = MCEFApi.initialize();

            if (init.isDone())
            {
                try
                {
                    api = MCEFApi.getInstance();
                }
                catch (Throwable t)
                {
                    BBSMod.LOGGER.error("[MCEF] getInstance failed", t);
                }
            }
        }

        return api;
    }

    /** Create (or resize) the editor browser. Returns false while the CEF
     *  engine is not ready yet (caller should retry next frame).
     *  {@code width}/{@code height} are GUI (scaled) units; the browser
     *  viewport is sized in physical pixels so the texture stays sharp. */
    public static boolean createBrowser(int width, int height, EditorBridge jsBridge)
    {
        MCEFApi mcef = getApi();

        if (mcef == null)
        {
            return false;
        }

        if (browser != null)
        {
            bridge = jsBridge;
            browser.resize(scale(width), scale(height));

            return true;
        }

        bridge = jsBridge;

        try
        {
            browser = mcef.createBrowser(editorPageDataUrl(), true);
            browser.resize(scale(width), scale(height));
            browser.setFocus(true);

            installJsQuery(browser.getCefBrowser());

            /* Register MCEF's native browser texture as a vanilla texture so
             * it can be drawn through GuiGraphics.blit(Identifier, ...). MCEF
             * uploads each paint frame into its own GpuTexture; we only expose
             * that view here. MCEF's onPaint path does NOT call the
             * CustomCefBrowserOsr paint listeners, so capturing pixels from a
             * listener is futile - we use the texture MCEF already maintains. */
            if (browserTex == null)
            {
                browserTex = new MCEFTexture(browser);
                browserTexId = Identifier.fromNamespaceAndPath("bbs_mod", "mcefbrowser");
                Minecraft.getInstance().getTextureManager().register(browserTexId, browserTex);
            }

            /* No delayed reload: the console channel (BBS_ACTION:) is the
             * primary JS->Java path and does not depend on navigation-time
             * bridge injection, so the editor paints instantly. The JSQuery
             * router is installed synchronously above, before CEF processes
             * the data: URL navigation, so the fallback also works. */

            BBSMod.LOGGER.info("[MCEF] Editor browser created ({}x{})", width, height);

            return true;
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] createBrowser failed", t);
            browser = null;

            return false;
        }
    }

    /** Resize the browser to follow the overlay area (GUI units). */
    public static void resizeBrowser(int width, int height)
    {
        if (browser != null)
        {
            browser.resize(scale(width), scale(height));
        }
    }

    /** Convert GUI (scaled) units to physical pixels. */
    private static int scale(int value)
    {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        return Math.max(1, (int) Math.round(value * guiScale));
    }

    /** Record the overlay's top-left screen position (GUI units). Input must
     *  be made relative to this origin before scaling, otherwise clicks land
     *  shifted by the panel margins. Called from UIOverlay every frame. */
    public static void setViewportOffset(double x, double y)
    {
        browserOffsetX = x;
        browserOffsetY = y;
    }

    /** Map a screen GUI X coordinate to the browser's internal pixel X. */
    private static int toBrowserX(double screenX)
    {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        return (int) Math.round((screenX - browserOffsetX) * guiScale);
    }

    /** Map a screen GUI Y coordinate to the browser's internal pixel Y. */
    private static int toBrowserY(double screenY)
    {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        return (int) Math.round((screenY - browserOffsetY) * guiScale);
    }

    public static void close()
    {
        if (browser != null)
        {
            try
            {
                browser.close();
            }
            catch (Throwable ignored)
            {
            }

            browser = null;
        }

        if (browserTexId != null)
        {
            try
            {
                Minecraft.getInstance().getTextureManager().release(browserTexId);
            }
            catch (Throwable ignored)
            {
            }

            browserTex = null;
            browserTexId = null;
        }
    }

    /* -------- JS bridge -------- */

    /**
     * Register the Java <- JS channel. Primary channel: console messages
     * (JS logs "BBS_ACTION:<json>", we intercept in onConsoleMessage). This
     * is reliable - it does not depend on CEF injecting a bridge object
     * during navigation. The JSQuery router is also installed as a fallback
     * (window.javaQuery), but the console channel is the one the page uses.
     */
    private static void installJsQuery(CefBrowser cef)
    {
        try
        {
            /* Console channel (primary). */
            cef.getClient().addDisplayHandler(new CefDisplayHandlerAdapter()
            {
                @Override
                public boolean onConsoleMessage(CefBrowser browser, LogSeverity level, String message,
                    String source, int line)
                {
                    if (message != null && message.startsWith("BBS_ACTION:"))
                    {
                        final String request = message.substring("BBS_ACTION:".length());

                        Minecraft.getInstance().execute(() ->
                        {
                            try
                            {
                                EditorBridge.handle(request, MCEFUI.bridge);
                            }
                            catch (Throwable t)
                            {
                                BBSMod.LOGGER.error("[MCEF] action failed: {} ({})", request, t.getMessage());
                            }
                        });

                        return true;
                    }

                    return false;
                }
            });

            /* JSQuery fallback. */
            CefMessageRouter router = CefMessageRouter.create(
                new CefMessageRouter.CefMessageRouterConfig("javaQuery", "cancelJavaQuery"));
            router.addHandler(new CefMessageRouterHandlerAdapter()
            {
                @Override
                public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request,
                    boolean persistent, CefQueryCallback callback)
                {
                    try
                    {
                        String result = EditorBridge.handle(request, MCEFUI.bridge);
                        callback.success(result);

                        return true;
                    }
                    catch (Throwable t)
                    {
                        BBSMod.LOGGER.error("[MCEF] JSQuery failed: {} ({})", request, t.getMessage());
                        callback.failure(0, t.getMessage());

                        return true;
                    }
                }
            }, false);

            cef.getClient().addMessageRouter(router);
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] installJsQuery failed", t);
        }
    }

    /** Push the current editor state into the page (Java -> JS). */
    public static void pushState()
    {
        if (browser == null || bridge == null)
        {
            return;
        }

        try
        {
            String json = EditorBridge.getStateJson(bridge);
            browser.getCefBrowser().executeJavaScript(
                "window.bbsState = " + json + "; if (window.__onState) __onState();",
                "", 0);
        }
        catch (Throwable ignored)
        {
        }
    }

    /* -------- input (called from UIScreen) -------- */

    /** Whether the given menu currently shows the HTML editor. */
    public static boolean isActive(UIBaseMenu menu)
    {
        if (browser == null || !(menu instanceof UIDashboard))
        {
            return false;
        }

        UIDashboard dashboard = (UIDashboard) menu;

        return dashboard.getPanels().panel instanceof UIFilmPanel panel
            && panel.ultralightOverlay != null
            && panel.ultralightOverlay.isVisible();
    }

    public static boolean onMouseClicked(UIBaseMenu menu, MouseButtonEvent event, boolean doubled)
    {
        if (isActive(menu))
        {
            lastMouseX = event.x();
            lastMouseY = event.y();

            browser.onMouseClicked(new MouseButtonEvent(
                toBrowserX(event.x()), toBrowserY(event.y()), event.buttonInfo()), doubled);

            return true;
        }

        return false;
    }

    public static boolean onMouseReleased(UIBaseMenu menu, MouseButtonEvent event)
    {
        if (isActive(menu))
        {
            lastMouseX = event.x();
            lastMouseY = event.y();

            browser.onMouseReleased(new MouseButtonEvent(
                toBrowserX(event.x()), toBrowserY(event.y()), event.buttonInfo()));

            return true;
        }

        return false;
    }

    public static boolean onMouseScrolled(UIBaseMenu menu, double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (isActive(menu))
        {
            lastMouseX = mouseX;
            lastMouseY = mouseY;

            browser.onMouseScrolled(toBrowserX(mouseX), toBrowserY(mouseY), verticalAmount);

            return true;
        }

        return false;
    }

    public static boolean onMouseMoved(UIBaseMenu menu, double x, double y)
    {
        if (isActive(menu))
        {
            lastMouseX = x;
            lastMouseY = y;

            browser.onMouseMoved(toBrowserX(x), toBrowserY(y));

            return true;
        }

        return false;
    }

    public static boolean onKeyPressed(UIBaseMenu menu, KeyEvent event)
    {
        if (isActive(menu))
        {
            browser.onKeyPressed(event);

            return true;
        }

        return false;
    }

    public static boolean onKeyReleased(UIBaseMenu menu, KeyEvent event)
    {
        if (isActive(menu))
        {
            browser.onKeyReleased(event);

            return true;
        }

        return false;
    }

    public static boolean onCharTyped(UIBaseMenu menu, CharacterEvent event)
    {
        if (isActive(menu))
        {
            browser.onCharTyped(event);

            return true;
        }

        return false;
    }

    /* -------- rendering -------- */

    /**
     * Returns the vanilla {@link Identifier} of the browser's current frame.
     *
     * The frame lives in MCEF's native {@code GpuTexture} (updated on every
     * CEF paint, exposed via {@link MCEFTexture#getTextureView()}). We blit
     * that texture through the supported {@code GuiGraphics.blit(Identifier,
     * ...)} path on MC 26.2. Returns null until MCEF has produced its first
     * paint (its GpuTexture is created lazily inside onPaint), at which point
     * the caller should show a dark backdrop instead.
     */
    public static Identifier renderTextureId()
    {
        if (browser == null || browserTexId == null)
        {
            return null;
        }

        try
        {
            /* getTextureView() is null until MCEF's first onPaint allocates
             * the GpuTexture. Guard so we never blit an empty texture. */
            if (browser.getTextureView() != null)
            {
                return browserTexId;
            }

            return null;
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] renderTextureId failed", t);

            return null;
        }
    }

    public static boolean isReady()
    {
        return browser != null;
    }

    /* -------- page -------- */

    /** Embed the editor HTML as a data: URL (works offline, no file server). */
    private static String editorPageDataUrl()
    {
        try
        {
            var stream = MCEFUI.class.getResourceAsStream("/assets/bbs/ultralight/editor_ui.html");

            if (stream != null)
            {
                String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                String b64 = Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));

                return "data:text/html;charset=utf-8;base64," + b64;
            }
        }
        catch (Exception ignored)
        {
        }

        return "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(
            "<html><body style='background:#222;color:#fff'>MCEF OK</body></html>".getBytes(StandardCharsets.UTF_8));
    }
}
