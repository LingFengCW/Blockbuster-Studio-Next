package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.BezierUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class ColorKeyframeFactory implements IKeyframeFactory<Color>
{
    private Color i = new Color();

    @Override
    public Color fromData(BaseType data)
    {
        if (!data.isNumeric())
        {
            return new Color();
        }

        return Color.rgba(data.asNumeric().intValue());
    }

    @Override
    public BaseType toData(Color value)
    {
        return new IntType(value.getARGBColor());
    }

    @Override
    public Color createEmpty()
    {
        return new Color().set(Colors.WHITE);
    }

    @Override
    public Color copy(Color value)
    {
        return value.copy();
    }

    @Override
    public Color interpolate(Keyframe<Color> preA, Keyframe<Color> a, Keyframe<Color> b, Keyframe<Color> postB, IInterp interpolation, float x)
    {
        if (interpolation.has(Interpolations.BEZIER))
        {
            this.i.r = (float) MathUtils.clamp(BezierUtils.get(a.getValue().r, b.getValue().r, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x), 0F, 1F);
            this.i.g = (float) MathUtils.clamp(BezierUtils.get(a.getValue().g, b.getValue().g, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x), 0F, 1F);
            this.i.b = (float) MathUtils.clamp(BezierUtils.get(a.getValue().b, b.getValue().b, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x), 0F, 1F);
            this.i.a = (float) MathUtils.clamp(BezierUtils.get(a.getValue().a, b.getValue().a, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x), 0F, 1F);

            return this.i;
        }

        return IKeyframeFactory.super.interpolate(preA, a, b, postB, interpolation, x);
    }

    @Override
    public Color interpolate(Color preA, Color a, Color b, Color postB, IInterp interpolation, float x)
    {
        this.i.r = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.r, a.r, b.r, postB.r, x)), 0F, 1F);
        this.i.g = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.g, a.g, b.g, postB.g, x)), 0F, 1F);
        this.i.b = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.b, a.b, b.b, postB.b, x)), 0F, 1F);
        this.i.a = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.a, a.a, b.a, postB.a, x)), 0F, 1F);

        return this.i;
    }
}