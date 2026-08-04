package lingfeng.bbsnext.ui.titlebar;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Title-screen projects button, built exactly like Mod Menu's mods button:
 * through the public {@link SpriteIconButton.Builder} API. 26.2's raw
 * SpriteIconButton constructor takes (width, height, ...) as its first two
 * params (x/y are hardcoded to 0 internally), so we go through the builder
 * (width/height/sprite-size are explicit there) and position the result
 * with setPosition().
 *
 * The PNG lives in textures/gui/sprites/, so the GUI atlas loads it
 * automatically (sprite id = bbs:projects_icon). A fixed message string is
 * used purely as an identity marker so the screen hook can detect whether
 * this button was already added on re-init.
 */
public final class ProjectsIconButton
{
    public static final Identifier ICON = Identifier.fromNamespaceAndPath("bbs", "projects_icon");

    public static final String MARKER = "bbs-works-button";

    private ProjectsIconButton()
    {
    }

    public static SpriteIconButton create(int x, int y)
    {
        /* Step-by-step builder calls (chained .size().sprite() loses the
         * builder type through the generic parent, so no chaining).
         * The 2-arg builder() returns Button.Builder; the 3-arg overload
         * (iconOnly=true -> CenteredIcon) returns SpriteIconButton.Builder. */
        SpriteIconButton.Builder builder = SpriteIconButton.builder(
            Component.literal(MARKER),
            (b) -> mchorse.bbs_mod.BBSModClient.openEditorFlow(null),
            true
        );

        builder.size(16, 16);
        builder.sprite(new WidgetSprites(ICON, ICON, ICON), 16, 16);

        SpriteIconButton button = builder.build();

        button.setPosition(x, y);

        return button;
    }

    public static boolean isProjectsButton(net.minecraft.client.gui.components.AbstractWidget widget)
    {
        return widget instanceof SpriteIconButton
            && MARKER.equals(widget.getMessage().getString());
    }
}
