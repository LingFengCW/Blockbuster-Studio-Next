/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.AnimationState
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class CreakingRenderState
extends LivingEntityRenderState {
    public final AnimationState invulnerabilityAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    public boolean eyesGlowing;
    public boolean canMove;
}

