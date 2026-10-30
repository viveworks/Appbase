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

package com.viveworks.appbase.client.service.redis.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultRedisClientIntegrationServiceConfiguration} builder defaults,
 * field accessors, and YAML round-trips. No Redis connection required.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultRedisClientIntegrationServiceConfigurationTest {

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void defaults_areCorrect() {
        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder().build();

        assertNotNull(cfg.getHostPortConfiguration());
        assertEquals("localhost", cfg.getHostPortConfiguration().getHost());
        assertEquals(6379, cfg.getHostPortConfiguration().getPort());
        assertNull(cfg.getDatabase());
        assertNull(cfg.getUsername());
        assertNull(cfg.getPassword());
        assertNull(cfg.getClientName());
        assertEquals(5_000L, cfg.getConnectTimeoutMs());
        assertEquals(60_000L, cfg.getCommandTimeoutMs());
        assertFalse(cfg.isSsl());
        assertTrue(cfg.isSslVerifyPeer());
        assertFalse(cfg.isStartTls());
        assertTrue(cfg.isAutoReconnect());
        assertFalse(cfg.isPingBeforeActivate());
        assertEquals(Integer.MAX_VALUE, cfg.getRequestQueueSize());
        assertTrue(cfg.isTcpNoDelay());
        assertFalse(cfg.isTcpKeepAlive());
        // New fields: all null by default
        assertNull(cfg.getDisconnectedBehavior());
        assertNull(cfg.getSslProvider());
        assertNull(cfg.getSslKeystore());
        assertNull(cfg.getSslKeystorePassword());
        assertNull(cfg.getSslTruststore());
        assertNull(cfg.getSslTruststorePassword());
        assertNull(cfg.getKeepAliveCount());
        assertNull(cfg.getKeepAliveIdle());
        assertNull(cfg.getKeepAliveInterval());
        assertNull(cfg.getProtocolVersion());
        assertTrue(cfg.isCommandLatencyMetricsEnabled());
    }

    @Test
    void builder_setsAllFieldsCorrectly() {
        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .hostPortConfiguration(
                                HostPortConfiguration.builder().host("redis.example.com").port(6380).build())
                        .database(2)
                        .username("alice")
                        .password("secret")
                        .clientName("appbase-test")
                        .connectTimeoutMs(3_000L)
                        .commandTimeoutMs(10_000L)
                        .ssl(true)
                        .sslVerifyPeer(false)
                        .startTls(true)
                        .autoReconnect(false)
                        .pingBeforeActivate(true)
                        .requestQueueSize(500)
                        .tcpNoDelay(false)
                        .tcpKeepAlive(true)
                        .build();

        assertEquals("redis.example.com", cfg.getHostPortConfiguration().getHost());
        assertEquals(6380, cfg.getHostPortConfiguration().getPort());
        assertEquals(Integer.valueOf(2), cfg.getDatabase());
        assertEquals("alice", cfg.getUsername());
        assertEquals("secret", cfg.getPassword());
        assertEquals("appbase-test", cfg.getClientName());
        assertEquals(3_000L, cfg.getConnectTimeoutMs());
        assertEquals(10_000L, cfg.getCommandTimeoutMs());
        assertTrue(cfg.isSsl());
        assertFalse(cfg.isSslVerifyPeer());
        assertTrue(cfg.isStartTls());
        assertFalse(cfg.isAutoReconnect());
        assertTrue(cfg.isPingBeforeActivate());
        assertEquals(500, cfg.getRequestQueueSize());
        assertFalse(cfg.isTcpNoDelay());
        assertTrue(cfg.isTcpKeepAlive());
    }

    @Test
    void equals_andHashCode_workCorrectly() {
        final DefaultRedisClientIntegrationServiceConfiguration a =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .hostPortConfiguration(
                                HostPortConfiguration.builder().host("h").port(1234).build())
                        .build();
        final DefaultRedisClientIntegrationServiceConfiguration b =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .hostPortConfiguration(
                                HostPortConfiguration.builder().host("h").port(1234).build())
                        .build();
        final DefaultRedisClientIntegrationServiceConfiguration c =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .hostPortConfiguration(
                                HostPortConfiguration.builder().host("h").port(9999).build())
                        .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }

    @Test
    void toString_containsKeyFields() {
        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .hostPortConfiguration(
                                HostPortConfiguration.builder().host("myredis").port(6380).build())
                        .ssl(true)
                        .build();
        final String s = cfg.toString();
        assertTrue(s.contains("myredis"));
        assertTrue(s.contains("6380"));
        assertTrue(s.contains("ssl=true"));
    }

    // ── Tests for new fields ──────────────────────────────────────────────

    @Test
    void disconnectedBehavior_isSetCorrectly() {
        // Arrange + Act
        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .disconnectedBehavior(DisconnectedBehavior.ACCEPT_COMMANDS)
                        .build();

        // Assert
        assertEquals(DisconnectedBehavior.ACCEPT_COMMANDS, cfg.getDisconnectedBehavior());
        assertEquals("ACCEPT_COMMANDS", cfg.getDisconnectedBehavior().getText());
    }

    @Test
    void sslKeystore_andPassword_areSetCorrectly() {
        // Arrange + Act
        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .sslKeystore("/etc/ssl/redis.jks")
                        .sslKeystorePassword("secret")
                        .build();

        // Assert — getters return configured values
        assertEquals("/etc/ssl/redis.jks", cfg.getSslKeystore());
        assertEquals("secret", cfg.getSslKeystorePassword());

        // Assert — toString masks the password
        final String s = cfg.toString();
        assertTrue(s.contains("/etc/ssl/redis.jks"));
        assertTrue(s.contains("[REDACTED]"));
        assertFalse(s.contains("secret"));
    }

    @Test
    void keepAlive_andProtocol_areSetCorrectly() {
        // Arrange + Act
        final DefaultRedisClientIntegrationServiceConfiguration cfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .keepAliveCount(5)
                        .keepAliveIdle(300_000L)
                        .protocolVersion(ProtocolVersion.RESP3)
                        .build();

        // Assert
        assertEquals(Integer.valueOf(5), cfg.getKeepAliveCount());
        assertEquals(Long.valueOf(300_000L), cfg.getKeepAliveIdle());
        assertEquals(ProtocolVersion.RESP3, cfg.getProtocolVersion());
        assertEquals("RESP3", cfg.getProtocolVersion().getText());
    }

    @Test
    void yamlRoundTrip_withNewEnumFields() throws Exception {
        // Arrange
        final DefaultRedisClientIntegrationServiceConfiguration original =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
                        .sslProvider(SslProvider.JDK)
                        .protocolVersion(ProtocolVersion.RESP2)
                        .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();

        // Act
        final String yaml = yamlMapper
                .writerFor(DefaultRedisClientIntegrationServiceConfiguration.class)
                .writeValueAsString(original);
        final DefaultRedisClientIntegrationServiceConfiguration deserialized = yamlMapper
                .readerFor(DefaultRedisClientIntegrationServiceConfiguration.class)
                .readValue(yaml);

        // Assert
        assertEquals(DisconnectedBehavior.REJECT_COMMANDS, deserialized.getDisconnectedBehavior());
        assertEquals(SslProvider.JDK, deserialized.getSslProvider());
        assertEquals(ProtocolVersion.RESP2, deserialized.getProtocolVersion());
    }

    @Test
    void commandLatencyMetricsEnabled_defaultsTrue_andIsOverridable() {
        // Arrange + Act
        final DefaultRedisClientIntegrationServiceConfiguration defaultCfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder().build();
        final DefaultRedisClientIntegrationServiceConfiguration disabledCfg =
                DefaultRedisClientIntegrationServiceConfiguration.builder()
                        .commandLatencyMetricsEnabled(false)
                        .build();

        // Assert
        assertTrue(defaultCfg.isCommandLatencyMetricsEnabled());
        assertFalse(disabledCfg.isCommandLatencyMetricsEnabled());
        assertTrue(disabledCfg.toString().contains("commandLatencyMetricsEnabled=false"));
    }
}
