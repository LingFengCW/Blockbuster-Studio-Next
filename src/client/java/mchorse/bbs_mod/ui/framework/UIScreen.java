package mchorse.bbs_mod.ui.framework;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.importers.IImportPathProvider;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.Importers;
import mchorse.bbs_mod.importers.types.IImporter;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.IFileDropListener;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.FFMpegUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UIScreen extends Screen implements IFileDropListener
{
    private UIBaseMenu menu;
    private UIRenderingContext context;

    private int lastGuiScale;

    public static void open(UIBaseMenu menu)
    {
        Minecraft.getInstance().gui.setScreen(new UIScreen(net.minecraft.network.chat.Component.empty(), menu));
    }

    public static UIBaseMenu getCurrentMenu()
    {
        Screen currentScreen = Minecraft.getInstance().gui.screen();

        if (currentScreen instanceof UIScreen uiScreen)
        {
            return uiScreen.menu;
        }

        return null;
    }

    public UIScreen(Component title, UIBaseMenu menu)
    {
        super(title);

        Minecraft mc = Minecraft.getInstance();

        this.menu = menu;
        this.context = new UIRenderingContext(new GuiGraphicsExtractor(mc, new GuiRenderState(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()));

        this.menu.context.setup(this.context);
    }

    public UIBaseMenu getMenu()
    {
        return this.menu;
    }

    public void update()
    {
        this.menu.update();
    }

    public void renderInWorld(LevelRenderContext context)
    {
        this.menu.renderInWorld(context);
    }

    public void filesDragged(List<Path> paths)
    {

        String[] filePaths = new String[paths.size()];
        int i = 0;

        for (Path path : paths)
        {
            filePaths[i] = path.toAbsolutePath().toString();

            i += 1;
        }

        this.acceptFilePaths(filePaths);
    }

    @Override
    public void removed()
    {
        super.removed();
        this.menu.onClose(null);
    }

    public void added()
    {
        super.added();
        this.menu.onOpen(null);
    }
    public boolean shouldPause()
    {
        return this.menu.canPause();
    }

    @Override
    protected void init()
    {
        super.init();

        this.ensureTopBar();

        this.menu.resize(this.width, this.height);
    }

    /**
     * The window tool bar is now mounted at the MC window level - every
     * screen (main menu, project library, editor, vanilla menus) gets the
     * same top menu bar from BBSModClient's AFTER_INIT hook. Nothing is
     * attached per-BBS-window anymore, so this method is intentionally a
     * no-op to avoid double bars.
     */
    private void ensureTopBar()
    {}

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);

        this.menu.resize(width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inside)
    {
        if (lingfeng.bbsnext.mcef.MCEFUI.onMouseClicked(this.menu, event, inside))
        {
            return true;
        }

        return this.menu.mouseClicked((int) event.x(), (int) event.y(), event.buttonInfo().button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (lingfeng.bbsnext.mcef.MCEFUI.onMouseScrolled(this.menu, mouseX, mouseY, horizontalAmount, verticalAmount))
        {
            return true;
        }

        return this.menu.mouseScrolled((int) mouseX, (int) mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (lingfeng.bbsnext.mcef.MCEFUI.onMouseReleased(this.menu, event))
        {
            return true;
        }

        return this.menu.mouseReleased((int) event.x(), (int) event.y(), event.buttonInfo().button());
    }

    @Override
    public void mouseMoved(double x, double y)
    {
        if (!lingfeng.bbsnext.mcef.MCEFUI.onMouseMoved(this.menu, x, y))
        {
            super.mouseMoved(x, y);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (lingfeng.bbsnext.mcef.MCEFUI.onKeyPressed(this.menu, event))
        {
            return true;
        }

        return this.menu.handleKey(event.key(), event.scancode(), BBSRendering.lastAction, event.modifiers());
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        if (lingfeng.bbsnext.mcef.MCEFUI.onKeyReleased(this.menu, event))
        {
            return true;
        }

        return this.menu.handleKey(event.key(), event.scancode(), GLFW.GLFW_RELEASE, event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event)
    {
        if (lingfeng.bbsnext.mcef.MCEFUI.onCharTyped(this.menu, event))
        {
            return true;
        }

        if (!event.codepointAsString().isEmpty())
        {
            this.menu.handleTextInput(event.codepointAsString().charAt(0));
        }

        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta)
    {
        /* Outside a world there is no game backdrop behind the BBS window,
         * so paint a neutral dark surface (never the old blue gradient bar).
         * Inside a world the game itself stays visible behind the editor. */
        if (this.minecraft != null && this.minecraft.level == null)
        {
            int w = this.width;
            int h = this.height;

            context.fillGradient(0, 0, w, h, 0xFF1B2027, 0xFF0F1318);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta)
    {
        super.extractRenderState(context, mouseX, mouseY, delta);

        /* The menu's batcher is created once with a throwaway extractor in
         * the UIScreen constructor; without re-binding it to the current
         * frame's extractor here, every fill/text the menu draws goes into a
         * render state the engine never submits - so the menu looks empty
         * while the background (drawn by this method's own context) shows. */
        try
        {
            this.context.batcher.setContext(context);

            this.menu.context.setTransition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            this.menu.renderMenu(this.context, mouseX, mouseY);
            this.menu.context.render.executeRunnables();
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("BBS menu render failed in extractRenderState (" + this.menu.getClass().getSimpleName() + ")", e);
        }

        /* Self-drawn window frame. With the system decoration removed by
         * WindowDecorMixin, the window edge would otherwise be a bare strip
         * of desktop. Draw a BBS border around the whole screen - skipped in
         * fullscreen where there is no border to fake. */
        this.drawWindowFrame(context);
    }

    private void drawWindowFrame(GuiGraphicsExtractor g)
    {
        if (this.isFullscreen())
        {
            return;
        }

        int w = this.width;
        int h = this.height;
        int primary = mchorse.bbs_mod.BBSSettings.primaryColor.get();
        int b = 2;

        g.fill(0, 0, w, b, primary);
        g.fill(0, h - b, w, h, primary);
        g.fill(0, 0, b, h, primary);
        g.fill(w - b, 0, w, h, primary);
    }

    /** True when the game window fills the whole monitor (fullscreen). */
    private boolean isFullscreen()
    {
        long handle = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();

        return handle != 0L && org.lwjgl.glfw.GLFW.glfwGetWindowMonitor(handle) != 0L;
    }

    @Override
    public void acceptFilePaths(String[] paths)
    {
        if (this.menu != null)
        {
            if (!FFMpegUtils.checkFFMPEG())
            {
                this.menu.context.notifyError(UIKeys.IMPORTER_FFMPEG_NOTIFICATION);

                return;
            }

            File directory = null;
            boolean open = true;

            for (IImportPathProvider provider : this.menu.getRoot().getChildren(IImportPathProvider.class))
            {
                directory = provider.getImporterPath();

                if (directory != null)
                {
                    open = false;

                    break;
                }
            }

            List<File> files = new ArrayList<>();

            for (String path : paths)
            {
                File file = new File(path);

                if (file.exists())
                {
                    files.add(file);
                }
            }

            ImporterContext context = new ImporterContext(files, directory);

            for (IImporter importer : Importers.getImporters())
            {
                if (importer.canImport(context))
                {
                    importer.importFiles(context);

                    if (open)
                    {
                        UIUtils.openFolder(context.getDestination(importer));
                    }

                    this.menu.context.notifySuccess(UIKeys.IMPORTER_SUCCESS_NOTIFICATION.format(importer.getName()));

                    return;
                }
            }
        }
    }
}


