/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.player.PlayerSkin
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.entity;

import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

public interface ClientAvatarEntity {
    public ClientAvatarState avatarState();

    public PlayerSkin getSkin();

    public // Could not load outer class - annotation placement on inner may be incorrect
     @Nullable Parrot.Variant getParrotVariantOnShoulder(boolean var1);

    public boolean showExtraEars();
}

