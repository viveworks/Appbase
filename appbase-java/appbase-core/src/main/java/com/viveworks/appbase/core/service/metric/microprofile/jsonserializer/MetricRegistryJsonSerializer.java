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

package com.viveworks.appbase.core.service.metric.microprofile.jsonserializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Timer;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * JSON serialization object (no de-serialization) for {@link MetricRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public final class MetricRegistryJsonSerializer {
    @JsonIgnore
    private final MetricRegistry metricRegistry;

    public MetricRegistryJsonSerializer(final MetricRegistry metricRegistry) {
        AppbaseUtil.notNull(metricRegistry, "metricRegistry");

        this.metricRegistry = metricRegistry;
    }

    private String getUniqueName(final MetricID metricID) {
        final String name = metricID.getName();

        final String tagsAsString = metricID.getTagsAsString();
        if (tagsAsString != null && !tagsAsString.isBlank()) {
            return name + "," + tagsAsString;
        }

        return name;
    }

    @JsonSerialize(keyAs = String.class, contentAs = GaugeJsonSerializer.class)
    public Map<String, GaugeJsonSerializer> getGauges() {
        final Map<String, GaugeJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, Gauge> gauges = metricRegistry.getGauges();
        if (gauges != null) {
            gauges.forEach(((metricID, gauge) -> {
                if (metricID != null && gauge != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final GaugeJsonSerializer gaugeJsonEncoder = new GaugeJsonSerializer(gauge);
                        result.put(uniqueName, gaugeJsonEncoder);
                    }
                }
            }));
        }

        return result;
    }

    @JsonSerialize(keyAs = String.class, contentAs = CounterJsonSerializer.class)
    public Map<String, CounterJsonSerializer> getCounters() {
        final Map<String, CounterJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, Counter> counters = metricRegistry.getCounters();
        if (counters != null) {
            counters.forEach(((metricID, counter) -> {
                if (metricID != null && counter != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final CounterJsonSerializer counterJsonEncoder = new CounterJsonSerializer(counter);
                        result.put(uniqueName, counterJsonEncoder);
                    }
                }
            }));
        }

        return result;
    }

    @JsonSerialize(keyAs = String.class, contentAs = ConcurrentGaugeJsonSerializer.class)
    public Map<String, ConcurrentGaugeJsonSerializer> getConcurrentGauges() {
        final Map<String, ConcurrentGaugeJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, ConcurrentGauge> concurrentGauges = metricRegistry.getConcurrentGauges();
        if (concurrentGauges != null) {
            concurrentGauges.forEach(((metricID, concurrentGauge) -> {
                if (metricID != null && concurrentGauge != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final ConcurrentGaugeJsonSerializer concurrentGaugeJsonEncoder =
                                new ConcurrentGaugeJsonSerializer(concurrentGauge);
                        result.put(uniqueName, concurrentGaugeJsonEncoder);
                    }
                }
            }));
        }

        return result;
    }

    @JsonSerialize(keyAs = String.class, contentAs = HistogramJsonSerializer.class)
    public Map<String, HistogramJsonSerializer> getHistograms() {
        final Map<String, HistogramJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, Histogram> histograms = metricRegistry.getHistograms();
        if (histograms != null) {
            histograms.forEach(((metricID, histogram) -> {
                if (metricID != null && histogram != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final HistogramJsonSerializer histogramJsonEncoder = new HistogramJsonSerializer(histogram);
                        result.put(uniqueName, histogramJsonEncoder);
                    }
                }
            }));
        }

        return result;
    }

    @JsonSerialize(keyAs = String.class, contentAs = MeterJsonSerializer.class)
    public Map<String, MeterJsonSerializer> getMeters() {
        final Map<String, MeterJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, Meter> meters = metricRegistry.getMeters();
        if (meters != null) {
            meters.forEach(((metricID, meter) -> {
                if (metricID != null && meter != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final MeterJsonSerializer meterJsonEncoder = new MeterJsonSerializer(meter);
                        result.put(uniqueName, meterJsonEncoder);
                    }
                }
            }));
        }

        return result;
    }

    @JsonSerialize(keyAs = String.class, contentAs = TimerJsonSerializer.class)
    public Map<String, TimerJsonSerializer> getTimers() {
        final Map<String, TimerJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, Timer> timers = metricRegistry.getTimers();
        if (timers != null) {
            timers.forEach((metricID, timer) -> {
                if (metricID != null && timer != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final TimerJsonSerializer timerJsonEncoder = new TimerJsonSerializer(timer);
                        result.put(uniqueName, timerJsonEncoder);
                    }
                }
            });
        }

        return result;
    }

    @JsonSerialize(keyAs = String.class, contentAs = SimpleTimerJsonSerializer.class)
    public Map<String, SimpleTimerJsonSerializer> getSimpleTimers() {
        final Map<String, SimpleTimerJsonSerializer> result = new TreeMap<>();

        final SortedMap<MetricID, SimpleTimer> simpleTimers = metricRegistry.getSimpleTimers();
        if (simpleTimers != null) {
            simpleTimers.forEach(((metricID, simpleTimer) -> {
                if (metricID != null && simpleTimer != null) {
                    final String uniqueName = getUniqueName(metricID);
                    if (uniqueName != null) {
                        final SimpleTimerJsonSerializer simpleTimerJsonEncoder =
                                new SimpleTimerJsonSerializer(simpleTimer);
                        result.put(uniqueName, simpleTimerJsonEncoder);
                    }
                }
            }));
        }

        return result;
    }

    @Override
    public String toString() {
        return "MetricRegistryJsonSerializer{" +
                "concurrentGauges=" + getConcurrentGauges() +
                "counters=" + getCounters() +
                "gauges=" + getGauges() +
                "histograms=" + getHistograms() +
                "meters=" + getMeters() +
                "simpleTimers=" + getSimpleTimers() +
                "timers=" + getTimers() +
                '}';
    }
}
