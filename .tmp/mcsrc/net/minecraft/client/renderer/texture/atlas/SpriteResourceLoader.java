/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.Identifier
 *  net.minecraft.server.packs.metadata.MetadataSectionType
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceMetadata
 *  net.minecraft.util.Mth
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture.atlas;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@FunctionalInterface
public interface SpriteResourceLoader {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static SpriteResourceLoader create(Set<MetadataSectionType<?>> additionalMetadataSections) {
        return (spriteLocation, resource) -> {
            FrameSize frameSize;
            NativeImage image;
            List additionalMetadata;
            Optional textureInfo;
            Optional animationInfo;
            try {
                ResourceMetadata metadata = resource.metadata();
                animationInfo = metadata.getSection(AnimationMetadataSection.TYPE);
                textureInfo = metadata.getSection(TextureMetadataSection.TYPE);
                additionalMetadata = metadata.getTypedSections((Collection)additionalMetadataSections);
            }
            catch (Exception e) {
                LOGGER.error("Unable to parse metadata from {}", (Object)spriteLocation, (Object)e);
                return null;
            }
            try (InputStream is = resource.open();){
                image = NativeImage.read(is);
            }
            catch (IOException e) {
                LOGGER.error("Using missing texture, unable to load {}", (Object)spriteLocation, (Object)e);
                return null;
            }
            if (animationInfo.isPresent()) {
                frameSize = ((AnimationMetadataSection)animationInfo.get()).calculateFrameSize(image.getWidth(), image.getHeight());
                if (!Mth.isMultipleOf((int)image.getWidth(), (int)frameSize.width()) || !Mth.isMultipleOf((int)image.getHeight(), (int)frameSize.height())) {
                    LOGGER.error("Image {} size {},{} is not multiple of frame size {},{}", new Object[]{spriteLocation, image.getWidth(), image.getHeight(), frameSize.width(), frameSize.height()});
                    image.close();
                    return null;
                }
            } else {
                frameSize = new FrameSize(image.getWidth(), image.getHeight());
            }
            return new SpriteContents(spriteLocation, frameSize, image, animationInfo, additionalMetadata, textureInfo);
        };
    }

    public @Nullable SpriteContents loadSprite(Identifier var1, Resource var2);
}

