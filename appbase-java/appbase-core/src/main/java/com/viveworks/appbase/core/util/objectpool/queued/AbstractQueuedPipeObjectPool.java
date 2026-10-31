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

package com.viveworks.appbase.core.util.objectpool.queued;

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;
import com.viveworks.appbase.core.util.objectpool.ObjectPool;
import com.viveworks.appbase.core.util.objectpool.Poolable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.agrona.concurrent.QueuedPipe;
import org.eclipse.microprofile.metrics.Gauge;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract implementation of an object pool using {@code org.queued.concurrent.QueuedPipe}.
 *
 * @author Navaneeth Mysore G
 * @param <T> The poolable type that this object pool is used for
 * @since 1.0.0
 */
@ThreadSafe
public abstract class AbstractQueuedPipeObjectPool<T extends Poolable> implements ObjectPool<T> {
    private final QueuedPipe<T> queuedPipe;
    private final ObjectFactory<T> objectFactory;

    private final Gauge addedCountGauge;
    private final Gauge removedCountGauge;
    private final Gauge remainingCapacityGauge;
    private final Gauge sizeGauge;

    @SuppressFBWarnings("OPM_OVERLY_PERMISSIVE_METHOD")
    protected AbstractQueuedPipeObjectPool(final MetricService metricService,
                                           final int objectPoolCapacity,
                                           final QueuedPipe<T> queuedPipe,
                                           final ObjectFactory<T> objectFactory)
    {
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.positive(objectPoolCapacity, "messageObjectPoolCapacity");
        AppbaseUtil.notNull(queuedPipe, "queuedPipe");
        AppbaseUtil.notNull(objectFactory, "objectFactory");

        this.queuedPipe = queuedPipe;
        this.objectFactory = objectFactory;

        for (int i = 0; i < objectPoolCapacity; ++i) {
            queuedPipe.add(objectFactory.newObject());
        }

        this.addedCountGauge = queuedPipe::addedCount;
        metricService
                .getMetricRegistry()
                .register(metricService.name(getClass(), "addedCount", "gauge"), addedCountGauge);
        this.removedCountGauge = queuedPipe::removedCount;
        metricService
                .getMetricRegistry()
                .register(metricService.name(getClass(), "removedCount", "gauge"), removedCountGauge);

        this.remainingCapacityGauge = queuedPipe::remainingCapacity;
        metricService
                .getMetricRegistry()
                .register(metricService.name(getClass(), "remainingCapacity", "gauge"), remainingCapacityGauge);

        this.sizeGauge = queuedPipe::size;
        metricService
                .getMetricRegistry()
                .register(metricService.name(getClass(), "size", "gauge"), remainingCapacityGauge);
    }

    @Override
    public T pollCleanObject() {
        T polledObject = queuedPipe.poll();
        if (polledObject == null) {
            polledObject = objectFactory.newObject();
        }

        if (!polledObject.isClean()) {
            polledObject.clean();
        }
        return polledObject;
    }

    @Override
    public void offerCleanObject(final T object) {
        AppbaseUtil.notNull(object, "object");

        if (!object.isClean()) {
            object.clean();
        }
        queuedPipe.offer(object);
    }

    public Gauge getAddedCountGauge() {
        return addedCountGauge;
    }

    public Gauge getRemovedCountGauge() {
        return removedCountGauge;
    }

    public Gauge getRemainingCapacityGauge() {
        return remainingCapacityGauge;
    }

    public Gauge getSizeGauge() {
        return sizeGauge;
    }

    @Override
    public void shutdown() {
        queuedPipe.clear();
    }
}
