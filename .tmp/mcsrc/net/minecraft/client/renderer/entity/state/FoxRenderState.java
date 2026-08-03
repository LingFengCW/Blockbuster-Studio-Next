/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.animal.fox.Fox$Variant
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.world.entity.animal.fox.Fox;

public class FoxRenderState
extends HoldingEntityRenderState {
    public float headRollAngle;
    public float crouchAmount;
    public boolean isCrouching;
    public boolean isSleeping;
    public boolean isSitting;
    public boolean isFaceplanted;
    public boolean isPouncing;
    public Fox.Variant variant = Fox.Variant.DEFAULT;
}

