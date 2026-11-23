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
package com.viveworks.appbase.client.service.zookeeper.accessor;

import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientService;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.RetryNTimes;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDistributedCounterZKA implements DistributedCounterZKA {
    private final DistributedAtomicLong distributedAtomicLong;

    private final Timer currentTimer;
    private final Timer incrementTimer;
    private final Timer decrementTimer;
    private final Timer addTimer;
    private final Timer subtractTimer;
    private final Timer setTimer;

    public DefaultDistributedCounterZKA(
            final AppbaseCore appbaseCore,
            final ZooKeeperClientService zooKeeperClientService,
            final String counterPath)
    {
        this(appbaseCore, zooKeeperClientService, counterPath, new RetryNTimes(5, 100));
    }

    public DefaultDistributedCounterZKA(
            final AppbaseCore appbaseCore,
            final ZooKeeperClientService zooKeeperClientService,
            final String counterPath,
            final RetryPolicy retryPolicy)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notBlank(counterPath, "counterPath");
        AppbaseUtil.notNull(retryPolicy, "retryPolicy");

        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.currentTimer = metricRegistry.timer(metricService.name(getClass(), "currentTimer"));
        this.incrementTimer = metricRegistry.timer(metricService.name(getClass(), "incrementTimer"));
        this.decrementTimer = metricRegistry.timer(metricService.name(getClass(), "decrementTimer"));
        this.addTimer = metricRegistry.timer(metricService.name(getClass(), "addTimer"));
        this.subtractTimer = metricRegistry.timer(metricService.name(getClass(), "subtractTimer"));
        this.setTimer = metricRegistry.timer(metricService.name(getClass(), "setTimer"));

        this.distributedAtomicLong = new DistributedAtomicLong(
                zooKeeperClientService.getClient(),
                counterPath,
                retryPolicy);
    }

    @Override
    public long current() throws ZooKeeperAccessorException {
        final Timer.Context timer = currentTimer.time();
        try {
            final AtomicValue<Long> atomicValue = distributedAtomicLong.get();
            return postValue(atomicValue);
        } catch (Exception e) {
            throw new ZooKeeperAccessorException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public long increment() throws ZooKeeperAccessorException {
        final Timer.Context timer = incrementTimer.time();
        try {
            final AtomicValue<Long> atomicValue = distributedAtomicLong.increment();
            return postValue(atomicValue);
        } catch (Exception e) {
            throw new ZooKeeperAccessorException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public long decrement() throws ZooKeeperAccessorException {
        final Timer.Context timer = decrementTimer.time();
        try {
            final AtomicValue<Long> atomicValue = distributedAtomicLong.decrement();
            return postValue(atomicValue);
        } catch (Exception e) {
            throw new ZooKeeperAccessorException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public long add(final long delta) throws ZooKeeperAccessorException {
        final Timer.Context timer = addTimer.time();
        try {
            final AtomicValue<Long> atomicValue = distributedAtomicLong.add(delta);
            return postValue(atomicValue);
        } catch (Exception e) {
            throw new ZooKeeperAccessorException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public long subtract(final long delta) throws ZooKeeperAccessorException {
        final Timer.Context timer = subtractTimer.time();
        try {
            final AtomicValue<Long> atomicValue = distributedAtomicLong.subtract(delta);
            return postValue(atomicValue);
        } catch (Exception e) {
            throw new ZooKeeperAccessorException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public long set(final long value) throws ZooKeeperAccessorException {
        final Timer.Context timer = setTimer.time();
        try {
            return distributedAtomicLong.trySet(value).postValue();
        } catch (Exception e) {
            throw new ZooKeeperAccessorException(e);
        } finally {
            timer.stop();
        }
    }

    private long postValue(final AtomicValue<Long> atomicValue) throws ZooKeeperAccessorException {
        AppbaseUtil.notNull(atomicValue, "atomicValue");

        if (!atomicValue.succeeded()) {
            throw new ZooKeeperAccessorException("ZooKeeper operation was not successful");
        }

        return atomicValue.postValue();
    }
}
