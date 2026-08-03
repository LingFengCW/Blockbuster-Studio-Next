package mchorse.bbs_mod.mixin;

import java.lang.reflect.Method;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.projects.UIProjectMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inserts the 【项目】 button at the very top of the vanilla title screen
 * button column, right above "Singleplayer". TitleScreen's layoutTable
 * ignores widgets added via AFTER_INIT, so this mixin hooks the actual
 * button construction path.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin
{
    /* Screen#addRenderableWidget is protected; resolve it reflectively once so
     * the mixin can add widgets without needing the title screen to expose
     * the method. The erased descriptor parameter type is GuiEventListener
     * (leftmost bound on T extends GuiEventListener & Renderable). */
    private static final Method ADD_RENDERABLE;

    static
    {
        Method m = null;

        try
        {
            m = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            m.setAccessible(true);
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Could not resolve Screen.addRenderableWidget", e);
        }

        ADD_RENDERABLE = m;
    }

    @Inject(method = "createNormalMenuOptions", at = @At("HEAD"))
    private void bbs$injectProjectButton(int topPos, int spacing, CallbackInfoReturnable<Integer> ci)
    {
        TitleScreen self = (TitleScreen) (Object) this;

        Button button = Button.builder(
            Component.literal(UIKeys.MAIN_MENU_PROJECTS.get()),
            (b) -> bbs$openModifiedDashboard()
        ).bounds(self.width / 2 - 100, topPos - 24, 200, 20).build();

        try
        {
            ADD_RENDERABLE.invoke(self, button);
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to insert 【项目】 button on title screen", e);
        }
    }

    /**
     * The main-menu 【项目】 entry opens the modified editor (UIDashboard)
     * directly. A current project is ensured (first existing one, or a new
     * default project) and a default scene is seeded so the dashboard opens
     * without error toasts - further scenes are created in the asset bin.
     */
    private static void bbs$openModifiedDashboard()
    {
        try
        {
            mchorse.bbs_mod.projects.ProjectManager projects = mchorse.bbs_mod.projects.ProjectManager.get();
            mchorse.bbs_mod.projects.BBSProject project = projects == null ? null : projects.getCurrent();

            if (project == null && projects != null)
            {
                java.util.List<mchorse.bbs_mod.projects.BBSProject> list = projects.getProjects();

                project = list.isEmpty() ? projects.create("My Project") : list.get(0);
                projects.setCurrent(project);
            }

            if (project != null)
            {
                mchorse.bbs_mod.projects.SceneManager scenes = mchorse.bbs_mod.projects.SceneManager.get();

                if (scenes != null && scenes.getCurrent() == null)
                {
                    scenes.setCurrent(scenes.create("Scene 1"));
                }
            }

            mchorse.bbs_mod.ui.framework.UIScreen.open(new mchorse.bbs_mod.ui.dashboard.UIDashboard());
        }
        catch (Exception e)
        {
            mchorse.bbs_mod.BBSMod.LOGGER.error("Failed to open the modified dashboard", e);
        }
    }
}