/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket
 *  net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import org.jspecify.annotations.Nullable;

public class DebugQueryHandler {
    private final ClientPacketListener connection;
    private int transactionId = -1;
    private @Nullable Consumer<CompoundTag> callback;

    public DebugQueryHandler(ClientPacketListener connection) {
        this.connection = connection;
    }

    public boolean handleResponse(int transactionId, @Nullable CompoundTag tag) {
        if (this.transactionId == transactionId && this.callback != null) {
            this.callback.accept(tag);
            this.callback = null;
            return true;
        }
        return false;
    }

    private int startTransaction(Consumer<CompoundTag> callback) {
        this.callback = callback;
        return ++this.transactionId;
    }

    public void queryEntityTag(int entityId, Consumer<CompoundTag> callback) {
        int transactionId = this.startTransaction(callback);
        this.connection.send((Packet<?>)new ServerboundEntityTagQueryPacket(transactionId, entityId));
    }

    public void queryBlockEntityTag(BlockPos blockPos, Consumer<CompoundTag> callback) {
        int transactionId = this.startTransaction(callback);
        this.connection.send((Packet<?>)new ServerboundBlockEntityTagQueryPacket(transactionId, blockPos));
    }
}

