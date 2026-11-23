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

package com.viveworks.appbase.core.service.metric;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.Unmodifiable;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Represents a metric service.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface MetricService extends AppbaseService {
    char METRIC_NAME_SEPARATOR = '.';


    /**
     * Get the {@link MetricRegistry} that this service manages. This registry must be used to instrument the code
     * using metric (e.g counter, timer, histogram and so forth). By default, this registry uses
     * <code>Micrometer</code> {@link io.micrometer.core.instrument.composite.CompositeMeterRegistry} as the
     * underlying registry. Additional meter registries can be added using
     * {@link #registerMicrometerRegistry(MeterRegistry)} for reporting metric.
     *
     * @return Metric registry
     */
    MetricRegistry getMetricRegistry();


    /**
     * Get the composite <code>Micrometer</code> {@link MeterRegistry} that this service manages. Instrumenting
     * code and driver-native latency recorders (e.g. Lettuce's command-latency recorder) bind to this registry
     * directly; metrics recorded on it fan out to every registered reporting registry, including JMX
     * (via {@link #registerMicrometerRegistry(MeterRegistry)}).
     *
     * @return composite meter registry
     */
    MeterRegistry getCompositeMeterRegistry();


    /**
     * Register executor metric.
     *
     * @param executorService Executor service
     * @param executorServiceName Executor service name for registration
     * @param tagsKeyValue Tag key value pairs
     */
    void registerExecutorServiceMetrics(
            ExecutorService executorService,
            String executorServiceName,
            String... tagsKeyValue);


    /**
     * Register <code>Micrometer</code> registry. This can be set during initialization and used for reporting metric.
     * For instrumenting code, {@link #getMetricRegistry()} must be used.
     *
     * @param meterRegistry Micrometer meter registry
     */
    void registerMicrometerRegistry(MeterRegistry meterRegistry);


    /**
     * Unregister <code>Micrometer</code> registry.
     *
     * @param meterRegistry Micrometer meter registry
     */
    void unregisterMicrometerRegistry(MeterRegistry meterRegistry);


    /**
     * Unregister <code>Micrometer</code> registry by type.
     *
     * @param meterRegistryType Micrometer meter registry type
     */
    void unregisterMicrometerRegistry(Class<? extends MeterRegistry> meterRegistryType);


    /**
     * Get <code>Micrometer</code> registries.
     *
     * @return Micrometer registries
     */
    @Unmodifiable
    Set<MeterRegistry> getMicrometerRegistries();


    /**
     * Clear (and close) <code>Micrometer</code> registries.
     */
    void clearMicrometerRegistries();


    /**
     * Return a JSON string with all the {@link org.eclipse.microprofile.metrics.Metric}s in
     * {@link MetricRegistry} of {@link #getMetricRegistry()}.
     *
     * @return JSON
     * @throws MetricServiceException if there were any errors during encoding
     */
    String serializeMetricRegistryAsJson() throws MetricServiceException;


    /**
     * Build the name of a metric.
     *
     * @param type Class defining the metric
     * @param suffix Suffix suggesting the metric usage
     * @param names Additional names to concatenate
     * @return Metric name
     */
    default String name(Class<?> type, String suffix, String... names) {
        AppbaseUtil.notNull(type, "type");
        AppbaseUtil.notBlank(suffix, "suffix");

        final StringBuilder nameBuilder = new StringBuilder(type.getName())
                .append(METRIC_NAME_SEPARATOR)
                .append(suffix);

        if (names != null) {
            for (final String name : names) {
                nameBuilder.append(METRIC_NAME_SEPARATOR).append(name);
            }
        }

        return nameBuilder.toString();
    }

    default String name(String suffix, String... names) {
        AppbaseUtil.notBlank(suffix, "suffix");

        final StringBuilder nameBuilder = new StringBuilder(suffix);
        if (names != null) {
            for (final String name : names) {
                nameBuilder.append(METRIC_NAME_SEPARATOR).append(name);
            }
        }

        return nameBuilder.toString();
    }

    /**
     * Convert <code>Micrometer</code> {@link Tags} to <code>Microprofile</code> {@link Tag} array.
     *
     * @param tags Micrometer tags
     * @return Microprofile tag array
     */
    static Tag[] toMicroProfileTags(final Tags tags) {
        if (tags != null) {
            final Iterator<io.micrometer.core.instrument.Tag> iterator = tags.iterator();
            final List<Tag> tagList = new ArrayList<>();

            while (iterator.hasNext()) {
                final io.micrometer.core.instrument.Tag tag = iterator.next();
                final String key = tag.getKey();
                final String value = tag.getValue();
                final Tag tag1 = new Tag(key, value);
                tagList.add(tag1);
            }

            final Tag[] tagArray = new Tag[tagList.size()];
            return tagList.toArray(tagArray);
        }

        return null;
    }
}
