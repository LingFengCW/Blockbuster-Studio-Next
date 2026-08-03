/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  net.minecraft.util.profiling.ProfileCollector
 *  net.minecraft.util.profiling.metrics.MetricCategory
 *  net.minecraft.util.profiling.metrics.MetricSampler
 *  net.minecraft.util.profiling.metrics.MetricsSamplerProvider
 *  net.minecraft.util.profiling.metrics.profiling.ProfilerSamplerAdapter
 *  net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider
 */
package net.minecraft.client.profiling;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.util.profiling.ProfileCollector;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsSamplerProvider;
import net.minecraft.util.profiling.metrics.profiling.ProfilerSamplerAdapter;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;

public class ClientMetricsSamplersProvider
implements MetricsSamplerProvider {
    private final LevelRenderer levelRenderer;
    private final LevelExtractor levelExtractor;
    private final Set<MetricSampler> samplers = new ObjectOpenHashSet();
    private final ProfilerSamplerAdapter samplerFactory = new ProfilerSamplerAdapter();

    public ClientMetricsSamplersProvider(LongSupplier wallTimeSource, LevelRenderer levelRenderer, LevelExtractor levelExtractor) {
        this.levelRenderer = levelRenderer;
        this.levelExtractor = levelExtractor;
        this.samplers.add(ServerMetricsSamplersProvider.tickTimeSampler((LongSupplier)wallTimeSource));
        this.registerStaticSamplers();
    }

    private void registerStaticSamplers() {
        this.samplers.addAll(ServerMetricsSamplersProvider.runtimeIndependentSamplers());
        this.samplers.add(MetricSampler.createExtractSampler((String)"totalChunks", (MetricCategory)MetricCategory.CHUNK_RENDERING, this.levelExtractor::totalSections));
        this.samplers.add(MetricSampler.createExtractSampler((String)"renderedChunks", (MetricCategory)MetricCategory.CHUNK_RENDERING, this.levelExtractor::countRenderedSections));
        this.samplers.add(MetricSampler.createExtractSampler((String)"lastViewDistance", (MetricCategory)MetricCategory.CHUNK_RENDERING, this.levelExtractor::lastViewDistance));
        SectionRenderDispatcher sectionRenderDispatcher = this.levelRenderer.sectionRenderDispatcher();
        if (sectionRenderDispatcher != null) {
            this.samplers.add(MetricSampler.createExtractSampler((String)"freeBufferCount", (MetricCategory)MetricCategory.CHUNK_RENDERING_DISPATCHING, sectionRenderDispatcher::getFreeBufferCount));
            this.samplers.add(MetricSampler.createExtractSampler((String)"compileQueueSize", (MetricCategory)MetricCategory.CHUNK_RENDERING_DISPATCHING, sectionRenderDispatcher::getCompileQueueSize));
        }
        this.samplers.add(MetricSampler.createExtractSampler((String)"gpuUtilization", (MetricCategory)MetricCategory.GPU, Minecraft.getInstance()::getGpuUtilization));
    }

    public Set<MetricSampler> samplers(Supplier<ProfileCollector> singleTickProfiler) {
        this.samplers.addAll(this.samplerFactory.newSamplersFoundInProfiler(singleTickProfiler));
        return this.samplers;
    }
}

