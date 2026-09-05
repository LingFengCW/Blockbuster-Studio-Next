package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.core.ValueString;

/**
 * A named "expression" preset for a {@link ModelForm}: an ordered list of
 * {@link GroupOverride}s that, when applied during rendering, toggle group
 * visibility and add relative transform offsets. Expressions let a single custom
 * model expose multiple appearance states (e.g. "angry", "hat on", "coat off")
 * without duplicating the whole model.
 */
public class ModelExpression extends mchorse.bbs_mod.settings.values.core.ValueGroup
{
    public final ValueString name = new ValueString("name", "");
    public final ValueList<GroupOverride> groups;

    public ModelExpression(String id)
    {
        super(id);

        this.groups = new ValueList<GroupOverride>("groups")
        {
            @Override
            protected GroupOverride create(String gid)
            {
                return new GroupOverride(gid);
            }
        };

        this.add(this.name);
        this.add(this.groups);
    }
}
