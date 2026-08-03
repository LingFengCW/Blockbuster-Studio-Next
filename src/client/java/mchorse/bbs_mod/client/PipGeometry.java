package mchorse.bbs_mod.client;

import com.mojang.blaze3d.platform.NativeImage;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Picture-in-picture geometry context (MC 26.2).
 *
 * During the GUI extraction phase it's illegal to open render passes, so
 * BBS model previews are deferred into picture-in-picture callbacks. While
 * such a callback is running, the active {@link SubmitNodeCollector} is
 * stored here so that deep rendering code (like ModelInstance) can submit
 * its geometry through the engine's legal channel.
 *
 * It also bridges BBS's raw GL textures into vanilla's texture manager,
 * because RenderTypes require an {@link Identifier}-registered texture.
 */
public class PipGeometry
{
    private static SubmitNodeCollector collector;
    private static Link lastTexture;

    private static final Map<Link, Identifier> bridgedTextures = new HashMap<>();
    private static int counter;

    private static final Map<String, Long> debugStamps = new HashMap<>();

    /**
     * Throttled debug logging (at most once per second per key), used to
     * diagnose the PiP model preview pipeline without spamming the log.
     */
    public static void debug(String key, String message)
    {
        long now = System.currentTimeMillis();
        Long last = debugStamps.get(key);

        if (last == null || now - last > 1000L)
        {
            debugStamps.put(key, now);
            System.out.println("[BBS PIP] " + message);
        }
    }

    public static void setCollector(SubmitNodeCollector newCollector)
    {
        collector = newCollector;
    }

    public static SubmitNodeCollector getCollector()
    {
        return collector;
    }

    public static void setLastTexture(Link texture)
    {
        lastTexture = texture;
    }

    public static Link getLastTexture()
    {
        return lastTexture;
    }

    public static net.minecraft.client.renderer.rendertype.RenderType getModelRenderType(Link texture)
    {
        return RenderTypes.entityTranslucent(bridge(texture));
    }

    /**
     * Register a BBS texture link as a vanilla texture, so it can be used
     * with vanilla render types.
     */
    public static Identifier bridge(Link link)
    {
        if (link == null)
        {
            return MissingTextureAtlasSprite.getLocation();
        }

        Identifier id = bridgedTextures.get(link);

        if (id != null)
        {
            return id;
        }

        try (InputStream stream = BBSModClient.getTextures().provider.getAsset(link))
        {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(() -> "bbs_mod pip " + link, image);

            id = Identifier.fromNamespaceAndPath("bbs_mod", "pip/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
        catch (Exception e)
        {
            id = MissingTextureAtlasSprite.getLocation();
        }

        bridgedTextures.put(link, id);

        return id;
    }

    /**
     * Drop a bridged texture (e.g. when the BBS texture got reloaded).
     */
    public static void unbridge(Link link)
    {
        Identifier id = bridgedTextures.remove(link);

        if (id != null && !id.equals(MissingTextureAtlasSprite.getLocation()))
        {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }

    /**
     * Returns the GpuTextureView of the bridged texture for a BBS link, or
     * null when unavailable. Used to bind BBS textures into 26.2 render
     * passes (RenderPass.bindTexture("Sampler0", ...)) so models sample
     * their real texture instead of an unbound sampler.
     */
    public static com.mojang.blaze3d.textures.GpuTextureView bridgeView(Link link)
    {
        Identifier id = bridge(link);

        if (id == null || id.equals(MissingTextureAtlasSprite.getLocation()))
        {
            return null;
        }

        var texture = Minecraft.getInstance().getTextureManager().getTexture(id);

        return texture == null ? null : texture.getTextureView();
    }

    /**
     * Returns a clamped, bilinear GpuSampler for the bridged BBS textures.
     */
    public static com.mojang.blaze3d.textures.GpuSampler bridgeSampler()
    {
        return com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
            .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR);
    }
}
