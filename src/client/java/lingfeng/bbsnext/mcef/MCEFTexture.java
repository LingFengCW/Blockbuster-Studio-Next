package lingfeng.bbsnext.mcef;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * Bridges MCEF's native browser texture into Minecraft's texture pipeline.
 *
 * MCEF Modern uploads every CEF paint frame directly into a MC 26.2
 * {@link GpuTexture}/{@link GpuTextureView} (created via the device's
 * {@code createTexture}). We do NOT read those pixels back; instead this
 * wrapper exposes MCEF's {@link GpuTextureView} through the standard
 * {@link AbstractTexture} interface so the browser can be drawn with the
 * normal {@code GuiGraphics.blit(Identifier, ...)} path - which is the only
 * texture route that works on MC 26.2's Vulkan backend.
 *
 * MCEF owns the underlying {@link GpuTexture}, so {@link #close()} is a no-op
 * here (the browser releases it on {@link MCEFBrowser#close()}).
 */
public class MCEFTexture extends AbstractTexture
{
    private final MCEFBrowser browser;

    public MCEFTexture(MCEFBrowser browser)
    {
        this.browser = browser;
    }

    @Override
    public GpuTexture getTexture()
    {
        return this.browser.getTexture();
    }

    @Override
    public GpuTextureView getTextureView()
    {
        return this.browser.getTextureView();
    }

    @Override
    public void close()
    {
        /* Owned by MCEF; do not release here. */
    }
}
