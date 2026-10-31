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

package com.viveworks.appbase.core.service.metric.microprofile.micrometer;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class MicroMetricRegistryTest {
    private static LogService logService;
    private static DefaultMetricService metricService;
    private static MicroMetricRegistry microMetricRegistry;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
        metricService.registerMicrometerRegistry(new SimpleMeterRegistry());
        microMetricRegistry = new MicroMetricRegistry(metricService.getCompositeMeterRegistry());
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testCounters() {
        Assertions.assertEquals(MetricRegistry.Type.BASE, microMetricRegistry.getType());

        final Counter counter1 = microMetricRegistry.counter("counter1");
        Assertions.assertNotNull(counter1);
        Assertions.assertEquals(0, counter1.getCount());
        counter1.inc();
        Assertions.assertEquals(1, counter1.getCount());

        final Counter counter11 = microMetricRegistry.counter("counter1");
        Assertions.assertEquals(1, counter11.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Counter counter2 = microMetricRegistry.counter("counter2", tags);
        Assertions.assertNotNull(counter2);
        Assertions.assertEquals(0, counter2.getCount());
        counter2.inc();
        counter2.inc();
        Assertions.assertEquals(2, counter2.getCount());

        final Counter counter21 = microMetricRegistry.counter("counter2", tags);
        Assertions.assertEquals(2, counter21.getCount());

        final Metadata metadata1 = new MetadataBuilder().withName("counter3").build();
        final Counter counter3 = microMetricRegistry.counter(metadata1);
        Assertions.assertNotNull(counter3);
        Assertions.assertEquals(0, counter3.getCount());
        counter3.inc();
        counter3.inc();
        counter3.inc();
        Assertions.assertEquals(3, counter3.getCount());

        final Metadata metadata2 = new MetadataBuilder().withName("counter4").build();
        final Counter counter4 = microMetricRegistry.counter(metadata2, tags);
        Assertions.assertNotNull(counter4);
        Assertions.assertEquals(0, counter4.getCount());
        counter4.inc(4);
        Assertions.assertEquals(4, counter4.getCount());

        final Tag[] tags1 = new Tag[0];
        final Counter counter5 = microMetricRegistry.counter("counter5", tags1);
        Assertions.assertNotNull(counter5);
        Assertions.assertEquals(0, counter5.getCount());
        counter5.inc(5);
        Assertions.assertEquals(5, counter5.getCount());

        final SortedSet<String> names = microMetricRegistry.getNames();
        Assertions.assertTrue(names.contains("counter1"));
        Assertions.assertTrue(names.contains("counter2"));
        Assertions.assertTrue(names.contains("counter3"));
        Assertions.assertTrue(names.contains("counter4"));
        Assertions.assertTrue(names.contains("counter5"));

        final SortedMap<MetricID, Counter> counters = microMetricRegistry.getCounters();
        Assertions.assertTrue(counters.size() >= 5);
        Assertions.assertTrue(counters.containsKey(new MetricID("counter1")));
        Assertions.assertTrue(counters.containsKey(new MetricID("counter2", tags)));
        Assertions.assertTrue(counters.containsKey(new MetricID("counter3")));
        Assertions.assertTrue(counters.containsKey(new MetricID("counter4", tags)));
        Assertions.assertTrue(counters.containsKey(new MetricID("counter5")));

        final Map<String, Metadata> metadata = microMetricRegistry.getMetadata();
        Assertions.assertNotNull(metadata);

        final Map<MetricID, Metric> metrics = microMetricRegistry.getMetrics();
        Assertions.assertNotNull(metrics);
        Assertions.assertFalse(metrics.isEmpty());

        final SortedSet<MetricID> metricIDs = microMetricRegistry.getMetricIDs();
        Assertions.assertNotNull(metricIDs);
        Assertions.assertTrue(metricIDs.contains(new MetricID("counter1")));
        Assertions.assertTrue(metricIDs.contains(new MetricID("counter2", tags)));
        Assertions.assertTrue(metricIDs.contains(new MetricID("counter3")));
        Assertions.assertTrue(metricIDs.contains(new MetricID("counter4", tags)));
        Assertions.assertTrue(metricIDs.contains(new MetricID("counter5")));

        final boolean removeResult1 = microMetricRegistry.remove("counter1");
        Assertions.assertTrue(removeResult1);
        final SortedSet<String> names1 = microMetricRegistry.getNames();
        Assertions.assertNotNull(names1);
        Assertions.assertFalse(names1.contains("counter1"));

        final MetricID counter3MetricID = new MetricID("counter2", tags);
        final boolean removeResult2 = microMetricRegistry.remove(counter3MetricID);
        Assertions.assertTrue(removeResult2);
        final SortedSet<MetricID> metricIDs1 = microMetricRegistry.getMetricIDs();
        Assertions.assertNotNull(metricIDs1);
        Assertions.assertFalse(metricIDs1.contains(counter3MetricID));

        final SortedMap<MetricID, Counter> counter41 = microMetricRegistry.getCounters((metricID, metric) -> {
            final String name = metricID.getName();
            return "counter4".equals(name);
        });
        Assertions.assertNotNull(counter41);
        Assertions.assertEquals(1, counter41.size());

        Assertions.assertNotNull(microMetricRegistry.getMicrometerMeterMap());
    }

    @Test
    void testHistograms() {
        final Histogram histogram1 = microMetricRegistry.histogram("histogram1");
        Assertions.assertNotNull(histogram1);
        Assertions.assertEquals(0, histogram1.getCount());
        histogram1.update(1);
        Assertions.assertEquals(1, histogram1.getCount());

        final Histogram histogram11 = microMetricRegistry.histogram("histogram1");
        Assertions.assertEquals(1, histogram11.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Histogram histogram2 = microMetricRegistry.histogram("histogram2", tags);
        Assertions.assertNotNull(histogram2);
        Assertions.assertEquals(0, histogram2.getCount());
        histogram2.update(1);
        histogram2.update(2);
        Assertions.assertEquals(2, histogram2.getCount());

        final Histogram histogram21 = microMetricRegistry.histogram("histogram2", tags);
        Assertions.assertEquals(2, histogram21.getCount());

        final Metadata metadata1 = new MetadataBuilder().withName("histogram3").build();
        final Histogram histogram3 = microMetricRegistry.histogram(metadata1);
        Assertions.assertNotNull(histogram3);
        Assertions.assertEquals(0, histogram3.getCount());
        histogram3.update(1);
        histogram3.update(2);
        histogram3.update(3);
        Assertions.assertEquals(3, histogram3.getCount());

        final Metadata metadata2 = new MetadataBuilder().withName("histogram4").build();
        final Histogram histogram4 = microMetricRegistry.histogram(metadata2, tags);
        Assertions.assertNotNull(histogram4);
        Assertions.assertEquals(0, histogram4.getCount());
        histogram4.update(1);
        histogram4.update(2);
        histogram4.update(3);
        histogram4.update(4);
        Assertions.assertEquals(4, histogram4.getCount());

        final Tag[] tags1 = new Tag[0];
        final Histogram histogram5 = microMetricRegistry.histogram("histogram5", tags1);
        Assertions.assertNotNull(histogram5);
        Assertions.assertEquals(0, histogram5.getCount());
        histogram5.update(1);
        histogram5.update(2);
        histogram5.update(3);
        histogram5.update(4);
        histogram5.update(5);
        Assertions.assertEquals(5, histogram5.getCount());

        final SortedSet<String> names = microMetricRegistry.getNames();
        Assertions.assertTrue(names.contains("histogram1"));
        Assertions.assertTrue(names.contains("histogram2"));
        Assertions.assertTrue(names.contains("histogram3"));
        Assertions.assertTrue(names.contains("histogram4"));
        Assertions.assertTrue(names.contains("histogram5"));

        final SortedMap<MetricID, Histogram> histograms = microMetricRegistry.getHistograms();
        Assertions.assertTrue(histograms.size() >= 5);
        Assertions.assertTrue(histograms.containsKey(new MetricID("histogram1")));
        Assertions.assertTrue(histograms.containsKey(new MetricID("histogram2", tags)));
        Assertions.assertTrue(histograms.containsKey(new MetricID("histogram3")));
        Assertions.assertTrue(histograms.containsKey(new MetricID("histogram4", tags)));
        Assertions.assertTrue(histograms.containsKey(new MetricID("histogram5")));

        microMetricRegistry.removeMatching((metricID, metric) -> {
            final String name = metricID.getName();
            return "histogram1".equals(name) && Histogram.class.isAssignableFrom(metric.getClass());
        });
        final SortedMap<MetricID, Histogram> histograms1 = microMetricRegistry.getHistograms();
        Assertions.assertTrue(histograms1.size() >= 4);
        Assertions.assertFalse(histograms1.containsKey(new MetricID("histogram1")));

        final SortedMap<MetricID, Histogram> histograms2 = microMetricRegistry.getHistograms((metricID, metric) -> {
            final String name = metricID.getName();
            return "histogram3".equals(name);
        });
        Assertions.assertEquals(1, histograms2.size());
    }

    @Test
    void testTimers() {
        final Timer timer1 = microMetricRegistry.timer("timer1");
        Assertions.assertNotNull(timer1);
        Assertions.assertEquals(0, timer1.getCount());
        timer1.update(Duration.ofMillis(100));
        Assertions.assertEquals(1, timer1.getCount());

        final Timer timer11 = microMetricRegistry.timer("timer1");
        Assertions.assertEquals(1, timer11.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Timer timer2 = microMetricRegistry.timer("timer2", tags);
        Assertions.assertNotNull(timer2);
        Assertions.assertEquals(0, timer2.getCount());
        timer2.update(Duration.ofMillis(100));
        timer2.update(Duration.ofMillis(100));
        Assertions.assertEquals(2, timer2.getCount());

        final Timer timer21 = microMetricRegistry.timer("timer2", tags);
        Assertions.assertEquals(2, timer21.getCount());

        final Metadata metadata1 = new MetadataBuilder().withName("timer3").build();
        final Timer timer3 = microMetricRegistry.timer(metadata1);
        Assertions.assertNotNull(timer3);
        Assertions.assertEquals(0, timer3.getCount());
        timer3.update(Duration.ofMillis(100));
        timer3.update(Duration.ofMillis(100));
        timer3.update(Duration.ofMillis(100));
        Assertions.assertEquals(3, timer3.getCount());

        final Metadata metadata2 = new MetadataBuilder().withName("timer4").build();
        final Timer timer4 = microMetricRegistry.timer(metadata2, tags);
        Assertions.assertNotNull(timer4);
        Assertions.assertEquals(0, timer4.getCount());
        timer4.update(Duration.ofMillis(100));
        timer4.update(Duration.ofMillis(100));
        timer4.update(Duration.ofMillis(100));
        timer4.update(Duration.ofMillis(100));
        Assertions.assertEquals(4, timer4.getCount());

        final Tag[] tags1 = new Tag[0];
        final Timer timer5 = microMetricRegistry.timer("timer5", tags1);
        Assertions.assertNotNull(timer5);
        Assertions.assertEquals(0, timer5.getCount());
        timer5.update(Duration.ofMillis(100));
        timer5.update(Duration.ofMillis(100));
        timer5.update(Duration.ofMillis(100));
        timer5.update(Duration.ofMillis(100));
        timer5.update(Duration.ofMillis(100));
        Assertions.assertEquals(5, timer5.getCount());

        final SortedMap<MetricID, Timer> timers = microMetricRegistry.getTimers();
        Assertions.assertNotNull(timers);
        Assertions.assertTrue(timers.size() >= 5);
        Assertions.assertTrue(timers.containsKey(new MetricID("timer1")));
        Assertions.assertTrue(timers.containsKey(new MetricID("timer2", tags)));
        Assertions.assertTrue(timers.containsKey(new MetricID("timer3")));
        Assertions.assertTrue(timers.containsKey(new MetricID("timer4", tags)));
        Assertions.assertTrue(timers.containsKey(new MetricID("timer5", tags1)));

        final SortedMap<MetricID, Timer> timers2 = microMetricRegistry.getTimers((metricID, metric) -> {
            final String name = metricID.getName();
            return "timer1".equals(name);
        });
        Assertions.assertNotNull(timers2);
        Assertions.assertTrue(timers2.size() >= 1);
        Assertions.assertTrue(timers2.containsKey(new MetricID("timer1")));
    }

    @Test
    void testMeters() {
        final Meter meter1 = microMetricRegistry.meter("meter1");
        Assertions.assertNotNull(meter1);
        Assertions.assertEquals(0, meter1.getCount());
        meter1.mark();
        Assertions.assertEquals(1, meter1.getCount());

        final Meter meter11 = microMetricRegistry.meter("meter1");
        Assertions.assertEquals(1, meter11.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Meter meter2 = microMetricRegistry.meter("meter2", tags);
        Assertions.assertNotNull(meter2);
        Assertions.assertEquals(0, meter2.getCount());
        meter2.mark();
        meter2.mark(2409L);
        Assertions.assertEquals(2410, meter2.getCount());

        final Meter meter21 = microMetricRegistry.meter("meter2", tags);
        Assertions.assertEquals(2410, meter21.getCount());

        final Metadata metadata1 = new MetadataBuilder().withName("meter3").build();
        final Meter meter3 = microMetricRegistry.meter(metadata1);
        Assertions.assertNotNull(meter3);
        Assertions.assertEquals(0, meter3.getCount());
        meter3.mark();
        meter3.mark();
        meter3.mark();
        Assertions.assertEquals(3, meter3.getCount());

        final Metadata metadata2 = new MetadataBuilder().withName("meter4").build();
        final Meter meter4 = microMetricRegistry.meter(metadata2, tags);
        Assertions.assertNotNull(meter4);
        Assertions.assertEquals(0, meter4.getCount());
        meter4.mark(1);
        meter4.mark(2);
        meter4.mark(3);
        meter4.mark(4);
        Assertions.assertEquals(10, meter4.getCount());

        final Tag[] tags1 = new Tag[0];
        final Meter meter5 = microMetricRegistry.meter("meter5", tags1);
        Assertions.assertNotNull(meter5);
        Assertions.assertEquals(0, meter5.getCount());
        meter5.mark();
        meter5.mark();
        meter5.mark();
        meter5.mark();
        meter5.mark();
        Assertions.assertEquals(5, meter5.getCount());

        final SortedMap<MetricID, Meter> meters = microMetricRegistry.getMeters();
        Assertions.assertNotNull(meters);
        Assertions.assertTrue(meters.size() >= 5);
        Assertions.assertTrue(meters.containsKey(new MetricID("meter1")));
        Assertions.assertTrue(meters.containsKey(new MetricID("meter2", tags)));
        Assertions.assertTrue(meters.containsKey(new MetricID("meter3")));
        Assertions.assertTrue(meters.containsKey(new MetricID("meter4", tags)));
        Assertions.assertTrue(meters.containsKey(new MetricID("meter5", tags1)));

        final SortedMap<MetricID, Meter> meters1 = microMetricRegistry.getMeters((metricID, metric) -> {
            final String name = metricID.getName();
            return "meter1".equals(name);
        });
        Assertions.assertNotNull(meters1);
        Assertions.assertTrue(meters1.size() >= 1);
        Assertions.assertTrue(meters1.containsKey(new MetricID("meter1")));
    }

    @Test
    void testConcurrentGauges() {
        final ConcurrentGauge concurrentGauge1 = microMetricRegistry.concurrentGauge("concurrent-gauge-1");
        Assertions.assertNotNull(concurrentGauge1);
        Assertions.assertEquals(0, concurrentGauge1.getCount());
        concurrentGauge1.inc();
        concurrentGauge1.inc();
        Assertions.assertEquals(2, concurrentGauge1.getCount());
        concurrentGauge1.dec();
        Assertions.assertEquals(1, concurrentGauge1.getCount());

        final ConcurrentGauge concurrentGauge11 = microMetricRegistry.concurrentGauge("concurrent-gauge-1");
        Assertions.assertEquals(1, concurrentGauge11.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final ConcurrentGauge concurrentGauge2 = microMetricRegistry.concurrentGauge("concurrent-gauge-2", tags);
        Assertions.assertNotNull(concurrentGauge2);
        Assertions.assertEquals(0, concurrentGauge2.getCount());
        concurrentGauge2.inc();
        concurrentGauge2.inc();
        concurrentGauge2.inc();
        concurrentGauge2.inc();
        concurrentGauge2.inc();
        Assertions.assertEquals(5, concurrentGauge2.getCount());
        final long max = concurrentGauge2.getMax();
        Assertions.assertNotEquals(0, max);
        final long min = concurrentGauge2.getMin();
        Assertions.assertNotEquals(0, min);

        final ConcurrentGauge concurrentGauge21 = microMetricRegistry.concurrentGauge("concurrent-gauge-2", tags);
        Assertions.assertEquals(5, concurrentGauge21.getCount());

        final Metadata metadata1 = new MetadataBuilder().withName("concurrent-gauge-3").build();
        final ConcurrentGauge concurrentGauge3 = microMetricRegistry.concurrentGauge(metadata1);
        Assertions.assertNotNull(concurrentGauge3);
        Assertions.assertEquals(0, concurrentGauge3.getCount());
        concurrentGauge3.inc();
        concurrentGauge3.inc();
        Assertions.assertEquals(2, concurrentGauge3.getCount());

        final Metadata metadata2 = new MetadataBuilder().withName("concurrent-gauge-4").build();
        final ConcurrentGauge concurrentGauge4 = microMetricRegistry.concurrentGauge(metadata2, tags);
        Assertions.assertNotNull(concurrentGauge4);
        Assertions.assertEquals(0, concurrentGauge4.getCount());
        concurrentGauge4.inc();
        concurrentGauge4.inc();
        concurrentGauge4.inc();
        Assertions.assertEquals(3, concurrentGauge4.getCount());

        final Tag[] tags1 = new Tag[0];
        final ConcurrentGauge concurrentGauge5 = microMetricRegistry.concurrentGauge("concurrent-gauge-5", tags1);
        Assertions.assertNotNull(concurrentGauge5);
        Assertions.assertEquals(0, concurrentGauge5.getCount());
        concurrentGauge5.inc();
        Assertions.assertEquals(1, concurrentGauge5.getCount());

        final SortedMap<MetricID, ConcurrentGauge> concurrentGauges = microMetricRegistry.getConcurrentGauges();
        Assertions.assertNotNull(concurrentGauges);
        Assertions.assertTrue(concurrentGauges.size() >= 5);
        Assertions.assertTrue(concurrentGauges.containsKey(new MetricID("concurrent-gauge-1")));
        Assertions.assertTrue(concurrentGauges.containsKey(new MetricID("concurrent-gauge-2", tags)));
        Assertions.assertTrue(concurrentGauges.containsKey(new MetricID("concurrent-gauge-3")));
        Assertions.assertTrue(concurrentGauges.containsKey(new MetricID("concurrent-gauge-4", tags)));
        Assertions.assertTrue(concurrentGauges.containsKey(new MetricID("concurrent-gauge-5")));

        final SortedMap<MetricID, ConcurrentGauge> concurrentGauges1 =
                microMetricRegistry.getConcurrentGauges((metricID, metric) -> {
                    final String name = metricID.getName();
                    return "concurrent-gauge-1".equals(name);
                });
        Assertions.assertNotNull(concurrentGauges1);
        Assertions.assertEquals(1, concurrentGauges1.size());
        Assertions.assertTrue(concurrentGauges1.containsKey(new MetricID("concurrent-gauge-1")));
    }

    @Test
    void testSimpleTimers() {
        final SimpleTimer simpleTimer1 = microMetricRegistry.simpleTimer("st1");
        Assertions.assertNotNull(simpleTimer1);
        Assertions.assertEquals(0, simpleTimer1.getCount());
        simpleTimer1.update(Duration.ofMillis(100));
        Assertions.assertEquals(1, simpleTimer1.getCount());

        final SimpleTimer simpleTimer11 = microMetricRegistry.simpleTimer("st1");
        Assertions.assertEquals(1, simpleTimer11.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final SimpleTimer simpleTimer2 = microMetricRegistry.simpleTimer("st2", tags);
        Assertions.assertNotNull(simpleTimer2);
        Assertions.assertEquals(0, simpleTimer2.getCount());
        simpleTimer2.update(Duration.ofMillis(100));
        simpleTimer2.update(Duration.ofMillis(100));
        Assertions.assertEquals(2, simpleTimer2.getCount());

        final SimpleTimer simpleTimer21 = microMetricRegistry.simpleTimer("st2", tags);
        Assertions.assertEquals(2, simpleTimer21.getCount());

        final Metadata metadata1 = new MetadataBuilder().withName("st3").build();
        final SimpleTimer simpleTimer3 = microMetricRegistry.simpleTimer(metadata1);
        Assertions.assertNotNull(simpleTimer3);
        Assertions.assertEquals(0, simpleTimer3.getCount());
        simpleTimer3.update(Duration.ofMillis(1000));
        simpleTimer3.update(Duration.ofMillis(1000));
        simpleTimer3.update(Duration.ofMillis(1000));
        Assertions.assertEquals(3, simpleTimer3.getCount());

        final Metadata metadata2 = new MetadataBuilder().withName("st4").build();
        final SimpleTimer simpleTimer4 = microMetricRegistry.simpleTimer(metadata2, tags);
        Assertions.assertNotNull(simpleTimer4);
        Assertions.assertEquals(0, simpleTimer4.getCount());
        simpleTimer4.update(Duration.ofMillis(1000));
        simpleTimer4.update(Duration.ofMillis(1000));
        simpleTimer4.update(Duration.ofMillis(1000));
        simpleTimer4.update(Duration.ofMillis(1000));
        Assertions.assertEquals(4, simpleTimer4.getCount());

        final Tag[] tags1 = new Tag[0];
        final SimpleTimer simpleTimer5 = microMetricRegistry.simpleTimer("st5", tags1);
        Assertions.assertNotNull(simpleTimer5);
        Assertions.assertEquals(0, simpleTimer5.getCount());
        simpleTimer5.update(Duration.ofMillis(1000));
        simpleTimer5.update(Duration.ofMillis(1000));
        simpleTimer5.update(Duration.ofMillis(1000));
        simpleTimer5.update(Duration.ofMillis(1000));
        simpleTimer5.update(Duration.ofMillis(1000));
        Assertions.assertEquals(5, simpleTimer5.getCount());

        final SortedMap<MetricID, SimpleTimer> simpleTimers = microMetricRegistry.getSimpleTimers();
        Assertions.assertNotNull(simpleTimers);
        Assertions.assertTrue(simpleTimers.size() >= 5);
        Assertions.assertTrue(simpleTimers.containsKey(new MetricID("st1")));
        Assertions.assertTrue(simpleTimers.containsKey(new MetricID("st2", tags)));
        Assertions.assertTrue(simpleTimers.containsKey(new MetricID("st3")));
        Assertions.assertTrue(simpleTimers.containsKey(new MetricID("st4", tags)));
        Assertions.assertTrue(simpleTimers.containsKey(new MetricID("st5", tags1)));

        final SortedMap<MetricID, SimpleTimer> simpleTimers1 =
                microMetricRegistry.getSimpleTimers((metricID, metric) -> {
                    final String name = metricID.getName();
                    return "st1".equals(name);
                });
        Assertions.assertNotNull(simpleTimers1);
        Assertions.assertEquals(1, simpleTimers1.size());
        Assertions.assertTrue(simpleTimers1.containsKey(new MetricID("st1")));
    }

    @Test
    void testRegisterCounter() {
        final CompositeMeterRegistry localCompositeMeterRegistry = new CompositeMeterRegistry();
        final io.micrometer.core.instrument.Counter counter = io.micrometer.core.instrument.Counter
                .builder("counter")
                .register(localCompositeMeterRegistry);

        final MicroCounter microCounter = new MicroCounter(counter);
        Assertions.assertNotNull(microCounter.getMicrometerCounter());
        Assertions.assertEquals(0, microCounter.getCount());

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Metadata metadata1 = new MetadataBuilder().withName("register-counter-1").build();
        final MicroCounter microCounter1 = microMetricRegistry.register(metadata1, microCounter, tags);
        Assertions.assertNotNull(microCounter1);
        Assertions.assertEquals(microCounter, microCounter1);

        final SortedMap<MetricID, Counter> counters = microMetricRegistry.getCounters((metricID, metric) -> {
            final String name = metricID.getName();
            return "register-counter-1".equals(name);
        });
        Assertions.assertNotNull(counters);
        Assertions.assertEquals(1, counters.size());
        Assertions.assertTrue(counters.containsKey(new MetricID("register-counter-1", tags)));
    }

    @Test
    void testRegisterGauge() {
        final Gauge<Integer> g1 = () -> 1;
        final Gauge<Integer> gauge1 = microMetricRegistry.register("register-gauge-1", g1);
        Assertions.assertNotNull(gauge1);
        Assertions.assertEquals(1, gauge1.getValue());
        Assertions.assertEquals(g1, gauge1);

        final Metadata metadata1 = new MetadataBuilder().withName("register-gauge-2").build();
        final Gauge<Integer> g2 = () -> 2;
        final Gauge<Integer> gauge2 = microMetricRegistry.register(metadata1, g2);
        Assertions.assertNotNull(gauge2);
        Assertions.assertEquals(2, gauge2.getValue());
        Assertions.assertEquals(g2, gauge2);

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Metadata metadata2 = new MetadataBuilder().withName("register-gauge-3").build();
        final Gauge<Integer> g3 = () -> 3;
        final Gauge<Integer> gauge3 = microMetricRegistry.register(metadata2, g3, tags);
        Assertions.assertNotNull(gauge3);
        Assertions.assertEquals(3, gauge3.getValue());
        Assertions.assertEquals(g3, gauge3);

        final SortedMap<MetricID, Gauge> gauges = microMetricRegistry.getGauges();
        Assertions.assertNotNull(gauges);
        Assertions.assertEquals(3, gauges.size());
        Assertions.assertTrue(gauges.containsKey(new MetricID("register-gauge-1")));
        Assertions.assertTrue(gauges.containsKey(new MetricID("register-gauge-2")));
        Assertions.assertTrue(gauges.containsKey(new MetricID("register-gauge-3", tags)));

        final SortedMap<MetricID, Gauge> gauges1 = microMetricRegistry.getGauges((metricID, metric) -> {
            final String name = metricID.getName();
            return "register-gauge-1".equals(name);
        });
        Assertions.assertNotNull(gauges1);
        Assertions.assertEquals(1, gauges1.size());
        Assertions.assertTrue(gauges1.containsKey(new MetricID("register-gauge-1")));
    }

    @Test
    void testRegisterHistogram() {
        final CompositeMeterRegistry localCompositeMeterRegistry = new CompositeMeterRegistry();
        final DistributionSummary distributionSummary = DistributionSummary
                .builder("register-histogram-summary1")
                .register(localCompositeMeterRegistry);
        final MicroHistogram microHistogram = new MicroHistogram(distributionSummary);

        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final Metadata metadata = new MetadataBuilder().withName("register-histogram-1").build();
        final MicroHistogram microHistogram1 = microMetricRegistry.register(metadata, microHistogram, tags);
        Assertions.assertNotNull(microHistogram1);
        Assertions.assertEquals(microHistogram, microHistogram1);

        final SortedMap<MetricID, Histogram> histograms = microMetricRegistry.getHistograms((metricID, metric) -> {
            final String name = metricID.getName();
            return "register-histogram-1".equals(name);
        });
        Assertions.assertNotNull(histograms);
        Assertions.assertEquals(1, histograms.size());
        Assertions.assertTrue(histograms.containsKey(new MetricID("register-histogram-1", tags)));
    }

    @Test
    void testRegisterMeter() {
        final CompositeMeterRegistry localCompositeMeterRegistry = new CompositeMeterRegistry();

        final List<io.micrometer.core.instrument.Tag> tagList = new ArrayList<>();
        final ImmutableTag tag = new ImmutableTag("tagName1", "tagValue1");
        tagList.add(tag);

        final com.codahale.metrics.Meter meter = new com.codahale.metrics.Meter();

        final String fifteenMinuteRateName = "register-meter-1" +
                MetricService.METRIC_NAME_SEPARATOR +
                MicroMetricRegistry.FIFTEEN_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> fifteenMinuteRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(fifteenMinuteRateName, meter, com.codahale.metrics.Meter::getFifteenMinuteRate)
                        .strongReference(true);
        fifteenMinuteRateGaugeBuilder.tags(tagList);
        final io.micrometer.core.instrument.Gauge fifteenMinuteRateGauge =
                fifteenMinuteRateGaugeBuilder.register(localCompositeMeterRegistry);


        final String fiveMinuteRateName = "register-meter-1" +
                MetricService.METRIC_NAME_SEPARATOR +
                MicroMetricRegistry.FIVE_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> fiveMinuteRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(fiveMinuteRateName, meter, com.codahale.metrics.Meter::getFiveMinuteRate)
                        .strongReference(true);
        fiveMinuteRateGaugeBuilder.tags(tagList);
        final io.micrometer.core.instrument.Gauge fiveMinuteRateGauge =
                fiveMinuteRateGaugeBuilder.register(localCompositeMeterRegistry);


        final String meanRateName = "register-meter-1" +
                MetricService.METRIC_NAME_SEPARATOR +
                MicroMetricRegistry.MEAN_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> meanRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(meanRateName, meter, com.codahale.metrics.Meter::getMeanRate)
                        .strongReference(true);
        meanRateGaugeBuilder.tags(tagList);
        final io.micrometer.core.instrument.Gauge meanRateGauge =
                meanRateGaugeBuilder.register(localCompositeMeterRegistry);


        final String oneMinuteRateName = "register-meter-1" +
                MetricService.METRIC_NAME_SEPARATOR +
                MicroMetricRegistry.ONE_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> oneMinuteRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(oneMinuteRateName, meter, com.codahale.metrics.Meter::getOneMinuteRate)
                        .strongReference(true);
        oneMinuteRateGaugeBuilder.tags(tagList);
        final io.micrometer.core.instrument.Gauge oneMinuteRateGauge =
                oneMinuteRateGaugeBuilder.register(localCompositeMeterRegistry);

        final Metadata metadata = new MetadataBuilder().withName("register-meter-1").build();
        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final MicroMeterGauge micrometerGauge = new MicroMeterGauge(
                meter, fifteenMinuteRateGauge, fiveMinuteRateGauge, meanRateGauge, oneMinuteRateGauge);
        final MicroMeterGauge micrometerGauge1 = microMetricRegistry.register(metadata, micrometerGauge, tags);
        Assertions.assertNotNull(micrometerGauge1);
        Assertions.assertEquals(micrometerGauge, micrometerGauge1);

        final SortedMap<MetricID, Meter> meters = microMetricRegistry.getMeters();
        Assertions.assertNotNull(meters);
        Assertions.assertTrue(meters.size() >= 1);
        Assertions.assertTrue(meters.containsKey(new MetricID("register-meter-1", tags)));

        final SortedMap<MetricID, Meter> meters1 = microMetricRegistry.getMeters((metricID, metric) -> {
            final String name = metricID.getName();
            return "register-meter-1".equals(name);
        });
        Assertions.assertNotNull(meters1);
        Assertions.assertEquals(1, meters1.size());
        Assertions.assertTrue(meters1.containsKey(new MetricID("register-meter-1", tags)));
    }

    @Test
    void testRegisterTimer() {
        final CompositeMeterRegistry localCompositeMeterRegistry = new CompositeMeterRegistry();

        final List<io.micrometer.core.instrument.Tag> tagList = new ArrayList<>();
        final ImmutableTag tag = new ImmutableTag("tagName1", "tagValue1");
        tagList.add(tag);

        final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer
                .builder("register-timer-1")
                .tags(tagList)
                .register(localCompositeMeterRegistry);
        final com.codahale.metrics.Meter codehaleMeter = new com.codahale.metrics.Meter();
        final MicroTimer microTimer = new MicroTimer(timer, codehaleMeter);

        final Metadata metadata = new MetadataBuilder().withName("register-timer-1").build();
        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");
        final MicroTimer microTimer1 = microMetricRegistry.register(metadata, microTimer, tags);
        Assertions.assertNotNull(microTimer1);
        Assertions.assertEquals(microTimer, microTimer1);

        final SortedMap<MetricID, Timer> timers = microMetricRegistry.getTimers();
        Assertions.assertNotNull(timers);
        Assertions.assertTrue(timers.containsKey(new MetricID("register-timer-1", tags)));

        final SortedMap<MetricID, Timer> timers1 = microMetricRegistry.getTimers((metricID, metric) -> {
            final String name = metricID.getName();
            return "register-timer-1".equals(name);
        });
        Assertions.assertNotNull(timers1);
        Assertions.assertEquals(1, timers1.size());
        Assertions.assertTrue(timers1.containsKey(new MetricID("register-timer-1", tags)));
    }

    @Test
    void testRegisterConcurrentGauge() {
        final CompositeMeterRegistry localCompositeMeterRegistry = new CompositeMeterRegistry();
        final DistributionSummary distributionSummary = DistributionSummary
                .builder("register-concurrent-gauge-summary1")
                .register(localCompositeMeterRegistry);
        final MicroConcurrentGauge microConcurrentGauge = new MicroConcurrentGauge(distributionSummary);

        final Metadata metadata = new MetadataBuilder().withName("register-concurrent-gauge-1").build();
        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");

        final MicroConcurrentGauge microConcurrentGauge1 =
                microMetricRegistry.register(metadata, microConcurrentGauge, tags);
        Assertions.assertNotNull(microConcurrentGauge1);
        Assertions.assertEquals(microConcurrentGauge, microConcurrentGauge1);

        final SortedMap<MetricID, ConcurrentGauge> concurrentGauges = microMetricRegistry.getConcurrentGauges();
        Assertions.assertNotNull(concurrentGauges);
        Assertions.assertTrue(concurrentGauges.containsKey(new MetricID("register-concurrent-gauge-1", tags)));

        final SortedMap<MetricID, ConcurrentGauge> concurrentGauges1 = microMetricRegistry.getConcurrentGauges(
                (metricID, metric) -> {
                    final String name = metricID.getName();
                    return "register-concurrent-gauge-1".equals(name);
                });
        Assertions.assertNotNull(concurrentGauges1);
        Assertions.assertEquals(1, concurrentGauges1.size());
        Assertions.assertTrue(concurrentGauges1.containsKey(new MetricID("register-concurrent-gauge-1", tags)));

    }

    @Test
    void testRegisterSimpleTimer() {
        final CompositeMeterRegistry localCompositeMeterRegistry = new CompositeMeterRegistry();

        final List<io.micrometer.core.instrument.Tag> tagList = new ArrayList<>();
        final ImmutableTag tag = new ImmutableTag("tagName1", "tagValue1");
        tagList.add(tag);

        final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer
                .builder("register-simple-timer-timer-1")
                .tags(tagList)
                .register(localCompositeMeterRegistry);
        final MicroSimpleTimer microSimpleTimer = new MicroSimpleTimer(timer);

        final Metadata metadata = new MetadataBuilder().withName("register-simple-timer-1").build();
        final Tag[] tags = new Tag[1];
        tags[0] = new Tag("tagName1", "tagValue1");

        final MicroSimpleTimer microSimpleTimer1 = microMetricRegistry.register(metadata, microSimpleTimer, tags);
        Assertions.assertNotNull(microSimpleTimer1);
        Assertions.assertEquals(microSimpleTimer, microSimpleTimer1);

        final SortedMap<MetricID, SimpleTimer> simpleTimers = microMetricRegistry.getSimpleTimers();
        Assertions.assertNotNull(simpleTimers);
        Assertions.assertTrue(simpleTimers.size() >= 1);
        Assertions.assertTrue(simpleTimers.containsKey(new MetricID("register-simple-timer-1", tags)));

        final SortedMap<MetricID, SimpleTimer> simpleTimers1 =
                microMetricRegistry.getSimpleTimers((metricID, metric) -> {
                    final String name = metricID.getName();
                    return "register-simple-timer-1".equals(name);
                });
        Assertions.assertNotNull(simpleTimers1);
        Assertions.assertEquals(1, simpleTimers1.size());
        Assertions.assertTrue(simpleTimers1.containsKey(new MetricID("register-simple-timer-1", tags)));
    }

    @Test
    void testRegisterJvmAndSystemMetrics() {
        final MicroMetricRegistry localMicroMetricRegistry = new MicroMetricRegistry(new CompositeMeterRegistry());
        localMicroMetricRegistry.registerJvmAndSystemMetrics();

        final SortedSet<String> names = localMicroMetricRegistry.getNames();
        Assertions.assertTrue(names.contains("jvm.buffer.count"));
        Assertions.assertTrue(names.contains("jvm.buffer.memory.used"));
        Assertions.assertTrue(names.contains("jvm.buffer.total.capacity"));
        Assertions.assertTrue(names.contains("jvm.memory.used"));
        Assertions.assertTrue(names.contains("jvm.memory.committed"));
        Assertions.assertTrue(names.contains("jvm.memory.max"));
        Assertions.assertTrue(names.contains("jvm.gc.max.data.size"));
        Assertions.assertTrue(names.contains("jvm.gc.live.data.size"));
        Assertions.assertTrue(names.contains("jvm.gc.memory.promoted"));
        Assertions.assertTrue(names.contains("jvm.threads.peak"));
        Assertions.assertTrue(names.contains("jvm.threads.daemon"));
        Assertions.assertTrue(names.contains("jvm.threads.live"));
        Assertions.assertTrue(names.contains("jvm.threads.states"));
        Assertions.assertTrue(names.contains("jvm.classes.loaded"));
        Assertions.assertTrue(names.contains("jvm.classes.unloaded"));
        Assertions.assertTrue(names.contains("system.cpu.count"));
        Assertions.assertTrue(names.contains("system.load.average.1m"));
        Assertions.assertTrue(names.contains("system.cpu.usage"));
        Assertions.assertTrue(names.contains("process.cpu.usage"));
    }
}
