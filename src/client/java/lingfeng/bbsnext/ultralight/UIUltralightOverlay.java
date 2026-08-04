package lingfeng.bbsnext.ultralight;

import com.labymedia.ultralight.input.UltralightMouseEventButton;
import com.labymedia.ultralight.input.UltralightMouseEventType;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.keys.KeyAction;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Full-screen overlay that renders the Ultralight HTML editor onto the
 * screen. Each frame the engine is advanced and the page pixels are
 * uploaded into a vanilla DynamicTexture which is drawn through the
 * GuiGraphicsExtractor.blit() pipeline (works on GL and Vulkan backends).
 *
 * Mouse, scroll and key events are forwarded into the web view so the HTML
 * UI is fully interactive. All events are swallowed here (return true) so
 * the native BBS editor underneath does not react while the HTML editor is
 * open.
 */
public class UIUltralightOverlay extends UIElement
{
    private final UIFilmPanel panel;
    private boolean created;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;

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
            UltralightUI.createView(this.area.w, this.area.h, new EditorBridge(this.panel));
            this.created = true;
        }
    }

    /** (Re)create the view after the first time, e.g. when re-opening. */
    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible)
        {
            this.created = false;
        }
    }

    /* -------- input forwarding -------- */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        UltralightUI.fireMouse(this.mx(context), this.my(context),
            UltralightMouseEventType.DOWN,
            context.mouseButton == 1 ? UltralightMouseEventButton.RIGHT : UltralightMouseEventButton.LEFT);

        return true;
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        UltralightUI.fireMouse(this.mx(context), this.my(context),
            UltralightMouseEventType.UP,
            context.mouseButton == 1 ? UltralightMouseEventButton.RIGHT : UltralightMouseEventButton.LEFT);

        return true;
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        UltralightUI.fireScroll(context.mouseWheel * 40D);

        return true;
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        KeyAction action = context.getKeyAction();

        if (action == KeyAction.PRESSED || action == KeyAction.REPEAT)
        {
            int key = context.getKeyCode();
            char typed = context.getInputCharacter();

            UltralightUI.fireKey(key, action == KeyAction.REPEAT);

            if (typed != 0)
            {
                UltralightUI.fireChar(typed);
            }
        }

        /* Swallow everything so the native editor / keybinds don't react. */
        return true;
    }

    private int mx(UIContext context)
    {
        return (int) (context.mouseX() - this.area.x);
    }

    private int my(UIContext context)
    {
        return (int) (context.mouseY() - this.area.y);
    }

    /* -------- rendering -------- */

    @Override
    public void render(UIContext context)
    {
        if (!this.isVisible())
        {
            return;
        }

        /* Fallback: if the layout system never called resize() while the
         * overlay was visible, create the view here (render runs every
         * frame, so the HTML editor always comes up). */
        if (!this.created && this.area.w > 0 && this.area.h > 0)
        {
            UltralightUI.createView(this.area.w, this.area.h, new EditorBridge(this.panel));
            this.created = true;
        }

        /* The BBS framework has no mouse-move hook, so forward movement
         * from the render pass (once per frame is enough for hover). */
        int mx = this.mx(context);
        int my = this.my(context);

        if (mx != this.lastMouseX || my != this.lastMouseY)
        {
            this.lastMouseX = mx;
            this.lastMouseY = my;

            UltralightUI.fireMouse(mx, my, UltralightMouseEventType.MOVED, UltralightMouseEventButton.LEFT);
        }

        UltralightUI.renderFrame();

        if (UltralightUI.isReady())
        {
            try
            {
                var graphics = context.batcher.getContext();

                if (graphics != null)
                {
                    graphics.blit(
                        UltralightUI.getTextureId(),
                        this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                        0F, 1F, 0F, 1F
                    );
                }
            }
            catch (Throwable e)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
            }
        }
        else
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
            context.batcher.text("Ultralight: SDK natives 未加载 (放入 gameDir/bbsnext/ultralight/)", this.area.x + 20, this.area.y + 20, Colors.LIGHTER_GRAY, true);
        }

        super.render(context);
    }
}
