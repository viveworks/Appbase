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

package com.viveworks.appbase.core.util.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BusySpinWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.LiteTimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.PhasedBackoffWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.TimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.WaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DisruptorService}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Event type
 * @since 1.0.0
 */
@ThreadSafe
public final class DefaultDisruptorService<T> implements DisruptorService<T> {
    private final AtomicReference<Logger> loggerRef;
    private final Disruptor<T> disruptor;
    private final RingBuffer<T> ringBuffer;
    private final AtomicBoolean shutdown;

    @SafeVarargs
    public DefaultDisruptorService(final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration,
                                   final EventFactory<T> eventFactory,
                                   final ExceptionHandler<T> exceptionHandler,
                                   final EventHandler<T>... eventHandlers)
    {
        loggerRef = new AtomicReference<>();

        this.disruptor = newDisruptor(
                defaultDisruptorServiceConfiguration, eventFactory, exceptionHandler, eventHandlers);
        this.ringBuffer = disruptor.start();

        this.shutdown = new AtomicBoolean(false);
    }

    @SafeVarargs
    public DefaultDisruptorService(final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration,
                                   final LogService logService,
                                   final MediatorService mediatorService,
                                   final MetricService metricService,
                                   final ValidationService validationService,
                                   final ExceptionHandler<T> exceptionHandler,
                                   final EventFactory<T> eventFactory,
                                   final EventHandler<T>... eventHandlers)
    {
        AppbaseUtil.notNull(defaultDisruptorServiceConfiguration, "defaultDisruptorServiceConfiguration");
        AppbaseUtil.validate(validationService, defaultDisruptorServiceConfiguration);
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(mediatorService, "mediatorService");
        AppbaseUtil.notNull(metricService, "metricService");
        AppbaseUtil.notNull(exceptionHandler, "exceptionHandler");
        AppbaseUtil.notNull(eventFactory, "eventFactory");
        AppbaseUtil.notNull(eventHandlers, "eventHandlers");

        final Logger logger = logService.getLogger(getClass());
        this.loggerRef = new AtomicReference<>(logger);

        this.disruptor = newDisruptor(
                defaultDisruptorServiceConfiguration, eventFactory, exceptionHandler, eventHandlers);
        this.ringBuffer = disruptor.start();

        this.shutdown = new AtomicBoolean(false);
    }

    @SafeVarargs
    private Disruptor<T> newDisruptor(
            final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration,
            final EventFactory<T> eventFactory,
            final ExceptionHandler<T> exceptionHandler,
            final EventHandler<T>... eventHandlers)
    {
        final ThreadFactoryConfiguration threadFactoryConfiguration =
                defaultDisruptorServiceConfiguration.getThreadFactoryConfiguration();
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                AppbaseUtil.defaultUncaughtExceptionHandler(loggerRef);
        final ThreadFactory threadFactory = AppbaseUtil.newThreadFactory(
                threadFactoryConfiguration,
                uncaughtExceptionHandler);

        final WaitStrategy waitStrategy = newWaitStrategy(
                defaultDisruptorServiceConfiguration.getWaitStrategyConfiguration());

        final Disruptor<T> localDisruptor = new Disruptor<>(
                eventFactory,
                defaultDisruptorServiceConfiguration.getRingBufferSize(),
                threadFactory,
                defaultDisruptorServiceConfiguration.getProducerType(),
                waitStrategy);
        localDisruptor.setDefaultExceptionHandler(exceptionHandler);
        localDisruptor.handleEventsWith(eventHandlers);

        return localDisruptor;
    }

