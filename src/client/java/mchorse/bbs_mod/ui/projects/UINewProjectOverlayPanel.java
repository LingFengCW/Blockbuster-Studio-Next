package mchorse.bbs_mod.ui.projects;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * "New project" dialog: a required project name plus a background world
 * picker (blank world / any local singleplayer save in .minecraft/saves).
 * The confirm button stays disabled until the name is non-empty, per the
 * PR-style design doc.
 */
public class UINewProjectOverlayPanel extends UIMessageBarOverlayPanel
{
    private final UITextbox name;
    private final BiConsumer<String, String> callback;
    private final List<UIButton> worldButtons = new ArrayList<>();
    private String selectedWorld = "";

    public UINewProjectOverlayPanel(BiConsumer<String, String> callback)
    {
        super(UIKeys.PROJECTS_CREATE, UIKeys.PROJECTS_NAME);

        this.callback = callback;

        this.name = new UITextbox(64, (s) -> this.updateConfirm());
        this.name.filename();
        this.name.placeholder(UIKeys.PROJECTS_NAME);

        var worldLabel = UI.label(UIKeys.PROJECTS_WORLD, 20, Colors.LIGHTER_GRAY);
        worldLabel.h(20);

        var worldScroll = UI.scrollView(5, 5);
        worldScroll.h(110);

        /* Blank world option first, then every local singleplayer save. */
        this.addWorld(worldScroll, UIKeys.PROJECTS_WORLD_EMPTY.get(), "");

        File saves = new File(Minecraft.getInstance().gameDirectory, "saves");

        if (saves.isDirectory())
        {
            File[] dirs = saves.listFiles(File::isDirectory);

            if (dirs != null)
            {
                for (File dir : dirs)
                {
                    if (new File(dir, "level.dat").exists())
                    {
                        this.addWorld(worldScroll, dir.getName(), dir.getName());
                    }
                }
            }
        }

        this.content.add(UI.label(UIKeys.PROJECTS_NAME), this.name, worldLabel, worldScroll);

        this.confirm.setEnabled(false);
    }

    private void addWorld(UIElement scroll, String label, String id)
    {
        UIButton button = new UIButton(IKey.raw(label), (b) ->
        {
            this.selectedWorld = id;

            for (UIButton wb : this.worldButtons)
            {
                wb.color(wb == b ? Colors.A50 | Colors.ACTIVE : 0);
            }
        });

        button.h(20);
        this.worldButtons.add(button);
        scroll.add(button);
    }

    private void updateConfirm()
    {
        this.confirm.setEnabled(!this.name.getText().trim().isEmpty());
    }

    @Override
    public void confirm()
    {
        String name = this.name.getText().trim();

        if (name.isEmpty())
        {
            return;
        }

        this.callback.accept(name, this.selectedWorld);

        super.confirm();
    }
}
