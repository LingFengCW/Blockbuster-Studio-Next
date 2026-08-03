package mchorse.bbs_mod.ui.framework.elements.utils;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Picture-in-picture render state for BBS model preview panels.
 *
 * This is the MC 26.2 legal way to render 3D content inside a GUI. The
 * callback is executed by the engine during GuiRenderer#prepare (draw
 * phase) instead of the extraction phase, where opening render passes
 * is forbidden.
 */
public record UIModelPipRenderState(
    UIModelPipRenderState.Callback callback,
    int x0,
    int y0,
    int x1,
    int y1,
    ScreenRectangle scissorArea,
    ScreenRectangle bounds
) implements PictureInPictureRenderState
{
    public UIModelPipRenderState(Callback callback, int x1, int y1, int x2, int y2, ScreenRectangle scissorArea)
    {
        this(callback, x1, y1, x2, y2, scissorArea, PictureInPictureRenderState.getBounds(x1, y1, x2, y2, scissorArea));
    }

    @Override
    public float scale()
    {
        return 16F;
    }

    @FunctionalInterface
    public interface Callback
    {
        void render(PoseStack poseStack, SubmitNodeCollector collector);
    }
}
