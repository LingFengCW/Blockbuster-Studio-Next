package mchorse.bbs_mod.ui.framework.elements.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Picture-in-picture renderer for BBS model preview panels.
 *
 * Registered through Fabric's PictureInPictureRendererRegistry; the engine
 * invokes {@link #renderToTexture} during the draw phase and composites the
 * resulting texture into the GUI at the correct layer.
 */
public class UIModelPipRenderer extends PictureInPictureRenderer<UIModelPipRenderState>
{
    public UIModelPipRenderer()
    {}

    @Override
    public Class<UIModelPipRenderState> getRenderStateClass()
    {
        return UIModelPipRenderState.class;
    }

    @Override
    protected void renderToTexture(UIModelPipRenderState state, PoseStack poseStack, SubmitNodeCollector collector)
    {
        mchorse.bbs_mod.client.PipGeometry.debug("renderToTexture", "UIModelPipRenderer.renderToTexture invoked");

        state.callback().render(poseStack, collector);
    }

    @Override
    protected String getTextureLabel()
    {
        return "bbs_mod model preview";
    }
}
