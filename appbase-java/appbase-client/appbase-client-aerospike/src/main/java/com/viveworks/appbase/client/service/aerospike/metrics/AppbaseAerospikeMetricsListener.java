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

import com.aerospike.client.cluster.Cluster;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.metrics.LatencyBuckets;
import com.aerospike.client.metrics.LatencyType;
import com.aerospike.client.metrics.MetricsListener;
import com.aerospike.client.metrics.MetricsPolicy;
import com.aerospike.client.metrics.NodeMetrics;
import com.viveworks.appbase.core.util.AppbaseUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Push-bridge {@link MetricsListener} that routes Aerospike driver-native metrics onto Appbase's
 * shared Micrometer composite {@link MeterRegistry} (owned by {@code MetricService}, auto-exported
 * to JMX). Wired via {@link MetricsPolicy#listener}: setting {@code listener} directly on the
 * driver's {@link MetricsPolicy} <b>replaces the default file-writer sink</b> the driver would
 * otherwise install, so once this listener is registered, {@link #onSnapshot(Cluster)} is the
 * <i>only</i> place driver metrics are surfaced.
 *
 * <p>On each snapshot, a Micrometer {@link Gauge} is registered once per (metric name, tag set) and
 * then only its backing value is updated on subsequent snapshots — this avoids leaking duplicate
 * meters into the registry every {@link MetricsPolicy#interval}. Per-node error/timeout counts are
 * bridged directly; per-latency-type histograms are bridged bucket-by-bucket, where the buckets
 * are exponential (bucket {@code i} spans {@code [2^(i * latencyShift), 2^((i+1) * latencyShift))}
 * milliseconds per {@link MetricsPolicy#latencyShift}). {@link LatencyType#NONE} is not a real
 * latency type (it exists only as an internal driver marker) and is skipped.</p>
 *
 * <p>The composite registry is owned by {@code MetricService} and is NEVER shut down or cleared by
 * this class — {@link #onDisable(Cluster)} and {@link #onNodeClose(Node)} are no-ops so that
 * last-seen gauge values remain readable after metrics are disabled or a node leaves the cluster.
 * {@code IAerospikeClient#disableMetrics()} stops further snapshots from reaching this listener; it
 * does not remove already-registered gauges.</p>
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AppbaseAerospikeMetricsListener implements MetricsListener {
    private final MeterRegistry registry;

    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    /**
     * Creates the listener bridging Aerospike driver metrics onto {@code registry}.
     *
     * @param registry the Micrometer registry gauges are registered on; typically Appbase's shared
     *         composite registry
     */
    public AppbaseAerospikeMetricsListener(final MeterRegistry registry) {
        this.registry = AppbaseUtil.notNull(registry, "registry");
    }

    @Override
    public void onEnable(final Cluster cluster, final MetricsPolicy policy) {
        // No-op: gauges are registered lazily as snapshots arrive, not at enable time.
    }

    @Override
    public void onSnapshot(final Cluster cluster) {
        for (final Node node : cluster.getNodes()) {
            final String nodeName = node.getName();
            final String host = node.getHost().name;
            gauge("aerospike.driver.node.errors", node.getErrorCount(), Tags.of("node", nodeName, "host", host));
            gauge("aerospike.driver.node.timeouts", node.getTimeoutCount(), Tags.of("node", nodeName, "host", host));

            final NodeMetrics metrics = node.getMetrics();
            if (metrics == null) {
                continue;
            }

            for (final LatencyType type : LatencyType.values()) {
                // NONE is an internal driver marker, not a real latency bucket dimension.
                if (type == LatencyType.NONE) {
                    continue;
                }

                final LatencyBuckets buckets = metrics.getLatencyBuckets(type.ordinal());
                if (buckets == null) {
                    continue;
                }

                for (int i = 0; i < buckets.getMax(); i++) {
                    gauge("aerospike.driver.latency", buckets.getBucket(i),
                            Tags.of("node", nodeName, "type", type.name(), "bucket", Integer.toString(i)));
                }
            }
        }
    }

    @Override
    public void onNodeClose(final Node node) {
        // No-op: leave last-seen gauges for this node readable rather than clearing them.
    }

    @Override
    public void onDisable(final Cluster cluster) {
        // No-op: the composite registry is owned by MetricService and must NEVER be shut down or
        // cleared here. disableMetrics() on the client stops further onSnapshot() calls; it does not
        // imply the previously published gauges should disappear.
    }

    /**
     * Registers a gauge for {@code name}/{@code tags} once, bound to a stable {@link AtomicLong}
     * holder, then updates the holder's value. Guarantees the registry never accumulates duplicate
     * meters for the same (name, tags) pair across repeated snapshots.
     *
     * @param name gauge name
     * @param value current value to publish
     * @param tags tag set identifying this gauge's dimensions
     */
    private void gauge(final String name, final long value, final Tags tags) {
        final AtomicLong holder = gauges.computeIfAbsent(name + tags, key -> {
            final AtomicLong atomicLong = new AtomicLong();
            Gauge.builder(name, atomicLong, AtomicLong::doubleValue).tags(tags).register(registry);
            return atomicLong;
        });
        holder.set(value);
    }
}
