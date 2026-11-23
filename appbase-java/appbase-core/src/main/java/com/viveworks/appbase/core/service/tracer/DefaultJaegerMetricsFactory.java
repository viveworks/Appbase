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

package com.viveworks.appbase.core.service.tracer;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Tag;
import org.slf4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link MetricsFactory}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@ThreadSafe
public class DefaultJaegerMetricsFactory implements MetricsFactory {
    private final Logger logger;
    private final MetricService metricService;

    public DefaultJaegerMetricsFactory(final LogService logService, final MetricService metricService) {
        AppbaseUtil.notNull(logService, "logService");
        AppbaseUtil.notNull(metricService, "metricService");

        this.logger = logService.getLogger(getClass());
        this.metricService = metricService;
    }

    @Override
    public Counter createCounter(final String name, final Map<String, String> tags) {
        AppbaseUtil.notBlank(name, "name");

        final Tag[] tagArray = translateTags(tags);
        final org.eclipse.microprofile.metrics.Counter counter;
        if (tagArray != null) {
            counter = metricService
                    .getMetricRegistry()
                    .counter(name, tagArray);
        } else {
            counter = metricService
                    .getMetricRegistry()
                    .counter(name);
        }

        logger.debug("Creating counter: {}, {}", name, tags);
        return counter::inc;
    }

    @Override
    public Timer createTimer(final String name, final Map<String, String> tags) {
        AppbaseUtil.notBlank(name, "name");

        final Tag[] tagArray = translateTags(tags);
        final org.eclipse.microprofile.metrics.Timer timer;
        if (tagArray != null) {
            timer = metricService
                    .getMetricRegistry()
                    .timer(name, tagArray);
        } else {
            timer = metricService
                    .getMetricRegistry()
                    .timer(name);
        }

        logger.debug("Creating timer: {}, {}", name, tags);
        return time -> timer.update(Duration.of(time, ChronoUnit.MICROS));
    }

    @Override
    public Gauge createGauge(final String name, final Map<String, String> tags) {
        AppbaseUtil.notBlank(name, "name");

        final AtomicLong atomicValue = new AtomicLong(0);
        final org.eclipse.microprofile.metrics.Gauge<Long> gauge = atomicValue::longValue;
        final Tag[] tagArray = translateTags(tags);
        if (tagArray != null) {
            metricService
                    .getMetricRegistry()
                    .register(name, gauge);
        } else {
            final MetadataBuilder metadataBuilder = new MetadataBuilder();
            final Metadata metadata = metadataBuilder
                    .withName(name)
                    .build();
            metricService
                    .getMetricRegistry()
                    .register(metadata, gauge, tagArray);
        }

        return atomicValue::addAndGet;
    }

    @SuppressFBWarnings("BL_BURYING_LOGIC")
    private Tag[] translateTags(final Map<String, String> tags) {
        if (tags != null) {
            final List<Tag> tagList = new ArrayList<>(tags.size());
            for (final Map.Entry<String, String> tag : tags.entrySet()) {
                final String key = tag.getKey();
                final String value = tag.getValue();
                if (key != null && value != null) {
                    tagList.add(new Tag(key, value));
                }
            }

            final Tag[] tagArray = new Tag[tagList.size()];
            return tagList.toArray(tagArray);
        }

        return null;
    }
}
