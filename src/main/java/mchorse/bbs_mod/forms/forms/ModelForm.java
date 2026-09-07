package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.forms.values.ValueActionsConfig;
import mchorse.bbs_mod.forms.values.ValueShapeKeys;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;

import java.util.ArrayList;
import java.util.List;

public class ModelForm extends Form
{
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueString model = new ValueString("model", "");
    /** Pipe-separated list of model ids that cycle over time (model group). Empty = single model. */
    public final ValueString modelGroup = new ValueString("modelGroup", "");
    /** Seconds each model in the group is shown before cycling to the next one. */
    public final ValueFloat modelMorphDur = new ValueFloat("modelMorphDur", 1.0F, 0.1F, 60F);
    public final ValuePose pose = new ValuePose("pose", new Pose());
    public final ValuePose poseOverlay = new ValuePose("pose_overlay", new Pose());
    public final ValueActionsConfig actions = new ValueActionsConfig("actions", new ActionsConfig());
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueShapeKeys shapeKeys = new ValueShapeKeys("shape_keys", new ShapeKeys());

    /** Named expression presets (model-group visibility + transform overrides). */
    public final ValueList<ModelExpression> expressions;

    /** Name of the expression currently applied during rendering ("" = none). */
    public final ValueString activeExpression = new ValueString("active_expression", "");

    public final List<ValuePose> additionalOverlays = new ArrayList<>();

    public ModelForm()
    {
        super();

        this.add(this.texture);
        this.add(this.model);
        this.add(this.modelGroup);
        this.add(this.modelMorphDur);
        this.add(this.pose);
        this.add(this.poseOverlay);

        for (int i = 0; i < BBSSettings.recordingPoseTransformOverlays.get(); i++)
        {
            ValuePose valuePose = new ValuePose("pose_overlay" + i, new Pose());

            this.additionalOverlays.add(valuePose);
            this.add(valuePose);
        }

        this.add(this.actions);
        this.add(this.color);
        this.add(this.shapeKeys);

        this.expressions = new ValueList<ModelExpression>("expressions")
        {
            @Override
            protected ModelExpression create(String id)
            {
                return new ModelExpression(id);
            }
        };

        this.add(this.expressions);
        this.add(this.activeExpression);
    }

    /** Look up an expression by its name (null/empty returns null). */
    public ModelExpression getExpression(String name)
    {
        if (name == null || name.isEmpty())
        {
            return null;
        }

        for (ModelExpression expression : this.expressions.getAllTyped())
        {
            if (name.equals(expression.name.get()))
            {
                return expression;
            }
        }

        return null;
    }

    /** Set the active expression by name (null/empty clears it). */
    public void setActiveExpression(String name)
    {
        this.activeExpression.set(name == null ? "" : name);
    }

    @Override
    public String getDefaultDisplayName()
    {
        return this.model.get();
    }
}