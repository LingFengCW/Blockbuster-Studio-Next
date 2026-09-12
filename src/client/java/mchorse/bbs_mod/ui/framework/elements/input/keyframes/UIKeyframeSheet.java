package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.util.ArrayList;
import java.util.List;

public class UIKeyframeSheet
{
    /* Meta data */
    public final String id;
    public final IKey title;
    public final int color;
    public boolean separator;

    private Icon icon;

    public final KeyframeChannel channel;
    public final KeyframeSelection selection;
    public final BaseValueBasic property;

    public UIKeyframeSheet(int color, boolean separator, KeyframeChannel channel, BaseValueBasic property)
    {
        this(channel.getId(), IKey.constant(property != null ? FormUtils.getForm(property).getTrackName(channel.getId()) : channel.getId()), color, separator, channel, property);
    }

    public UIKeyframeSheet(String id, IKey title, int color, boolean separator, KeyframeChannel channel, BaseValueBasic property)
    {
        this.id = id;
        this.title = title;
        this.color = color;
        this.separator = separator;

        this.channel = channel;
        this.selection = new KeyframeSelection(channel);
        this.property = property;
    }

    public UIKeyframeSheet icon(Icon icon)
    {
        this.icon = icon;

        return this;
    }

    public Icon getIcon()
    {
        return this.icon;
    }

    public List<Integer> sort()
    {
        List<Keyframe> selected = this.selection.getSelected();
        List<Integer> lastSelection = new ArrayList<>(this.selection.getIndices());

        this.channel.sort();
        this.selection.clear();

        List keyframes = this.channel.getKeyframes();

        for (Keyframe keyframe : selected)
        {
            this.selection.add(keyframes.indexOf(keyframe));
        }

        return lastSelection;
    }

    public void setTickBy(float diff, boolean dirty)
    {
        for (Keyframe keyframe : this.selection.getSelected())
        {
            keyframe.setTick(keyframe.getTick() + diff, dirty);
        }
    }

    public void setDuration(float duration)
    {
        for (Keyframe keyframe : this.selection.getSelected())
        {
            keyframe.setDuration(duration);
        }
    }

    public void setValue(Object value, Object selectedValue, boolean dirty)
    {
        Number valueNumber = value instanceof Number ? (Number) value : 0D;

        for (Keyframe keyframe : this.selection.getSelected())
        {
            if (selectedValue instanceof Double)
            {
                keyframe.setValue((double) keyframe.getValue() + valueNumber.doubleValue() - (double) selectedValue, dirty);
            }
            else if (selectedValue instanceof Float)
            {
                keyframe.setValue((float) keyframe.getValue() + valueNumber.floatValue() - (float) selectedValue, dirty);
            }
            else if (selectedValue instanceof Integer)
            {
                keyframe.setValue((int) keyframe.getValue() + valueNumber.intValue() - (int) selectedValue, dirty);
            }
            else
            {
                keyframe.setValue(this.channel.getFactory().copy(value), dirty);
            }
        }
    }

    public void setInterpolation(Interpolation interpolation)
    {
        for (Keyframe keyframe : this.selection.getSelected())
        {
            boolean wasBezier = keyframe.getInterpolation().getInterp() == Interpolations.BEZIER;

            keyframe.getInterpolation().copy(interpolation);

            if (interpolation.getInterp() == Interpolations.BEZIER && !wasBezier)
            {
                this.initBezierHandles(keyframe);
            }
        }
    }

    /**
     * Initialize tangent handles to follow the linear slope of the adjacent
     * segment, so a freshly eased keyframe shows a visible smooth curve
     * instead of flat (horizontal) handles. Handle length is ~1/3 of the
     * neighbouring segment, clamped to a sane minimum.
     */
    private void initBezierHandles(Keyframe keyframe)
    {
        List keyframes = this.channel.getKeyframes();
        int index = keyframes.indexOf(keyframe);

        if (index < 0)
        {
            return;
        }

        IKeyframeFactory factory = this.channel.getFactory();
        Keyframe prev = index > 0 ? (Keyframe) keyframes.get(index - 1) : null;
        Keyframe next = index < keyframes.size() - 1 ? (Keyframe) keyframes.get(index + 1) : null;

        if (next != null)
        {
            float dt = Math.max(1F, next.getTick() - keyframe.getTick());
            float len = Math.max(2F, dt * 0.33F);
            float slope = (float) (factory.getY(next.getValue()) - factory.getY(keyframe.getValue())) / dt;

            keyframe.rx = len;
            keyframe.ry = slope * len;
        }

        if (prev != null)
        {
            float dt = Math.max(1F, keyframe.getTick() - prev.getTick());
            float len = Math.max(2F, dt * 0.33F);
            float slope = (float) (factory.getY(keyframe.getValue()) - factory.getY(prev.getValue())) / dt;

            keyframe.lx = len;
            keyframe.ly = -slope * len;
        }
    }

    public void remove(Keyframe keyframe)
    {
        int index = this.channel.getKeyframes().indexOf(keyframe);

        if (index >= 0)
        {
            this.selection.remove(index);
            this.channel.remove(index);
        }
    }
}

