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
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.joml.Matrix3x2f;
import org.cef.CefSettings.LogSeverity;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import lingfeng.bbsnext.client.GlTextureBridge;
import mchorse.bbs_mod.ui.supporters.Supporters;
import mchorse.bbs_mod.ui.supporters.Supporter;

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
 *   - Rendering: MCEF's native GpuTextureView is composited into the GUI by
 *     {@link #renderBrowser(GuiGraphicsExtractor, UIBaseMenu, int, int)}, which
 *     adds a BlitRenderState to the live guiRenderState during
 *     UIScreen.extractRenderState - the only path that reaches the screen on
 *     MC 26.2 (a direct GuiGraphics.blit(GpuTextureView, ...) does NOT draw).
 *   - Java -> JS: executeJavaScript pushes the editor state as
 *     `window.bbsState`.
 *   - JS -> Java: CEF JSQuery (`window.javaQuery`) routes actions to
 *     {@link EditorBridge}.
 */
public class MCEFUI
{
    private static MCEFApi api;
    private static MCEFBrowser browser;
    private static IHtmlBridge bridge;
    private static UIOverlay activeOverlay;
    private static String currentPageUrl;
    private static boolean initialized;

    /* The browser frame lives in MCEF's own native GpuTextureView; we draw it
     * directly via GuiGraphics.blit(GpuTextureView, ...) - no TextureManager
     * wrapper, no GL readback. */

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

    /* Editor layout fractions (0..1) reported by the page (title+menu+toolbar
     * band, asset-panel width, timeline height). Previously used to carve a
     * transparent "hole" in the editor so the 3D world showed through; now the
     * world is instead pushed into the HTML centre as a PNG (see capturePreview),
     * so these are retained only as reported metrics and are no longer read for
     * compositing. */
    private static float viewportTopFrac = 0.08f;
    private static float viewportLeftFrac = 0.14f;
    private static float viewportBottomFrac = 0.22f;

    /* Live 3D-world preview push (delivered to the HTML centre as a PNG). */
    private static int previewTick = 0;
    private static int previewFrame = 0;
    private static boolean previewToggle = false;

    /* When a system-level (OS) Swing dialog is open we stop blitting the
     * browser and stop pushing preview frames, so the native window is the
     * only thing on screen and is never visually buried under the full-screen
     * HTML editor. Toggled by EditorBridge.openNativeCreate. */
    private static volatile boolean browserSuspended = false;

    /** Pause/resume browser blitting (used while an OS-native dialog is open
     *  so the native window is never buried under the full-screen HTML). */
    public static void setBrowserSuspended(boolean suspended)
    {
        browserSuspended = suspended;
    }

    public static void setViewportMetrics(float top, float left, float bottom)
    {
        if (top > 0f && top < 1f) viewportTopFrac = top;
        if (left > 0f && left < 1f) viewportLeftFrac = left;
        if (bottom > 0f && bottom < 1f) viewportBottomFrac = bottom;
    }

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
    public static boolean createBrowser(int width, int height, IHtmlBridge jsBridge)
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
            /* Opaque (transparent=false): a transparent OSR browser can report
             * its first paint with empty dirty rects, which makes MCEF Modern's
             * onPaintInternal return early and never upload a GPU texture - so
             * getTextureView() stays null forever and the editor only shows the
             * dark backdrop. Opaque guarantees a non-empty first paint. */
            browser = mcef.createBrowser(jsBridge.pageUrl(), false);
            browser.resize(scale(width), scale(height));
            browser.setFocus(true);
            currentPageUrl = jsBridge.pageUrl();

            installJsQuery(browser.getCefBrowser());

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
                                MCEFUI.bridge.handle(request);
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
                    Minecraft.getInstance().execute(() ->
                    {
                        try
                        {
                            String result = MCEFUI.bridge.handle(request);

                            callback.success(result);
                        }
                        catch (Throwable t)
                        {
                            BBSMod.LOGGER.error("[MCEF] JSQuery failed: {} ({})", request, t.getMessage());

                            callback.failure(0, t.getMessage());
                        }
                    });

                    return true;
                }
            }, false);

            cef.getClient().addMessageRouter(router);

            /* Re-inject the sandboxed live-UI script whenever a page finishes
             * loading, so it survives editor<->dashboard navigation. */
            cef.getClient().addLoadHandler(new CefLoadHandlerAdapter()
            {
                @Override
                public void onLoadEnd(CefBrowser b, CefFrame frame, int httpStatusCode)
                {
                    if (frame != null && frame.isMain())
                    {
                        lingfeng.bbsnext.update.LiveUi.injectIfReady();
                    }
                }
            });
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
            String json = bridge.getStateJson();
            browser.getCefBrowser().executeJavaScript(
                "window.bbsState = " + json + "; if (window.__onState) __onState();",
                "", 0);
        }
        catch (Throwable ignored)
        {
        }
    }

    /** Run arbitrary JavaScript in the active page context. Used by the
     *  sandboxed live-UI channel (LiveUi). The script can only touch the DOM
     *  and the existing window.send() action channel - it has no native
     *  filesystem / process access. Runs on the render thread. */
    public static void injectScript(String js)
    {
        if (browser == null)
        {
            return;
        }

        try
        {
            Minecraft.getInstance().execute(() ->
            {
                try
                {
                    browser.getCefBrowser().executeJavaScript(js, browser.getCefBrowser().getURL(), 1);
                }
                catch (Throwable t)
                {
                    BBSMod.LOGGER.error("[MCEF] injectScript failed", t);
                }
            });
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] injectScript schedule failed", t);
        }
    }

    /* -------- input (called from UIScreen) -------- */

    /** Whether any HTML overlay (editor or dashboard) is currently active and
     *  should receive input / be composited. Gated by the live overlay so a
     *  single browser instance can host either page. */
    public static boolean isActive(UIBaseMenu menu)
    {
        /* Guard against the warm-up / teardown window: the overlay can be
         * visible (so callers think input should route to the page) while the
         * browser instance is still null - e.g. setActiveOverlay() ran but
         * createBrowser() hasn't completed yet, or close() already nulled the
         * browser while the overlay is still flagged visible. Routing events
         * into a null browser NPEs, so require it here. */
        return browser != null && activeOverlay != null && activeOverlay.isVisible();
    }

    public static void setActiveOverlay(UIOverlay overlay)
    {
        activeOverlay = overlay;

        /* A single browser instance hosts both the editor and dashboard
         * pages. When the active overlay changes, navigate to that page's
         * URL (only if it differs from what is already loaded) so the right
         * HTML is shown. No-op while the engine is still warming up. */
        if (browser != null && overlay != null)
        {
            String url = overlay.getBridge().pageUrl();

            if (!url.equals(currentPageUrl))
            {
                browser.getCefBrowser().loadURL(url);
                currentPageUrl = url;
            }
        }
    }

    public static void clearActiveOverlay(UIOverlay overlay)
    {
        if (activeOverlay == overlay)
        {
            activeOverlay = null;
        }
    }

    public static boolean onMouseClicked(UIBaseMenu menu, MouseButtonEvent event, boolean doubled)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                lastMouseX = event.x();
                lastMouseY = event.y();

                browser.onMouseClicked(new MouseButtonEvent(
                    toBrowserX(event.x()), toBrowserY(event.y()), event.buttonInfo()), doubled);
            }

            return true;
        }

        return false;
    }

    public static boolean onMouseReleased(UIBaseMenu menu, MouseButtonEvent event)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                lastMouseX = event.x();
                lastMouseY = event.y();

                browser.onMouseReleased(new MouseButtonEvent(
                    toBrowserX(event.x()), toBrowserY(event.y()), event.buttonInfo()));
            }

            return true;
        }

        return false;
    }

    public static boolean onMouseScrolled(UIBaseMenu menu, double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                lastMouseX = mouseX;
                lastMouseY = mouseY;

                browser.onMouseScrolled(toBrowserX(mouseX), toBrowserY(mouseY), verticalAmount);
            }

            return true;
        }

        return false;
    }

    public static boolean onMouseMoved(UIBaseMenu menu, double x, double y)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                lastMouseX = x;
                lastMouseY = y;

                browser.onMouseMoved(toBrowserX(x), toBrowserY(y));
            }

            return true;
        }

        return false;
    }

    public static boolean onKeyPressed(UIBaseMenu menu, KeyEvent event)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                browser.onKeyPressed(event);
            }

            return true;
        }

        return false;
    }

    public static boolean onKeyReleased(UIBaseMenu menu, KeyEvent event)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                browser.onKeyReleased(event);
            }

            return true;
        }

        return false;
    }

    public static boolean onCharTyped(UIBaseMenu menu, CharacterEvent event)
    {
        if (activeOverlay != null && activeOverlay.isVisible())
        {
            if (browser != null)
            {
                browser.onCharTyped(event);
            }

            return true;
        }

        return false;
    }

    /* -------- rendering -------- */

    /** Direct handle to MCEF's current browser frame texture, or null until
     *  the first paint. It is composited onto the screen by
     *  {@link #renderBrowser(GuiGraphicsExtractor, UIBaseMenu, int, int)}. */
    public static GpuTextureView getTextureView()
    {
        if (browser == null)
        {
            return null;
        }

        try
        {
            return browser.getTextureView();
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] getTextureView failed", t);

            return null;
        }
    }

    public static boolean isReady()
    {
        return browser != null;
    }

    /**
     * Composite the MCEF browser frame into the GUI render state.
     *
     * <p>MC 26.2 draws the whole screen from a deferred {@code GuiRenderState}
     * built during {@code extractRenderState}. A raw {@code GpuTextureView}
     * therefore cannot be drawn with a normal {@code GuiGraphics.blit(...)}
     * inside a render method - that call never reaches the screen and leaves
     * only the gray fallback backdrop. The only correct path (and exactly
     * what the official MCEF Modern test mod does) is to add a
     * {@link BlitRenderState} to {@code guiRenderState} here, in
     * {@code extractRenderState}. The browser is drawn fullscreen, on top of
     * every native dashboard element, so the HTML editor owns the whole view.
     *
     * @param context the live extractor bound by UIScreen this frame
     * @param menu    the active base menu (only draws when the HTML editor is open)
     * @param width   full GUI (scaled) width
     * @param height  full GUI (scaled) height
     */
    public static void renderBrowser(GuiGraphicsExtractor context, UIBaseMenu menu, int width, int height)
    {
        if (browser == null || !isActive(menu))
        {
            return;
        }

        GpuTextureView view = getTextureView();

        if (view == null)
        {
            return;
        }

        try
        {
            /* Draw the full opaque HTML editor page on top of the native UI.
             * The live 3D world is delivered to the HTML centre as a PNG image
             * (see capturePreview) so the editor "player" shows a real picture
             * rather than a dead decoration - no transparent hole needed. */
            context.guiRenderState.addGuiElement(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                new Matrix3x2f(context.pose()),
                0, 0, width, height,
                0.0F, 1.0F, 0.0F, 1.0F,
                0xFFFFFFFF,
                context.scissorStack.peek()));

            /* Preview readback is paused while an OS-native dialog is open
             * (browserSuspended) so camera/scene creation doesn't fight the
             * PNG writer. The editor page itself keeps rendering - we must
             * NOT skip the blit, or the screen shows the raw clear colour
             * (a solid blue/black) behind the native window. */
            if (!browserSuspended)
            {
                capturePreview();
            }
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] renderBrowser failed", t);
        }
    }

    /**
     * Pushes the live 3D world (Minecraft's main render target) into the HTML
     * editor centre as a PNG image, so the preview "player" shows a real
     * picture. Throttled to keep the GPU readback cheap.
     *
     * <p>Backend-agnostic: {@code GlTextureBridge.captureMainRenderTarget}
     * uses the vanilla {@code Screenshot.takeScreenshot(RenderTarget)} path,
     * so the preview works on both the OpenGL and Vulkan backends. The PNG is
     * written to a ping-pong pair of files (preview_a/b.png) so the browser
     * always reloads the latest frame (no file:// cache staleness).
     */
    private static void capturePreview()
    {
        if ((previewTick++ % 4) != 0)
        {
            return;
        }

        if (currentPageUrl == null || !currentPageUrl.contains("editor_ui"))
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.gameRenderer == null || mc.gameRenderer.mainRenderTarget() == null)
        {
            /* No world is loaded (e.g. the editor was opened from the title
             * screen / dashboard with no level) so there is nothing to
             * capture. Tell the page to show its placeholder instead of
             * leaving the <img> on a broken/empty source. */
            if (previewFrame != -1)
            {
                previewFrame = -1;
                injectScript("window.__previewMode='none';");
            }

            return;
        }

        try
        {
            /* Backend-agnostic capture: Screenshot.takeScreenshot works on both
             * OpenGL and Vulkan, so the live preview is no longer limited to
             * the OpenGL backend. */
            byte[] png = GlTextureBridge.captureMainRenderTarget(mc.gameRenderer.mainRenderTarget(), 640);

            if (png == null)
            {
                return;
            }

            Path dir = FabricLoader.getInstance().getGameDir().resolve("bbs_editor");
            Files.createDirectories(dir);

            String name = previewToggle ? "preview_b.png" : "preview_a.png";
            previewToggle = !previewToggle;

            Files.write(dir.resolve(name), png);

            if (previewFrame < 0)
            {
                previewFrame = 0;
            }

            previewFrame++;
            injectScript("window.__previewMode='gl';window.__previewFrame=" + previewFrame + ";window.__previewFile='" + name + "';");
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] capturePreview failed", t);
        }
    }

    /* -------- page -------- */

    /** SVG icons bundled with the mod, extracted next to the HTML so the page
     *  can reference them with plain relative <img src="svg/xxx.svg"> (real
     *  files, not inlined). Kept in sync with src/.../editor/svg/. */
    private static final String[] SVG_FILES = {
        "work", "scene", "sequence", "character", "entity", "particle", "item",
        "backpack", "camera", "play", "pause", "tostart", "undo", "redo",
        "save", "close", "plus", "trash", "settings", "target", "rename"
    };

    /** Extract every hosted HTML page (editor + dashboard) and the bundled SVG
     *  icons to a real folder on disk so the pages can reference external SVG
     *  files with plain relative <img src="svg/xxx.svg"> (a data: URL page
     *  cannot resolve relative file references, and Chromium blocks data:
     *  origins from loading file: resources). */
    private static void extractPages()
    {
        try
        {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("bbs_editor");

            /* Wipe + rebuild the whole cache if this build's version stamp
             * differs (or is missing). Without this, a silently-failed extract
             * or CEF's file cache would keep serving an ancient editor_ui.html
             * even after the jar was updated. */
            ensureFreshCache(dir);

            Path svgDir = dir.resolve("svg");

            Files.createDirectories(svgDir);

            copyResource("/assets/bbs/editor/editor_ui.html", dir.resolve(versionedName("editor_ui.html")));
            copyResource("/assets/bbs/editor/dashboard_ui.html", dir.resolve(versionedName("dashboard_ui.html")));

            for (String name : SVG_FILES)
            {
                copyResource("/assets/bbs/editor/svg/" + name + ".svg", svgDir.resolve(name + ".svg"));
            }

            /* Supporter/developer avatar banners, referenced by the dashboard
             * supporters page as <img src="banners/xxx.png">. */
            try
            {
                Path bannerDir = dir.resolve("banners");

                Files.createDirectories(bannerDir);

                Supporters supporters = new Supporters();

                supporters.setup();

                copyBanners(bannerDir, supporters.getCCSupporters());
                copyBanners(bannerDir, supporters.getDevelopers());
            }
            catch (Throwable t)
            {
                BBSMod.LOGGER.error("[MCEF] failed to extract banners", t);
            }
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.error("[MCEF] failed to extract pages", t);
        }
    }

    /** Copy every supporter/developer banner PNG next to the HTML pages so the
     *  dashboard can render their avatars with plain relative <img> tags. */
    private static void copyBanners(Path bannerDir, List<Supporter> list)
    {
        for (Supporter s : list)
        {
            if (s.banner == null)
            {
                continue;
            }

            String path = s.banner.path;
            String file = path.substring(path.lastIndexOf('/') + 1);

            if (!file.isEmpty())
            {
                copyResource("/assets/bbs/assets/textures/banners/" + file, bannerDir.resolve(file));
            }
        }
    }

    /** Keep the on-disk HTML/SVG cache in lock-step with this build. If the
     *  version stamp is missing or differs from the running mod version, wipe
     *  the entire {@code bbs_editor} folder and recreate it, so no stale
     *  editor_ui.html / dashboard_ui.html / svg linger. This is what prevented
     *  the "old webpage keeps showing after an update" bug. */
    private static void ensureFreshCache(Path dir)
    {
        String ver = currentModVersion();

        try
        {
            Path stamp = dir.resolve(".bbs_version");
            boolean stale = true;

            if (Files.isDirectory(dir))
            {
                try
                {
                    String existing = Files.readString(stamp, StandardCharsets.UTF_8).trim();

                    stale = !existing.equals(ver);
                }
                catch (Exception ignored)
                {
                    stale = true;
                }
            }

            if (stale)
            {
                deleteRecursive(dir);
                Files.createDirectories(dir);
            }

            Files.writeString(stamp, ver, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
        catch (Throwable t)
        {
            BBSMod.LOGGER.warn("[MCEF] cache version check failed, forcing clean extract", t);

            try
            {
                deleteRecursive(dir);
                Files.createDirectories(dir);
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    /** Recursively delete a folder (used to purge a stale cache). Never throws. */
    private static void deleteRecursive(Path dir)
    {
        try
        {
            if (Files.exists(dir))
            {
                try (var walk = Files.walk(dir))
                {
                    walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p ->
                        {
                            try
                            {
                                Files.deleteIfExists(p);
                            }
                            catch (Exception ignored)
                            {
                            }
                        });
                }
            }
        }
        catch (Throwable ignored)
        {
        }
    }

    /** Friendly mod version (e.g. "2.0.241"); falls back to "unknown". */
    private static String currentModVersion()
    {
        try
        {
            return FabricLoader.getInstance()
                .getModContainer("bbs-next")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        }
        catch (Throwable t)
        {
            return "unknown";
        }
    }

    private static void copyResource(String resource, Path out)
    {
        try (InputStream in = MCEFUI.class.getResourceAsStream(resource))
        {
            if (in != null)
            {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Throwable ignored)
        {
        }
    }

    /** Resolve the absolute file:// URL for one of the hosted HTML pages,
     *  extracting them first. Falls back to an inline data: URL if extraction
     *  fails (e.g. the file is missing from the jar). */
    public static String pageFileUrl(String fileName)
    {
        extractPages();

        Path file = FabricLoader.getInstance().getGameDir().resolve("bbs_editor").resolve(versionedName(fileName));

        if (Files.exists(file))
        {
            /* Version-stamped filename is the real cache-buster: Chromium keys
             * its file:// cache on the full path, so a new build writing a
             * differently-named file is always reloaded. A ?v= query string is
             * unreliable here — CEF frequently ignores query strings for
             * file:// resources, which is exactly what served the ancient
             * editor_ui.html after an update. */
            return file.toUri().toString();
        }

        return pageDataUrl("/assets/bbs/editor/" + fileName);
    }

    /** Stamp the running mod version into a hosted page's file name so each
     *  build gets a distinct file:// path. CEF keys its file cache on the
     *  full path, so this reliably busts stale HTML without relying on a
     *  query string (which Chromium often ignores for file:// URLs). */
    private static String versionedName(String fileName)
    {
        return versionedName(fileName, currentModVersion());
    }

    private static String versionedName(String fileName, String ver)
    {
        int dot = fileName.lastIndexOf('.');

        if (dot < 0)
        {
            return fileName + "_" + ver;
        }

        return fileName.substring(0, dot) + "_" + ver + fileName.substring(dot);
    }

    /** Embed a classpath HTML resource as a data: URL (offline fallback). */
    private static String pageDataUrl(String resource)
    {
        try (InputStream stream = MCEFUI.class.getResourceAsStream(resource))
        {
            if (stream != null)
            {
                byte[] bytes = stream.readAllBytes();

                return "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        }
        catch (Exception ignored)
        {
        }

        return "data:text/html;charset=utf-8;base64," + Base64.getEncoder().encodeToString(
            "<html><body style='background:#222;color:#fff'>MCEF page missing</body></html>".getBytes(StandardCharsets.UTF_8));
    }
}
