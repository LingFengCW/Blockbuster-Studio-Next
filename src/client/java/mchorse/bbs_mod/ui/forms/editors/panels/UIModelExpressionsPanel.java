package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.GroupOverride;
import mchorse.bbs_mod.forms.forms.ModelExpression;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Editor for {@link ModelForm} expressions (model-group visibility + transform
 * presets). Lets the user define named expressions, each made of group overrides
 * that toggle visibility and offset a group's transform. The active expression
 * is applied live during render (editor preview and world playback) by
 * {@link ModelFormRenderer}.
 */
public class UIModelExpressionsPanel extends UIFormPanel<ModelForm>
{
    private static final String NONE = "（无）";

    private String selectedExpr = "";

    public UIModelExpressionsPanel(UIForm editor)
    {
        super(editor);
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);
        this.rebuild();
    }

    private void rebuild()
    {
        this.options.removeAll();

        if (this.form == null)
        {
            return;
        }

        this.options.add(UI.label(IKey.constant("表情（模型组预设）")));

        /* Active expression selector */
        String active = this.form.activeExpression.get();
        UIButton activeBtn = new UIButton(IKey.constant("当前: " + (active.isEmpty() ? NONE : active)), (b) ->
        {
            UIListOverlayPanel list = new UIListOverlayPanel(IKey.constant("选择当前表情"), (name) ->
            {
                this.form.setActiveExpression(name == null || NONE.equals(name) ? "" : name);
                this.rebuild();
            });

            List<String> names = new ArrayList<>();

            names.add(NONE);

            for (ModelExpression ex : this.form.expressions.getAllTyped())
            {
                names.add(ex.name.get());
            }

            list.addValues(names);
            list.setValue(active.isEmpty() ? NONE : active);
            UIOverlay.addOverlay(this.getContext(), list);
        });

        this.options.add(activeBtn);

        /* Add expression */
        this.options.add(new UIButton(IKey.constant("＋ 添加表情"), (b) ->
        {
            ModelExpression ex = new ModelExpression("expr_" + this.form.expressions.getAllTyped().size());

            ex.name.set("expr_" + this.form.expressions.getAllTyped().size());
            this.form.expressions.add(ex);
            this.form.expressions.sync();
            this.selectedExpr = ex.name.get();
            this.rebuild();
        }));

        /* Per-expression rows */
        for (ModelExpression ex : this.form.expressions.getAllTyped())
        {
            boolean open = ex.name.get().equals(this.selectedExpr);

            UIButton toggle = new UIButton(IKey.constant((open ? "▾ " : "▸ ") + ex.name.get()), (b) ->
            {
                this.selectedExpr = open ? "" : ex.name.get();
                this.rebuild();
            });

            this.options.add(toggle);

            if (open)
            {
                this.buildExpressionEditor(ex);
            }
        }

        this.options.resize();
    }

    private void buildExpressionEditor(ModelExpression ex)
    {
        this.options.add(UI.label(IKey.constant("名称")));

        UITextbox rename = new UITextbox((s) ->
        {
            ex.name.set(s == null ? "" : s);
            this.rebuild();
        });

        rename.textbox.setText(ex.name.get());
        this.options.add(rename);

        this.options.add(new UIButton(IKey.constant("删除表情"), (b) ->
        {
            this.form.expressions.getAllTyped().remove(ex);
            this.form.expressions.sync();

            if (this.form.activeExpression.get().equals(ex.name.get()))
            {
                this.form.setActiveExpression("");
            }

            this.selectedExpr = "";
            this.rebuild();
        }));

        this.options.add(UI.label(IKey.constant("组覆盖")));

        this.options.add(new UIButton(IKey.constant("＋ 添加组覆盖"), (b) ->
        {
            ModelInstance model = ModelFormRenderer.getModel(this.form);
            Collection<String> groups = model == null ? Collections.emptySet() : model.model.getAllGroupKeys();

            UIListOverlayPanel list = new UIListOverlayPanel(IKey.constant("选择组"), (g) ->
            {
                if (g == null || g.isEmpty())
                {
                    return;
                }

                GroupOverride ov = new GroupOverride("ov_" + ex.groups.getAllTyped().size());

                ov.group.set(g);
                ex.groups.add(ov);
                ex.groups.sync();
                this.rebuild();
            });

            list.addValues(new ArrayList<>(groups));
            UIOverlay.addOverlay(this.getContext(), list);
        }));

        for (GroupOverride ov : ex.groups.getAllTyped())
        {
            this.options.add(UI.label(IKey.constant("组: " + ov.group.get())));

            UIToggle visible = new UIToggle(IKey.constant("显示"), ov.visible.get(), (t) -> ov.visible.set(t.getValue()));

            this.options.add(visible);

            UIPropTransform transform = new UIPropTransform();

            transform.callbacks(() -> ov.transform);
            transform.setTransform(ov.transform.get());
            this.options.add(transform);

            this.options.add(new UIButton(IKey.constant("删除覆盖"), (b) ->
            {
                ex.groups.getAllTyped().remove(ov);
                ex.groups.sync();
                this.rebuild();
            }));
        }
    }
}
