package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.utils.context.ContextAction;

public class UISimpleContextMenu extends UIContextMenu
{
    public UIList<ContextAction> actions;

    private ContextAction action;

    private UISimpleContextMenu subMenu;
    private UISimpleContextMenu parentMenu;

    public UISimpleContextMenu()
    {
        super();

        this.actions = new UIActionList((action) ->
        {
            if (action.get(0).runnable != null)
            {
                this.action = action.get(0);
            }
        });

        this.actions.cancelScrollEdge().full(this);
        this.add(this.actions);
    }

    public void setParentMenu(UISimpleContextMenu parentMenu)
    {
        this.parentMenu = parentMenu;
    }

    @Override
    public boolean isEmpty()
    {
        return this.actions.getList().isEmpty();
    }

    @Override
    public void setMouse(UIContext context)
    {
        int w = 100;

        for (ContextAction action : this.actions.getList())
        {
            w = Math.max(action.getWidth(context.batcher.getFont()), w);
        }

        this.set(context.mouseX(), context.mouseY(), w, 0).h(this.actions.scroll.scrollSize).maxH(context.menu.height - 10).bounds(context.menu.overlay, 5);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        this.updateSubMenu(context);
    }

    /** Open/close the nested sub-menu while hovering the parent items. */
    private void updateSubMenu(UIContext context)
    {
        ContextAction hovered = null;
        int hoverIndex = -1;

        if (this.actions.area.isInside(context))
        {
            hoverIndex = this.actions.getHoveredIndex(context);

            if (hoverIndex >= 0 && hoverIndex < this.actions.getList().size())
            {
                ContextAction candidate = this.actions.getList().get(hoverIndex);

                if (candidate.hasSubMenu())
                {
                    hovered = candidate;
                }
            }
        }

        if (hovered == null)
        {
            if (this.subMenu != null)
            {
                this.subMenu.removeFromParent();
                this.subMenu = null;
            }

            return;
        }

        boolean rebuild = this.subMenu == null || this.subMenu.actions.getList() != hovered.subActions;

        if (rebuild)
        {
            if (this.subMenu != null)
            {
                this.subMenu.removeFromParent();
            }

            UISimpleContextMenu sub = new UISimpleContextMenu();

            sub.setParentMenu(this);
            sub.actions.getList().addAll(hovered.subActions);
            sub.set(this.area.ex() + 3, this.area.y + 3 + hoverIndex * 20, 150, 0).h(hovered.subActions.size() * 20).bounds(context.menu.overlay, 5);
            sub.resize();

            context.menu.overlay.add(sub);

            this.subMenu = sub;
        }
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.action != null)
        {
            this.action.runnable.run();
            this.removeFromParent();

            /* Close the whole chain (parent menus) once a leaf was picked. */
            if (this.parentMenu != null)
            {
                this.parentMenu.removeFromParent();
            }

            return true;
        }

        return super.subMouseReleased(context);
    }

    public void pick(int index)
    {
        this.actions.setIndex(index);

        ContextAction action = this.actions.getCurrentFirst();

        if (action != null && action.runnable != null)
        {
            action.runnable.run();
            this.removeFromParent();

            if (this.parentMenu != null)
            {
                this.parentMenu.removeFromParent();
            }
        }
    }
}
