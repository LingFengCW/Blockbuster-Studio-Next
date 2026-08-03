/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
 *  net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
 *  net.minecraft.util.Util
 *  net.minecraft.util.debugchart.LocalSampleLogger
 */
package net.minecraft.client.multiplayer;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;
import net.minecraft.util.debugchart.LocalSampleLogger;

public class PingDebugMonitor {
    private final ClientPacketListener connection;
    private final LocalSampleLogger delayTimer;

    public PingDebugMonitor(ClientPacketListener connection, LocalSampleLogger delayTimer) {
        this.connection = connection;
        this.delayTimer = delayTimer;
    }

    public void tick() {
        this.connection.send((Packet<?>)new ServerboundPingRequestPacket(Util.getMillis()));
    }

    public void onPongReceived(ClientboundPongResponsePacket packet) {
        this.delayTimer.logSample(Util.getMillis() - packet.time());
    }
}

