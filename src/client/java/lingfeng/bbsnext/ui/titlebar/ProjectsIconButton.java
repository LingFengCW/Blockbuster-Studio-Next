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
        /* 26.2 的 SpriteIconButton 构造器参数是:
         * (x, y, message, spriteWidth, spriteHeight, spriteOffsetX,
         *  spriteOffsetY, sprites, onPress, tooltip, narration, flag)
         * 注意: 没有 width/height 参数位 —— 父类构造时宽高固定为 0,
         * 必须构造后显式设置, 否则按钮渲染异常(全屏/错位). */
        super(x, y, Component.empty(), 16, 16, 0, 0,
            new WidgetSprites(ICON, ICON, ICON),
            (b) -> mchorse.bbs_mod.BBSModClient.openEditorFlow(null),
            Component.empty(),
            (btn) -> Component.empty(),
            true);

        this.width = 16;
        this.height = 16;
    }
}
