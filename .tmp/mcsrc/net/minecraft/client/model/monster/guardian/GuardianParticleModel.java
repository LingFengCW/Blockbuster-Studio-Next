/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Unit
 */
package net.minecraft.client.model.monster.guardian;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

public class GuardianParticleModel
extends Model<Unit> {
    public GuardianParticleModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }
}

