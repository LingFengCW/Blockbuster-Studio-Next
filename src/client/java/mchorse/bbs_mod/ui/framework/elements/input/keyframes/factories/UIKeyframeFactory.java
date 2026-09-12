package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.IKeyframeShapeRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.KeyframeShapeRenderers;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class UIKeyframeFactory <T> extends UIElement
{
    private static final Map<IKeyframeFactory, IUIKeyframeFactoryFactory> FACTORIES = new HashMap<>();
    private static final Map<IKeyframeFactory, Integer> SCROLLS = new HashMap<>();

    public UIScrollView scroll;
    public UITrackpad tick;
    public UITrackpad duration;
    public UIIcon interp;

    public UIIcon shape;
    public UIColor color;

    /* Bezier tangent handle numeric editors (only relevant for BEZIER interpolation) */
    public UITrackpad lxPad;
    public UITrackpad lyPad;
    public UITrackpad rxPad;
    public UITrackpad ryPad;

    private UIElement lxRow;
    private UIElement lyRow;
    private UIElement rxRow;
    private UIElement ryRow;
    private boolean bezierHandlesVisible;

    protected Keyframe<T> keyframe;
    protected UIKeyframes editor;

    static
    {
        register(KeyframeFactories.ANCHOR, UIAnchorKeyframeFactory::new);
        register(KeyframeFactories.BOOLEAN, UIBooleanKeyframeFactory::new);
        register(KeyframeFactories.COLOR, UIColorKeyframeFactory::new);
        register(KeyframeFactories.FLOAT, UIFloatKeyframeFactory::new);
        register(KeyframeFactories.DOUBLE, UIDoubleKeyframeFactory::new);
        register(KeyframeFactories.INTEGER, UIIntegerKeyframeFactory::new);
        register(KeyframeFactories.LONG, UILongKeyframeFactory::new);
        register(KeyframeFactories.LINK, UILinkKeyframeFactory::new);
        register(KeyframeFactories.POSE, UIPoseKeyframeFactory::new);
        register(KeyframeFactories.STRING, UIStringKeyframeFactory::new);
        register(KeyframeFactories.TRANSFORM, UITransformKeyframeFactory::new);
        register(KeyframeFactories.VECTOR3F, UIVector3fKeyframeFactory::new);
        register(KeyframeFactories.VECTOR4F, UIVector4fKeyframeFactory::new);
        register(KeyframeFactories.BLOCK_STATE, UIBlockStateKeyframeFactory::new);
        register(KeyframeFactories.ITEM_STACK, UIItemStackKeyframeFactory::new);
        register(KeyframeFactories.ACTIONS_CONFIG, UIActionsConfigKeyframeFactory::new);
        register(KeyframeFactories.SHAPE_KEYS, UIShapeKeysKeyframeFactory::new);
        register(KeyframeFactories.PARTICLE_SETTINGS, UIParticleSettingsKeyframeFactory::new);
    }

    public static <T> void register(IKeyframeFactory<T> clazz, IUIKeyframeFactoryFactory<T> factory)
    {
        FACTORIES.put(clazz, factory);
    }

    public static void saveScroll(UIKeyframeFactory editor)
    {
        if (editor != null)
        {
            SCROLLS.put(editor.keyframe.getFactory(), (int) editor.scroll.scroll.getScroll());
        }
    }

    public static <T> UIKeyframeFactory createPanel(Keyframe<T> keyframe, UIKeyframes editor)
    {
        IUIKeyframeFactoryFactory<T> factory = FACTORIES.get(keyframe.getFactory());
        UIKeyframeFactory uiEditor = factory == null ? null : factory.create(keyframe, editor);

        if (uiEditor != null)
        {
            uiEditor.scroll.scroll.setScroll(SCROLLS.getOrDefault(keyframe.getFactory(), 0));
        }

        return uiEditor;
    }

    public UIKeyframeFactory(Keyframe<T> keyframe, UIKeyframes editor)
    {
        this.keyframe = keyframe;
        this.editor = editor;

        this.scroll = UI.scrollView(5, 10);
        this.scroll.scroll.cancelScrolling();
        this.scroll.full(this);

        this.tick = new UITrackpad(this::setTick);
        this.tick.tooltip(UIKeys.KEYFRAMES_TICK);
        this.tick.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        this.tick.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());
        this.duration = new UITrackpad((v) -> this.setDuration(v.floatValue()));
        this.duration.limit(0, Float.MAX_VALUE).tooltip(UIKeys.KEYFRAMES_FORCED_DURATION);
        this.interp = new UIIcon(Icons.GRAPH, (b) ->
        {
            Interpolation interp = this.keyframe.getInterpolation();
            UIInterpolationContextMenu menu = new UIInterpolationContextMenu(interp);

            this.getContext().replaceContextMenu(menu.callback(() ->
            {
                this.editor.getGraph().setInterpolation(interp);
                this.syncBezierHandles();
            }));
        });
        this.interp.tooltip(new InterpolationTooltip(0F, 0.5F, () -> this.keyframe.getInterpolation()));
        this.interp.keys().register(Keys.KEYFRAMES_INTERP, this.interp::clickItself).category(UIKeys.KEYFRAMES_KEYS_CATEGORY);

        this.color = new UIColor((c) ->
        {
            for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
            {
                for (Keyframe kf : sheet.selection.getSelected()) kf.setColor(new Color().set(c));
            }
        });
        this.color.setColor(keyframe.getColor() == null ? 0 : keyframe.getColor().getRGBColor());
        this.color.tooltip(UIKeys.KEYFRAMES_CHANGE_COLOR);
        this.color.context((menu) ->
        {
            menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, () ->
            {
                for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
                {
                    for (Keyframe kf : sheet.selection.getSelected()) kf.setColor(null);
                }

                this.color.setColor(0);
            });
        });

        this.shape = new UIIcon(Icons.SHAPES, (b) ->
        {
            KeyframeShape currentShape = keyframe.getShape() == null ? KeyframeShape.SQUARE : keyframe.getShape();

            this.getContext().replaceContextMenu((menu) ->
            {
                for (KeyframeShape shape : KeyframeShape.values())
                {
                    IKeyframeShapeRenderer shapeRenderer = KeyframeShapeRenderers.SHAPES.get(shape);

                    menu.action(shapeRenderer.getIcon(), shapeRenderer.getLabel(), shape == currentShape, () ->
                    {
                        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
                        {
                            for (Keyframe kf : sheet.selection.getSelected())
                            {
                                kf.setShape(shape);
                            }
                        }
                    });
                }
            });
        });
        this.shape.tooltip(UIKeys.KEYFRAMES_CHANGE_SHAPE);

        this.scroll.add(UI.row(this.interp, this.tick, this.duration));
        this.scroll.add(UI.row(this.shape, this.color));

        /* Bezier tangent handle numeric editors (added to the scroll only
         * when the selected keyframe uses BEZIER interpolation) */
        this.lxPad = this.createHandlePad((v) -> this.keyframe.lx = (float) v.doubleValue());
        this.lyPad = this.createHandlePad((v) -> this.keyframe.ly = (float) v.doubleValue());
        this.rxPad = this.createHandlePad((v) -> this.keyframe.rx = (float) v.doubleValue());
        this.ryPad = this.createHandlePad((v) -> this.keyframe.ry = (float) v.doubleValue());

        this.lxRow = this.handleField(IKey.raw("LX"), this.lxPad);
        this.lyRow = this.handleField(IKey.raw("LY"), this.lyPad);
        this.rxRow = this.handleField(IKey.raw("RX"), this.rxPad);
        this.ryRow = this.handleField(IKey.raw("RY"), this.ryPad);

        this.add(this.scroll);

        /* Fill data */
        this.tick.setValue(TimeUtils.toTime(keyframe.getTick()));
        this.duration.setValue(TimeUtils.toTime(keyframe.getDuration()));
        this.syncBezierHandles();
    }

    private UITrackpad createHandlePad(Consumer<Double> callback)
    {
        UITrackpad pad = new UITrackpad(callback);

        pad.limit(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        pad.tooltip(IKey.raw("Bezier tangent handle offset (in ticks / value units)"));
        pad.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        pad.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());

        return pad;
    }

    private UIElement handleField(IKey label, UITrackpad pad)
    {
        UILabel text = UI.label(label, 20);

        text.w(28);

        return UI.row(5, 0, 20, text, pad);
    }

    /**
     * Sync the bezier handle trackpads with the keyframe's tangent handle
     * values, and add or remove the handle rows from the scroll depending on
     * whether the current keyframe uses BEZIER interpolation (the rows are
     * physically removed rather than hidden so they don't leave empty gaps).
     */
    public void syncBezierHandles()
    {
        boolean bezier = this.keyframe.getInterpolation().getInterp() == Interpolations.BEZIER;

        if (bezier != this.bezierHandlesVisible)
        {
            this.bezierHandlesVisible = bezier;

            if (bezier)
            {
                this.scroll.add(this.lxRow, this.lyRow, this.rxRow, this.ryRow);
            }
            else
            {
                this.scroll.remove(this.lxRow);
                this.scroll.remove(this.lyRow);
                this.scroll.remove(this.rxRow);
                this.scroll.remove(this.ryRow);
            }

            this.scroll.resize();
        }

        if (bezier)
        {
            this.lxPad.setValue(this.keyframe.lx);
            this.lyPad.setValue(this.keyframe.ly);
            this.rxPad.setValue(this.keyframe.rx);
            this.ryPad.setValue(this.keyframe.ry);
        }
    }

    public Keyframe<T> getKeyframe()
    {
        return this.keyframe;
    }

    public void setTick(double tick)
    {
        double time = TimeUtils.fromTime(tick);

        this.editor.getGraph().setTick((float) time, false);
    }

    public void setDuration(float value)
    {
        this.editor.getGraph().setDuration(value);
    }

    public void setValue(Object value)
    {
        this.editor.getGraph().setValue(value, true);
    }

    public void update()
    {
        this.tick.setValue(TimeUtils.toTime(this.keyframe.getTick()));
        this.syncBezierHandles();
    }

    public static interface IUIKeyframeFactoryFactory <T>
    {
        public UIKeyframeFactory<T> create(Keyframe<T> keyframe, UIKeyframes editor);
    }
}
