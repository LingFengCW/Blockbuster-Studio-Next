/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.logging.LogUtils
 *  net.minecraft.ChatFormatting
 *  net.minecraft.CrashReport
 *  net.minecraft.CrashReportCategory
 *  net.minecraft.ReportType
 *  net.minecraft.core.Holder
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.Connection
 *  net.minecraft.network.DisconnectionDetails
 *  net.minecraft.network.PacketListener
 *  net.minecraft.network.PacketProcessor
 *  net.minecraft.network.ServerboundPacketListener
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.PacketUtils
 *  net.minecraft.network.protocol.common.ClientCommonPacketListener
 *  net.minecraft.network.protocol.common.ClientboundClearDialogPacket
 *  net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
 *  net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket
 *  net.minecraft.network.protocol.common.ClientboundDisconnectPacket
 *  net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
 *  net.minecraft.network.protocol.common.ClientboundPingPacket
 *  net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket
 *  net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
 *  net.minecraft.network.protocol.common.ClientboundServerLinksPacket
 *  net.minecraft.network.protocol.common.ClientboundShowDialogPacket
 *  net.minecraft.network.protocol.common.ClientboundStoreCookiePacket
 *  net.minecraft.network.protocol.common.ClientboundTransferPacket
 *  net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket
 *  net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
 *  net.minecraft.network.protocol.common.ServerboundPongPacket
 *  net.minecraft.network.protocol.common.ServerboundResourcePackPacket
 *  net.minecraft.network.protocol.common.ServerboundResourcePackPacket$Action
 *  net.minecraft.network.protocol.common.custom.BrandPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.DiscardedPayload
 *  net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket
 *  net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket
 *  net.minecraft.resources.Identifier
 *  net.minecraft.server.ServerLinks
 *  net.minecraft.server.ServerLinks$Entry
 *  net.minecraft.server.ServerLinks$KnownLinkType
 *  net.minecraft.server.ServerLinks$UntrustedEntry
 *  net.minecraft.server.dialog.Dialog
 *  net.minecraft.util.Util
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.dialog.DialogScreens;
import net.minecraft.client.gui.screens.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ClientCommonPacketListenerImpl
implements ClientCommonPacketListener {
    private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable((String)"disconnect.lost");
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Minecraft minecraft;
    protected final Connection connection;
    protected final @Nullable ServerData serverData;
    protected @Nullable String serverBrand;
    protected final WorldSessionTelemetryManager telemetryManager;
    protected final @Nullable Screen postDisconnectScreen;
    protected boolean isTransferring;
    private final List<DeferredPacket> deferredPackets = new ArrayList<DeferredPacket>();
    protected final Map<Identifier, byte[]> serverCookies;
    protected Map<String, String> customReportDetails;
    private ServerLinks serverLinks;
    protected final Map<UUID, PlayerInfo> seenPlayers;
    protected boolean seenInsecureChatWarning;

    protected ClientCommonPacketListenerImpl(Minecraft minecraft, Connection connection, CommonListenerCookie cookie) {
        this.minecraft = minecraft;
        this.connection = connection;
        this.serverData = cookie.serverData();
        this.serverBrand = cookie.serverBrand();
        this.telemetryManager = cookie.telemetryManager();
        this.postDisconnectScreen = cookie.postDisconnectScreen();
        this.serverCookies = cookie.serverCookies();
        this.customReportDetails = cookie.customReportDetails();
        this.serverLinks = cookie.serverLinks();
        this.seenPlayers = new HashMap<UUID, PlayerInfo>(cookie.seenPlayers());
        this.seenInsecureChatWarning = cookie.seenInsecureChatWarning();
    }

    public ServerLinks serverLinks() {
        return this.serverLinks;
    }

    public void onPacketError(Packet packet, Exception cause) {
        LOGGER.error("Failed to handle packet {}, disconnecting", (Object)packet, (Object)cause);
        Optional<Path> report = this.storeDisconnectionReport(packet, cause);
        Optional<URI> bugReportLink = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        this.connection.disconnect(new DisconnectionDetails((Component)Component.translatable((String)"disconnect.packetError"), report, bugReportLink));
    }

    public DisconnectionDetails createDisconnectionInfo(Component reason, Throwable cause) {
        Optional<Path> report = this.storeDisconnectionReport(null, cause);
        Optional<URI> bugReportUrl = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        return new DisconnectionDetails(reason, report, bugReportUrl);
    }

    private Optional<Path> storeDisconnectionReport(@Nullable Packet packet, Throwable cause) {
        CrashReport report = CrashReport.forThrowable((Throwable)cause, (String)"Packet handling error");
        PacketUtils.fillCrashReport((CrashReport)report, (PacketListener)this, (Packet)packet);
        Path debugDir = this.minecraft.gameDirectory.toPath().resolve("debug");
        Path reportFile = debugDir.resolve("disconnect-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Optional bugReportLink = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT);
        List extraComments = bugReportLink.map(link -> List.of("Server bug reporting link: " + String.valueOf(link.link()))).orElse(List.of());
        if (report.saveToFile(reportFile, ReportType.NETWORK_PROTOCOL_ERROR, extraComments)) {
            return Optional.of(reportFile);
        }
        return Optional.empty();
    }

    public boolean shouldHandleMessage(Packet<?> packet) {
        if (super.shouldHandleMessage(packet)) {
            return true;
        }
        return this.isTransferring && (packet instanceof ClientboundStoreCookiePacket || packet instanceof ClientboundTransferPacket);
    }

    public void handleKeepAlive(ClientboundKeepAlivePacket packet) {
        this.sendWhen((Packet<? extends ServerboundPacketListener>)new ServerboundKeepAlivePacket(packet.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
    }

    public void handlePing(ClientboundPingPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        this.send((Packet<?>)new ServerboundPongPacket(packet.getId()));
    }

    public void handleCustomPayload(ClientboundCustomPayloadPacket packet) {
        CustomPacketPayload payload = packet.payload();
        if (payload instanceof DiscardedPayload) {
            return;
        }
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        if (payload instanceof BrandPayload) {
            BrandPayload brand = (BrandPayload)payload;
            this.serverBrand = brand.brand();
            this.telemetryManager.onServerBrandReceived(brand.brand());
        } else {
            this.handleCustomPayload(payload);
        }
    }

    protected abstract void handleCustomPayload(CustomPacketPayload var1);

    public void handleResourcePackPush(ClientboundResourcePackPushPacket packet) {
        ServerData.ServerPackStatus serverPackStatus;
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        UUID packId = packet.id();
        URL url = ClientCommonPacketListenerImpl.parseResourcePackUrl(packet.url());
        if (url == null) {
            this.connection.send((Packet)new ServerboundResourcePackPacket(packId, ServerboundResourcePackPacket.Action.INVALID_URL));
            return;
        }
        String hash = packet.hash();
        boolean required = packet.required();
        ServerData.ServerPackStatus serverPackStatus2 = serverPackStatus = this.serverData != null ? this.serverData.getResourcePackStatus() : ServerData.ServerPackStatus.PROMPT;
        if (serverPackStatus == ServerData.ServerPackStatus.PROMPT || required && serverPackStatus == ServerData.ServerPackStatus.DISABLED) {
            this.minecraft.gui.setScreen(this.addOrUpdatePackPrompt(packId, url, hash, required, packet.prompt().orElse(null)));
        } else {
            this.minecraft.getDownloadedPackSource().pushPack(packId, url, hash);
        }
    }

    public void handleResourcePackPop(ClientboundResourcePackPopPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        packet.id().ifPresentOrElse(id -> this.minecraft.getDownloadedPackSource().popPack((UUID)id), () -> this.minecraft.getDownloadedPackSource().popAll());
    }

    private static Component preparePackPrompt(Component header, @Nullable Component prompt) {
        if (prompt == null) {
            return header;
        }
        return Component.translatable((String)"multiplayer.texturePrompt.serverPrompt", (Object[])new Object[]{header, prompt});
    }

    private static @Nullable URL parseResourcePackUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if ("http".equals(protocol) || "https".equals(protocol)) {
                return url;
            }
        }
        catch (MalformedURLException e) {
            return null;
        }
        return null;
    }

    public void handleRequestCookie(ClientboundCookieRequestPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        this.connection.send((Packet)new ServerboundCookieResponsePacket(packet.key(), this.serverCookies.get(packet.key())));
    }

    public void handleStoreCookie(ClientboundStoreCookiePacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        this.serverCookies.put(packet.key(), packet.payload());
    }

    public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        this.customReportDetails = packet.details();
    }

    public void handleServerLinks(ClientboundServerLinksPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        List untrustedEntries = packet.links();
        ImmutableList.Builder trustedEntries = ImmutableList.builderWithExpectedSize((int)untrustedEntries.size());
        for (ServerLinks.UntrustedEntry entry : untrustedEntries) {
            try {
                URI parsedLink = Util.parseAndValidateUntrustedUri((String)entry.link());
                trustedEntries.add((Object)new ServerLinks.Entry(entry.type(), parsedLink));
            }
            catch (Exception e) {
                LOGGER.warn("Received invalid link for type {}:{}", new Object[]{entry.type(), entry.link(), e});
            }
        }
        this.serverLinks = new ServerLinks((List)trustedEntries.build());
    }

    public void handleShowDialog(ClientboundShowDialogPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        this.showDialog((Holder<Dialog>)packet.dialog(), this.minecraft.gui.screen());
    }

    protected abstract DialogConnectionAccess createDialogAccess();

    public void showDialog(Holder<Dialog> dialog, @Nullable Screen activeScreen) {
        this.showDialog(dialog, this.createDialogAccess(), activeScreen);
    }

    protected void showDialog(Holder<Dialog> dialog, DialogConnectionAccess connectionAccess, @Nullable Screen activeScreen) {
        Screen previousScreen;
        if (activeScreen instanceof DialogScreen.WarningScreen) {
            Screen screen;
            DialogScreen.WarningScreen existingWarningScreen = (DialogScreen.WarningScreen)activeScreen;
            Screen hiddenScreen = existingWarningScreen.returnScreen();
            if (hiddenScreen instanceof DialogScreen) {
                DialogScreen hiddenDialog = (DialogScreen)hiddenScreen;
                screen = hiddenDialog.previousScreen();
            } else {
                screen = hiddenScreen;
            }
            Screen previousScreen2 = screen;
            DialogScreen<Dialog> newDialogScreen = DialogScreens.createFromData((Dialog)dialog.value(), previousScreen2, connectionAccess);
            if (newDialogScreen != null) {
                existingWarningScreen.updateReturnScreen(newDialogScreen);
            } else {
                LOGGER.warn("Failed to show dialog for data {}", dialog);
            }
            return;
        }
        if (activeScreen instanceof DialogScreen) {
            DialogScreen existingDialog = (DialogScreen)activeScreen;
            previousScreen = existingDialog.previousScreen();
        } else if (activeScreen instanceof WaitingForResponseScreen) {
            WaitingForResponseScreen waitScreen = (WaitingForResponseScreen)activeScreen;
            previousScreen = waitScreen.previousScreen();
        } else {
            previousScreen = activeScreen;
        }
        DialogScreen<Dialog> screen = DialogScreens.createFromData((Dialog)dialog.value(), previousScreen, connectionAccess);
        if (screen != null) {
            this.minecraft.gui.setScreen(screen);
        } else {
            LOGGER.warn("Failed to show dialog for data {}", dialog);
        }
    }

    public void handleClearDialog(ClientboundClearDialogPacket packet) {
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        this.clearDialog();
    }

    public void clearDialog() {
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof DialogScreen.WarningScreen) {
            DialogScreen.WarningScreen existingWarningScreen = (DialogScreen.WarningScreen)screen;
            Screen currentReturnScreen = existingWarningScreen.returnScreen();
            if (currentReturnScreen instanceof DialogScreen) {
                DialogScreen dialogScreen = (DialogScreen)currentReturnScreen;
                existingWarningScreen.updateReturnScreen(dialogScreen.previousScreen());
            }
        } else {
            screen = this.minecraft.gui.screen();
            if (screen instanceof DialogScreen) {
                DialogScreen dialog = (DialogScreen)screen;
                this.minecraft.gui.setScreen(dialog.previousScreen());
            }
        }
    }

    public void handleTransfer(ClientboundTransferPacket packet) {
        this.isTransferring = true;
        PacketUtils.ensureRunningOnSameThread((Packet)packet, (PacketListener)this, (PacketProcessor)this.minecraft.packetProcessor());
        if (this.serverData == null) {
            throw new IllegalStateException("Cannot transfer to server from singleplayer");
        }
        this.connection.disconnect((Component)Component.translatable((String)"disconnect.transfer"));
        this.connection.setReadOnly();
        this.connection.handleDisconnection();
        ServerAddress address = new ServerAddress(packet.host(), packet.port());
        ConnectScreen.startConnecting(Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new), this.minecraft, address, this.serverData, false, new TransferState(this.serverCookies, this.seenPlayers, this.seenInsecureChatWarning));
    }

    public void handleDisconnect(ClientboundDisconnectPacket packet) {
        this.connection.disconnect(packet.reason());
    }

    protected void sendDeferredPackets() {
        Iterator<DeferredPacket> iterator = this.deferredPackets.iterator();
        while (iterator.hasNext()) {
            DeferredPacket deferredPacket = iterator.next();
            if (deferredPacket.sendCondition().getAsBoolean()) {
                this.send(deferredPacket.packet);
                iterator.remove();
                continue;
            }
            if (deferredPacket.expirationTime() > Util.getMillis()) continue;
            iterator.remove();
        }
    }

    public void send(Packet<?> packet) {
        this.connection.send(packet);
    }

    public void onDisconnect(DisconnectionDetails details) {
        this.telemetryManager.onDisconnect();
        this.minecraft.disconnect(this.createDisconnectScreen(details), this.isTransferring);
        LOGGER.warn("Client disconnected with reason: {}", (Object)details.reason().getString());
    }

    public void fillListenerSpecificCrashDetails(CrashReport report, CrashReportCategory connectionDetails) {
        connectionDetails.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
        connectionDetails.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<none>");
        connectionDetails.setDetail("Server brand", () -> this.serverBrand);
        if (!this.customReportDetails.isEmpty()) {
            CrashReportCategory serverDetailsCategory = report.addCategory("Custom Server Details");
            this.customReportDetails.forEach((arg_0, arg_1) -> ((CrashReportCategory)serverDetailsCategory).setDetail(arg_0, arg_1));
        }
    }

    protected Screen createDisconnectScreen(DisconnectionDetails details) {
        Screen callbackScreen = Objects.requireNonNullElseGet(this.postDisconnectScreen, () -> this.serverData != null ? new JoinMultiplayerScreen(new TitleScreen()) : new TitleScreen());
        if (this.serverData != null && this.serverData.isRealm()) {
            return new DisconnectedScreen(callbackScreen, GENERIC_DISCONNECT_MESSAGE, details, CommonComponents.GUI_BACK);
        }
        return new DisconnectedScreen(callbackScreen, GENERIC_DISCONNECT_MESSAGE, details);
    }

    public @Nullable String serverBrand() {
        return this.serverBrand;
    }

    private void sendWhen(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier condition, Duration expireAfterDuration) {
        if (condition.getAsBoolean()) {
            this.send(packet);
        } else {
            this.deferredPackets.add(new DeferredPacket(packet, condition, Util.getMillis() + expireAfterDuration.toMillis()));
        }
    }

    private Screen addOrUpdatePackPrompt(UUID packId, URL url, String hash, boolean required, @Nullable Component prompt) {
        Screen currentScreen = this.minecraft.gui.screen();
        if (currentScreen instanceof PackConfirmScreen) {
            PackConfirmScreen promptScreen = (PackConfirmScreen)currentScreen;
            return promptScreen.update(this.minecraft, packId, url, hash, required, prompt);
        }
        return new PackConfirmScreen(this, this.minecraft, currentScreen, List.of(new PackConfirmScreen.PendingRequest(packId, url, hash)), required, prompt);
    }

    private record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
    }

    private class PackConfirmScreen
    extends ConfirmScreen {
        private final List<PendingRequest> requests;
        private final @Nullable Screen parentScreen;
        final /* synthetic */ ClientCommonPacketListenerImpl this$0;

        private PackConfirmScreen(ClientCommonPacketListenerImpl clientCommonPacketListenerImpl, @Nullable Minecraft minecraft, Screen parentScreen, List<PendingRequest> requests, @Nullable boolean required, Component prompt) {
            ClientCommonPacketListenerImpl clientCommonPacketListenerImpl2 = clientCommonPacketListenerImpl;
            Objects.requireNonNull(clientCommonPacketListenerImpl2);
            this.this$0 = clientCommonPacketListenerImpl2;
            super(result -> {
                minecraft.gui.setScreen(parentScreen);
                DownloadedPackSource packSource = minecraft.getDownloadedPackSource();
                if (result) {
                    if (this$0.serverData != null) {
                        this$0.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                    }
                    packSource.allowServerPacks();
                } else {
                    packSource.rejectServerPacks();
                    if (required) {
                        this$0.connection.disconnect((Component)Component.translatable((String)"multiplayer.requiredTexturePrompt.disconnect"));
                    } else if (this$0.serverData != null) {
                        this$0.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                    }
                }
                for (PendingRequest request : requests) {
                    packSource.pushPack(request.id, request.url, request.hash);
                }
                if (this$0.serverData != null) {
                    ServerList.saveSingleServer(this$0.serverData);
                }
            }, (Component)(required ? Component.translatable((String)"multiplayer.requiredTexturePrompt.line1") : Component.translatable((String)"multiplayer.texturePrompt.line1")), ClientCommonPacketListenerImpl.preparePackPrompt((Component)(required ? Component.translatable((String)"multiplayer.requiredTexturePrompt.line2").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}) : Component.translatable((String)"multiplayer.texturePrompt.line2")), prompt), required ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES, required ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO);
            this.requests = requests;
            this.parentScreen = parentScreen;
        }

        public PackConfirmScreen update(Minecraft minecraft, UUID id, URL url, String hash, boolean required, @Nullable Component prompt) {
            ImmutableList extendedRequests = ImmutableList.builderWithExpectedSize((int)(this.requests.size() + 1)).addAll(this.requests).add((Object)new PendingRequest(id, url, hash)).build();
            return new PackConfirmScreen(this.this$0, minecraft, this.parentScreen, (List<PendingRequest>)extendedRequests, required, prompt);
        }

        private record PendingRequest(UUID id, URL url, String hash) {
        }
    }

    protected abstract class CommonDialogAccess
    implements DialogConnectionAccess {
        final /* synthetic */ ClientCommonPacketListenerImpl this$0;

        protected CommonDialogAccess(ClientCommonPacketListenerImpl this$0) {
            ClientCommonPacketListenerImpl clientCommonPacketListenerImpl = this$0;
            Objects.requireNonNull(clientCommonPacketListenerImpl);
            this.this$0 = clientCommonPacketListenerImpl;
        }

        @Override
        public void disconnect(Component message) {
            this.this$0.connection.disconnect(message);
            this.this$0.connection.handleDisconnection();
        }

        @Override
        public void openDialog(Holder<Dialog> dialog, @Nullable Screen activeScreen) {
            this.this$0.showDialog(dialog, this, activeScreen);
        }

        @Override
        public void sendCustomAction(Identifier id, Optional<Tag> payload) {
            this.this$0.send((Packet<?>)new ServerboundCustomClickActionPacket(id, payload));
        }

        @Override
        public ServerLinks serverLinks() {
            return this.this$0.serverLinks();
        }
    }
}

