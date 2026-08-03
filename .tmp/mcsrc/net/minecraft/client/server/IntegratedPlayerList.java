/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.LayeredRegistryAccess
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.RegistryLayer
 *  net.minecraft.server.notifications.NotificationService
 *  net.minecraft.server.players.NameAndId
 *  net.minecraft.server.players.PlayerList
 *  net.minecraft.world.level.storage.PlayerDataStorage
 */
package net.minecraft.client.server;

import java.net.SocketAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;

public class IntegratedPlayerList
extends PlayerList {
    public IntegratedPlayerList(IntegratedServer server, LayeredRegistryAccess<RegistryLayer> registryHolder, PlayerDataStorage playerDataStorage) {
        super((MinecraftServer)server, registryHolder, playerDataStorage, (NotificationService)server.notificationManager());
        this.setViewDistance(10);
    }

    public Component canPlayerLogin(SocketAddress address, NameAndId nameAndId) {
        if (this.getServer().isSingleplayerOwner(nameAndId) && this.getPlayerByName(nameAndId.name()) != null) {
            return Component.translatable((String)"multiplayer.disconnect.name_taken");
        }
        return super.canPlayerLogin(address, nameAndId);
    }

    public IntegratedServer getServer() {
        return (IntegratedServer)super.getServer();
    }
}

