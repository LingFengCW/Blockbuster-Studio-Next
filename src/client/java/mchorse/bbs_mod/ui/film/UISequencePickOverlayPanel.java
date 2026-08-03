package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.Sequence;
import mchorse.bbs_mod.projects.SequenceManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

/** Pick one of the project's other sequences to nest into the current one. */
public class UISequencePickOverlayPanel extends UIMessageBarOverlayPanel
{
    public UISequencePickOverlayPanel(SequenceManager manager, Sequence source, Consumer<Sequence> callback)
    {
        super(IKey.raw("Nest a sequence"), UIKeys.ASSETS_TITLE);

        var list = UI.scrollView(5, 5);

        list.h(220);

        boolean any = false;

        for (Sequence candidate : manager.getSequences())
        {
            if (candidate == source)
            {
                continue;
            }

            any = true;

            UIButton button = new UIButton(IKey.raw("\uD83E\uDDE9 " + candidate.name), (b) ->
            {
                callback.accept(candidate);
                this.removeFromParent();
            });

            button.h(20);
            list.add(button);
        }

        if (!any)
        {
            var hint = UI.label(UIKeys.ASSETS_EMPTY, 18, Colors.GRAY);

            hint.h(18);
            list.add(hint);
        }

        this.content.add(list);
    }
}
