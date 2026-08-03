package mchorse.bbs_mod.ui.titlebar;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.projects.UIProjectMenu;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The 【项目】 entry button on the vanilla title screen.
 *
 * Per the PR-style redesign: a plain flat button, 180x24, sitting at the
 * very top of the vanilla button column (singleplayer / multiplayer /
 * realms / options / quit). It opens the project library without loading
 * any world data. Colours follow the design doc: #333333 idle, #555555
 * hovered, #222222 pressed, white pixel-ish text.
 */
public class TitleProjectButton extends AbstractButton
{
    private static final int COLOR_IDLE = 0xFF333333;
    private static final int COLOR_HOVER = 0xFF555555;
    private static final int COLOR_PRESSED = 0xFF222222;

    public TitleProjectButton(int x, int y)
    {
        super(x, y, 180, 24, Component.literal(UIKeys.MAIN_MENU_PROJECTS.get()));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a)
    {
        int color = this.isHovered() ? COLOR_HOVER : COLOR_IDLE;

        g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);

        String label = this.getMessage().getString();

        g.text(Minecraft.getInstance().font, label,
            this.getX() + (this.getWidth() - Minecraft.getInstance().font.width(label)) / 2,
            this.getY() + (this.getHeight() - Minecraft.getInstance().font.lineHeight) / 2,
            Colors.WHITE);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick)
    {
        UIScreen.open(new UIProjectMenu());
    }

    @Override
    public void onPress(InputWithModifiers input)
    {}

    @Override
    public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output)
    {
        this.defaultButtonNarrationText(output);
    }
}
