package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.function.Consumer;

/**
 * "Import from this project" dialog: pick one of the project's existing
 * scenes (each holds a film with its recorded replays) to use as an asset.
 * Picking a scene confirms and closes the dialog.
 */
public class UIScenePickOverlayPanel extends UIMessageBarOverlayPanel
{
    public UIScenePickOverlayPanel(Consumer<Scene> callback)
    {
        super(UIKeys.ASSETS_IMPORT, UIKeys.ASSETS_PROJECT);

        var list = UI.scrollView(5, 5);

        list.h(220);

        SceneManager scenes = SceneManager.get();

        if (scenes != null && !scenes.getScenes().isEmpty())
        {
            for (Scene scene : scenes.getScenes())
            {
                UIButton button = new UIButton(IKey.raw(scene.name), (b) ->
                {
                    callback.accept(scene);
                    this.removeFromParent();
                });

                button.h(20);
                list.add(button);
            }
        }
        else
        {
            var hint = UI.label(UIKeys.ASSETS_EMPTY, 18, mchorse.bbs_mod.utils.colors.Colors.GRAY);

            hint.h(18);
            list.add(hint);
        }

        this.content.add(list);
    }
}
