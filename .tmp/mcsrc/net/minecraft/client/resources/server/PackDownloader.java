/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.packs.DownloadQueue$BatchResult
 *  net.minecraft.server.packs.DownloadQueue$DownloadRequest
 */
package net.minecraft.client.resources.server;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.packs.DownloadQueue;

public interface PackDownloader {
    public void download(Map<UUID, DownloadQueue.DownloadRequest> var1, Consumer<DownloadQueue.BatchResult> var2);
}

