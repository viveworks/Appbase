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

package com.viveworks.appbase.core.service.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration;
import com.viveworks.appbase.core.service.log.appender.disruptor.DisruptorMediatorEventDelegatingAppenderConfiguration;
import com.viveworks.appbase.core.service.log.appender.lucene.LuceneAppenderConfiguration;
import com.viveworks.appbase.core.service.log.filter.burst.BurstTurboLogEventFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.duplicate.DuplicateLogTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.service.log.filter.event.DisruptorLogAndTurboLogEventFilterDelegatorConfiguration;
import com.viveworks.appbase.core.service.log.filter.metrics.MetricsTurboFilterHandlerConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultLogServiceConfigurationTest {
    @Test
    void testYamlSerde() throws Exception {
        final DefaultLogServiceConfiguration defaultLogServiceConfiguration
                = DefaultLogServiceConfiguration
                .builder()
                .burstLogTurboFilterHandlerConfiguration(
                        BurstTurboLogEventFilterHandlerConfiguration.builder().build())
                .defaultLogAccessorConfiguration(DefaultLogAccessorConfiguration.builder().build())
                .disruptorLogAndTurboLogEventFilterDelegatorConfiguration(
                        DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.builder().build())
                .disruptorMediatorEventDelegatingAppenderConfiguration(
                        DisruptorMediatorEventDelegatingAppenderConfiguration.builder().build())
                .duplicateLogTurboFilterHandlerConfiguration(
                        DuplicateLogTurboFilterHandlerConfiguration.builder().build())
                .logbackXml(null)
                .luceneAppenderConfiguration(LuceneAppenderConfiguration.builder().build())
                .metricsTurboFilterHandlerConfiguration(MetricsTurboFilterHandlerConfiguration.builder().build())
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultLogServiceConfiguration.class)
                .writeValueAsString(defaultLogServiceConfiguration);

        final DefaultLogServiceConfiguration defaultLogServiceConfiguration1 = yamlMapper
                .readerFor(DefaultLogServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                defaultLogServiceConfiguration.getDefaultLogAccessorConfiguration(),
                defaultLogServiceConfiguration1.getDefaultLogAccessorConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration(),
                defaultLogServiceConfiguration1.getDisruptorLogAndTurboLogEventFilterDelegatorConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getDisruptorMediatorEventDelegatingAppenderConfiguration(),
                defaultLogServiceConfiguration1.getDisruptorMediatorEventDelegatingAppenderConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getLuceneAppenderConfiguration(),
                defaultLogServiceConfiguration1.getLuceneAppenderConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getLuceneLogAndTurboLogEventFilterListenerConfiguration(),
                defaultLogServiceConfiguration1.getLuceneLogAndTurboLogEventFilterListenerConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getLuceneLogIndexConfiguration(),
                defaultLogServiceConfiguration1.getLuceneLogIndexConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getBurstTurboLogEventFilterHandlerConfiguration(),
                defaultLogServiceConfiguration1.getBurstTurboLogEventFilterHandlerConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getDuplicateLogTurboFilterHandlerConfiguration(),
                defaultLogServiceConfiguration1.getDuplicateLogTurboFilterHandlerConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getMetricsTurboFilterHandlerConfiguration(),
                defaultLogServiceConfiguration1.getMetricsTurboFilterHandlerConfiguration());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.getLogbackXml(),
                defaultLogServiceConfiguration1.getLogbackXml());
        Assertions.assertEquals(
                defaultLogServiceConfiguration,
                defaultLogServiceConfiguration1);
        Assertions.assertEquals(
                defaultLogServiceConfiguration.hashCode(),
                defaultLogServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                defaultLogServiceConfiguration.toString(),
                defaultLogServiceConfiguration1.toString());
    }

    @Test
    void luceneLogIndexIsDisabledByDefault() {
        // R3 (internal work item 1073, spec §10): a zero-config log service defaults its Lucene INDEX off (asserted here);
        // the appender and store default off too — covered by LuceneAppenderConfigurationTest and
        // LuceneLogStoreConfigurationTest. Together: no hot-path indexing by default; opt in explicitly.
        Assertions.assertFalse(
                DefaultLogServiceConfiguration.builder().build()
                        .getLuceneLogIndexConfiguration().isEnabled(),
                "the log-service config must default its Lucene index to DISABLED (R3 §10)");
    }
}
