package mchorse.bbs_mod.ui.projects;

import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "New character" dialog: a name field, a single-choice form type picker
 * (mob / model / particle / block) and a multi-choice list of extra
 * options (actor replay role, shadow, looping). The picked type creates
 * the matching empty form on the replay.
 */
public class UINewEntityOverlayPanel extends UIMessageBarOverlayPanel
{
    private final UITextbox name;
    private final Consumer<Result> callback;
    private final List<UIButton> typeButtons = new ArrayList<>();
    private final List<UIButton> optionButtons = new ArrayList<>();

    private FormType selectedType = FormType.MOB;
    private boolean actor = false;
    private boolean shadow = true;
    private boolean looping = false;

    public enum FormType
    {
        MOB(UIKeys.ENTITY_TYPE_MOB, () -> new MobForm()),
        MODEL(UIKeys.ENTITY_TYPE_MODEL, () -> new ModelForm()),
        PARTICLE(UIKeys.ENTITY_TYPE_PARTICLE, () -> new ParticleForm()),
        BLOCK(UIKeys.ENTITY_TYPE_BLOCK, () -> new BlockForm());

        public final IKey label;
        public final java.util.function.Supplier<Form> factory;

        FormType(IKey label, java.util.function.Supplier<Form> factory)
        {
            this.label = label;
            this.factory = factory;
        }
    }

    public static class Result
    {
        public final String name;
        public final FormType type;
        public final boolean actor;
        public final boolean shadow;
        public final boolean looping;

        public Result(String name, FormType type, boolean actor, boolean shadow, boolean looping)
        {
            this.name = name;
            this.type = type;
            this.actor = actor;
            this.shadow = shadow;
            this.looping = looping;
        }
    }

    public UINewEntityOverlayPanel(Consumer<Result> callback)
    {
        super(UIKeys.ASSETS_NEW_CHARACTER, UIKeys.ASSETS_NEW_CHARACTER);

        this.callback = callback;

        this.name = new UITextbox(64, (s) -> {});
        this.name.filename();
        this.name.placeholder(UIKeys.ASSETS_NEW_CHARACTER);

        /* Single-choice type row. */
        var typeLabel = UI.label(UIKeys.ENTITY_TYPE, 20, Colors.LIGHTER_GRAY);
        typeLabel.h(20);

        var typeRow = UI.row(5);
        typeRow.h(20);

        for (FormType type : FormType.values())
        {
            UIButton button = new UIButton(type.label, (b) -> this.selectType(type));

            button.h(20);
            this.typeButtons.add(button);
            typeRow.add(button);
        }

        /* Multi-choice option row. */
        var optionLabel = UI.label(UIKeys.ENTITY_OPTIONS, 20, Colors.LIGHTER_GRAY);
        optionLabel.h(20);

        var optionRow = UI.row(5);
        optionRow.h(20);

        UIButton actorBtn = new UIButton(UIKeys.ENTITY_ACTOR, (b) -> { this.actor = !this.actor; this.refreshButtons(); });
        UIButton shadowBtn = new UIButton(UIKeys.ENTITY_SHADOW, (b) -> { this.shadow = !this.shadow; this.refreshButtons(); });
        UIButton loopBtn = new UIButton(UIKeys.ENTITY_LOOP, (b) -> { this.looping = !this.looping; this.refreshButtons(); });

        actorBtn.h(20);
        shadowBtn.h(20);
        loopBtn.h(20);
        this.optionButtons.add(actorBtn);
        this.optionButtons.add(shadowBtn);
        this.optionButtons.add(loopBtn);
        optionRow.add(actorBtn, shadowBtn, loopBtn);

        this.content.add(UI.label(UIKeys.ASSETS_NEW_CHARACTER), this.name, typeLabel, typeRow, optionLabel, optionRow);

        this.refreshButtons();
    }

    private void selectType(FormType type)
    {
        this.selectedType = type;
        this.refreshButtons();
    }

    private void refreshButtons()
    {
        for (int i = 0; i < this.typeButtons.size(); i++)
        {
            this.typeButtons.get(i).color(this.selectedType == FormType.values()[i] ? Colors.A50 | Colors.ACTIVE : 0);
        }

        this.optionButtons.get(0).color(this.actor ? Colors.A50 | Colors.ACTIVE : 0);
        this.optionButtons.get(1).color(this.shadow ? Colors.A50 | Colors.ACTIVE : 0);
        this.optionButtons.get(2).color(this.looping ? Colors.A50 | Colors.ACTIVE : 0);
    }

    @Override
    public void confirm()
    {
        String name = this.name.getText().trim();

        this.callback.accept(new Result(name, this.selectedType, this.actor, this.shadow, this.looping));

        super.confirm();
    }
}
