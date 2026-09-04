package mchorse.bbs_mod.ui.projects;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.function.Consumer;

/**
 * "New project" dialog: a required project name only. A project (work) does
 * NOT need a background world — only individual scenes do — so the world
 * picker was intentionally removed here. The confirm button stays disabled
 * until the name is non-empty.
 */
public class UINewProjectOverlayPanel extends UIMessageBarOverlayPanel
{
    private final UITextbox name;
    private final Consumer<String> callback;

    public UINewProjectOverlayPanel(Consumer<String> callback)
    {
        super(UIKeys.PROJECTS_CREATE, UIKeys.PROJECTS_NAME);

        this.callback = callback;

        this.name = new UITextbox(64, (s) -> this.updateConfirm());
        this.name.filename();
        this.name.placeholder(UIKeys.PROJECTS_NAME);

        this.content.add(UI.label(UIKeys.PROJECTS_NAME), this.name);

        this.confirm.setEnabled(false);
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

        this.callback.accept(name);

        super.confirm();
    }
}
