package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UILongKeyframeFactory extends UIKeyframeFactory<Long>
{
    private UITrackpad value;
    private UIBezierHandles handles;

    public UILongKeyframeFactory(Keyframe<Long> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad(this::setValue);
        this.value.setValue(keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);

        this.scroll.add(this.value, this.handles.createColumn());
    }

    @Override
    public void update()
    {
        super.update();

        this.value.setValue(this.keyframe.getValue());
        this.handles.update();
    }
}
