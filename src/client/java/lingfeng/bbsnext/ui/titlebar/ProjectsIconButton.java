package lingfeng.bbsnext.ui.titlebar;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Title-screen projects button: a 16x16 vanilla
 * {@link SpriteIconButton.CenteredIcon} fed by a GUI-atlas sprite
 * (textures/gui/sprites/ loads it automatically - sprite id
 * bbs:projects_icon, no manual texture registration needed).
 */
public class ProjectsIconButton extends SpriteIconButton.CenteredIcon
{
    public static final Identifier ICON = Identifier.fromNamespaceAndPath("bbs", "projects_icon");

    public ProjectsIconButton(int x, int y)
    {
        /* 16x16 button, 12x12 icon. */
        super(x, y, Component.empty(), 16, 16, 12, 12,
            new WidgetSprites(ICON, ICON, ICON),
            (b) -> mchorse.bbs_mod.BBSModClient.openEditorFlow(null),
            Component.empty(),
            (btn) -> Component.empty(),
            true);
    }
}
