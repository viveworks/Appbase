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

package com.viveworks.appbase.core.util.disruptor.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.dsl.ProducerType;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BusySpinWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.LiteTimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.PhasedBackoffWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.TimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultDisruptorServiceConfigurationTest {
    @Test
    void testSerdeWithBlockingWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(BlockingWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }

    @Test
    void testSerdeWithBusySpinWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(BusySpinWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }

    @Test
    void testSerdeWithLiteTimeoutBlockingWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(LiteTimeoutBlockingWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }

    @Test
    void testSerdeWithPhasedBackoffWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(PhasedBackoffWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }

    @Test
    void testSerdeWithSleepingWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(SleepingWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }

    @Test
    void testSerdeWithTimeoutBlockingWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(TimeoutBlockingWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }

    @Test
    void testSerdeWithYieldingWaitStrategyConfiguration() throws Exception {
        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration =
                DefaultDisruptorServiceConfiguration
                        .builder()
                        .eventHandlerCount(1)
                        .producerType(ProducerType.SINGLE)
                        .ringBufferSize(64)
                        .threadFactoryConfiguration(ThreadFactoryConfiguration.builder().build())
                        .waitStrategyConfiguration(YieldingWaitStrategyConfiguration.builder().build())
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultDisruptorServiceConfiguration.class)
                .writeValueAsString(disruptorServiceConfiguration);

        final DefaultDisruptorServiceConfiguration disruptorServiceConfiguration1 = yamlMapper
                .readerFor(DefaultDisruptorServiceConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                disruptorServiceConfiguration.getEventHandlerCount(),
                disruptorServiceConfiguration1.getEventHandlerCount());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getProducerType(),
                disruptorServiceConfiguration1.getProducerType());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getRingBufferSize(),
                disruptorServiceConfiguration1.getRingBufferSize());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getThreadFactoryConfiguration(),
                disruptorServiceConfiguration1.getThreadFactoryConfiguration());
        Assertions.assertEquals(
                disruptorServiceConfiguration.getWaitStrategyConfiguration(),
                disruptorServiceConfiguration1.getWaitStrategyConfiguration());

        Assertions.assertEquals(
                disruptorServiceConfiguration,
                disruptorServiceConfiguration1);
        Assertions.assertEquals(
                disruptorServiceConfiguration.hashCode(),
                disruptorServiceConfiguration1.hashCode());
        Assertions.assertEquals(
                disruptorServiceConfiguration.toString(),
                disruptorServiceConfiguration1.toString());
    }
}
