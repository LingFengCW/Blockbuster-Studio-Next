package mchorse.bbs_mod.forms.renderers.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * RecolorVertexSodiumConsumer - Sodium variant that delegates to RecolorVertexConsumer.
 * In MC 26.2 this is essentially identical to the base class.
 */
public class RecolorVertexSodiumConsumer extends RecolorVertexConsumer
{
    public RecolorVertexSodiumConsumer(VertexConsumer parent)
    {
        super(parent, null);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a)
    {
        return consumer.setColor(r, g, b, a);
    }
}
