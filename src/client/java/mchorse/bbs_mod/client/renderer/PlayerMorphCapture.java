package mchorse.bbs_mod.client.renderer;

import net.minecraft.world.entity.player.Player;

/**
 * Bridge that carries the player entity from AvatarRenderer.extractRenderState
 * (the only hook that still receives the actual entity, because render states
 * carry no entity reference) across to LivingEntityRendererMixin, where the
 * morphed model is submitted inside the inherited submit().
 *
 * The capture and the render hook live in two different mixin target classes
 * (AvatarRenderer vs LivingEntityRenderer), so the value cannot be a per-mixin
 * field — it is stored here as a shared static ThreadLocal instead.
 */
public final class PlayerMorphCapture
{
    public static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();
    public static final ThreadLocal<Float> TICK = new ThreadLocal<>();

    private PlayerMorphCapture()
    {
    }
}
