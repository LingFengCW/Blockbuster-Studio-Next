package mchorse.bbs_mod.ui.projects;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.projects.Backpack;
import mchorse.bbs_mod.projects.BBSProject;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backpack browser: shows every item in the cross-project asset library.
 * Allows exporting the current project's form categories into the
 * backpack, importing an item into the current project, and deleting items.
 */
public class UIBackpackMenu extends UIBaseMenu
{
    private UIScrollView list;
    private UIButton importBtn;
    private UIButton deleteBtn;

    private String selected;
    private final Map<String, UIButton> buttons = new HashMap<>();

    public UIBackpackMenu()
    {
        UIElement column = UI.column(10);
        column.relative(this.main).xy(0.5F, 0.5F).w(420).h(460).anchor(0.5F, 0.5F);

        column.add(UI.label(UIKeys.BACKPACK_TITLE, 28));

        this.list = UI.scrollView(5, 5);
        this.list.h(340);
        column.add(this.list);

        UIButton exportBtn = new UIButton(UIKeys.BACKPACK_EXPORT, (b) -> this.exportAll());
        this.importBtn = new UIButton(UIKeys.BACKPACK_IMPORT, (b) -> this.importSelected());
        this.deleteBtn = new UIButton(UIKeys.BACKPACK_DELETE, (b) -> this.deleteSelected());

        this.importBtn.setEnabled(false);
        this.deleteBtn.setEnabled(false);

        UIElement buttons = UI.row(5);
        buttons.h(24);
        UIButton backBtn = new UIButton(UIKeys.SCENES_BACK, (b) -> UIScreen.open(new UIProjectMenu()));

        buttons.add(exportBtn, this.importBtn, this.deleteBtn, backBtn);

        column.add(buttons);

        this.main.add(column);

        this.refresh();
    }

    private void refresh()
    {
        this.buttons.clear();
        this.list.removeAll();

        java.util.List<String> items = Backpack.getItems();

        if (items.isEmpty())
        {
            UIElement label = UI.label(UIKeys.BACKPACK_EMPTY, 20);

            label.h(20);
            this.list.add(label);
        }

        for (String item : items)
        {
            UIButton button = new UIButton(IKey.constant(item), (b) -> this.select(item));

            button.h(24);
            this.buttons.put(item, button);
            this.list.add(button);
        }

        this.list.resize();
        this.select(null);
    }

    private void select(String item)
    {
        this.selected = item;

        for (Map.Entry<String, UIButton> entry : this.buttons.entrySet())
        {
            entry.getValue().color(entry.getKey().equals(item) ? Colors.A50 | Colors.ACTIVE : 0);
        }

        this.importBtn.setEnabled(item != null && ProjectManager.get().getCurrent() != null);
        this.deleteBtn.setEnabled(item != null);
    }

    private void exportAll()
    {
        BBSProject project = ProjectManager.get().getCurrent();

        if (project == null)
        {
            return;
        }

        List<String> errors = Backpack.exportCategories(project);

        if (errors.isEmpty())
        {
            this.context.notifySuccess(IKey.constant("Exported current project to backpack"));
        }
        else
        {
            for (String error : errors)
            {
                this.context.notifyError(IKey.constant(error));
            }
        }

        this.refresh();
    }

    private void importSelected()
    {
        BBSProject project = ProjectManager.get().getCurrent();

        if (project == null || this.selected == null)
        {
            return;
        }

        List<String> errors = Backpack.importCategory(project, this.selected);

        if (errors.isEmpty())
        {
            this.context.notifySuccess(IKey.constant("Imported '" + this.selected + "' into current project"));
        }
        else
        {
            for (String error : errors)
            {
                this.context.notifyError(IKey.constant(error));
            }
        }

        this.refresh();
    }

    private void deleteSelected()
    {
        if (this.selected == null)
        {
            return;
        }

        String item = this.selected;

        UIOverlay.addOverlay(this.context, new UIConfirmOverlayPanel(
            UIKeys.BACKPACK_DELETE,
            IKey.constant(item),
            (result) ->
            {
                if (result)
                {
                    Backpack.deleteItem(item);
                    this.refresh();
                }
            }
        ));
    }

    public static void open()
    {
        UIScreen.open(new UIBackpackMenu());
    }
}
