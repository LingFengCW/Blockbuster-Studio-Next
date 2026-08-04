package mchorse.bbs_mod.ultralight;

import com.labymedia.ultralight.input.UltralightMouseEventButton;
import com.labymedia.ultralight.input.UltralightMouseEventType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Full-screen overlay that renders the Ultralight HTML editor onto the
 * screen. Each frame it advances the engine, uploads the frame into a GL
 * texture and blits it through BBS's batcher; mouse events are forwarded
 * into the web view so the HTML UI is fully interactive.
 */
public class UIUltralightOverlay extends UIElement
{
    private final UIFilmPanel panel;
    private boolean created;

    public UIUltralightOverlay(UIFilmPanel panel)
    {
        this.panel = panel;
        this.markContainer();
    }

    @Override
    public void resize()
    {
        super.resize();

        if (this.isVisible() && this.area.w > 0 && this.area.h > 0 && !this.created)
        {
            UltralightUI.createView(this.area.w, this.area.h);
            this.created = true;
        }
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        UltralightUI.fireMouse(context.mouseX() - this.area.x, context.mouseY() - this.area.y,
            UltralightMouseEventType.DOWN,
            context.mouseButton == 1 ? UltralightMouseEventButton.RIGHT : UltralightMouseEventButton.LEFT);

        return true;
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        UltralightUI.fireMouse(context.mouseX() - this.area.x, context.mouseY() - this.area.y,
            UltralightMouseEventType.UP,
            context.mouseButton == 1 ? UltralightMouseEventButton.RIGHT : UltralightMouseEventButton.LEFT);

        return true;
    }

    @Override
    public void render(UIContext context)
    {
        if (!this.isVisible())
        {
            return;
        }

        UltralightUI.render();

        if (UltralightUI.isReady())
        {
            context.batcher.texturedBox(
                UltralightUI.getTexture(), Colors.WHITE,
                this.area.x, this.area.y, this.area.w, this.area.h,
                0, 0, 1, 1, 1, 1
            );
        }
        else
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
            context.batcher.text("Ultralight: SDK natives 未加载 (放入 gameDir/bbsnext/ultralight/)", this.area.x + 20, this.area.y + 20, Colors.LIGHTER_GRAY, true);
        }

        super.render(context);
    }
}
