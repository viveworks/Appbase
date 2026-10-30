/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
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
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * Abstract implementation of pooled {@link BufferAllocator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractPooledBufferAllocator implements BufferAllocator {
    protected final PooledBufferAllocatorConfiguration pooledBufferAllocatorConfiguration;
    protected final PooledByteBufAllocator pooledByteBufAllocator;

    protected AbstractPooledBufferAllocator(
            final AppbaseCore appbaseCore,
            final boolean useDirectBuffer,
            final PooledBufferAllocatorConfiguration pooledBufferAllocatorConfiguration)
    {
        AppbaseUtil.notNull(pooledBufferAllocatorConfiguration, "pooledBufferAllocatorConfiguration");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        appbaseCore.getLogService().getLogger(getClass()).info(
                "Initializing {} with {}", getClass(), pooledBufferAllocatorConfiguration);

        this.pooledBufferAllocatorConfiguration = pooledBufferAllocatorConfiguration;
        final Integer directMemoryCacheAlignment = pooledBufferAllocatorConfiguration.getDirectMemoryCacheAlignment();
        final Integer maxOrder = pooledBufferAllocatorConfiguration.getMaxOrder();
        final Integer pageSize = pooledBufferAllocatorConfiguration.getPageSize();
        final Integer smallCacheSize = pooledBufferAllocatorConfiguration.getSmallCacheSize();
        final Integer normalCacheSize = pooledBufferAllocatorConfiguration.getNormalCacheSize();
        final Boolean useCacheForAllThreads = pooledBufferAllocatorConfiguration.getUseCacheForAllThreads();
        Integer numberOfDirectArenas = pooledBufferAllocatorConfiguration.getNumberOfDirectArenas();
        Integer numberOfHeapArenas = pooledBufferAllocatorConfiguration.getNumberOfHeapArenas();
        if (useCacheForAllThreads != null &&
                smallCacheSize != null &&
                normalCacheSize != null &&
                pageSize != null &&
                maxOrder != null &&
                directMemoryCacheAlignment != null &&
                (numberOfDirectArenas != null || numberOfHeapArenas != null))
        {
            if (numberOfDirectArenas == null) {
                numberOfDirectArenas = 0;
            }
            if (numberOfHeapArenas == null) {
                numberOfHeapArenas = 0;
            }
            this.pooledByteBufAllocator = new PooledByteBufAllocator(
                    useDirectBuffer,
                    numberOfHeapArenas,
                    numberOfDirectArenas,
                    pageSize,
                    maxOrder,
                    smallCacheSize,
                    normalCacheSize,
                    useCacheForAllThreads,
                    directMemoryCacheAlignment);
        } else {
            this.pooledByteBufAllocator = new PooledByteBufAllocator(useDirectBuffer);
        }
        registerMetrics(appbaseCore.getMetricService());
    }

    protected void registerMetrics(final MetricService metricService) {
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();

        final PooledByteBufAllocatorMetric metric = pooledByteBufAllocator.metric();
        metricRegistry.register(
                MetricRegistry.name(getClass(), "usedDirectMemory"),
                (Gauge<Long>) metric::usedDirectMemory);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "usedHeapMemory"),
                (Gauge<Long>) metric::usedHeapMemory);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "chunkSize"),
                (Gauge<Integer>) metric::chunkSize);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "normalCacheSize"),
                (Gauge<Integer>) metric::normalCacheSize);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "numThreadLocalCaches"),
                (Gauge<Integer>) metric::numThreadLocalCaches);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "smallCacheSize"),
                (Gauge<Integer>) metric::smallCacheSize);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "numDirectArenas"),
                (Gauge<Integer>) metric::numDirectArenas);
        metricRegistry.register(
                MetricRegistry.name(getClass(), "numHeapArenas"),
                (Gauge<Integer>) metric::numHeapArenas);
    }

    protected Buffer newBuffer(final ByteBuf byteBuf) {
        return new DefaultBuffer(byteBuf, this, getBufferPoolingStrategy(), getBufferSource());
    }

    @Override
    public Buffer buffer() {
        final ByteBuf byteBuf = pooledByteBufAllocator.buffer();
        return newBuffer(byteBuf);
    }

    @Override
    public Buffer buffer(final int initialCapacity) {
        AppbaseUtil.positiveOrZero(initialCapacity, "initialCapacity");

        final ByteBuf byteBuf = pooledByteBufAllocator.buffer(initialCapacity);
        return newBuffer(byteBuf);
    }

    @Override
    public Buffer buffer(final int initialCapacity, final int maxCapacity) {
        AppbaseUtil.positiveOrZero(initialCapacity, "initialCapacity");
        AppbaseUtil.positiveOrZero(maxCapacity, "maxCapacity");

        final ByteBuf byteBuf = pooledByteBufAllocator.buffer(initialCapacity, maxCapacity);
        return newBuffer(byteBuf);
    }

    @Override
    public BufferPoolingStrategy getBufferPoolingStrategy() {
        return BufferPoolingStrategy.POOLED;
    }

    @Override
    public ByteBufAllocator getNettyByteBufAllocator() {
        return pooledByteBufAllocator;
    }
}
