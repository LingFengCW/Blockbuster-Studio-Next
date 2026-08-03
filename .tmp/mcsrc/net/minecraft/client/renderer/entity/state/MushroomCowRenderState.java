/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.animal.cow.MushroomCow$Variant
 */
package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.cow.MushroomCow;

public class MushroomCowRenderState
extends LivingEntityRenderState {
    public MushroomCow.Variant variant = MushroomCow.Variant.RED;
    public final BlockModelRenderState mushroomModel = new BlockModelRenderState();
}

