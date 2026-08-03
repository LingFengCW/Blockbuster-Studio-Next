package mchorse.bbs_mod.ui.animation;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/**
 * Curve canvas: draws one KeyframeChannel as a graph with draggable
 * keyframe points and bezier handles. Click empty space to add a keyframe,
 * drag a point vertically to change its value / horizontally to change its
 * tick, drag the bezier handles to shape the curve between keyframes.
 */
public class UICurveCanvas extends UIElement
{
    public KeyframeChannel<Float> channel;

    public Keyframe<Float> selected;

    private boolean draggingPoint;
    private boolean draggingHandle;
    private boolean draggingLeft;

    public UICurveCanvas()
    {
        this.channel = null;
    }

    public void setChannel(KeyframeChannel<Float> channel)
    {
        this.channel = channel;
        this.selected = null;
    }

    private float minX()
    {
        return 0F;
    }

    private float maxX()
    {
        if (this.channel == null || this.channel.isEmpty())
        {
            return 100F;
        }

        return (float) Math.max(100F, this.channel.getLength());
    }

    private float minY()
    {
        return -180F;
    }

    private float maxY()
    {
        return 180F;
    }

    private float toX(float tick)
    {
        return this.area.x + 30 + (tick - this.minX()) / (this.maxX() - this.minX()) * (this.area.w - 60);
    }

    private float toY(float value)
    {
        return this.area.ey() - 20 - (value - this.minY()) / (this.maxY() - this.minY()) * (this.area.h - 40);
    }

    private float fromX(float x)
    {
        return (x - this.area.x - 30) / Math.max(1, this.area.w - 60) * (this.maxX() - this.minX()) + this.minX();
    }

    private float fromY(float y)
    {
        return (this.area.ey() - 20 - y) / Math.max(1, this.area.h - 40) * (this.maxY() - this.minY()) + this.minY();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.channel == null || this.channel.isEmpty())
        {
            return super.subMouseClicked(context);
        }

        if (this.area.isInside(context.mouseX, context.mouseY))
        {
            float x = context.mouseX;
            float y = context.mouseY;

            /* Try to hit a bezier handle first, then a keyframe point. */
            for (Keyframe<Float> keyframe : this.channel.getKeyframes())
            {
                float kx = this.toX(keyframe.getTick());
                float ky = this.toY(keyframe.getValue());

                float hx = kx + keyframe.rx * 4F;
                float hy = ky - keyframe.ry * 4F;

                if (Math.abs(x - hx) < 6 && Math.abs(y - hy) < 6)
                {
                    this.selected = keyframe;
                    this.draggingHandle = true;
                    this.draggingLeft = false;

                    return true;
                }

                hx = kx - keyframe.lx * 4F;
                hy = ky + keyframe.ly * 4F;

                if (Math.abs(x - hx) < 6 && Math.abs(y - hy) < 6)
                {
                    this.selected = keyframe;
                    this.draggingHandle = true;
                    this.draggingLeft = true;

                    return true;
                }
            }

            for (Keyframe<Float> keyframe : this.channel.getKeyframes())
            {
                float kx = this.toX(keyframe.getTick());
                float ky = this.toY(keyframe.getValue());

                if (Math.abs(x - kx) < 6 && Math.abs(y - ky) < 6)
                {
                    this.selected = keyframe;
                    this.draggingPoint = true;

                    return true;
                }
            }

            /* Empty space: add a keyframe at the clicked position. */
            float tick = this.fromX(x);
            float value = this.fromY(y);

            int index = this.channel.insert(tick, value);

            this.selected = this.channel.get(index);
            this.draggingPoint = true;

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.draggingPoint = false;
        this.draggingHandle = false;

        return super.subMouseReleased(context);
    }

    private void processDrag(UIContext context)
    {
        if (this.selected == null || this.channel == null)
        {
            return;
        }

        if (this.draggingPoint)
        {
            float tick = Math.max(0F, this.fromX(context.mouseX));
            float value = this.fromY(context.mouseY);

            this.selected.setTick(tick);
            this.selected.setValue(value);
            this.channel.sort();
        }
        else if (this.draggingHandle)
        {
            float kx = this.toX(this.selected.getTick());
            float ky = this.toY(this.selected.getValue());

            float dx = (context.mouseX - kx) / 4F;
            float dy = (ky - context.mouseY) / 4F;

            if (this.draggingLeft)
            {
                this.selected.lx = Math.max(0F, -dx);
                this.selected.ly = dy;
            }
            else
            {
                this.selected.rx = Math.max(0F, dx);
                this.selected.ry = dy;
            }
        }
    }

    @Override
    public void render(UIContext context)
    {
        /* Drag handling: the framework has no mouse-moved event, so
         * dragging is applied every frame while the button is held. */
        this.processDrag(context);

        super.render(context);

        if (this.channel == null)
        {
            return;
        }

        /* Background */
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50 | 0x151515);

        /* Zero line */
        float zy = this.toY(0F);

        context.batcher.box(this.area.x, zy, this.area.ex(), zy + 1, Colors.A50 | Colors.ACTIVE);

        if (this.channel.isEmpty())
        {
            return;
        }

        /* Curve segments (bezier between keyframes) */
        java.util.List<Keyframe<Float>> keyframes = this.channel.getKeyframes();

        for (int i = 0; i < keyframes.size() - 1; i++)
        {
            Keyframe<Float> a = keyframes.get(i);
            Keyframe<Float> b = keyframes.get(i + 1);

            float ax = this.toX(a.getTick());
            float ay = this.toY(a.getValue());
            float bx = this.toX(b.getTick());
            float by = this.toY(b.getValue());

            float c1x = ax + a.rx * 4F;
            float c1y = ay - a.ry * 4F;
            float c2x = bx - b.lx * 4F;
            float c2y = by + b.ly * 4F;

            float prevX = ax;
            float prevY = ay;

            for (int s = 1; s <= 24; s++)
            {
                float t = s / 24F;
                float mt = 1F - t;
                float cx = mt * mt * mt * ax + 3 * mt * mt * t * c1x + 3 * mt * t * t * c2x + t * t * t * bx;
                float cy = mt * mt * mt * ay + 3 * mt * mt * t * c1y + 3 * mt * t * t * c2y + t * t * t * by;

                context.batcher.box(Math.min(prevX, cx), Math.min(prevY, cy), Math.max(prevX, cx) + 1, Math.max(prevY, cy) + 1, Colors.WHITE | Colors.A75);

                prevX = cx;
                prevY = cy;
            }
        }

        /* Keyframe points + bezier handles */
        for (Keyframe<Float> keyframe : keyframes)
        {
            float kx = this.toX(keyframe.getTick());
            float ky = this.toY(keyframe.getValue());

            boolean isSelected = keyframe == this.selected;
            int color = isSelected ? Colors.ACTIVE : Colors.WHITE;

            context.batcher.box(kx - 3, ky - 3, kx + 3, ky + 3, color);

            /* Bezier handles */
            float h1x = kx + keyframe.rx * 4F;
            float h1y = ky - keyframe.ry * 4F;
            float h2x = kx - keyframe.lx * 4F;
            float h2y = ky + keyframe.ly * 4F;

            context.batcher.box(h1x - 2, h1y - 2, h1x + 2, h1y + 2, 0xff66ccff);
            context.batcher.box(h2x - 2, h2y - 2, h2x + 2, h2y + 2, 0xff66ccff);
        }
    }
}
