/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.ChatFormatting
 *  net.minecraft.SharedConstants
 *  net.minecraft.network.chat.ClickEvent
 *  net.minecraft.network.chat.ClickEvent$OpenFile
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.Util
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Screenshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SCREENSHOT_DIR = "screenshots";

    public static void grab(File workDir, RenderTarget target, Consumer<Component> callback) {
        Screenshot.grab(workDir, null, target, 1, callback);
    }

    public static void grab(Minecraft minecraft, boolean debugPanoramaRequested) {
        if (debugPanoramaRequested && SharedConstants.DEBUG_PANORAMA_SCREENSHOT) {
            minecraft.showDebugChat(minecraft.grabPanoramixScreenshot(minecraft.gameDirectory));
        } else {
            Screenshot.grab(minecraft.gameDirectory, minecraft.gameRenderer.mainRenderTarget(), message -> minecraft.execute(() -> minecraft.showDebugChat((Component)message)));
        }
    }

    public static void grab(File workDir, @Nullable String forceName, RenderTarget target, int downscaleFactor, Consumer<Component> callback) {
        Screenshot.takeScreenshot(target, downscaleFactor, image -> {
            File picDir = new File(workDir, SCREENSHOT_DIR);
            picDir.mkdir();
            File file = forceName == null ? Screenshot.getFile(picDir) : new File(picDir, forceName);
            Util.ioPool().execute(() -> {
                try (NativeImage twrVar0$ = image;){
                    image.writeToFile(file);
                    MutableComponent component = Component.literal((String)file.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle(s -> s.withClickEvent((ClickEvent)new ClickEvent.OpenFile(file.getAbsoluteFile())));
                    callback.accept((Component)Component.translatable((String)"screenshot.success", (Object[])new Object[]{component}));
                }
                catch (Exception e) {
                    LOGGER.warn("Couldn't save screenshot", (Throwable)e);
                    callback.accept((Component)Component.translatable((String)"screenshot.failure", (Object[])new Object[]{e.getMessage()}));
                }
            });
        });
    }

    public static void takeScreenshot(RenderTarget target, Consumer<NativeImage> callback) {
        Screenshot.takeScreenshot(target, 1, callback);
    }

    public static void takeScreenshot(RenderTarget target, int downscaleFactor, Consumer<NativeImage> callback) {
        int width = target.width;
        int height = target.height;
        GpuTexture sourceTexture = target.getColorTexture();
        if (sourceTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        }
        if (width % downscaleFactor != 0 || height % downscaleFactor != 0) {
            throw new IllegalArgumentException("Image size is not divisible by downscale factor");
        }
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Screenshot buffer", 9, (long)width * (long)height * (long)sourceTexture.getFormat().blockSize());
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(sourceTexture, buffer, 0L, () -> {
            try (GpuBufferSlice.MappedView read = buffer.map(true, false);){
                int outputHeight = height / downscaleFactor;
                int outputWidth = width / downscaleFactor;
                NativeImage image = new NativeImage(outputWidth, outputHeight, false);
                for (int y = 0; y < outputHeight; ++y) {
                    for (int x = 0; x < outputWidth; ++x) {
                        if (downscaleFactor == 1) {
                            int argb = read.data().getInt((x + y * width) * sourceTexture.getFormat().blockSize());
                            image.setPixelABGR(x, height - y - 1, argb | 0xFF000000);
                            continue;
                        }
                        int red = 0;
                        int green = 0;
                        int blue = 0;
                        for (int i = 0; i < downscaleFactor; ++i) {
                            for (int j = 0; j < downscaleFactor; ++j) {
                                int argb = read.data().getInt((x * downscaleFactor + i + (y * downscaleFactor + j) * width) * sourceTexture.getFormat().blockSize());
                                red += ARGB.red((int)argb);
                                green += ARGB.green((int)argb);
                                blue += ARGB.blue((int)argb);
                            }
                        }
                        int sampleCount = downscaleFactor * downscaleFactor;
                        image.setPixelABGR(x, outputHeight - y - 1, ARGB.color((int)255, (int)(red / sampleCount), (int)(green / sampleCount), (int)(blue / sampleCount)));
                    }
                }
                callback.accept(image);
            }
            buffer.close();
        }, 0);
    }

    private static File getFile(File picDir) {
        String name = Util.getFilenameFormattedDateTime();
        int count = 1;
        File file;
        while ((file = new File(picDir, name + (String)(count == 1 ? "" : "_" + count) + ".png")).exists()) {
            ++count;
        }
        return file;
    }
}

