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

package com.viveworks.appbase.core.service.metric;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.jmx.registry.JmxMeterRegistryConfiguration;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultMetricServiceTest {
    private static LogService logService;
    private static DefaultMetricService metricService;
    private static CompositeMeterRegistry customCompositeMeterRegistry;
    private static DefaultMetricService customCompositeMeterRegistryService;
    private static ExecutorService executorService;
    private static DefaultMetricService metricServiceWithJmxMeterRegistry;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        customCompositeMeterRegistry = new CompositeMeterRegistry();
        customCompositeMeterRegistryService = new DefaultMetricService(
                logService,
                customCompositeMeterRegistry,
                DefaultMetricServiceConfiguration.builder().build());
        metricServiceWithJmxMeterRegistry = new DefaultMetricService(
                logService,
                customCompositeMeterRegistry,
                DefaultMetricServiceConfiguration
                        .builder()
                        .jmxMeterRegistryConfiguration(JmxMeterRegistryConfiguration.builder().enabled(true).build())
                        .build());
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (customCompositeMeterRegistry != null) {
            customCompositeMeterRegistry.close();
        }

        if (metricServiceWithJmxMeterRegistry != null) {
            metricServiceWithJmxMeterRegistry.shutdown();
        }

        if (customCompositeMeterRegistryService != null) {
            customCompositeMeterRegistryService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    @Order(1)
    void testDefaultConstructor() throws Exception {
        Assertions.assertNotNull(metricService.getConfiguration());
        Assertions.assertNotNull(metricService.getMetricRegistry());

        metricService.reload(DefaultMetricServiceConfiguration.builder().build());
        Assertions.assertNotNull(metricService.getConfiguration());
        Assertions.assertNotNull(metricService.getMetricRegistry());
        Assertions.assertEquals(0, metricService.getReloadFailureCounter().get());
        Assertions.assertEquals(1, metricService.getReloadTimeInMillisMap().size());

        metricService.shutdown();
        Assertions.assertNotNull(metricService.getConfiguration());
        Assertions.assertEquals(0, metricService.getShutdownFailureCounter().get());
        Assertions.assertNotNull(metricService.getShutdownTimeInMillisMap());
    }

    @Test
    @Order(2)
    void testCustomCompositeMeterRegistry() {
        final CompositeMeterRegistry newCustomCompositeMeterRegistry = new CompositeMeterRegistry();
        final DefaultMetricService newCustomCompositeMeterRegistryService = new DefaultMetricService(
                logService,
                newCustomCompositeMeterRegistry,
                DefaultMetricServiceConfiguration.builder().build());
        Assertions.assertNotNull(newCustomCompositeMeterRegistryService.getConfiguration());
        Assertions.assertNotNull(newCustomCompositeMeterRegistryService.getMetricRegistry());
    }

    @Test
    @Order(3)
    void testRegisterExecutorServiceMetrics() {
        executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        final String executorServiceName = "executor-name-1";
        metricService.registerExecutorServiceMetrics(executorService, executorServiceName);

        final CompositeMeterRegistry compositeMeterRegistry = metricService.getCompositeMeterRegistry();

        final Search search1 = compositeMeterRegistry.find(
                executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.completed");
        final Gauge gauge1 = search1.gauge();
        Assertions.assertNotNull(gauge1);

        final Search search2 = compositeMeterRegistry.find(
                executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.active");
        final Gauge gauge2 = search2.gauge();
        Assertions.assertNotNull(gauge2);

        final Search search3 = compositeMeterRegistry.find(
                executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.queued");
        final Gauge gauge3 = search3.gauge();
        Assertions.assertNotNull(gauge3);

        final Search search4 = compositeMeterRegistry.find(
                executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.queue.remaining");
        final Gauge gauge4 = search4.gauge();
        Assertions.assertNotNull(gauge4);

        final Search search5 = compositeMeterRegistry.find(
                executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.pool.size");
        final Gauge gauge5 = search5.gauge();
        Assertions.assertNotNull(gauge5);

        final Search search6 = compositeMeterRegistry.find(
                executorServiceName + MetricService.METRIC_NAME_SEPARATOR + "executor.pool.size");
        final Gauge gauge6 = search6.gauge();
        Assertions.assertNotNull(gauge6);

        final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        final String executorServiceName1 = "fork-join-pool-1";
        metricService.registerExecutorServiceMetrics(forkJoinPool, executorServiceName1);

        final Search search7 = compositeMeterRegistry.find(
                executorServiceName1 + MetricService.METRIC_NAME_SEPARATOR + "executor.running");
        final Gauge gauge7 = search7.gauge();
        Assertions.assertNotNull(gauge7);
    }

    @Test
    @Order(4)
    void testCollections() {
        final SimpleMeterRegistry simpleMeterRegistry1 = new SimpleMeterRegistry();
        metricService.registerMicrometerRegistry(simpleMeterRegistry1);

        final Set<MeterRegistry> micrometerRegistries1 = metricService.getMicrometerRegistries();
        Assertions.assertNotNull(micrometerRegistries1);
        Assertions.assertEquals(1, micrometerRegistries1.size());

        metricService.unregisterMicrometerRegistry(simpleMeterRegistry1);
        final Set<MeterRegistry> micrometerRegistries2 = metricService.getMicrometerRegistries();
        Assertions.assertNotNull(micrometerRegistries2);
        Assertions.assertTrue(micrometerRegistries1.isEmpty());

        final SimpleMeterRegistry simpleMeterRegistry2 = new SimpleMeterRegistry();
        metricService.registerMicrometerRegistry(simpleMeterRegistry2);
        final Set<MeterRegistry> micrometerRegistries3 = metricService.getMicrometerRegistries();
        Assertions.assertNotNull(micrometerRegistries3);
        Assertions.assertEquals(1, micrometerRegistries3.size());

        metricService.unregisterMicrometerRegistry(SimpleMeterRegistry.class);
        final Set<MeterRegistry> micrometerRegistries4 = metricService.getMicrometerRegistries();
        Assertions.assertNotNull(micrometerRegistries4);
        Assertions.assertTrue(micrometerRegistries4.isEmpty());

        final SimpleMeterRegistry simpleMeterRegistry3 = new SimpleMeterRegistry();
        metricService.registerMicrometerRegistry(simpleMeterRegistry3);
        final Set<MeterRegistry> micrometerRegistries5 = metricService.getMicrometerRegistries();
        Assertions.assertNotNull(micrometerRegistries5);
        Assertions.assertEquals(1, micrometerRegistries5.size());

        metricService.clearMicrometerRegistries();
        final Set<MeterRegistry> micrometerRegistries6 = metricService.getMicrometerRegistries();
        Assertions.assertNotNull(micrometerRegistries6);
        Assertions.assertTrue(micrometerRegistries6.isEmpty());

        final ServiceStatus status = metricService.getStatus();
        Assertions.assertEquals(ServiceStatus.STARTED, status);
    }

    @Test
    @Order(5)
    void testSerializeMetricRegistryAsJson() throws Exception {
        final org.eclipse.microprofile.metrics.Gauge<Integer> gauge1 = () -> 1;
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        metricRegistry.register("gauge1", gauge1);

        final org.eclipse.microprofile.metrics.Gauge<Integer> gauge2 = () -> 2;
        metricRegistry.register("gauge2", gauge2);

        metricRegistry.counter("counter1");
        metricRegistry.counter("counter2");

        metricRegistry.concurrentGauge("concurrentGauge1");
        metricRegistry.concurrentGauge("concurrentGauge2");

        metricRegistry.histogram("histogram1");
        metricRegistry.histogram("histogram2");

        metricRegistry.meter("meter1");
        metricRegistry.meter("meter2");

        metricRegistry.timer("timer1");
        metricRegistry.timer("timer2");

        metricRegistry.simpleTimer("simpleTimer1");
        metricRegistry.simpleTimer("simpleTimer2");

        final String metricRegistryAsJson = metricService.serializeMetricRegistryAsJson();
        Assertions.assertNotNull(metricRegistryAsJson);
    }

    @Test
    @Order(6)
    void testInitJmxMeterRegistryIfEnabled() {
        metricServiceWithJmxMeterRegistry.initJmxMeterRegistryIfEnabled();
    }

    @Test
    @Order(7)
    void testGetCompositeMeterRegistryOnInterface() {
        final MetricService metricServiceAsInterface = metricService;
        final MeterRegistry compositeMeterRegistry = metricServiceAsInterface.getCompositeMeterRegistry();

        Assertions.assertNotNull(compositeMeterRegistry);
        Assertions.assertTrue(compositeMeterRegistry instanceof CompositeMeterRegistry);
        Assertions.assertSame(compositeMeterRegistry, metricServiceAsInterface.getCompositeMeterRegistry());

        // The composite registry is the same one backing getMetricRegistry(): a meter recorded
        // directly on it is discoverable through the composite (and would fan out to any
        // registered reporting registry, e.g. JMX).
        compositeMeterRegistry.counter("task7.compositeMeterRegistry.test").increment();
        Assertions.assertNotNull(
                compositeMeterRegistry.find("task7.compositeMeterRegistry.test").counter());
    }
}
