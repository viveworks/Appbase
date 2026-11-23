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

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.disruptor.configuration.DefaultDisruptorServiceConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BusySpinWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.PhasedBackoffWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.TimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultDisruptorServiceTest {
    private static LogService logService;
    private static MediatorService mediatorService;
    private static MetricService metricService;
    private static ValidationService validationService;
    private static DefaultDisruptorService<TestDisruptorEvent> defaultDisruptorService;
    private static Logger logger;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        logger = logService.getLogger(DefaultDisruptorServiceTest.class);
        metricService = CoreTestFixtures.getMetricService(logService);
        validationService = CoreTestFixtures.getValidationService(logService, metricService);
        mediatorService = CoreTestFixtures.getMediatorService(logService, metricService, validationService);

        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(YieldingWaitStrategyConfiguration.builder().build())
                        .build();
        final EventFactory<TestDisruptorEvent> eventFactory = new TestDisruptorEventFactory();
        final EventHandler<TestDisruptorEvent> eventHandler = new TestDisruptorEventHandler();
        defaultDisruptorService = new DefaultDisruptorService<>(
                defaultDisruptorServiceConfiguration,
                logService,
                mediatorService,
                metricService,
                validationService,
                new TestDisruptorExceptionHandler(),
                eventFactory,
                eventHandler);
    }

    @AfterAll
    static void tearDown() {
        if (defaultDisruptorService != null) {
            defaultDisruptorService.shutdown();
        }

        if (mediatorService != null) {
            mediatorService.shutdown();
        }

        if (validationService != null) {
            validationService.shutdown();
        }

        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testWithBlockingWaitStrategy() {
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                .builder()
                .waitStrategyConfiguration(BlockingWaitStrategyConfiguration.builder().build())
                .build();
        final DefaultDisruptorService newDefaultDisruptorService =
                newDefaultDisruptorService(defaultDisruptorServiceConfiguration);
        assertDefaultDisruptorService(newDefaultDisruptorService);
    }

    @Test
    void testWithBusySpinWaitStrategy() {
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(BusySpinWaitStrategyConfiguration.builder().build())
                        .build();
        final DefaultDisruptorService newDefaultDisruptorService =
                newDefaultDisruptorService(defaultDisruptorServiceConfiguration);
        assertDefaultDisruptorService(newDefaultDisruptorService);
    }

    @Test
    void testWithLiteTimeoutBlockingWaitStrategy() {
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(TimeoutBlockingWaitStrategyConfiguration.builder().build())
                        .build();
        final DefaultDisruptorService newDefaultDisruptorService =
                newDefaultDisruptorService(defaultDisruptorServiceConfiguration);
        assertDefaultDisruptorService(newDefaultDisruptorService);
    }

    @Test
    void testWithPhasedBackoffWaitStrategy() {
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(PhasedBackoffWaitStrategyConfiguration.builder().build())
                        .build();
        final DefaultDisruptorService newDefaultDisruptorService =
                newDefaultDisruptorService(defaultDisruptorServiceConfiguration);
        assertDefaultDisruptorService(newDefaultDisruptorService);
    }

    @Test
    void testWithSleepingWaitStrategy() {
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(SleepingWaitStrategyConfiguration.builder().build())
                        .build();
        final DefaultDisruptorService newDefaultDisruptorService =
                newDefaultDisruptorService(defaultDisruptorServiceConfiguration);
        assertDefaultDisruptorService(newDefaultDisruptorService);
    }

    @Test
    void testWithTimeoutBlockingWaitStrategy() {
        final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .waitStrategyConfiguration(TimeoutBlockingWaitStrategyConfiguration.builder().build())
                        .build();
        final DefaultDisruptorService newDefaultDisruptorService =
                newDefaultDisruptorService(defaultDisruptorServiceConfiguration);
        assertDefaultDisruptorService(newDefaultDisruptorService);
    }

    @Test
    void testWithYieldingWaitStrategy() {
        assertDefaultDisruptorService(defaultDisruptorService);
    }

    private static DefaultDisruptorService<TestDisruptorEvent> newDefaultDisruptorService(
            final DefaultDisruptorServiceConfiguration defaultDisruptorServiceConfiguration)
    {
        final EventFactory<TestDisruptorEvent> eventFactory = new TestDisruptorEventFactory();
        final ExceptionHandler<TestDisruptorEvent> exceptionHandler = new DefaultExceptionHandler<>();
        final EventHandler<TestDisruptorEvent> eventHandler = new TestDisruptorEventHandler();
        final DefaultDisruptorService<TestDisruptorEvent> newDefaultDisruptorService = new DefaultDisruptorService<>(
                defaultDisruptorServiceConfiguration, eventFactory, exceptionHandler, eventHandler);
        newDefaultDisruptorService.setLogService(logService);

        return newDefaultDisruptorService;
    }

    private void assertDefaultDisruptorService(final DefaultDisruptorService<?> defaultDisruptorService) {
        Assertions.assertFalse(defaultDisruptorService.isShutdown());
        Assertions.assertNotNull(defaultDisruptorService.getDisruptor());

        final RingBuffer<?> ringBuffer = defaultDisruptorService.getRingBuffer();
        Assertions.assertNotNull(ringBuffer);

        final long remainingCapacity = ringBuffer.remainingCapacity();
        Assertions.assertTrue(remainingCapacity > 0);

        Assertions.assertNotNull(defaultDisruptorService.getLogger());

        defaultDisruptorService.shutdown();
        Assertions.assertTrue(defaultDisruptorService.isShutdown());
    }

    private static class TestDisruptorEvent {
        private String field;

        public String getField() {
            return field;
        }

        public void setField(final String field) {
            this.field = field;
        }
    }

    private static class TestDisruptorEventHandler implements EventHandler<TestDisruptorEvent> {
        @Override
        public void onEvent(final TestDisruptorEvent event,
                            final long sequence,
                            final boolean endOfBatch) throws Exception
        {
            logger.info("Event {}", event);
        }
    }

    private static class TestDisruptorEventFactory implements EventFactory<TestDisruptorEvent> {
        @Override
        public TestDisruptorEvent newInstance() {
            return new TestDisruptorEvent();
        }
    }

    private static class TestDisruptorExceptionHandler implements ExceptionHandler<TestDisruptorEvent> {
        @Override
        public void handleEventException(final Throwable ex, final long sequence, final TestDisruptorEvent event) {
            logger.error("Exception in handling disruptor event: {}", event, ex);
        }

        @Override
        public void handleOnStartException(final Throwable ex) {
            throw new AppbaseRuntimeException("Error while starting the disruptor", ex);
        }

        @Override
        public void handleOnShutdownException(final Throwable ex) {
            logger.error("Error while shutting down the disruptor", ex);
        }
    }
}
