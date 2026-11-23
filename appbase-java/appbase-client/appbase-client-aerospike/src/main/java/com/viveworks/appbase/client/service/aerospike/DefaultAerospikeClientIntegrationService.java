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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.EventLoop;
import com.aerospike.client.async.EventLoops;
import com.aerospike.client.async.NettyEventLoops;
import com.aerospike.client.metrics.MetricsPolicy;
import com.aerospike.client.policy.ClientPolicy;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.aerospike.configuration.AerospikeNodeConfiguration;
import com.viveworks.appbase.client.service.aerospike.configuration.AuthMode;
import com.viveworks.appbase.client.service.aerospike.configuration.DefaultAerospikeClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.aerospike.configuration.ReadModeAP;
import com.viveworks.appbase.client.service.aerospike.configuration.ReadModeSC;
import com.viveworks.appbase.client.service.aerospike.configuration.ReplicaPolicy;
import com.viveworks.appbase.client.service.aerospike.metrics.AppbaseAerospikeMetricsListener;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AerospikeClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAerospikeClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <IAerospikeClient, ClusterIdValue, DefaultAerospikeClientIntegrationServiceConfiguration>
        implements AerospikeClientIntegrationService
{
    private final AtomicReference<EventLoops> eventLoopsRef;

    private final AtomicReference<AerospikeClient> aerospikeClientRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultAerospikeClientIntegrationServiceConfiguration
            defaultAerospikeClientIntegrationServiceConfiguration;



    public DefaultAerospikeClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<IAerospikeClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final EventLoops eventLoops,
            final AerospikeClient aerospikeClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, IAerospikeClient.class);

        AppbaseUtil.notNull(eventLoops, "eventLoops");
        AppbaseUtil.notNull(aerospikeClient, "aerospikeClient");

        this.eventLoopsRef = new AtomicReference<>(eventLoops);
        this.aerospikeClientRef = new AtomicReference<>(aerospikeClient);
        this.defaultAerospikeClientIntegrationServiceConfiguration = null;
    }

    public DefaultAerospikeClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<IAerospikeClient, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultAerospikeClientIntegrationServiceConfiguration
                    defaultAerospikeClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, IAerospikeClient.class);

        AppbaseUtil.notNull(
                defaultAerospikeClientIntegrationServiceConfiguration,
                "defaultAerospikeClientIntegrationServiceConfiguration");

        this.defaultAerospikeClientIntegrationServiceConfiguration = defaultAerospikeClientIntegrationServiceConfiguration;
        this.eventLoopsRef = new AtomicReference<>();
        this.aerospikeClientRef = new AtomicReference<>();

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultAerospikeClientIntegrationServiceConfiguration,
                "defaultAerospikeClientIntegrationServiceConfiguration");

        final Set<AerospikeNodeConfiguration> aerospikeNodeConfigurations =
                defaultAerospikeClientIntegrationServiceConfiguration.getAerospikeNodeConfigurations();
        final Host[] hosts = new Host[aerospikeNodeConfigurations.size()];
        int index = 0;
        for (final AerospikeNodeConfiguration aerospikeNodeConfiguration : aerospikeNodeConfigurations) {
            final Host host = host(aerospikeNodeConfiguration);
            hosts[index] = host;
            ++index;
        }

        final ClientPolicy clientPolicy = new ClientPolicy();

        // Connection settings
        final Integer timeout = defaultAerospikeClientIntegrationServiceConfiguration.getTimeout();
        if (timeout != null) { clientPolicy.timeout = timeout; }

        final Integer loginTimeout = defaultAerospikeClientIntegrationServiceConfiguration.getLoginTimeout();
        if (loginTimeout != null) { clientPolicy.loginTimeout = loginTimeout; }

        final Integer maxConnsPerNode = defaultAerospikeClientIntegrationServiceConfiguration.getMaxConnsPerNode();
        if (maxConnsPerNode != null) { clientPolicy.maxConnsPerNode = maxConnsPerNode; }

        final Integer minConnsPerNode = defaultAerospikeClientIntegrationServiceConfiguration.getMinConnsPerNode();
        if (minConnsPerNode != null) { clientPolicy.minConnsPerNode = minConnsPerNode; }

        final Integer connPoolsPerNode = defaultAerospikeClientIntegrationServiceConfiguration.getConnPoolsPerNode();
        if (connPoolsPerNode != null) { clientPolicy.connPoolsPerNode = connPoolsPerNode; }

        final Integer maxSocketIdle = defaultAerospikeClientIntegrationServiceConfiguration.getMaxSocketIdle();
        if (maxSocketIdle != null) { clientPolicy.maxSocketIdle = maxSocketIdle; }

        final Integer tendInterval = defaultAerospikeClientIntegrationServiceConfiguration.getTendInterval();
        if (tendInterval != null) { clientPolicy.tendInterval = tendInterval; }

        final Boolean failIfNotConnected =
                defaultAerospikeClientIntegrationServiceConfiguration.getFailIfNotConnected();
        if (failIfNotConnected != null) { clientPolicy.failIfNotConnected = failIfNotConnected; }

        // Auth settings
        final String user = defaultAerospikeClientIntegrationServiceConfiguration.getUser();
        if (user != null) { clientPolicy.user = user; }

        final String password = defaultAerospikeClientIntegrationServiceConfiguration.getPassword();
        if (password != null) { clientPolicy.password = password; }

        final AuthMode authMode = defaultAerospikeClientIntegrationServiceConfiguration.getAuthMode();
        if (authMode != null) {
            clientPolicy.authMode =
                    com.aerospike.client.policy.AuthMode.valueOf(authMode.getText());
        }

        // TLS
        final Boolean tlsEnabled = defaultAerospikeClientIntegrationServiceConfiguration.getTlsEnabled();
        if (Boolean.TRUE.equals(tlsEnabled)) {
            clientPolicy.tlsPolicy = new com.aerospike.client.policy.TlsPolicy();
        }

        // Read policies — applied via readPolicyDefault on ClientPolicy
        final ReadModeSC readModeSC = defaultAerospikeClientIntegrationServiceConfiguration.getReadModeSC();
        if (readModeSC != null) {
            clientPolicy.readPolicyDefault.readModeSC =
                    com.aerospike.client.policy.ReadModeSC.valueOf(readModeSC.getText());
        }

        final ReadModeAP readModeAP = defaultAerospikeClientIntegrationServiceConfiguration.getReadModeAP();
        if (readModeAP != null) {
            clientPolicy.readPolicyDefault.readModeAP =
                    com.aerospike.client.policy.ReadModeAP.valueOf(readModeAP.getText());
        }

        final ReplicaPolicy replicaPolicy =
                defaultAerospikeClientIntegrationServiceConfiguration.getReplicaPolicy();
        if (replicaPolicy != null) {
            clientPolicy.readPolicyDefault.replica =
                    com.aerospike.client.policy.Replica.valueOf(replicaPolicy.getText());
        }

        // Rack awareness
        final Boolean rackAware = defaultAerospikeClientIntegrationServiceConfiguration.getRackAware();
        if (rackAware != null) { clientPolicy.rackAware = rackAware; }

        final List<Integer> rackIds = defaultAerospikeClientIntegrationServiceConfiguration.getRackIds();
        if (rackIds != null && !rackIds.isEmpty()) {
            clientPolicy.rackId = rackIds.get(0);
        }

        final EventLoopGroup sharedEventLoopGroup = new NioEventLoopGroup();
        final EventLoops eventLoops = new NettyEventLoops(sharedEventLoopGroup);
        clientPolicy.eventLoops = eventLoops;
        clientPolicy.threadPool = appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedExecutorService();
        clientPolicy.sharedThreadPool = true;

        final AerospikeClient aerospikeClient = new AerospikeClient(clientPolicy, hosts);
        this.eventLoopsRef.set(eventLoops);
        this.aerospikeClientRef.set(aerospikeClient);

        // Route driver-native metrics onto Appbase's shared Micrometer composite (→ JMX) when enabled.
        configureDriverMetrics(aerospikeClient);

        super.postInit();
    }

    /**
     * Enables Aerospike driver-native metrics on {@code client}, bridging its {@code MetricsListener}
     * snapshots onto Appbase's shared Micrometer composite registry (owned by {@code MetricService},
     * auto-exported to JMX). No-op when {@link #isDriverMetricsEnabled()} is {@code false}.
     *
     * <p>Setting {@link MetricsPolicy#listener} directly replaces the driver's default file-writer
     * sink with this push bridge — see {@link AppbaseAerospikeMetricsListener} for the rationale.
     * Protected testable seam: takes the {@link IAerospikeClient} interface (not the concrete
     * {@link AerospikeClient}) so it can be unit-tested with a mock client, without running the full
     * {@code initImpl()} config/connect graph.</p>
     *
     * @param client the driver client to enable metrics on
     */
    protected void configureDriverMetrics(final IAerospikeClient client) {
        if (!isDriverMetricsEnabled()) {
            return;
        }

        final MetricsPolicy metricsPolicy = new MetricsPolicy();
        metricsPolicy.listener = new AppbaseAerospikeMetricsListener(
                appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry());
        client.enableMetrics(metricsPolicy);
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return defaultAerospikeClientIntegrationServiceConfiguration != null
                && defaultAerospikeClientIntegrationServiceConfiguration.isDriverMetricsEnabled();
    }

    private Host host(final AerospikeNodeConfiguration aerospikeNodeConfiguration) {
        AppbaseUtil.notNull(aerospikeNodeConfiguration, "aerospikeNodeConfiguration");

        final String hostnameOrIp = aerospikeNodeConfiguration.getHostnameOrIp();
        AppbaseUtil.notNull(hostnameOrIp, "hostnameOrIp");

        final int port = aerospikeNodeConfiguration.getPort();
        AppbaseUtil.positiveOrZero(port, "port");

        final String tlsName = aerospikeNodeConfiguration.getTlsName();
        final Host host;
        if (tlsName != null) {
            host = new Host(hostnameOrIp, tlsName, port);
        } else {
            host = new Host(hostnameOrIp, port);
        }

        return host;
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultAerospikeClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final AerospikeClient aerospikeClient = getClient();

        // Stop the driver-metrics listener BEFORE closing the client. Only the listener's future
        // snapshots stop — the composite registry itself is owned by MetricService and is NEVER
        // touched here.
        if (isDriverMetricsEnabled() && aerospikeClient != null) {
            aerospikeClient.disableMetrics();
        }

        if (aerospikeClient != null) {
            aerospikeClient.close();
        }

        // TODO Phase-2: Shutdown event loop and executor service only if it is not using the shared ones

        aerospikeClientRef.set(null);
    }

    @Override
    public AerospikeClient getClient() {
        return aerospikeClientRef.get();
    }

    @Override
    public EventLoop nextEventLoop() {
        return eventLoopsRef.get().next();
    }

    /**
     * Returns the shared {@link EventLoops} this integration service's {@link AerospikeClient} was
     * built with, so that a non-owning {@code AerospikeReactorClient} can be built on top of the same
     * event loops rather than creating its own.
     *
     * @return the shared {@link EventLoops}
     */
    @Override
    public EventLoops getEventLoops() {
        return eventLoopsRef.get();
    }

    @Override
    public boolean isHealthy() {
        final AerospikeClient aerospikeClient = getClient();
        if (aerospikeClient == null) {
            return false;
        }

        return aerospikeClient.isConnected();
    }
}
