/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.core.util.net.buffer;

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.AppbaseCore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * Abstract implementation of unpooled {@link BufferAllocator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractUnpooledBufferAllocator implements BufferAllocator {
    protected final UnpooledBufferAllocatorConfiguration unpooledBufferAllocatorConfiguration;
    protected final UnpooledByteBufAllocator unpooledByteBufAllocator;

    protected AbstractUnpooledBufferAllocator(
            final AppbaseCore appbaseCore,
            final boolean useDirectBuffer,
            final UnpooledBufferAllocatorConfiguration unpooledBufferAllocatorConfiguration)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(unpooledBufferAllocatorConfiguration, "unpooledBufferAllocatorConfiguration");

        appbaseCore.getLogService().getLogger(getClass()).info(
                        "Initializing {} with {}", getClass(), unpooledBufferAllocatorConfiguration);

        this.unpooledBufferAllocatorConfiguration = unpooledBufferAllocatorConfiguration;

        final boolean disableLeakDetector = unpooledBufferAllocatorConfiguration.isDisableLeakDetector();
        final Boolean tryNoCleaner = unpooledBufferAllocatorConfiguration.getTryNoCleaner();
        if (tryNoCleaner != null) {
            this.unpooledByteBufAllocator =
                    new UnpooledByteBufAllocator(useDirectBuffer, disableLeakDetector, tryNoCleaner);
        } else {
            this.unpooledByteBufAllocator =
                    new UnpooledByteBufAllocator(useDirectBuffer, disableLeakDetector);
        }

        registerMetrics(appbaseCore.getMetricService());
    }

    protected void registerMetrics(final MetricService metricService) {
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();

        final ByteBufAllocatorMetric metric = unpooledByteBufAllocator.metric();
        metricRegistry.register(
                MetricRegistry.name(getClass(), "usedDirectMemory"),
                (Gauge<Long>) metric::usedDirectMemory);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "usedHeapMemory"),
                (Gauge<Long>) metric::usedHeapMemory);
    }

    protected Buffer newBuffer(final ByteBuf byteBuf) {
        return new DefaultBuffer(byteBuf, this, getBufferPoolingStrategy(), getBufferSource());
    }

    @Override
    public Buffer buffer() {
        final ByteBuf byteBuf = unpooledByteBufAllocator.buffer();
        return newBuffer(byteBuf);
    }

    @Override
    public Buffer buffer(final int initialCapacity) {
        final ByteBuf byteBuf = unpooledByteBufAllocator.buffer(initialCapacity);
        return newBuffer(byteBuf);
    }

    @Override
    public Buffer buffer(final int initialCapacity, final int maxCapacity) {
        final ByteBuf byteBuf = unpooledByteBufAllocator.buffer(initialCapacity, maxCapacity);
        return newBuffer(byteBuf);
    }

    @Override
    public BufferPoolingStrategy getBufferPoolingStrategy() {
        return BufferPoolingStrategy.UNPOOLED;
    }

    @Override
    public ByteBufAllocator getNettyByteBufAllocator() {
        return unpooledByteBufAllocator;
    }
}
