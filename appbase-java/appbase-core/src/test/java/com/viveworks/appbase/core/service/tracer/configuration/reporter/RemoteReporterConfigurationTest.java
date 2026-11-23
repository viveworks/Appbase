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

package com.viveworks.appbase.core.service.tracer.configuration.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class RemoteReporterConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final RemoteReporterConfiguration remoteReporterConfiguration = RemoteReporterConfiguration
                .builder()
                .agentHost("agentHost")
                .agentPort(1234)
                .authPassword("password")
                .authToken("token")
                .authUsername("username")
                .closeEnqueueTimeoutMillis(1000)
                .collectorEndpoint("http://collectorEndpoint")
                .flushIntervalMillis(2000)
                .maxPacketSizeBytes(1024)
                .maxQueueSize(100)
                .metricsPrefix("metrixPrefix-")
                .build();

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(RemoteReporterConfiguration.class)
                .writeValueAsString(remoteReporterConfiguration);
        final RemoteReporterConfiguration remoteReporterConfiguration1 = yamlMapper
                .readerFor(RemoteReporterConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(
                remoteReporterConfiguration.getAgentHost(),
                remoteReporterConfiguration1.getAgentHost());
        Assertions.assertEquals(
                remoteReporterConfiguration.getAgentPort(),
                remoteReporterConfiguration1.getAgentPort());
        Assertions.assertEquals(
                remoteReporterConfiguration.getAuthPassword(),
                remoteReporterConfiguration1.getAuthPassword());
        Assertions.assertEquals(
                remoteReporterConfiguration.getAuthToken(),
                remoteReporterConfiguration1.getAuthToken());
        Assertions.assertEquals(
                remoteReporterConfiguration.getAuthUsername(),
                remoteReporterConfiguration1.getAuthUsername());
        Assertions.assertEquals(
                remoteReporterConfiguration.getCollectorEndpoint(),
                remoteReporterConfiguration1.getCollectorEndpoint());
        Assertions.assertEquals(
                remoteReporterConfiguration.getCloseEnqueueTimeoutMillis(),
                remoteReporterConfiguration1.getCloseEnqueueTimeoutMillis());
        Assertions.assertEquals(
                remoteReporterConfiguration.getFlushIntervalMillis(),
                remoteReporterConfiguration1.getFlushIntervalMillis());
        Assertions.assertEquals(
                remoteReporterConfiguration.getMaxPacketSizeBytes(),
                remoteReporterConfiguration1.getMaxPacketSizeBytes());
        Assertions.assertEquals(
                remoteReporterConfiguration.getMaxQueueSize(),
                remoteReporterConfiguration1.getMaxQueueSize());
        Assertions.assertEquals(
                remoteReporterConfiguration.getMetricsPrefix(),
                remoteReporterConfiguration1.getMetricsPrefix());
        Assertions.assertEquals(
                remoteReporterConfiguration,
                remoteReporterConfiguration1);
        Assertions.assertEquals(
                remoteReporterConfiguration.hashCode(),
                remoteReporterConfiguration1.hashCode());
        Assertions.assertEquals(
                remoteReporterConfiguration.toString(),
                remoteReporterConfiguration1.toString());
    }
}
