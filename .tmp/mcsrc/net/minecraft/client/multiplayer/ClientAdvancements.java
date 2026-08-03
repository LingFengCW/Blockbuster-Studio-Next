/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.advancements.AdvancementNode
 *  net.minecraft.advancements.AdvancementProgress
 *  net.minecraft.advancements.AdvancementTree
 *  net.minecraft.advancements.AdvancementTree$Listener
 *  net.minecraft.advancements.DisplayInfo
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
 *  net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket
 *  net.minecraft.resources.Identifier
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ClientAdvancements {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final WorldSessionTelemetryManager telemetryManager;
    private final AdvancementTree tree = new AdvancementTree();
    private final Map<AdvancementHolder, AdvancementProgress> progress = new Object2ObjectOpenHashMap();
    private @Nullable Listener listener;
    private @Nullable AdvancementHolder selectedTab;

    public ClientAdvancements(Minecraft minecraft, WorldSessionTelemetryManager telemetryManager) {
        this.minecraft = minecraft;
        this.telemetryManager = telemetryManager;
    }

    public void update(ClientboundUpdateAdvancementsPacket packet) {
        if (packet.shouldReset()) {
            this.tree.clear();
            this.progress.clear();
        }
        this.tree.remove(packet.getRemoved());
        this.tree.addAll((Collection)packet.getAdded());
        for (Map.Entry entry : packet.getProgress().entrySet()) {
            AdvancementNode node = this.tree.get((Identifier)entry.getKey());
            if (node != null) {
                AdvancementProgress progress = (AdvancementProgress)entry.getValue();
                progress.update(node.advancement().requirements());
                this.progress.put(node.holder(), progress);
                if (this.listener != null) {
                    this.listener.onUpdateAdvancementProgress(node, progress);
                }
                if (packet.shouldReset() || !progress.isDone()) continue;
                if (this.minecraft.level != null) {
                    this.telemetryManager.onAdvancementDone(this.minecraft.level, node.holder());
                }
                Optional display = node.advancement().display();
                if (!packet.shouldShowAdvancements() || !display.isPresent() || !((DisplayInfo)display.get()).shouldShowToast()) continue;
                this.minecraft.gui.toastManager().addToast(new AdvancementToast(node.holder()));
                continue;
            }
            LOGGER.warn("Server informed client about progress for unknown advancement {}", entry.getKey());
        }
    }

    public AdvancementTree getTree() {
        return this.tree;
    }

    public void setSelectedTab(@Nullable AdvancementHolder selectedTab, boolean tellServer) {
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null && selectedTab != null && tellServer) {
            connection.send((Packet<?>)ServerboundSeenAdvancementsPacket.openedTab((AdvancementHolder)selectedTab));
        }
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab;
            if (this.listener != null) {
                this.listener.onSelectedTabChanged(selectedTab);
            }
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        this.tree.setListener((AdvancementTree.Listener)listener);
        if (listener != null) {
            this.progress.forEach((holder, progress) -> {
                AdvancementNode node = this.tree.get(holder);
                if (node != null) {
                    listener.onUpdateAdvancementProgress(node, (AdvancementProgress)progress);
                }
            });
            listener.onSelectedTabChanged(this.selectedTab);
        }
    }

    public @Nullable AdvancementHolder get(Identifier id) {
        AdvancementNode node = this.tree.get(id);
        return node != null ? node.holder() : null;
    }

    public static interface Listener
    extends AdvancementTree.Listener {
        public void onUpdateAdvancementProgress(AdvancementNode var1, AdvancementProgress var2);

        public void onSelectedTabChanged(@Nullable AdvancementHolder var1);
    }
}

