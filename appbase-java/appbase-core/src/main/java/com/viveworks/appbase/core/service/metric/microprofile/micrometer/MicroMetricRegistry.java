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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Sampling;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link MetricRegistry} implementation using <code>Micrometer</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MethodCount")
public class MicroMetricRegistry extends MetricRegistry {
    static final String FIFTEEN_MINUTE_RATE_GAUGE_SUFFIX = "fifteenMinuteRateGauge";
    static final String FIVE_MINUTE_RATE_GAUGE_SUFFIX = "fiveMinuteRateGauge";
    static final String MEAN_RATE_GAUGE_SUFFIX = "meanRateGauge";
    static final String ONE_MINUTE_RATE_GAUGE_SUFFIX = "oneMinuteRateGauge";

    private static final String PERCENTILE_999_SUFFIX = "percentile" + MetricService.METRIC_NAME_SEPARATOR + "999";
    private static final String PERCENTILE_99_SUFFIX = "percentile" + MetricService.METRIC_NAME_SEPARATOR + "99";
    private static final String PERCENTILE_98_SUFFIX = "percentile" + MetricService.METRIC_NAME_SEPARATOR + "98";
    private static final String PERCENTILE_95_SUFFIX = "percentile" + MetricService.METRIC_NAME_SEPARATOR + "95";
    private static final String PERCENTILE_75_SUFFIX = "percentile" + MetricService.METRIC_NAME_SEPARATOR + "75";

    private static final String MAX_TIME_DURATION_NANOS = "maxTimeDurationNanos";
    private static final String MIN_TIME_DURATION_NANOS = "minTimeDurationNanos";
    private static final String MAX = "max";
    private static final String MIN = "min";
    private static final String SNAPSHOT = "snapshot";
    private static final String MEDIAN = "median";
    private static final String STD_DEV = "stdDev";
    private static final String MEAN = "mean";

    private static final double PERCENTILE_75 = 0.75;
    private static final double PERCENTILE_95 = 0.95;
    private static final double PERCENTILE_98 = 0.98;
    private static final double PERCENTILE_99 = 0.99;
    private static final double PERCENTILE_999 = 0.999;

