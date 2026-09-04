package mchorse.bbs_mod.settings.values.numeric;

import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueLong extends BaseValueNumber<Long>
{
    public ValueLong(String id, Long defaultValue)
    {
        this(id, defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public ValueLong(String id, Long defaultValue, Long min, Long max)
    {
        super(id, KeyframeFactories.LONG, defaultValue, min, max);
    }

    @Override
    protected Long clamp(Long value)
    {
        return MathUtils.clamp(value, this.min, this.max);
    }

    @Override
    public String toString()
    {
        return Long.toString(this.value);
    }
}
