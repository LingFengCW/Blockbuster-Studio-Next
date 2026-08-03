/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.logging.LogUtils
 *  io.netty.bootstrap.Bootstrap
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelException
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelInitializer
 *  io.netty.channel.ChannelOption
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.Connection
 *  net.minecraft.network.DisconnectionDetails
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentUtils
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.ResolutionContext
 *  net.minecraft.network.chat.ResolutionContext$LimitBehavior
 *  net.minecraft.network.chat.contents.objects.PlayerSprite
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.ping.ClientboundPongResponsePacket
 *  net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
 *  net.minecraft.network.protocol.status.ClientStatusPacketListener
 *  net.minecraft.network.protocol.status.ClientboundStatusResponsePacket
 *  net.minecraft.network.protocol.status.ServerStatus
 *  net.minecraft.network.protocol.status.ServerStatus$Players
 *  net.minecraft.network.protocol.status.ServerboundStatusRequestPacket
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.network.EventLoopGroupHolder
 *  net.minecraft.server.players.NameAndId
 *  net.minecraft.util.Util
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.LegacyServerPinger;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class ServerStatusPinger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component CANT_CONNECT_MESSAGE = Component.translatable((String)"multiplayer.status.cannot_connect").withColor(-65536);
    private static final ResolutionContext DESCRIPTION_SANITIZE_CONTEXT = ResolutionContext.builder().withObjectInfoValidator(description -> !(description instanceof PlayerSprite)).setDepthLimit(16).setDepthLimitBehavior(ResolutionContext.LimitBehavior.DISCARD_REMAINING).build();
    private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

    public void pingServer(final ServerData data, final Runnable onPersistentDataChange, final Runnable onPongResponse, final EventLoopGroupHolder eventLoopGroupHolder) throws UnknownHostException {
        final ServerAddress rawAddress = ServerAddress.parseString(data.ip);
        Optional<InetSocketAddress> resolvedAddress = ServerNameResolver.DEFAULT.resolveAddress(rawAddress).map(ResolvedServerAddress::asInetSocketAddress);
        if (resolvedAddress.isEmpty()) {
            this.onPingFailed(ConnectScreen.UNKNOWN_HOST_MESSAGE, data);
            return;
        }
        final InetSocketAddress address = resolvedAddress.get();
        final Connection connection = Connection.connectToServer((InetSocketAddress)address, (EventLoopGroupHolder)eventLoopGroupHolder, null);
        this.connections.add(connection);
        data.motd = Component.translatable((String)"multiplayer.status.pinging");
        data.playerList = Collections.emptyList();
        ClientStatusPacketListener listener = new ClientStatusPacketListener(){
            private boolean success;
            private boolean receivedPing;
            private long pingStart;
            final /* synthetic */ ServerStatusPinger this$0;
            {
                ServerStatusPinger serverStatusPinger = this$0;
                Objects.requireNonNull(serverStatusPinger);
                this.this$0 = serverStatusPinger;
            }

            public void handleStatusResponse(ClientboundStatusResponsePacket packet) {
                if (this.receivedPing) {
                    connection.disconnect((Component)Component.translatable((String)"multiplayer.status.unrequested"));
                    return;
                }
                this.receivedPing = true;
                ServerStatus status = packet.status();
                data.motd = 1.sanitizeDescription(status.description());
                status.version().ifPresentOrElse(version -> {
                    data2.version = Component.literal((String)version.name());
                    data2.protocol = version.protocol();
                }, () -> {
                    data2.version = Component.translatable((String)"multiplayer.status.old");
                    data2.protocol = 0;
                });
                status.players().ifPresentOrElse(players -> {
                    data2.status = ServerStatusPinger.formatPlayerCount(players.online(), players.max());
                    data2.players = players;
                    if (!players.sample().isEmpty()) {
                        ArrayList<Component> playerNames = new ArrayList<Component>(players.sample().size());
                        for (NameAndId profile : players.sample()) {
                            MutableComponent playerName = profile.equals((Object)MinecraftServer.ANONYMOUS_PLAYER_PROFILE) ? Component.translatable((String)"multiplayer.status.anonymous_player") : Component.literal((String)profile.name());
                            playerNames.add((Component)playerName);
                        }
                        if (players.sample().size() < players.online()) {
                            playerNames.add((Component)Component.translatable((String)"multiplayer.status.and_more", (Object[])new Object[]{players.online() - players.sample().size()}));
                        }
                        data2.playerList = playerNames;
                    } else {
                        data2.playerList = List.of();
                    }
                }, () -> {
                    data2.status = Component.translatable((String)"multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY);
                });
                status.favicon().ifPresent(newIcon -> {
                    if (!Arrays.equals(newIcon.iconBytes(), data.getIconBytes())) {
                        data.setIconBytes(ServerData.validateIcon(newIcon.iconBytes()));
                        onPersistentDataChange.run();
                    }
                });
                this.pingStart = Util.getMillis();
                connection.send((Packet)new ServerboundPingRequestPacket(this.pingStart));
                this.success = true;
            }

            private static Component sanitizeDescription(Component original) {
                try {
                    return ComponentUtils.resolve((ResolutionContext)DESCRIPTION_SANITIZE_CONTEXT, (Component)original);
                }
                catch (CommandSyntaxException e) {
                    LOGGER.warn("Failed to sanitize status {}", (Object)original, (Object)e);
                    return Component.empty();
                }
            }

            public void handlePongResponse(ClientboundPongResponsePacket packet) {
                long then = this.pingStart;
                long now = Util.getMillis();
                data.ping = now - then;
                connection.disconnect((Component)Component.translatable((String)"multiplayer.status.finished"));
                onPongResponse.run();
            }

            public void onDisconnect(DisconnectionDetails details) {
                if (!this.success) {
                    this.this$0.onPingFailed(details.reason(), data);
                    this.this$0.pingLegacyServer(address, rawAddress, data, eventLoopGroupHolder);
                }
            }

            public boolean isAcceptingMessages() {
                return connection.isConnected();
            }
        };
        try {
            connection.initiateServerboundStatusConnection(rawAddress.getHost(), rawAddress.getPort(), listener);
            connection.send((Packet)ServerboundStatusRequestPacket.INSTANCE);
        }
        catch (Throwable t) {
            LOGGER.error("Failed to ping server {}", (Object)rawAddress, (Object)t);
        }
    }

    private void onPingFailed(Component reason, ServerData data) {
        LOGGER.error("Can't ping {}: {}", (Object)data.ip, (Object)reason.getString());
        data.motd = CANT_CONNECT_MESSAGE;
        data.status = CommonComponents.EMPTY;
    }

    private void pingLegacyServer(InetSocketAddress resolvedAddress, final ServerAddress rawAddress, final ServerData data, EventLoopGroupHolder eventLoopGroupHolder) {
        ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(eventLoopGroupHolder.eventLoopGroup())).handler((ChannelHandler)new ChannelInitializer<Channel>(this){
            {
                Objects.requireNonNull(this$0);
            }

            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, (Object)true);
                }
                catch (ChannelException channelException) {
                    // empty catch block
                }
                channel.pipeline().addLast(new ChannelHandler[]{new LegacyServerPinger(rawAddress, (protocolVersion, gameVersion, motd, players, maxPlayers) -> {
                    data.setState(ServerData.State.INCOMPATIBLE);
                    data2.version = Component.literal((String)gameVersion);
                    data2.motd = Component.literal((String)motd);
                    data2.status = ServerStatusPinger.formatPlayerCount(players, maxPlayers);
                    data2.players = new ServerStatus.Players(maxPlayers, players, List.of());
                })});
            }
        })).channel(eventLoopGroupHolder.channelCls())).connect(resolvedAddress.getAddress(), resolvedAddress.getPort());
    }

    public static Component formatPlayerCount(int curPlayers, int maxPlayers) {
        MutableComponent current = Component.literal((String)Integer.toString(curPlayers)).withStyle(ChatFormatting.GRAY);
        MutableComponent max = Component.literal((String)Integer.toString(maxPlayers)).withStyle(ChatFormatting.GRAY);
        return Component.translatable((String)"multiplayer.status.player_count", (Object[])new Object[]{current, max}).withStyle(ChatFormatting.DARK_GRAY);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void tick() {
        List<Connection> list = this.connections;
        synchronized (list) {
            Iterator<Connection> iterator = this.connections.iterator();
            while (iterator.hasNext()) {
                Connection connection = iterator.next();
                if (connection.isConnected()) {
                    connection.tick();
                    continue;
                }
                iterator.remove();
                connection.handleDisconnection();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeAll() {
        List<Connection> list = this.connections;
        synchronized (list) {
            Iterator<Connection> iterator = this.connections.iterator();
            while (iterator.hasNext()) {
                Connection connection = iterator.next();
                if (!connection.isConnected()) continue;
                iterator.remove();
                connection.disconnect((Component)Component.translatable((String)"multiplayer.status.cancelled"));
            }
        }
    }
}

