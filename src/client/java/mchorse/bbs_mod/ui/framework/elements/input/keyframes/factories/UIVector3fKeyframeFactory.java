package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.joml.Vector3f;

public class UIVector3fKeyframeFactory extends UIKeyframeFactory<Vector3f>
{
    private UITrackpad x;
    private UITrackpad y;
    private UITrackpad z;

    public UIVector3fKeyframeFactory(Keyframe<Vector3f> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        Vector3f value = keyframe.getValue();

        this.x = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.x.setValue(value.x);
        this.y = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.y.setValue(value.y);
        this.z = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.z.setValue(value.z);

        this.scroll.add(UI.row(this.x, this.y), UI.row(this.z));
    }

    private Vector3f getValue()
    {
        return new Vector3f(
            (float) this.x.getValue(), (float) this.y.getValue(),
            (float) this.z.getValue()
        );
    }
}
