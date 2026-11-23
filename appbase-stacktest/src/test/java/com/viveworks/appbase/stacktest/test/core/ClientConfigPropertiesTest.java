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

package com.viveworks.appbase.stacktest.test.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.service.aerospike.configuration.AuthMode;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientServiceConfiguration;
import com.viveworks.appbase.client.service.aerospike.configuration.ReadModeAP;
import com.viveworks.appbase.client.service.aerospike.configuration.ReadModeSC;
import com.viveworks.appbase.client.service.aerospike.configuration.ReplicaPolicy;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.DefaultElasticsearchLowLevelRestClientServiceConfiguration;
import com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel.configuration.NodeSelectorType;
import com.viveworks.appbase.client.service.kafka.configuration.GroupProtocol;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientServiceConfiguration;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.redis.configuration.DefaultRedisClientServiceConfiguration;
import com.viveworks.appbase.client.service.redis.configuration.DisconnectedBehavior;
import com.viveworks.appbase.client.service.redis.configuration.ProtocolVersion;
import com.viveworks.appbase.client.service.zookeeper.configuration.ConnectionHandlingPolicyType;
import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stack-test level deserialization tests for new config properties across all client modules.
 *
 * <p>Verifies that new YAML properties added in P-014 round-trip correctly end-to-end.
 * Each test reads a YAML resource file, deserializes the outer service configuration wrapper,
 * navigates to the inner integration-service configuration, and asserts the new property values.
 * No live infrastructure is required.</p>
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ClientConfigPropertiesTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    private ObjectMapper yamlMapper() {
        return YamlUtil.getYamlMapper();
    }

    /**
     * Verifies new Redis properties (DisconnectedBehavior, keepAlive*, ProtocolVersion)
     * deserialize correctly from {@code appbase-service-redis.yaml}.
     */
    @Test
    void redisNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-redis.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-redis.yaml");

            // Act
            final DefaultRedisClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultRedisClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultRedisClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultRedisClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertEquals(DisconnectedBehavior.DEFAULT, inner.getDisconnectedBehavior());
            assertEquals(Integer.valueOf(9), inner.getKeepAliveCount());
            assertEquals(Long.valueOf(7_200_000L), inner.getKeepAliveIdle());
            assertEquals(Long.valueOf(75_000L), inner.getKeepAliveInterval());
            assertEquals(ProtocolVersion.RESP2, inner.getProtocolVersion());
        }
    }

    /**
     * Verifies the stack-test Redis configuration supplies the pool infrastructure required at startup.
     */
    @Test
    void redisStackTestConfig_deserializesRequiredPoolInfrastructure() throws Exception {
        try (final InputStream is = getClass().getResourceAsStream("/appbase-stacktest-redis.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-stacktest-redis.yaml");

            final DefaultRedisClientServiceConfiguration configuration = yamlMapper()
                    .readerFor(DefaultRedisClientServiceConfiguration.class)
                    .readValue(is);

            assertNotNull(configuration.getCustomAppbaseClientPoolManagerConfiguration());
            assertNotNull(configuration.getCustomAppbaseClientPoolConfiguration());
        }
    }

    /**
     * Verifies new Kafka producer properties (SecurityProtocol, socketConnectionSetupTimeoutMs,
     * partitionerIgnoreKeys) deserialize correctly from {@code appbase-service-kafka-producer.yaml}.
     */
    @Test
    void kafkaProducerNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-kafka-producer.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-kafka-producer.yaml");

            // Act
            final DefaultKafkaProducerClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultKafkaProducerClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultKafkaProducerClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertEquals(SecurityProtocol.PLAINTEXT, inner.getSecurityProtocol());
            assertEquals(Long.valueOf(10_000L), inner.getSocketConnectionSetupTimeoutMs());
            assertEquals(Boolean.FALSE, inner.getPartitionerIgnoreKeys());
        }
    }

    /**
     * Verifies new Kafka consumer properties (SecurityProtocol, GroupProtocol, retryBackoffMaxMs)
     * deserialize correctly from {@code appbase-service-kafka-consumer.yaml}.
     */
    @Test
    void kafkaConsumerNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-kafka-consumer.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-kafka-consumer.yaml");

            // Act
            final DefaultKafkaConsumerClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultKafkaConsumerClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultKafkaConsumerClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultKafkaConsumerClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertEquals(SecurityProtocol.PLAINTEXT, inner.getSecurityProtocol());
            assertEquals(GroupProtocol.CONSUMER, inner.getGroupProtocol());
            assertEquals(Long.valueOf(1_000L), inner.getRetryBackoffMaxMs());
        }
    }

    /**
     * Verifies new Aerospike properties (timeout, loginTimeout, maxConnsPerNode, failIfNotConnected,
     * authMode, readModeSC, readModeAP, replicaPolicy) deserialize correctly from
     * {@code appbase-service-aerospike.yaml}.
     */
    @Test
    void aerospikeNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-aerospike.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-aerospike.yaml");

            // Act
            final DefaultAerospikeClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultAerospikeClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultAerospikeClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultAerospikeClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertEquals(Integer.valueOf(1000), inner.getTimeout());
            assertEquals(Integer.valueOf(5000), inner.getLoginTimeout());
            assertEquals(Integer.valueOf(300), inner.getMaxConnsPerNode());
            assertEquals(Boolean.TRUE, inner.getFailIfNotConnected());
            assertEquals(AuthMode.INTERNAL, inner.getAuthMode());
            assertEquals(ReadModeSC.SESSION, inner.getReadModeSC());
            assertEquals(ReadModeAP.ONE, inner.getReadModeAP());
            assertEquals(ReplicaPolicy.SEQUENCE, inner.getReplicaPolicy());
        }
    }

    /**
     * Verifies new Cassandra properties (username, sslEnabled, requestTimeout)
     * deserialize correctly from {@code appbase-service-cassandra.yaml}.
     */
    @Test
    void cassandraNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-cassandra.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-cassandra.yaml");

            // Act
            final DefaultCassandraClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultCassandraClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultCassandraClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultCassandraClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertEquals("cassandra", inner.getUsername());
            assertEquals(Boolean.TRUE, inner.isSslEnabled());
            assertEquals(Duration.ofSeconds(30), inner.getRequestTimeout());
        }
    }

    /**
     * Verifies new ZooKeeper properties (authInfoList, connectionHandlingPolicy)
     * deserialize correctly from {@code appbase-service-zookeeper.yaml}.
     */
    @Test
    void zookeeperNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-zookeeper.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-zookeeper.yaml");

            // Act
            final DefaultZooKeeperClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultZooKeeperClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultZooKeeperClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultZooKeeperClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertNotNull(inner.getAuthInfoList());
            assertEquals(1, inner.getAuthInfoList().size());
            assertEquals("digest", inner.getAuthInfoList().get(0).getScheme());
            assertEquals("appbase:stacktest", inner.getAuthInfoList().get(0).getAuth());
            assertEquals(ConnectionHandlingPolicyType.STANDARD, inner.getConnectionHandlingPolicy());
        }
    }

    /**
     * Verifies new Elasticsearch properties (nodeSelector, compressionEnabled)
     * deserialize correctly from {@code appbase-service-elasticsearch.yaml}.
     */
    @Test
    void elasticsearchNewProperties_deserializeFromYaml() throws Exception {
        // Arrange
        try (final InputStream is = getClass().getResourceAsStream("/appbase-service-elasticsearch.yaml")) {
            assertNotNull(is, "YAML resource not found: /appbase-service-elasticsearch.yaml");

            // Act
            final DefaultElasticsearchLowLevelRestClientServiceConfiguration outer = yamlMapper()
                    .readerFor(DefaultElasticsearchLowLevelRestClientServiceConfiguration.class)
                    .readValue(is);

            // Assert
            final DefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration inner =
                    outer.getDefaultDefaultElasticsearchLowLevelRestClientIntegrationServiceConfiguration();
            assertNotNull(inner);
            assertEquals(NodeSelectorType.ANY, inner.getNodeSelector());
            assertTrue(inner.getCompressionEnabled());
        }
    }
}
