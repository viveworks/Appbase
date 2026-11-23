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

package com.viveworks.appbase.client.service.aerospike.metrics;

import com.aerospike.client.Host;
import com.aerospike.client.cluster.Cluster;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.metrics.LatencyBuckets;
import com.aerospike.client.metrics.LatencyType;
import com.aerospike.client.metrics.NodeMetrics;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link AppbaseAerospikeMetricsListener}.
 *
 * <p>Uses a real {@link SimpleMeterRegistry} (rather than a mock) so gauge values can be asserted
 * directly, and mocks of the driver's {@link Cluster}/{@link Node}/{@link NodeMetrics}/
 * {@link LatencyBuckets} — these are the exact objects {@code onSnapshot} is handed by the driver's
 * push loop and none of them require a live cluster connection to construct.</p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppbaseAerospikeMetricsListenerTest {
    private SimpleMeterRegistry registry;
    private AppbaseAerospikeMetricsListener listener;
    private Cluster cluster;
    private Node node;
    private NodeMetrics nodeMetrics;
    private LatencyBuckets latencyBuckets;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        listener = new AppbaseAerospikeMetricsListener(registry);

        cluster = Mockito.mock(Cluster.class);
        node = Mockito.mock(Node.class);
        nodeMetrics = Mockito.mock(NodeMetrics.class);
        latencyBuckets = Mockito.mock(LatencyBuckets.class);

        Mockito.when(cluster.getNodes()).thenReturn(new Node[] {node});
        Mockito.when(node.getName()).thenReturn("n1");
        Mockito.when(node.getHost()).thenReturn(new Host("h1", 3000));
        Mockito.when(node.getErrorCount()).thenReturn(7L);
        Mockito.when(node.getTimeoutCount()).thenReturn(3L);
        Mockito.when(node.getMetrics()).thenReturn(nodeMetrics);

        Mockito.when(nodeMetrics.getLatencyBuckets(Mockito.anyInt())).thenReturn(latencyBuckets);
        Mockito.when(latencyBuckets.getMax()).thenReturn(2);
        Mockito.when(latencyBuckets.getBucket(0)).thenReturn(5L);
        Mockito.when(latencyBuckets.getBucket(1)).thenReturn(9L);
    }

    @Test
    void testOnSnapshotRegistersNodeErrorAndTimeoutGauges() {
        // Act
        listener.onSnapshot(cluster);

        // Assert
        Assertions.assertEquals(7.0,
                registry.get("aerospike.driver.node.errors").tags("node", "n1").gauge().value());
        Assertions.assertEquals(3.0,
                registry.get("aerospike.driver.node.timeouts").tags("node", "n1").gauge().value());
    }

    @Test
    void testOnSnapshotRegistersLatencyBucketGauge() {
        // Act
        listener.onSnapshot(cluster);

        // Assert
        Assertions.assertEquals(5.0,
                registry.get("aerospike.driver.latency")
                        .tags("node", "n1", "type", LatencyType.READ.name(), "bucket", "0")
                        .gauge()
                        .value());
    }

    @Test
    void testOnSnapshotSkipsLatencyTypeNone() {
        // Act
        listener.onSnapshot(cluster);

        // Assert — LatencyType.NONE must never be bridged as a latency gauge dimension.
        Assertions.assertThrows(MeterNotFoundException.class, () ->
                registry.get("aerospike.driver.latency")
                        .tags("node", "n1", "type", LatencyType.NONE.name(), "bucket", "0")
                        .gauge());
    }

    @Test
    void testRepeatedSnapshotsUpdateSameGaugeWithoutDuplicating() {
        // Arrange
        listener.onSnapshot(cluster);
        final int meterCountAfterFirstSnapshot = registry.getMeters().size();

        Mockito.when(node.getErrorCount()).thenReturn(11L);

        // Act
        listener.onSnapshot(cluster);

        // Assert — same gauge instance now reads the updated value, no new meter was registered.
        Assertions.assertEquals(11.0,
                registry.get("aerospike.driver.node.errors").tags("node", "n1").gauge().value());
        Assertions.assertEquals(meterCountAfterFirstSnapshot, registry.getMeters().size());
    }

    @Test
    void testOnDisableAndOnNodeCloseDoNotClearRegistry() {
        // Arrange
        listener.onSnapshot(cluster);

        // Act
        listener.onDisable(cluster);
        listener.onNodeClose(node);

        // Assert — last-seen gauge values remain resolvable after disable/node-close.
        Assertions.assertEquals(7.0,
                registry.get("aerospike.driver.node.errors").tags("node", "n1").gauge().value());
    }
}