    private WaitStrategy newWaitStrategy(final WaitStrategyConfiguration waitStrategyConfiguration) {
        if (waitStrategyConfiguration instanceof BlockingWaitStrategyConfiguration) {
            final BlockingWaitStrategyConfiguration blockingWaitStrategyConfiguration =
                    (BlockingWaitStrategyConfiguration) waitStrategyConfiguration;

            if (blockingWaitStrategyConfiguration.isEnabled()) {
                return new BlockingWaitStrategy();
            }
        } else if (waitStrategyConfiguration instanceof BusySpinWaitStrategyConfiguration) {
            final BusySpinWaitStrategyConfiguration busySpinWaitStrategyConfiguration =
                    (BusySpinWaitStrategyConfiguration) waitStrategyConfiguration;

            if (busySpinWaitStrategyConfiguration.isEnabled()) {
                return new BusySpinWaitStrategy();
            }
        } else if (waitStrategyConfiguration instanceof LiteTimeoutBlockingWaitStrategyConfiguration) {
            final LiteTimeoutBlockingWaitStrategyConfiguration liteTimeoutBlockingWaitStrategyConfiguration =
                    (LiteTimeoutBlockingWaitStrategyConfiguration) waitStrategyConfiguration;

            if (liteTimeoutBlockingWaitStrategyConfiguration.isEnabled()) {
                final long timeout = liteTimeoutBlockingWaitStrategyConfiguration.getTimeout();
                final TimeUnit units = liteTimeoutBlockingWaitStrategyConfiguration.getUnits();
                return new LiteTimeoutBlockingWaitStrategy(timeout, units);
            }
        } else if (waitStrategyConfiguration instanceof PhasedBackoffWaitStrategyConfiguration) {
            final PhasedBackoffWaitStrategyConfiguration phasedBackoffWaitStrategyConfiguration =
                    (PhasedBackoffWaitStrategyConfiguration) waitStrategyConfiguration;

            final WaitStrategyConfiguration fallbackWaitStrategyConfiguration =
                    phasedBackoffWaitStrategyConfiguration.getWaitStrategyConfiguration();

            if (fallbackWaitStrategyConfiguration instanceof PhasedBackoffWaitStrategyConfiguration) {
                throw new AppbaseRuntimeException(PhasedBackoffWaitStrategy.class.getName() +
                        " cannot fallback on itself. Configuration used: " +
                        phasedBackoffWaitStrategyConfiguration);
            }

            if (phasedBackoffWaitStrategyConfiguration.isEnabled()) {
                return new PhasedBackoffWaitStrategy(
                        phasedBackoffWaitStrategyConfiguration.getSpinTimeout(),
                        phasedBackoffWaitStrategyConfiguration.getYieldTimeout(),
                        phasedBackoffWaitStrategyConfiguration.getUnits(),
                        newWaitStrategy(phasedBackoffWaitStrategyConfiguration.getWaitStrategyConfiguration()));
            }
        } else if (waitStrategyConfiguration instanceof SleepingWaitStrategyConfiguration) {
            final SleepingWaitStrategyConfiguration sleepingWaitStrategyConfiguration =
                    (SleepingWaitStrategyConfiguration) waitStrategyConfiguration;

            if (sleepingWaitStrategyConfiguration.isEnabled()) {
                return new SleepingWaitStrategy(
                        sleepingWaitStrategyConfiguration.getRetries(),
                        sleepingWaitStrategyConfiguration.getSleepTimeNanoSeconds());
            }
        } else if (waitStrategyConfiguration instanceof TimeoutBlockingWaitStrategyConfiguration) {
            final TimeoutBlockingWaitStrategyConfiguration timeoutBlockingWaitStrategyConfiguration =
                    (TimeoutBlockingWaitStrategyConfiguration) waitStrategyConfiguration;

            if (timeoutBlockingWaitStrategyConfiguration.isEnabled()) {
                return new TimeoutBlockingWaitStrategy(
                        timeoutBlockingWaitStrategyConfiguration.getTimeout(),
                        timeoutBlockingWaitStrategyConfiguration.getUnits());
            }
        } else if (waitStrategyConfiguration instanceof YieldingWaitStrategyConfiguration) {
            final YieldingWaitStrategyConfiguration yieldingWaitStrategyConfiguration =
                    (YieldingWaitStrategyConfiguration) waitStrategyConfiguration;

            if (yieldingWaitStrategyConfiguration.isEnabled()) {
                return new YieldingWaitStrategy();
            }
        }

        throw new AppbaseRuntimeException("Cannot create an implementation of " +
                WaitStrategy.class.getName() +
                " from " +
                waitStrategyConfiguration);
    }

    @Override
    public Disruptor<T> getDisruptor() {
        return disruptor;
    }

    @Override
    public RingBuffer<T> getRingBuffer() {
        return ringBuffer;
    }

    /**
     * Set {@link LogService}.
     *
     * @param logService Log service to set
     */
    public void setLogService(final LogService logService) {
        final Logger logger = logService.getLogger(getClass());
        loggerRef.set(logger);
    }

    public Logger getLogger() {
        return loggerRef.get();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        final Logger logger = loggerRef.get();
        if (logger != null) {
            logger.info("Shutting down {}", getClass().getName());
        }

        try {
            disruptor.shutdown();
        } catch (Exception ex) {
            if (logger != null) {
                logger.error("Error shutting down the disruptor", ex);
            }
        }
    }
}