    private static final String JVM_BUFFER_PREFIX = "jvm.buffer.";
    private static final String JVM_MEMORY_PREFIX = "jvm.memory.";
    private static final String JVM_GC_MAX_DATA_SIZE = "jvm.gc.max.data.size";
    private static final String JVM_GC_LIVE_DATA_SIZE = "jvm.gc.live.data.size";
    private static final String JVM_THREADS_PREFIX = "jvm.threads.";
    private static final String JVM_CLASSES_LOADED = "jvm.classes.loaded";
    private static final String SYSTEM_PREFIX = "system.";
    private static final String PROCESS_PREFIX = "process.";
    private static final String JVM_GC_MEMORY_PROMOTED = "jvm.gc.memory.promoted";
    private static final String JVM_GC_MEMORY_ALLOCATED = "jvm.gc.memory.allocated";
    private static final String JVM_GC_CONCURRENT_PHASE_TIME = "jvm.gc.concurrent.phase.time";
    private static final String JVM_GC_PAUSE = "jvm.gc.pause";
    private static final String JVM_CLASSES_UNLOADED = "jvm.classes.unloaded";

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroMetricRegistry.class);

    private final CompositeMeterRegistry compositeMeterRegistry;
    private final Map<MetricID, io.micrometer.core.instrument.Meter> micrometerMeterMap;
    private final Map<MetricID, Object> microProfileMetricMap;

    public MicroMetricRegistry(final CompositeMeterRegistry compositeMeterRegistry) {
        AppbaseUtil.notNull(compositeMeterRegistry, "compositeMeterRegistry");

        this.compositeMeterRegistry = compositeMeterRegistry;
        this.micrometerMeterMap = new ConcurrentHashMap<>();
        this.microProfileMetricMap = new ConcurrentHashMap<>();
    }

    @Override
    public <T extends Metric> T register(final String name,
                                         final T metric) throws IllegalArgumentException
    {
        return register(name, metric, (Tag[]) null);
    }

    @Override
    public <T extends Metric> T register(final Metadata metadata,
                                         final T metric) throws IllegalArgumentException
    {
        final String name = checkMetadata(metadata);
        return register(name, metric, (Tag[]) null);
    }

    @Override
    public <T extends Metric> T register(final Metadata metadata,
                                         final T metric,
                                         final Tag... tags) throws IllegalArgumentException
    {
        final String name = checkMetadata(metadata);
        return register(name, metric, tags);
    }

    @SuppressWarnings({"unchecked", "checkstyle:RightCurly"})
    private <T extends Metric> T register(final String name,
                                          final T metric,
                                          final Tag... tags) throws IllegalArgumentException
    {
        AppbaseUtil.notBlank(name, "name");
        AppbaseUtil.notNull(metric, "metric");

        final List<io.micrometer.core.instrument.Tag> tagList = toMicrometerTags(tags);
        final Search search = compositeMeterRegistry.find(name);
        if (tagList != null) {
            search.tags(tagList);
        }

        final Class<? extends Metric> metricType = metric.getClass();
        if (Counter.class.isAssignableFrom(metricType)) {
            final Counter microProfileCounter = (Counter) metric;
            registerCounter(microProfileCounter, search, name, tagList);
        }
        else if (Gauge.class.isAssignableFrom(metricType)) {
            final Gauge<? extends Number> microProfileGauge = (Gauge<? extends Number>) metric;
            registerGauge(microProfileGauge, search, name, tagList);
        }
        else if (Histogram.class.isAssignableFrom(metricType)) {
            final Histogram microProfileHistogram = (Histogram) metric;
            registerHistogram(microProfileHistogram, search, name, tagList);
        }
        else if (Meter.class.isAssignableFrom(metricType)) {
            final Meter microProfileMeter = (Meter) metric;
            registerMeter(microProfileMeter, search, name, tagList);
        }
        else if (Timer.class.isAssignableFrom(metricType)) {
            final Timer microProfileTimer = (Timer) metric;
            registerTimer(microProfileTimer, search, name, tagList);
        }
        else if (ConcurrentGauge.class.isAssignableFrom(metricType)) {
            final ConcurrentGauge concurrentGauge = (ConcurrentGauge) metric;
            registerConcurrentGauge(concurrentGauge, search, name, tagList);
        }
        else if (SimpleTimer.class.isAssignableFrom(metricType)) {
            final SimpleTimer simpleTimer = (SimpleTimer) metric;
            registerSimpleTimer(simpleTimer, search, name, tagList);
        }

        return metric;
    }

    private void registerSimpleTimer(final SimpleTimer microProfileSimpleTimer,
                                     final Search search,
                                     final String name,
                                     final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Gauge searchGauge = search.gauge();
        if (searchGauge == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder =
                    io.micrometer.core.instrument.Gauge
                            .builder(name, microProfileSimpleTimer::getCount)
                            .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge gauge = builder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), microProfileSimpleTimer, gauge);

            final io.micrometer.core.instrument.Gauge.Builder maxTimeDurationBuilder =
                    io.micrometer.core.instrument.Gauge
                            .builder(
                                    name + MetricService.METRIC_NAME_SEPARATOR + MAX_TIME_DURATION_NANOS,
                                    () -> {
                                        final Duration maxTimeDuration = microProfileSimpleTimer.getMaxTimeDuration();
                                        if (maxTimeDuration != null) {
                                            return maxTimeDuration.toNanos();
                                        }
                                        return 0L;
                                    })
                            .strongReference(true);
            if (tagList != null) {
                maxTimeDurationBuilder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge maxTimeDurationGauge =
                    maxTimeDurationBuilder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), null, maxTimeDurationGauge);

            final io.micrometer.core.instrument.Gauge.Builder minTimeDurationBuilder =
                    io.micrometer.core.instrument.Gauge
                            .builder(
                                    name + MetricService.METRIC_NAME_SEPARATOR + MIN_TIME_DURATION_NANOS,
                                    () -> {
                                        final Duration minTimeDuration = microProfileSimpleTimer.getMinTimeDuration();
                                        if (minTimeDuration != null) {
                                            return minTimeDuration.toNanos();
                                        }
                                        return 0L;
                                    })
                            .strongReference(true);
            if (tagList != null) {
                minTimeDurationBuilder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge minTimeDurationGauge =
                    minTimeDurationBuilder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), null, minTimeDurationGauge);
        }
    }

    private void registerConcurrentGauge(final ConcurrentGauge microProfileConcurrentGauge,
                                         final Search search,
                                         final String name,
                                         final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Gauge searchGauge = search.gauge();
        if (searchGauge == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder =
                    io.micrometer.core.instrument.Gauge
                            .builder(name, microProfileConcurrentGauge::getCount)
                            .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge gauge = builder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), microProfileConcurrentGauge, gauge);

            final io.micrometer.core.instrument.Gauge.Builder maxBuilder =
                    io.micrometer.core.instrument.Gauge
                            .builder(
                                    name + MetricService.METRIC_NAME_SEPARATOR + MAX,
                                    microProfileConcurrentGauge::getMax)
                            .strongReference(true);
            if (tagList != null) {
                maxBuilder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge maxGauge = maxBuilder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), null, maxGauge);

            final io.micrometer.core.instrument.Gauge.Builder minBuilder =
                    io.micrometer.core.instrument.Gauge
                            .builder(
                                    name + MetricService.METRIC_NAME_SEPARATOR + MIN,
                                    microProfileConcurrentGauge::getMin)
                            .strongReference(true);
            if (tagList != null) {
                minBuilder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge minGauge = minBuilder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), null, minGauge);
        }
    }

    private void registerTimer(final Timer microProfileTimer,
                               final Search search,
                               final String name,
                               final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Gauge searchGauge = search.gauge();
        if (searchGauge == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder =
                    io.micrometer.core.instrument.Gauge
                            .builder(name, microProfileTimer::getCount)
                            .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge gauge = builder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), microProfileTimer, gauge);

            registerMetered(microProfileTimer, name, tagList);
            registerSnapshot(microProfileTimer, name, tagList);
        }
    }

    private void registerMeter(final Meter microProfileMeter,
                               final Search search,
                               final String name,
                               final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Gauge searchGauge = search.gauge();
        if (searchGauge == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder =
                    io.micrometer.core.instrument.Gauge
                            .builder(name, microProfileMeter::getCount)
                            .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge gauge = builder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), microProfileMeter, gauge);

            registerMetered(microProfileMeter, name, tagList);
        }
    }

    private void registerMetered(final Metered metered,
                                 final String name,
                                 final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final String fifteenMinuteRateName =
                name + MetricService.METRIC_NAME_SEPARATOR + FIFTEEN_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder fifteenMinuteRateBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(fifteenMinuteRateName, metered::getFifteenMinuteRate)
                        .strongReference(true);
        if (tagList != null) {
            fifteenMinuteRateBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge fifteenMinuteRateGauge =
                fifteenMinuteRateBuilder.register(compositeMeterRegistry);
        addMetric(fifteenMinuteRateName, Tags.of(tagList), null, fifteenMinuteRateGauge);


        final String fiveMinuteRateName =
                name + MetricService.METRIC_NAME_SEPARATOR + FIVE_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder fiveMinuteRateBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(fiveMinuteRateName, metered::getFiveMinuteRate)
                        .strongReference(true);
        if (tagList != null) {
            fiveMinuteRateBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge fiveMinuteRateGauge =
                fiveMinuteRateBuilder.register(compositeMeterRegistry);
        addMetric(fiveMinuteRateName, Tags.of(tagList), null, fiveMinuteRateGauge);


        final String meanRateName = name + MetricService.METRIC_NAME_SEPARATOR + MEAN_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder meanRateBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(meanRateName, metered::getMeanRate)
                        .strongReference(true);
        if (tagList != null) {
            meanRateBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge meanRateGauge =
                meanRateBuilder.register(compositeMeterRegistry);
        addMetric(meanRateName, Tags.of(tagList), null, meanRateGauge);


        final String oneMinuteRateName = name + MetricService.METRIC_NAME_SEPARATOR + ONE_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder oneMinuteRateBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(oneMinuteRateName, metered::getOneMinuteRate)
                        .strongReference(true);
        if (tagList != null) {
            oneMinuteRateBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge oneMinuteRateGauge =
                oneMinuteRateBuilder.register(compositeMeterRegistry);
        addMetric(oneMinuteRateName, Tags.of(tagList), null, oneMinuteRateGauge);
    }

    private void registerSnapshot(final Sampling sampling,
                                  final String name,
                                  final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final String baseName = name + SNAPSHOT;


        final String medianName = baseName + MEDIAN;
        final io.micrometer.core.instrument.Gauge.Builder medianBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(medianName, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.getMedian();
                        })
                        .strongReference(true);
        if (tagList != null) {
            medianBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge medianGauge = medianBuilder.register(compositeMeterRegistry);
        addMetric(medianName, Tags.of(tagList), sampling, medianGauge);


        final String percentile999Name = baseName + PERCENTILE_999_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder percentile999Builder =
                io.micrometer.core.instrument.Gauge
                        .builder(percentile999Name, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.get999thPercentile();
                        })
                        .strongReference(true);
        if (tagList != null) {
            percentile999Builder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge percentile999Gauge =
                percentile999Builder.register(compositeMeterRegistry);
        addMetric(percentile999Name, Tags.of(tagList), null, percentile999Gauge);


        final String percentile99Name = baseName + PERCENTILE_99_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder percentile99Builder =
                io.micrometer.core.instrument.Gauge
                        .builder(percentile99Name, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.get99thPercentile();
                        })
                        .strongReference(true);
        if (tagList != null) {
            percentile99Builder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge percentile99Gauge =
                percentile99Builder.register(compositeMeterRegistry);
        addMetric(percentile99Name, Tags.of(tagList), null, percentile99Gauge);


        final String percentile98Name = baseName + PERCENTILE_98_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder percentile98Builder =
                io.micrometer.core.instrument.Gauge
                        .builder(percentile98Name, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.get98thPercentile();
                        })
                        .strongReference(true);
        if (tagList != null) {
            percentile98Builder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge percentile98Gauge =
                percentile98Builder.register(compositeMeterRegistry);
        addMetric(percentile98Name, Tags.of(tagList), null, percentile98Gauge);


        final String percentile95Name = baseName + PERCENTILE_95_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder percentile95Builder =
                io.micrometer.core.instrument.Gauge
                        .builder(percentile95Name, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.get95thPercentile();
                        })
                        .strongReference(true);
        if (tagList != null) {
            percentile95Builder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge percentile95Gauge =
                percentile95Builder.register(compositeMeterRegistry);
        addMetric(percentile95Name, Tags.of(tagList), null, percentile95Gauge);


        final String percentile75Name = baseName + PERCENTILE_75_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder percentile75Builder =
                io.micrometer.core.instrument.Gauge
                        .builder(percentile75Name, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.get75thPercentile();
                        })
                        .strongReference(true);
        if (tagList != null) {
            percentile75Builder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge percentile75Gauge =
                percentile75Builder.register(compositeMeterRegistry);
        addMetric(percentile75Name, Tags.of(tagList), null, percentile75Gauge);


        final String sizeName = baseName + "size";
        final io.micrometer.core.instrument.Gauge.Builder sizeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(sizeName, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.size();
                        })
                        .strongReference(true);
        if (tagList != null) {
            sizeBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge sizeGauge = sizeBuilder.register(compositeMeterRegistry);
        addMetric(sizeName, Tags.of(tagList), null, sizeGauge);


        final String stdDevName = baseName + STD_DEV;
        final io.micrometer.core.instrument.Gauge.Builder stdDevBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(stdDevName, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.getStdDev();
                        })
                        .strongReference(true);
        if (tagList != null) {
            stdDevBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge stdDevGauge = stdDevBuilder.register(compositeMeterRegistry);
        addMetric(stdDevName, Tags.of(tagList), null, stdDevGauge);


        final String minName = baseName + MIN;
        final io.micrometer.core.instrument.Gauge.Builder minBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(minName, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.getMin();
                        })
                        .strongReference(true);
        if (tagList != null) {
            minBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge minGauge = minBuilder.register(compositeMeterRegistry);
        addMetric(minName, Tags.of(tagList), null, minGauge);


        final String meanName = baseName + MEAN;
        final io.micrometer.core.instrument.Gauge.Builder meanBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(meanName, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.getMean();
                        })
                        .strongReference(true);
        if (tagList != null) {
            meanBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge meanGauge = meanBuilder.register(compositeMeterRegistry);
        addMetric(meanName, Tags.of(tagList), null, meanGauge);


        final String maxName = baseName + MAX;
        final io.micrometer.core.instrument.Gauge.Builder maxBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(maxName, () -> {
                            final Snapshot snapshot = sampling.getSnapshot();
                            return snapshot.getMax();
                        })
                        .strongReference(true);
        if (tagList != null) {
            maxBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge maxGauge = maxBuilder.register(compositeMeterRegistry);
        addMetric(maxName, Tags.of(tagList), null, maxGauge);
    }

    private void registerHistogram(final Histogram microProfileHistogram,
                                   final Search search,
                                   final String name,
                                   final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Gauge searchGauge = search.gauge();
        if (searchGauge == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder =
                    io.micrometer.core.instrument.Gauge
                            .builder(name, microProfileHistogram::getCount)
                            .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge gauge = builder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), microProfileHistogram, gauge);

            registerSnapshot(microProfileHistogram, name, tagList);
        }
    }

    private void registerCounter(final Counter microProfileCounter,
                                 final Search search,
                                 final String name,
                                 final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Counter searchCounter = search.counter();
        if (searchCounter == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder =
                    io.micrometer.core.instrument.Gauge
                            .builder(name, microProfileCounter::getCount)
                            .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge gauge = builder.register(compositeMeterRegistry);

            addMetric(name, Tags.of(tagList), microProfileCounter, gauge);
        }
    }

    private void registerGauge(final Gauge<? extends Number> microProfileGauge,
                               final Search search,
                               final String name,
                               final List<io.micrometer.core.instrument.Tag> tagList)
    {
        final io.micrometer.core.instrument.Gauge searchGauge = search.gauge();
        if (searchGauge == null) {
            final io.micrometer.core.instrument.Gauge.Builder builder = io.micrometer.core.instrument.Gauge
                    .builder(name, microProfileGauge::getValue)
                    .strongReference(true);
            if (tagList != null) {
                builder.tags(tagList);
            }
            final io.micrometer.core.instrument.Gauge micrometerGauge = builder.register(compositeMeterRegistry);
            addMetric(name, Tags.of(tagList), microProfileGauge, micrometerGauge);
        }
    }

    /**
     * Register JVM and System metric.
     */
    @SuppressWarnings({"checkstyle:RightCurly", "checkstyle:LeftCurly", "checkstyle:EqualsAvoidNull"})
    public void registerJvmAndSystemMetrics() {
        final JvmMemoryMetrics jvmMemoryMetrics = new JvmMemoryMetrics();
        jvmMemoryMetrics.bindTo(compositeMeterRegistry);

        final JvmGcMetrics jvmGcMetrics = new JvmGcMetrics();
        jvmGcMetrics.bindTo(compositeMeterRegistry);

        final JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        jvmThreadMetrics.bindTo(compositeMeterRegistry);

        final ClassLoaderMetrics classLoaderMetrics = new ClassLoaderMetrics();
        classLoaderMetrics.bindTo(compositeMeterRegistry);

        final ProcessorMetrics processorMetrics = new ProcessorMetrics();
        processorMetrics.bindTo(compositeMeterRegistry);

        compositeMeterRegistry.forEachMeter(meter -> {
            final io.micrometer.core.instrument.Meter.Id id = meter.getId();
            final String name = id.getName();
            final List<io.micrometer.core.instrument.Tag> tagList = id.getTags();
            final Tags tags = Tags.of(tagList);

            if (name.startsWith(JVM_BUFFER_PREFIX) || name.startsWith(JVM_MEMORY_PREFIX) ||
                    JVM_GC_MAX_DATA_SIZE.equals(name) || JVM_GC_LIVE_DATA_SIZE.equals(name) ||
                    name.startsWith(JVM_THREADS_PREFIX) ||
                    JVM_CLASSES_LOADED.equals(name) ||
                    name.startsWith(SYSTEM_PREFIX) || name.startsWith(PROCESS_PREFIX))
            {
                if (meter instanceof final io.micrometer.core.instrument.Gauge micrometerGauge) {
                    final Gauge<Number> microProfileGauge = micrometerGauge::value;
                    addMetric(name, tags, microProfileGauge, micrometerGauge);
                } else if (meter instanceof final FunctionCounter functionCounter) {
                    final Gauge<Number> microProfileGauge = functionCounter::count;
                    addMetric(name, tags, microProfileGauge, functionCounter);
                } else {
                    LOGGER.warn("Skipping JVM metric '{}': unrecognized meter type {}",
                            name, meter.getClass().getSimpleName());
                }
            }
            else if (JVM_GC_MEMORY_PROMOTED.equals(name) || JVM_GC_MEMORY_ALLOCATED.equals(name))
            {
                if (meter instanceof final io.micrometer.core.instrument.Counter micrometerCounter) {
                    final MicroCounter microCounter = new MicroCounter(micrometerCounter);
                    addMetric(name, tags, microCounter, micrometerCounter);
                } else {
                    LOGGER.warn("Skipping GC memory metric '{}': unrecognized meter type {}",
                            name, meter.getClass().getSimpleName());
                }
            }
            else if (JVM_GC_CONCURRENT_PHASE_TIME.equals(name) || JVM_GC_PAUSE.equals(name))
            {
                if (meter instanceof final io.micrometer.core.instrument.Timer micrometerTimer) {
                    final com.codahale.metrics.Meter codehaleMeter = new com.codahale.metrics.Meter();
                    final MicroTimer microTimer = new MicroTimer(micrometerTimer, codehaleMeter);
                    addMetric(name, tags, microTimer, micrometerTimer);
                } else {
                    LOGGER.warn("Skipping GC timing metric '{}': unrecognized meter type {}",
                            name, meter.getClass().getSimpleName());
                }
            }
            else if (JVM_CLASSES_UNLOADED.equals(name))
            {
                if (meter instanceof final FunctionCounter functionCounter) {
                    final Gauge<Number> gauge = functionCounter::count;
                    addMetric(name, tags, gauge, functionCounter);
                } else {
                    LOGGER.warn("Skipping classes-unloaded metric '{}': unrecognized meter type {}",
                            name, meter.getClass().getSimpleName());
                }
            }
        });
    }

    @Override
    public Counter counter(final String name) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name);
        if (metric != null && MicroCounter.class.equals(metric.getClass())) {
            return (MicroCounter) metric;
        }

        final io.micrometer.core.instrument.Counter counter = compositeMeterRegistry.counter(name);
        final MicroCounter microCounter = new MicroCounter(counter);
        addMetric(name, null, microCounter, counter);

        return microCounter;
    }

    @Override
    public Counter counter(final String name, final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name, tags);
        if (metric != null && MicroCounter.class.equals(metric.getClass())) {
            return (MicroCounter) metric;
        }

        if (tags != null && tags.length > 0) {
            final List<io.micrometer.core.instrument.Tag> micrometerTags = toMicrometerTags(tags);
            if (micrometerTags != null && !micrometerTags.isEmpty()) {
                final io.micrometer.core.instrument.Counter counter =
                        compositeMeterRegistry.counter(name, micrometerTags);
                final MicroCounter microCounter = new MicroCounter(counter);
                final Tags tags1 = Tags.of(micrometerTags);
                addMetric(name, tags1, microCounter, counter);
                return microCounter;

            }
        }

        return counter(name);
    }

    @Override
    public Counter counter(final Metadata metadata) {
        final String name = checkMetadata(metadata);
        return counter(name);
    }

    @Override
    public Counter counter(final Metadata metadata, final Tag... tags) {
        final String name = checkMetadata(metadata);
        return counter(name, tags);
    }

    @Override
    public ConcurrentGauge concurrentGauge(final String name) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name);
        if (metric != null && MicroConcurrentGauge.class.equals(metric.getClass())) {
            return (MicroConcurrentGauge) metric;
        }

        final DistributionSummary distributionSummary = DistributionSummary
                .builder(name)
                .publishPercentileHistogram(false)
                .register(compositeMeterRegistry);
        final MicroConcurrentGauge microConcurrentGauge = new MicroConcurrentGauge(distributionSummary);
        addMetric(name, null, microConcurrentGauge, distributionSummary);

        return microConcurrentGauge;
    }

    @Override
    public ConcurrentGauge concurrentGauge(final String name, final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name, tags);
        if (metric != null && MicroConcurrentGauge.class.equals(metric.getClass())) {
            return (MicroConcurrentGauge) metric;
        }

        if (tags != null && tags.length > 0) {
            final List<io.micrometer.core.instrument.Tag> micrometerTags = toMicrometerTags(tags);
            if (micrometerTags != null && !micrometerTags.isEmpty()) {
                final DistributionSummary distributionSummary = DistributionSummary
                        .builder(name)
                        .tags(micrometerTags)
                        .publishPercentileHistogram(false)
                        .register(compositeMeterRegistry);
                final MicroConcurrentGauge microConcurrentGauge = new MicroConcurrentGauge(distributionSummary);
                addMetric(name, Tags.of(micrometerTags), microConcurrentGauge, distributionSummary);

                return microConcurrentGauge;
            }
        }

        return concurrentGauge(name);
    }

    @Override
    public ConcurrentGauge concurrentGauge(final Metadata metadata) {
        final String name = checkMetadata(metadata);
        return concurrentGauge(name);
    }

    @Override
    public ConcurrentGauge concurrentGauge(final Metadata metadata, final Tag... tags) {
        final String name = checkMetadata(metadata);
        return concurrentGauge(name, tags);
    }

    @Override
    public Histogram histogram(final String name) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name);
        if (metric != null && MicroHistogram.class.equals(metric.getClass())) {
            return (MicroHistogram) metric;
        }

        final DistributionSummary distributionSummary = DistributionSummary
                .builder(name)
                .publishPercentiles(
                        PERCENTILE_75,
                        PERCENTILE_95,
                        PERCENTILE_98,
                        PERCENTILE_99,
                        PERCENTILE_999)
                .publishPercentileHistogram(true)
                .register(compositeMeterRegistry);

        final MicroHistogram microHistogram = new MicroHistogram(distributionSummary);
        addMetric(name, null, microHistogram, distributionSummary);
        return microHistogram;
    }

    @Override
    public Histogram histogram(final String name, final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name, tags);
        if (metric != null && MicroHistogram.class.equals(metric.getClass())) {
            return (MicroHistogram) metric;
        }

        if (tags != null && tags.length > 0) {
            final List<io.micrometer.core.instrument.Tag> tagList = toMicrometerTags(tags);
            final DistributionSummary distributionSummary = DistributionSummary
                    .builder(name)
                    .tags(tagList)
                    .publishPercentiles(
                            PERCENTILE_75,
                            PERCENTILE_95,
                            PERCENTILE_98,
                            PERCENTILE_99,
                            PERCENTILE_999)
                    .publishPercentileHistogram(true)
                    .register(compositeMeterRegistry);
            final MicroHistogram microHistogram = new MicroHistogram(distributionSummary);
            Tags tags1 = null;
            if (tagList != null && !tagList.isEmpty()) {
                tags1 = Tags.of(tagList);
            }
            addMetric(name, tags1, microHistogram, distributionSummary);
            return microHistogram;
        }

        return histogram(name);
    }

    @Override
    public Histogram histogram(final Metadata metadata) {
        final String name = checkMetadata(metadata);
        return histogram(name);
    }

    @Override
    public Histogram histogram(final Metadata metadata, final Tag... tags) {
        final String name = checkMetadata(metadata);
        return histogram(name, tags);
    }

    @Override
    public Meter meter(final String name) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name);
        if (metric != null && MicroMeterGauge.class.equals(metric.getClass())) {
            return (MicroMeterGauge) metric;
        }

        return meter(name, (Tag[]) null);
    }

    @Override
    public Meter meter(final String name, final Tag... tags) {
        AppbaseUtil.notNull(name, "name");

        final Metric metric = getMetric(name, tags);
        if (metric != null && MicroMeterGauge.class.equals(metric.getClass())) {
            return (MicroMeterGauge) metric;
        }

        final List<io.micrometer.core.instrument.Tag> tagList;
        if (tags != null) {
            tagList = toMicrometerTags(tags);
        } else {
            tagList = null;
        }

        final String fifteenMinuteRateName =
                name + MetricService.METRIC_NAME_SEPARATOR + FIFTEEN_MINUTE_RATE_GAUGE_SUFFIX;
        final com.codahale.metrics.Meter meter = new com.codahale.metrics.Meter();
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> fifteenMinuteRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(fifteenMinuteRateName, meter, com.codahale.metrics.Meter::getFifteenMinuteRate)
                        .strongReference(true);
        if (tagList != null) {
            fifteenMinuteRateGaugeBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge fifteenMinuteRateGauge =
                fifteenMinuteRateGaugeBuilder.register(compositeMeterRegistry);


        final String fiveMinuteRateName = name + MetricService.METRIC_NAME_SEPARATOR + FIVE_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> fiveMinuteRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(fiveMinuteRateName, meter, com.codahale.metrics.Meter::getFiveMinuteRate)
                        .strongReference(true);
        if (tagList != null) {
            fiveMinuteRateGaugeBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge fiveMinuteRateGauge =
                fiveMinuteRateGaugeBuilder.register(compositeMeterRegistry);


        final String meanRateName = name + MetricService.METRIC_NAME_SEPARATOR + MEAN_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> meanRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(meanRateName, meter, com.codahale.metrics.Meter::getMeanRate)
                        .strongReference(true);
        if (tagList != null) {
            meanRateGaugeBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge meanRateGauge =
                meanRateGaugeBuilder.register(compositeMeterRegistry);


        final String oneMinuteRateName = name + MetricService.METRIC_NAME_SEPARATOR + ONE_MINUTE_RATE_GAUGE_SUFFIX;
        final io.micrometer.core.instrument.Gauge.Builder<com.codahale.metrics.Meter> oneMinuteRateGaugeBuilder =
                io.micrometer.core.instrument.Gauge
                        .builder(oneMinuteRateName, meter, com.codahale.metrics.Meter::getOneMinuteRate)
                        .strongReference(true);
        if (tagList != null) {
            oneMinuteRateGaugeBuilder.tags(tagList);
        }
        final io.micrometer.core.instrument.Gauge oneMinuteRateGauge =
                oneMinuteRateGaugeBuilder.register(compositeMeterRegistry);

        Tags tags1 = null;
        if (tagList != null) {
            tags1 = Tags.of(tagList);
        }

        final MicroMeterGauge micrometerGauge = new MicroMeterGauge(
                meter, fifteenMinuteRateGauge, fiveMinuteRateGauge, meanRateGauge, oneMinuteRateGauge);

        addMetric(name, tags1, micrometerGauge, null);
        addMetric(fifteenMinuteRateName, tags1, null, fifteenMinuteRateGauge);
        addMetric(fiveMinuteRateName, tags1, null, fiveMinuteRateGauge);
        addMetric(meanRateName, tags1, null, meanRateGauge);
        addMetric(oneMinuteRateName, tags1, null, oneMinuteRateGauge);

        return micrometerGauge;
    }

    @Override
    public Meter meter(final Metadata metadata) {
        final String name = checkMetadata(metadata);
        return meter(name);
    }

    @Override
    public Meter meter(final Metadata metadata, final Tag... tags) {
        final String name = checkMetadata(metadata);
        return meter(name, tags);
    }

    @Override
    public Timer timer(final String name) {
        AppbaseUtil.notNull(name, "name");

        final Metric metric = getMetric(name);
        if (metric != null && MicroTimer.class.equals(metric.getClass())) {
            return (MicroTimer) metric;
        }

        final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer
                .builder(name)
                .publishPercentiles(
                        PERCENTILE_75,
                        PERCENTILE_95,
                        PERCENTILE_98,
                        PERCENTILE_99,
                        PERCENTILE_999)
                .publishPercentileHistogram(true)
                .register(compositeMeterRegistry);
        final com.codahale.metrics.Meter codehaleMeter = new com.codahale.metrics.Meter();
        final MicroTimer microTimer = new MicroTimer(timer, codehaleMeter);
        addMetric(name, null, microTimer, timer);

        return microTimer;
    }

    @Override
    public Timer timer(final String name, final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name, tags);
        if (metric != null && MicroTimer.class.equals(metric.getClass())) {
            return (MicroTimer) metric;
        }

        if (tags != null && tags.length > 0) {
            final List<io.micrometer.core.instrument.Tag> tagList = toMicrometerTags(tags);
            final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer
                    .builder(name)
                    .tags(tagList)
                    .publishPercentiles(
                            PERCENTILE_75,
                            PERCENTILE_95,
                            PERCENTILE_98,
                            PERCENTILE_99,
                            PERCENTILE_999)
                    .publishPercentileHistogram(true)
                    .register(compositeMeterRegistry);
            final com.codahale.metrics.Meter codehaleMeter = new com.codahale.metrics.Meter();
            final MicroTimer microTimer = new MicroTimer(timer, codehaleMeter);
            Tags tags1 = null;
            if (tagList != null && !tagList.isEmpty()) {
                tags1 = Tags.of(tagList);
            }
            addMetric(name, tags1, microTimer, timer);

            return microTimer;
        }

        return timer(name);
    }

    @Override
    public Timer timer(final Metadata metadata) {
        final String name = checkMetadata(metadata);
        return timer(name);
    }

    @Override
    public Timer timer(final Metadata metadata, final Tag... tags) {
        final String name = checkMetadata(metadata);
        return timer(name, tags);
    }

    @Override
    public SimpleTimer simpleTimer(final String name) {
        AppbaseUtil.notNull(name, "name");

        final Metric metric = getMetric(name);
        if (metric != null && MicroSimpleTimer.class.equals(metric.getClass())) {
            return (MicroSimpleTimer) metric;
        }

        final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer
                .builder(name)
                .publishPercentileHistogram(false)
                .register(compositeMeterRegistry);
        final MicroSimpleTimer microSimpleTimer = new MicroSimpleTimer(timer);
        addMetric(name, null, microSimpleTimer, timer);

        return microSimpleTimer;
    }

    @Override
    public SimpleTimer simpleTimer(final String name, final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final Metric metric = getMetric(name, tags);
        if (metric != null && MicroSimpleTimer.class.equals(metric.getClass())) {
            return (MicroSimpleTimer) metric;
        }

        if (tags != null && tags.length > 0) {
            final List<io.micrometer.core.instrument.Tag> tagList = toMicrometerTags(tags);
            final io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Timer
                    .builder(name)
                    .tags(tagList)
                    .publishPercentileHistogram(false)
                    .register(compositeMeterRegistry);
            final MicroSimpleTimer microSimpleTimer = new MicroSimpleTimer(timer);
            addMetric(name, Tags.of(tagList), microSimpleTimer, timer);
            return microSimpleTimer;
        }

        return simpleTimer(name);
    }

    @Override
    public SimpleTimer simpleTimer(final Metadata metadata) {
        final String name = checkMetadata(metadata);
        return simpleTimer(name);
    }

    @Override
    public SimpleTimer simpleTimer(final Metadata metadata, final Tag... tags) {
        final String name = checkMetadata(metadata);
        return simpleTimer(name, tags);
    }

    private boolean remove(final String name, @Nullable final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final Search search = compositeMeterRegistry.find(name);
        if (tags != null && tags.length > 0) {
            final List<io.micrometer.core.instrument.Tag> tagList = toMicrometerTags(tags);
            search.tags(tagList);
        }

        final Collection<io.micrometer.core.instrument.Counter> counters = search.counters();
        counters.forEach(compositeMeterRegistry::remove);

        final Collection<FunctionCounter> functionCounters = search.functionCounters();
        functionCounters.forEach(compositeMeterRegistry::remove);

        final Collection<FunctionTimer> functionTimers = search.functionTimers();
        functionTimers.forEach(compositeMeterRegistry::remove);

        final Collection<io.micrometer.core.instrument.Gauge> gauges = search.gauges();
        gauges.forEach(compositeMeterRegistry::remove);

        final Collection<LongTaskTimer> longTaskTimers = search.longTaskTimers();
        longTaskTimers.forEach(compositeMeterRegistry::remove);

        final Collection<io.micrometer.core.instrument.Meter> meters = search.meters();
        meters.forEach(compositeMeterRegistry::remove);

        final Collection<DistributionSummary> summaries = search.summaries();
        summaries.forEach(compositeMeterRegistry::remove);

        final Collection<TimeGauge> timeGauges = search.timeGauges();
        timeGauges.forEach(compositeMeterRegistry::remove);

        final Collection<io.micrometer.core.instrument.Timer> timers = search.timers();
        timers.forEach(compositeMeterRegistry::remove);

        return true;
    }

    @Override
    public boolean remove(final String name) {
        AppbaseUtil.notBlank(name, "name");

        final boolean remove = remove(name, (Tag[]) null);
        final MetricID metricID = new MetricID(name);
        micrometerMeterMap.remove(metricID);
        microProfileMetricMap.remove(metricID);

        return remove;
    }

    @Override
    public boolean remove(final MetricID metricID) {
        AppbaseUtil.notNull(metricID, "metricID");

        final String name = metricID.getName();
        AppbaseUtil.notBlank(name, "name");

        final boolean remove = remove(name, metricID.getTagsAsArray());
        micrometerMeterMap.remove(metricID);
        microProfileMetricMap.remove(metricID);

        return remove;
    }

    @Override
    public void removeMatching(final MetricFilter filter) {
        AppbaseUtil.notNull(filter, "filter");

        final List<io.micrometer.core.instrument.Meter> meters = compositeMeterRegistry.getMeters();
        meters.forEach(meter -> {
            final io.micrometer.core.instrument.Meter.Id id = meter.getId();
            final String name = id.getName();
            final List<io.micrometer.core.instrument.Tag> tags = id.getTags();

            final MetricID metricID;
            final Tag[] microProfileTags = MetricService.toMicroProfileTags(Tags.of(tags));
            if (microProfileTags != null && microProfileTags.length > 0) {
                metricID = new MetricID(name, microProfileTags);
            } else {
                metricID = new MetricID(name);
            }

            final Metric metric = (Metric) microProfileMetricMap.get(metricID);
            if (metric != null) {
                if (filter.matches(metricID, metric)) {
                    compositeMeterRegistry.remove(meter);
                    microProfileMetricMap.remove(metricID);
                    micrometerMeterMap.remove(metricID);
                }
            }
        });
    }

    @Override
    public SortedSet<String> getNames() {
        final SortedSet<String> resultSet = new TreeSet<>();

        microProfileMetricMap.forEach(((metricID, metric) -> {
            if (metricID != null) {
                final String name = metricID.getName();
                if (name != null) {
                    resultSet.add(name);
                }
            }
        }));

        return Collections.unmodifiableSortedSet(resultSet);
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        final SortedSet<MetricID> resultSet = new TreeSet<>();

        microProfileMetricMap.forEach(((metricID, metric) -> {
            if (metricID != null) {
                resultSet.add(metricID);
            }
        }));

        return Collections.unmodifiableSortedSet(resultSet);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getMetrics(Gauge.class, null);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(final MetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return getMetrics(Counter.class, null);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(final MetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return getMetrics(ConcurrentGauge.class, null);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(final MetricFilter filter) {
        return getMetrics(ConcurrentGauge.class, filter);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return getMetrics(Histogram.class, null);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(final MetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return getMetrics(Meter.class, null);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(final MetricFilter filter) {
        return getMetrics(Meter.class, filter);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getMetrics(Timer.class, null);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(final MetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return getMetrics(SimpleTimer.class, null);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(final MetricFilter filter) {
        return getMetrics(SimpleTimer.class, filter);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<MetricID, T> getMetrics(final Class<T> type,
                                                                 final MetricFilter filter)
    {
        if (type == null && filter == null) {
            throw new AppbaseRuntimeException("One of type or filter must not be null");
        }

        final SortedMap<MetricID, T> resultMap = new TreeMap<>();
        if (filter != null) {
            microProfileMetricMap.forEach(((metricID, metric) -> {
                if (filter.matches(metricID, (Metric) metric)) {
                    if (type == null) {
                        resultMap.put(metricID, (T) metric);
                    } else if (type.isAssignableFrom(metric.getClass())) {
                        resultMap.put(metricID, (T) metric);
                    }
                }
            }));
        } else {
            microProfileMetricMap.forEach(((metricID, metric) -> {
                if (type.isAssignableFrom(metric.getClass())) {
                    resultMap.put(metricID, (T) metric);
                }
            }));
        }

        return Collections.unmodifiableSortedMap(resultMap);
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        final Map<MetricID, Metric> metrics = new HashMap<>();
        microProfileMetricMap.forEach(((metricID, o) -> {
            if (o instanceof Metric) {
                metrics.put(metricID, (Metric) o);
            }
        }));

        return Collections.unmodifiableMap(metrics);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Map.of();
    }

    @Override
    public Type getType() {
        return Type.BASE;
    }

    public Map<MetricID, io.micrometer.core.instrument.Meter> getMicrometerMeterMap() {
        return micrometerMeterMap;
    }

    private List<io.micrometer.core.instrument.Tag> toMicrometerTags(final Tag... tags) {
        final List<io.micrometer.core.instrument.Tag> tagList = new ArrayList<>();
        if (tags != null && tags.length > 0) {
            for (final Tag tag : tags) {
                if (tag != null) {
                    final String tagName = tag.getTagName();
                    final String tagValue = tag.getTagValue();

                    if (!AppbaseUtil.isBlank(tagName) && !AppbaseUtil.isBlank(tagValue)) {
                        final io.micrometer.core.instrument.Tag micrometerTag = new ImmutableTag(tagName, tagValue);
                        tagList.add(micrometerTag);
                    }
                }
            }
        }

        return tagList;
    }

    private String checkMetadata(final Metadata metadata) {
        AppbaseUtil.notNull(metadata, "metadata");

        final String name = metadata.getName();
        AppbaseUtil.notBlank(name, "name");

        return name;
    }

    private MetricID getMetricID(final String name, final Tag... tags) {
        AppbaseUtil.notBlank(name, "name");

        final MetricID metricID;
        if (tags != null) {
            metricID = new MetricID(name, tags);
        } else {
            metricID = new MetricID(name);
        }

        return metricID;
    }

    private Metric getMetric(final String name, final Tag... tags) {
        final MetricID metricID = getMetricID(name, tags);
        return (Metric) microProfileMetricMap.get(metricID);
    }

    private void addMetric(final String name,
                           @Nullable final Tags tags,
                           final Object metric,
                           final io.micrometer.core.instrument.Meter meter)
    {
        AppbaseUtil.notBlank(name, "name");

        final MetricID metricID;
        if (tags != null) {
            final Tag[] tagArray = MetricService.toMicroProfileTags(tags);
            metricID = new MetricID(name, tagArray);
        } else {
            metricID = new MetricID(name);
        }

        if (metric != null) {
            microProfileMetricMap.put(metricID, metric);
        }

        if (meter != null) {
            micrometerMeterMap.put(metricID, meter);
        }
    }

    public CompositeMeterRegistry getCompositeMeterRegistry() {
        return compositeMeterRegistry;
    }
}
