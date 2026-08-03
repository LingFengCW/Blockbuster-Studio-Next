/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerGamePacketListener
 */
package net.minecraft.client.multiplayer.prediction;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

@FunctionalInterface
public interface PredictiveAction {
    public Packet<ServerGamePacketListener> predict(int var1);
}

