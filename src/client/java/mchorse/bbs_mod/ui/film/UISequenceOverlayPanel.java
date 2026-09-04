package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.Scene;
import mchorse.bbs_mod.projects.SceneManager;
import mchorse.bbs_mod.projects.Sequence;
import mchorse.bbs_mod.projects.SequenceManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Nested sequence link manager (design doc 3.3, tab 3): shows every
 * reference of a sequence (scene / child sequence / audio), lets the user
 * remove ("break the link") or add references. Adding a child sequence runs
 * the cycle and depth guards and reports them inline when rejected.
 */
public class UISequenceOverlayPanel extends UIMessageBarOverlayPanel
{
    private final Sequence sequence;
    private final SequenceManager manager;
    private final mchorse.bbs_mod.ui.framework.elements.UIScrollView refs;
    private final mchorse.bbs_mod.ui.framework.elements.utils.UILabel status;

    public UISequenceOverlayPanel(Sequence sequence)
    {
        super(IKey.raw(sequence.name), UIKeys.ASSETS_TITLE);

        this.sequence = sequence;
        this.manager = SequenceManager.get();

        UIButton addScene = new UIButton(IKey.raw("+ " + UIKeys.SEQUENCE_ADD_SCENE.get()), (b) -> this.addSceneRef());
        UIButton addSeq = new UIButton(IKey.raw("+ " + UIKeys.SEQUENCE_ADD_SEQUENCE.get()), (b) -> this.addSequenceRef());

        var bar = UI.row(4);

        bar.h(20);
        bar.add(addScene, addSeq);

        this.refs = UI.scrollView(5, 5);
        this.refs.h(200);

        this.status = UI.label(IKey.raw(""), 18, Colors.LIGHTER_GRAY);
        this.status.h(18);

        this.content.add(bar, this.refs, this.status);

        this.refreshRows();
    }

    private void refreshRows()
    {
        this.refs.removeAll();

        for (Sequence.SequenceRef ref : this.sequence.refs)
        {
            String type = switch (ref.type)
            {
                case Sequence.SequenceRef.SCENE -> UIKeys.SEQUENCE_REF_SCENE.get();
                case Sequence.SequenceRef.SEQUENCE -> UIKeys.SEQUENCE_REF_SEQUENCE.get();
                case Sequence.SequenceRef.AUDIO -> UIKeys.SEQUENCE_REF_AUDIO.get();
                default -> UIKeys.SEQUENCE_REF_UNKNOWN.get();
            };
            String trim = ref.in >= 0 ? "  [" + ref.in + ".." + ref.out + "]" : "";
            String label = type + " " + this.resolveName(ref) + trim;

            UIButton row = new UIButton(IKey.raw(label), (b) -> {});

            row.h(20);
            row.context((menu) -> menu.action(Icons.TRASH, UIKeys.SEQUENCE_BREAK_LINK, () ->
            {
                this.manager.removeRef(this.sequence, ref);
                this.refreshRows();
            }));
            this.refs.add(row);
        }

        if (this.sequence.refs.isEmpty())
        {
            var hint = UI.label(UIKeys.ASSETS_EMPTY, 18, Colors.GRAY);

            hint.h(18);
            this.refs.add(hint);
        }

        this.refs.resize();
    }

    private String resolveName(Sequence.SequenceRef ref)
    {
        if (Sequence.SequenceRef.SCENE.equals(ref.type))
        {
            SceneManager scenes = SceneManager.get();
            Scene scene = scenes == null ? null : scenes.getById(ref.id);

            return scene == null ? ref.id : scene.name;
        }

        if (Sequence.SequenceRef.SEQUENCE.equals(ref.type))
        {
            Sequence child = this.manager == null ? null : this.manager.getById(ref.id);

            return child == null ? ref.id : child.name;
        }

        return ref.id;
    }

    private void setStatus(String text, int color)
    {
        this.status.label = IKey.raw(text);
        this.status.color(color);
    }

    private void addSceneRef()
    {
        if (SceneManager.get() == null)
        {
            return;
        }

        UIOverlayOverlay.show(this, new UIScenePickOverlayPanel((scene) ->
        {
            this.manager.addRef(this.sequence, Sequence.SequenceRef.SCENE, scene.id);
            this.refreshRows();
            this.setStatus("Scene \u2713", Colors.LIGHTER_GRAY);
        }));
    }

    private void addSequenceRef()
    {
        if (this.manager == null)
        {
            return;
        }

        var pick = new UISequencePickOverlayPanel(this.manager, this.sequence, (child) ->
        {
            if (this.manager.hasCycle(this.sequence, child.id))
            {
                this.setStatus("Cycle detected - rejected", Colors.RED);

                return;
            }

            if (this.manager.depthOf(child.id) >= SequenceManager.MAX_DEPTH)
            {
                this.setStatus("Nesting depth limit (" + SequenceManager.MAX_DEPTH + ") reached", Colors.RED);

                return;
            }

            this.manager.addRef(this.sequence, Sequence.SequenceRef.SEQUENCE, child.id);
            this.refreshRows();
            this.setStatus("Sequence linked \u2713", Colors.LIGHTER_GRAY);
        });

        UIOverlayOverlay.show(this, pick);
    }

    /** Small helper: push another overlay panel above the current one. */
    private static class UIOverlayOverlay
    {
        static void show(mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel host, mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel next)
        {
            mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay.addOverlay(host.getContext(), next);
        }
    }
}
