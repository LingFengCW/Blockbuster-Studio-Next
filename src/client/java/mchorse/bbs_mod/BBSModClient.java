package mchorse.bbs_mod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.platform.Window;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.misc.CurveClientClip;
import mchorse.bbs_mod.camera.clips.misc.TrackerClientClip;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.GameIconPlugin;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.entity.ActorEntityRenderer;
import mchorse.bbs_mod.client.renderer.entity.GunProjectileEntityRenderer;
import mchorse.bbs_mod.client.renderer.item.GunItemRenderer;
import mchorse.bbs_mod.client.renderer.item.ModelBlockItemRenderer;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.FramebufferManager;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.items.GunZoom;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.particles.ParticleManager;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLError;
import mchorse.bbs_mod.resources.packs.URLRepository;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.resources.packs.URLTextureErrorCallback;
import mchorse.bbs_mod.selectors.EntitySelectors;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.scenes.UISceneMenu;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.projects.UIProjectMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockEditorMenu;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.ui.utils.keys.KeybindSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import mchorse.bbs_mod.utils.VideoRecorder;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.MinecraftSourcePack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class BBSModClient implements ClientModInitializer
{
    public static final Logger LOGGER = LoggerFactory.getLogger("BBSClient");
    /** True when a GL context is available (OpenGL backend, not Vulkan). */
    public static boolean GL_AVAILABLE;
    private static boolean bbsResourcesInitialized;
    private static int bbsResourcesRetries;
    private static TextureManager textures;
    private static FramebufferManager framebuffers;
    private static SoundManager sounds;
    private static L10n l10n;

    private static ModelManager models;
    private static FormCategories formCategories;
    private static ScreenshotRecorder screenshotRecorder;
    private static VideoRecorder videoRecorder;
    private static EntitySelectors selectors;

    private static ParticleManager particles;

    private static KeyMapping keyDashboard;
    private static KeyMapping keyTimeline;
    private static KeyMapping keyItemEditor;
    private static KeyMapping keyPlayFilm;
    private static KeyMapping keyPauseFilm;
    private static KeyMapping keyRecordReplay;
    private static KeyMapping keyRecordVideo;
    private static KeyMapping keyOpenReplays;
    private static KeyMapping keyOpenMorphing;
    private static KeyMapping keyDemorph;
    private static KeyMapping keyTeleport;
    private static KeyMapping keyZoom;

    private static UIDashboard dashboard;

    private static CameraController cameraController = new CameraController();
    private static ModelBlockItemRenderer modelBlockItemRenderer = new ModelBlockItemRenderer();
    private static GunItemRenderer gunItemRenderer = new GunItemRenderer();
    private static Films films;
    private static GunZoom gunZoom;

    private static float originalFramebufferScale;

    public static TextureManager getTextures()
    {
        return textures;
    }

    public static FramebufferManager getFramebuffers()
    {
        return framebuffers;
    }

    public static SoundManager getSounds()
    {
        return sounds;
    }

    public static L10n getL10n()
    {
        return l10n;
    }

    public static ModelManager getModels()
    {
        return models;
    }

    public static FormCategories getFormCategories()
    {
        return formCategories;
    }

    public static ScreenshotRecorder getScreenshotRecorder()
    {
        return screenshotRecorder;
    }

    public static VideoRecorder getVideoRecorder()
    {
        return videoRecorder;
    }

    public static EntitySelectors getSelectors()
    {
        return selectors;
    }

    public static ParticleManager getParticles()
    {
        return particles;
    }

    public static CameraController getCameraController()
    {
        return cameraController;
    }

    public static Films getFilms()
    {
        return films;
    }

    public static GunZoom getGunZoom()
    {
        return gunZoom;
    }

    public static KeyMapping getKeyZoom()
    {
        return keyZoom;
    }

    public static KeyMapping getKeyRecordVideo()
    {
        return keyRecordVideo;
    }

    public static UIDashboard getDashboard()
    {
        if (dashboard == null)
        {
            dashboard = new UIDashboard();
        }

        return dashboard;
    }

    /**
     * The single entry point of the editor: work -> scene -> dashboard.
     *
     * No work picked yet? Show the work browser. Work picked but no scene
     * open? Show the scene browser. Otherwise open the dashboard and make
     * sure the active scene's content is actually loaded into the editor.
     *
     * @param panel optional panel to focus once the dashboard is up
     */
    public static void openEditorFlow(Class<?> panel)
    {
        try
        {
            openEditorFlowUnchecked(panel);
        }
        catch (Exception e)
        {
            LOGGER.error("openEditorFlow failed, falling back to the dashboard", e);

            try
            {
                UIScreen.open(getDashboard());
            }
            catch (Exception ex)
            {
                LOGGER.error("Failed to open the dashboard either", ex);
            }
        }
    }

    private static void openEditorFlowUnchecked(Class<?> panel)
    {
        /* Always land on the dashboard - with or without a current work.
         * The dashboard's default panel is the work library, where the user
         * picks or creates a work; the legacy full-screen work picker is no
         * longer part of this flow. */
        UIDashboard dashboard = getDashboard();

        UIScreen.open(dashboard);

        if (panel != null)
        {
            Object found = dashboard.getPanel(panel);

            if (found instanceof mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel dashboardPanel)
            {
                dashboard.setPanel(dashboardPanel);
            }
        }
    }

    /**
     * F key - the classic Blockbuster entry to the timeline, active while
     * the editor is open. Outside the editor it does nothing: per the
     * PR-style redesign the editor is only entered from the main menu
     * 【项目】 button.
     */
    private static void keyOpenTimeline()
    {
        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            UIFilmPanel film = dashboard.getPanel(UIFilmPanel.class);

            if (film != null)
            {
                dashboard.setPanel(film);
            }
        }
    }

    public static int getGUIScale()
    {
        int scale = BBSSettings.userIntefaceScale.get();

        if (scale == 0)
        {
            return Minecraft.getInstance().options.guiScale().get();
        }

        return scale;
    }

    public static float getOriginalFramebufferScale()
    {
        return Math.max(originalFramebufferScale, 1);
    }

    public static ModelProperties getItemStackProperties(ItemStack stack)
    {
        ModelBlockItemRenderer.Item item = modelBlockItemRenderer.get(stack);

        if (item != null)
        {
            return item.entity.getProperties();
        }

        GunItemRenderer.Item gunItem = gunItemRenderer.get(stack);

        if (gunItem != null)
        {
            return gunItem.properties;
        }

        return null;
    }

    public static void onEndKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info)
    {
        if (action != GLFW.GLFW_PRESS)
        {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        Morph morph = Morph.getMorph(player);

        /* Animation state trigger */
        if (morph != null && morph.getForm() != null && morph.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_MORPH);
            form.playState(state);
        }))
            return;

        /* Animation state trigger for items*/
        ModelProperties main = getItemStackProperties(player.getItemInHand(InteractionHand.MAIN_HAND));
        ModelProperties offhand = getItemStackProperties(player.getItemInHand(InteractionHand.OFF_HAND));

        if (main != null && main.getForm() != null && main.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_MAIN_HAND_ITEM);
            form.playState(state);
        }))
            return;

        if (offhand != null && offhand.getForm() != null && offhand.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_OFF_HAND_ITEM);
            form.playState(state);
        }))
            return;

        /* Change form based on the hotkey */
        for (Form form : BBSModClient.getFormCategories().getRecentForms().getCategories().get(0).getForms())
        {
            if (form.hotkey.get() == key)
            {
                ClientNetwork.sendPlayerForm(form);

                return;
            }
        }

        for (UserFormCategory category : BBSModClient.getFormCategories().getUserForms().categories)
        {
            for (Form form : category.getForms())
            {
                if (form.hotkey.get() == key)
                {
                    ClientNetwork.sendPlayerForm(form);

                    return;
                }
            }
        }
    }

    @Override
    public void onInitializeClient()
    {
        AssetProvider provider = BBSMod.getProvider();

        textures = new TextureManager(provider);
        framebuffers = new FramebufferManager();
        sounds = new SoundManager(provider);
        l10n = new L10n();
        l10n.register((lang) -> Collections.singletonList(Link.assets("strings/" + lang + ".json")));
        l10n.reload();

        BBSMod.events.post(new RegisterL10nEvent(l10n));

        /* Detect rendering backend once at startup.
         * OpenGL → GL11 calls are safe. Vulkan → skip all GL calls.
         * Defer to first tick if window is not yet available. */
        try
        {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().getWindow() != null)
            {
                Object backend = Minecraft.getInstance().getWindow().backend();
                String cls = backend.getClass().getName();
                GL_AVAILABLE = cls.contains("GlBackend") || cls.contains("opengl");
            }
        }
        catch (Throwable t)
        {
            LOGGER.warn("Failed to detect graphics backend, assuming non-GL", t);
        }

        LOGGER.info("Graphics backend: GL_AVAILABLE=" + GL_AVAILABLE);

        /* MC 26.2: register the picture-in-picture renderer used by model
           preview panels (legal replacement for extraction-phase rendering) */
        net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.register((ctx) -> new mchorse.bbs_mod.ui.framework.elements.utils.UIModelPipRenderer());

        File parentFile = BBSMod.getSettingsFolder().getParentFile();

        particles = new ParticleManager(() -> new File(BBSMod.getAssetsFolder(), "particles"));

        models = new ModelManager(provider);
        formCategories = new FormCategories();
        screenshotRecorder = new ScreenshotRecorder(new File(parentFile, "screenshots"));
        videoRecorder = new VideoRecorder();
        selectors = new EntitySelectors();
        selectors.read();
        films = new Films();

        /* BBSResources.init() is deferred to ClientLifecycleEvents.CLIENT_STARTED (see the
           registration near the end of onInitializeClient). Constructing registry ItemStacks
           such as new ItemStack(Items.STICK) during the client entry point throws
           "Components not bound yet" because the built-in registries are not frozen until
           after onInitializeClient runs. */

        URLRepository repository = new URLRepository(new File(parentFile, "url_cache"));

        provider.register(new URLSourcePack("http", repository));
        provider.register(new URLSourcePack("https", repository));

        KeybindSettings.registerClasses();

        BBSMod.setupConfig(Icons.KEY_CAP, "keybinds", new File(BBSMod.getSettingsFolder(), "keybinds.json"), KeybindSettings::register);

        BBSMod.events.post(new RegisterClientSettingsEvent());

        BBSSettings.language.postCallback((v, f) -> reloadLanguage(getLanguageKey()));
        BBSSettings.editorSeconds.postCallback((v, f) ->
        {
            if (dashboard != null && dashboard.getPanels().panel instanceof UIFilmPanel panel)
            {
                panel.fillData();
            }
        });

        BBSSettings.tooltipStyle.modes(
            UIKeys.ENGINE_TOOLTIP_STYLE_LIGHT,
            UIKeys.ENGINE_TOOLTIP_STYLE_DARK
        );

        BBSSettings.keystrokeMode.modes(
            UIKeys.ENGINE_KEYSTROKES_POSITION_AUTO,
            UIKeys.ENGINE_KEYSTROKES_POSITION_BOTTOM_LEFT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_BOTTOM_RIGHT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_TOP_RIGHT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_TOP_LEFT
        );

        UIKeys.C_KEYBIND_CATGORIES.load(KeyCombo.getCategoryKeys());
        UIKeys.C_KEYBIND_CATGORIES_TOOLTIP.load(KeyCombo.getCategoryKeys());

        /* Replace audio clip with client version that plays audio */
        BBSMod.getFactoryCameraClips()
            .register(Link.bbs("audio"), AudioClientClip.class, new ClipFactoryData(Icons.SOUND, 0xffc825))
            .register(Link.bbs("tracker"), TrackerClientClip.class, new ClipFactoryData(Icons.USER, 0x4cedfc))
            .register(Link.bbs("curve"), CurveClientClip.class, new ClipFactoryData(Icons.ARC, 0xff1493));

        /* Keybinds */
        keyDashboard = this.createKey("dashboard", GLFW.GLFW_KEY_0);
        keyTimeline = this.createKey("timeline", GLFW.GLFW_KEY_F);
        keyItemEditor = this.createKey("item_editor", GLFW.GLFW_KEY_HOME);
        keyPlayFilm = this.createKey("play_film", GLFW.GLFW_KEY_RIGHT_CONTROL);
        keyPauseFilm = this.createKey("pause_film", GLFW.GLFW_KEY_BACKSLASH);
        keyRecordReplay = this.createKey("record_replay", GLFW.GLFW_KEY_RIGHT_ALT);
        keyRecordVideo = this.createKey("record_video", GLFW.GLFW_KEY_F4);
        keyOpenReplays = this.createKey("open_replays", GLFW.GLFW_KEY_RIGHT_SHIFT);
        keyOpenMorphing = this.createKey("open_morphing", GLFW.GLFW_KEY_B);
        keyDemorph = this.createKey("demorph", GLFW.GLFW_KEY_PERIOD);
        keyTeleport = this.createKey("teleport", GLFW.GLFW_KEY_Y);
        keyZoom = this.createKeyMouse("zoom", 2);

        /* World rendering - after entities and terrain */
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(this::onRenderLevel);

        LevelRenderEvents.END_MAIN.register((context) ->
        {
            if (videoRecorder.isRecording() && BBSRendering.canRender)
            {
                videoRecorder.recordFrame();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            dashboard = null;
            films = new Films();

            ClientNetwork.resetInteractionHandshake();
            films.reset();
            cameraController.reset();
        });

        ClientTickEvents.START_CLIENT_TICK.register((client) ->
        {
            BBSRendering.startTick();
        });

        ClientTickEvents.END_LEVEL_TICK.register((client) ->
        {
            Minecraft mc = Minecraft.getInstance();

            if (!mc.isPaused())
            {
                /* updateEndLevel removed */;
            }

            BBSResources.tick();
        });

        ClientTickEvents.END_CLIENT_TICK.register((client) ->
        {
            Minecraft mc = Minecraft.getInstance();

            /* screen tick removed in MC 26.2 - moved to private in Gui */

            /* Deferred BBSResources.init: retry each tick until data components bind. */
            if (!bbsResourcesInitialized && bbsResourcesRetries < 3000)
            {
                try
                {
                    BBSResources.init();
                    bbsResourcesInitialized = true;
                    BBSModClient.LOGGER.info("BBSResources.init completed after " + bbsResourcesRetries + " retries");
                }
                catch (NullPointerException e)
                {
                    bbsResourcesRetries++;

                    if (bbsResourcesRetries % 100 == 0)
                    {
                        BBSModClient.LOGGER.info("BBSResources.init retry " + bbsResourcesRetries);
                    }
                }
                catch (Exception e)
                {
                    BBSModClient.LOGGER.error("BBSResources.init permanently failed", e);
                    bbsResourcesInitialized = true;
                }
            }
            else if (!bbsResourcesInitialized && bbsResourcesRetries >= 3000)
            {
                BBSModClient.LOGGER.warn("BBSResources.init: gave up after " + bbsResourcesRetries + " retries");
                bbsResourcesInitialized = true;
                bbsResourcesInitialized = true;
            }

            cameraController.update();

            if (!mc.isPaused())
            {
                films.update();
                modelBlockItemRenderer.update();
                gunItemRenderer.update();
                textures.update();
            }

            /* PR-style redesign: the editor is only reachable from the main
             * menu 【项目】 button. The old global 0/B hotkeys are removed so
             * the dashboard no longer pops up mid-game. */
            while (keyTimeline.consumeClick()) this.keyOpenTimeline();
            while (keyItemEditor.consumeClick()) this.keyOpenModelBlockEditor(mc);
            while (keyPlayFilm.consumeClick()) this.keyPlayFilm();
            while (keyPauseFilm.consumeClick()) this.keyPauseFilm();
            while (keyRecordReplay.consumeClick()) this.keyRecordReplay();
            while (keyRecordVideo.consumeClick())
            {
                Window window = mc.getWindow();
                int width = Math.max(window.getWidth(), 2);
                int height = Math.max(window.getHeight(), 2);

                if (width % 2 == 1) width -= width % 2;
                if (height % 2 == 1) height -= height % 2;

                videoRecorder.toggleRecording(BBSRendering.getTexture().id, width, height);
                BBSRendering.setCustomSize(videoRecorder.isRecording(), width, height);
            }
            while (keyOpenReplays.consumeClick()) this.keyOpenReplays();
            while (keyDemorph.consumeClick()) ClientNetwork.sendPlayerForm(null);
            while (keyTeleport.consumeClick()) this.keyTeleport();

            if (mc.player != null)
            {
                boolean zoom = keyZoom.isDown();
                ItemStack stack = mc.player.getMainHandItem();

                if (gunZoom == null && zoom && stack.getItem() == BBSMod.GUN_ITEM)
                {
                    GunProperties properties = GunProperties.get(stack);

                    ClientNetwork.sendZoom(true);
                    gunZoom = new GunZoom(properties.fovTarget, properties.fovInterp, properties.fovDuration);
                }
            }
        });

        /* HUD rendering - attach before chat layer */
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("bbs", "hud_renderer"),
            (GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker tickDelta) ->
            {
                BBSRendering.renderHud(graphics, tickDelta.getGameTimeDeltaPartialTick(false));

                if (gunZoom != null)
                {
                    gunZoom.update(keyZoom.isDown(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));

                    if (gunZoom.canBeRemoved())
                    {
                        ClientNetwork.sendZoom(false);
                        gunZoom = null;
                    }
                }
            }
        );

        /* Main menu entry point (PR-style redesign: a 【项目】 button at the
         * very top of the vanilla button column, 180x24, flat styled).
         *
         * This deliberately does NOT go through a mixin: bbs.client.mixins.json
         * is not registered in fabric.mod.json (19 of its client mixins target
         * methods that no longer exist in 26.2, so turning it on crashes the
         * game). The Fabric screen API gives us the same injection point with
         * zero mixin risk. */
        /* The in-game drawn menu bar (TitleToolbarWidget) was mounted on the
         * title screen by AFTER_INIT, painting an 18px full-width dark bar
         * over the top of the screen (left over from the undecorated-window
         * era, complete with a self-window frame). It is removed entirely.
         *
         * The 【项目】 entry on the title screen is added here through the
         * Fabric screen API (Screens.getWidgets) - the vanilla Button
         * created by the old TitleScreenMixin never received clicks in 26.2
         * and was dropped. Clicking it opens the project picker, where the
         * user selects a work to edit. */
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
        {
            if (screen instanceof net.minecraft.client.gui.screens.TitleScreen titleScreen)
            {
                /* Icon button in the bottom-right corner, built like Mod
                 * Menu's mods button: vanilla SpriteIconButton.CenteredIcon
                 * fed by a GUI-atlas sprite (textures/gui/sprites/ loads it
                 * automatically - no manual texture registration needed).
                 * The window's gui-scaled size matches the widget coordinate
                 * space, so the corner position is exact at any GUI scale. */
                /* Mod Menu places its 20x20 mods button 22px from the
                 * bottom-right corner; same spot for the works button. */
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                int btnX = mc.getWindow().getGuiScaledWidth() - 16;
                int btnY = mc.getWindow().getGuiScaledHeight() - 16;

                /* AFTER_INIT fires on every TitleScreen init (returning from
                 * a world, resizing, etc.); skip if the button is already in
                 * this screen's widget list, otherwise they pile up across
                 * the whole screen. */
                boolean alreadyAdded = false;

                for (net.minecraft.client.gui.components.AbstractWidget w : net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(titleScreen))
                {
                    if (w instanceof lingfeng.bbsnext.ui.titlebar.ProjectsIconButton)
                    {
                        alreadyAdded = true;

                        break;
                    }
                }

                if (!alreadyAdded)
                {
                    net.minecraft.client.gui.components.AbstractButton button = new lingfeng.bbsnext.ui.titlebar.ProjectsIconButton(btnX, btnY);

                    net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(titleScreen).add(button);
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register((e) -> BBSResources.stopWatchdog());
        ClientLifecycleEvents.CLIENT_STARTED.register((e) ->
        {
            BBSRendering.setupFramebuffer();
            provider.register(new MinecraftSourcePack());

            Window window = Minecraft.getInstance().getWindow();

            originalFramebufferScale = window.getWidth() / (float)window.getWidth();

            /* Replace the game window icon after launch (no-op until bbs_mod/icon.png is supplied). */
            GameIconPlugin.apply();

            /* The native Win32 menu bar is gone: its WM_COMMAND callbacks
             * ran on the window message thread and stalled the main thread,
             * and hot-reloading it across screens never worked. The in-game
             * top bar (UITopBar) is mounted on every UIBaseMenu instead. */

            /* MC 26.2: init is deferred because ExtraFormSection.initiate() creates an
             * ItemStack which requires data components to be bound. CLIENT_STARTED fires
             * too early; we set a flag and let END_CLIENT_TICK retry. */
            bbsResourcesInitialized = false;
        });

        URLTextureErrorCallback.EVENT.register((url, error) ->
        {
            UIBaseMenu menu = UIScreen.getCurrentMenu();

            if (menu != null)
            {
                url = url.substring(0, MathUtils.clamp(url.length(), 0, 40));

                if (error == URLError.FFMPEG)
                {
                    menu.context.notifyError(UIKeys.TEXTURE_URL_ERROR_FFMPEG.format(url));
                }
                else if (error == URLError.HTTP_ERROR)
                {
                    menu.context.notifyError(UIKeys.TEXTURE_URL_ERROR_HTTP.format(url));
                }
            }
        });

        BBSRendering.setup();

        /* Network */
        ClientNetwork.setup();

        /* Entity renderers */
        EntityRendererRegistry.register(BBSMod.ACTOR_ENTITY, ActorEntityRenderer::new);
        EntityRendererRegistry.register(BBSMod.GUN_PROJECTILE_ENTITY, GunProjectileEntityRenderer::new);

        BlockEntityRendererRegistry.register(BBSMod.MODEL_BLOCK_ENTITY, ModelBlockEntityRenderer::new);

        /* Item renderers */
        /* registerItemRenderer removed */;
        /* registerItemRenderer removed */;

        /* Create folders */
        BBSMod.getAudioFolder().mkdirs();
        BBSMod.getAssetsPath("textures").mkdirs();

        for (String path : List.of("alex", "alex_simple", "steve", "steve_simple"))
        {
            BBSMod.getAssetsPath("models/emoticons/" + path + "/").mkdirs();
        }

        for (String path : List.of("alex", "alex_bends", "eyes", "eyes_1px", "steve", "steve_bends"))
        {
            BBSMod.getAssetsPath("models/player/" + path + "/").mkdirs();
        }
    }

    private void onRenderLevel(LevelRenderContext context)
    {
        if (!BBSRendering.isIrisShadersEnabled())
        {
            BBSRendering.renderCoolStuff(context);
        }

        if (BBSSettings.chromaSkyEnabled.get())
        {
            float d = BBSSettings.chromaSkyBillboard.get();

            if (d > 0)
            {
                PoseStack stack = context.poseStack();
                Color color = Colors.COLOR.set(BBSSettings.chromaSkyColor.get());

                stack.pushPose();
                stack.setIdentity();
                stack.translate(0F, 0F, -d);

                // [MC 26.2] Use new BufferBuilder API - no endVertex needed
                ByteBufferBuilder byteBuffer = new ByteBufferBuilder(36);
                BufferBuilder builder = new BufferBuilder(byteBuffer, PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                Matrix4f matrix = stack.last().pose();
                builder.addVertex(matrix, -d, -d, 0).setColor(color.r, color.g, color.b, 1F);
                builder.addVertex(matrix, d, -d, 0).setColor(color.r, color.g, color.b, 1F);
                builder.addVertex(matrix, d, d, 0).setColor(color.r, color.g, color.b, 1F);
                builder.addVertex(matrix, -d, d, 0).setColor(color.r, color.g, color.b, 1F);
                MeshData mesh = builder.buildOrThrow();

                stack.popPose();
            }
        }
    }

    private KeyMapping createKey(String id, int key)
    {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + BBSMod.MOD_ID + "." + id,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            key,
            KeyMapping.Category.MISC
        ));
    }

    private KeyMapping createKeyMouse(String id, int button)
    {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + BBSMod.MOD_ID + "." + id,
            com.mojang.blaze3d.platform.InputConstants.Type.MOUSE,
            button,
            KeyMapping.Category.MISC
        ));
    }

    private void keyOpenModelBlockEditor(Minecraft mc)
    {
        ItemStack stack = mc.player.getItemBySlot(EquipmentSlot.MAINHAND);
        ModelBlockItemRenderer.Item item = modelBlockItemRenderer.get(stack);
        GunItemRenderer.Item gunItem = gunItemRenderer.get(stack);

        if (item != null)
        {
            UIScreen.open(new UIModelBlockEditorMenu(item.entity.getProperties()));
        }
        else if (gunItem != null)
        {
            UIScreen.open(new UIModelBlockEditorMenu(gunItem.properties));
        }
    }

    private void keyPlayFilm()
    {
        UIFilmPanel panel = getDashboard().getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null)
        {
            Films.playFilm(panel.getData().getId(), false);
        }
    }

    private void keyPauseFilm()
    {
        UIFilmPanel panel = getDashboard().getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null)
        {
            Films.pauseFilm(panel.getData().getId());
        }
    }

    private void keyRecordReplay()
    {
        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null)
        {
            Recorder recorder = getFilms().getRecorder();

            if (recorder != null)
            {
                recorder = BBSModClient.getFilms().stopRecording();

                if (recorder == null || recorder.hasNotStarted() || panel.getData() == null)
                {
                    return;
                }

                panel.applyRecordedKeyframes(recorder, panel.getData());
            }
            else
            {
                Replay replay = panel.replayEditor.getReplay();
                int index = panel.getData().replays.getList().indexOf(replay);

                if (index >= 0)
                {
                    getFilms().startRecording(panel.getData(), index, 0);
                }
            }
        }
    }

    private void keyOpenReplays()
    {
        UIDashboard dashboard = getDashboard();

        UIScreen.open(dashboard);

        if (dashboard.getPanels().panel instanceof UIFilmPanel panel && panel.getData() != null)
        {
            panel.preview.openReplays();
        }
        else
        {
            UIFilmPanel film = dashboard.getPanel(UIFilmPanel.class);

            if (film != null)
            {
                dashboard.setPanel(film);
            }
        }
    }

    private void keyTeleport()
    {
        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null)
        {
            panel.replayEditor.teleport();
        }
    }

    public static String getLanguageKey()
    {
        return getLanguageKey(BBSSettings.language.get());
    }

    public static String getLanguageKey(String key)
    {
        if (key.isEmpty())
        {
            /* MC 26.2: onInitializeClient runs before Minecraft.options is
             * initialized. Fall back to an empty string; L10n.reload() will
             * only load en_us until reloadLanguage() is called later with
             * the proper client language code from the options callback. */
            Minecraft mc = Minecraft.getInstance();

            if (mc != null && mc.options != null)
            {
                key = mc.options.languageCode;
            }
        }

        return key;
    }

    public static void reloadLanguage(String language)
    {
        l10n.reload(language, BBSMod.getProvider());
    }

    /**
     * Reports a failure: logs it AND surfaces it to the user through the active
     * UI's error channel (notifyError). Previously these failures were only
     * written to the log, so a broken custom model, a failed audio load or a
     * shader error was completely invisible to the player. When no menu/screen
     * is open the call degrades to logging only, so it can never affect startup
     * or crash the game.
     */
    public static void reportError(String context, Throwable t)
    {
        LOGGER.error(context, t);

        try
        {
            UIBaseMenu menu = UIScreen.getCurrentMenu();

            if (menu != null)
            {
                String detail = (t != null && t.getMessage() != null) ? t.getMessage() : "";
                menu.context.notifyError(IKey.constant("[BBS] " + context + (detail.isEmpty() ? "" : ": " + detail)));
            }
        }
        catch (Throwable ignored)
        {
            LOGGER.warn("Failed to report error to UI (reportError self-failure)", ignored);
        }
    }
}
