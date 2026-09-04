package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.BezierUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.joml.Vector3f;

public class Vector3fKeyframeFactory implements IKeyframeFactory<Vector3f>
{
    private Vector3f i = new Vector3f();

    @Override
    public Vector3f fromData(BaseType data)
    {
        return data.isList() ? DataStorageUtils.vector3fFromData(data.asList()) : new Vector3f();
    }

    @Override
    public BaseType toData(Vector3f value)
    {
        return DataStorageUtils.vector3fToData(value);
    }

    @Override
    public Vector3f createEmpty()
    {
        return new Vector3f();
    }

    @Override
    public Vector3f copy(Vector3f value)
    {
        return new Vector3f(value);
    }

    @Override
    public Vector3f interpolate(Keyframe<Vector3f> preA, Keyframe<Vector3f> a, Keyframe<Vector3f> b, Keyframe<Vector3f> postB, IInterp interpolation, float x)
    {
        if (interpolation.has(Interpolations.BEZIER))
        {
            this.i.x = (float) BezierUtils.get(a.getValue().x, b.getValue().x, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x);
            this.i.y = (float) BezierUtils.get(a.getValue().y, b.getValue().y, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x);
            this.i.z = (float) BezierUtils.get(a.getValue().z, b.getValue().z, a.getTick(), b.getTick(), a.rx, a.ry, b.lx, b.ly, x);

            return this.i;
        }

        return IKeyframeFactory.super.interpolate(preA, a, b, postB, interpolation, x);
    }

    @Override
    public Vector3f interpolate(Vector3f preA, Vector3f a, Vector3f b, Vector3f postB, IInterp interpolation, float x)
    {
        this.i.x = (float) interpolation.interpolate(IInterp.context.set(preA.x, a.x, b.x, postB.x, x));
        this.i.y = (float) interpolation.interpolate(IInterp.context.set(preA.y, a.y, b.y, postB.y, x));
        this.i.z = (float) interpolation.interpolate(IInterp.context.set(preA.z, a.z, b.z, postB.z, x));

        return this.i;
    }
}
