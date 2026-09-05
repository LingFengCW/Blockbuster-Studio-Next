package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.pose.Transform;

/**
 * A single group override inside a {@link ModelExpression}: makes one model
 * group (identified by its {@code id}) either visible or hidden, and applies a
 * relative transform offset on top of the group's initial transform.
 *
 * <p>Offsets are additive (matched to {@code Model.applyPose} semantics):
 * translate/rotate are added, scale is multiplied. A default identity transform
 * leaves the group untouched.</p>
 */
public class GroupOverride extends mchorse.bbs_mod.settings.values.core.ValueGroup
{
    public final ValueString group = new ValueString("group", "");
    public final ValueBoolean visible = new ValueBoolean("visible", true);
    public final ValueTransform transform = new ValueTransform("transform", new Transform());

    public GroupOverride(String id)
    {
        super(id);

        this.add(this.group);
        this.add(this.visible);
        this.add(this.transform);
    }
}
