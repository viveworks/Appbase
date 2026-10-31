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

package com.viveworks.appbase.client.service.zookeeper;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.zookeeper.configuration.AuthInfoConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.DefaultZooKeeperClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.BoundedExponentialBackoffRetryConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.ExponentialBackoffRetryConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryForeverConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryNTimesConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryOneTimeConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryPolicyConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryUntilElapsedConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.SessionFailedRetryConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.AppbaseCharset;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.apache.curator.RetryPolicy;
import org.apache.curator.SessionFailedRetryPolicy;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.api.CompressionProvider;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.retry.RetryUntilElapsed;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ZooKeeperClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultZooKeeperClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <CuratorFramework, ClusterIdValue, DefaultZooKeeperClientIntegrationServiceConfiguration>
        implements ZooKeeperClientIntegrationService
{
    private final AtomicReference<CuratorFramework> curatorFrameworkRef;

    // Metrics: Curator has no driver-native metrics registry, so the connection-state gauge is
    // fed by a ConnectionStateListener rather than polled from a live client field.
    private final AtomicInteger connectionStateCode = new AtomicInteger(0);

    @Nullable
    private ConnectionStateListener connectionStateListener;

    @Nullable
    private Meter connectionStateGauge;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultZooKeeperClientIntegrationServiceConfiguration
            defaultZooKeeperClientIntegrationServiceConfiguration;

    public DefaultZooKeeperClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<CuratorFramework, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final CuratorFramework curatorFramework)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, CuratorFramework.class);

        AppbaseUtil.notNull(curatorFramework, "curatorFramework");

        this.curatorFrameworkRef = new AtomicReference<>(curatorFramework);
        this.defaultZooKeeperClientIntegrationServiceConfiguration = null;
    }

    public DefaultZooKeeperClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<CuratorFramework, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultZooKeeperClientIntegrationServiceConfiguration
                    defaultZooKeeperClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, CuratorFramework.class);

        AppbaseUtil.notNull(
                defaultZooKeeperClientIntegrationServiceConfiguration,
                "defaultZooKeeperClientIntegrationServiceConfiguration");

        this.curatorFrameworkRef = new AtomicReference<>();
        this.defaultZooKeeperClientIntegrationServiceConfiguration = defaultZooKeeperClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultZooKeeperClientIntegrationServiceConfiguration,
                "defaultZooKeeperClientIntegrationServiceConfiguration");

        final CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();


        final Set<HostPortConfiguration> hostPortConfigurations =
                defaultZooKeeperClientIntegrationServiceConfiguration.getHostPortConfigurations();
        AppbaseUtil.notEmpty(hostPortConfigurations, "hostPortConfigurations");
        final String connectString = AppbaseClientUtil.hostPortCsv(hostPortConfigurations);
        builder.connectString(connectString);


        final RetryPolicyConfiguration retryPolicyConfiguration =
                defaultZooKeeperClientIntegrationServiceConfiguration.getRetryPolicyConfiguration();
        final RetryPolicy retryPolicy = retryPolicy(retryPolicyConfiguration);
        builder.retryPolicy(retryPolicy);


        final TimeoutConfiguration maxCloseWaitTimeoutConfiguration =
                defaultZooKeeperClientIntegrationServiceConfiguration.getMaxCloseWaitTimeoutConfiguration();
        final long maxCloseWaitMs =
                AppbaseNetUtil.convertTimeoutConfiguration(maxCloseWaitTimeoutConfiguration, TimeUnit.MILLISECONDS);
        builder.maxCloseWaitMs((int) maxCloseWaitMs);


        final TimeoutConfiguration connectionTimeoutConfiguration =
                defaultZooKeeperClientIntegrationServiceConfiguration.getConnectionTimeoutConfiguration();
        final long connectTimeoutMs =
                AppbaseNetUtil.convertTimeoutConfiguration(connectionTimeoutConfiguration, TimeUnit.MILLISECONDS);
        builder.connectionTimeoutMs((int) connectTimeoutMs);


        final TimeoutConfiguration sessionTimeoutConfiguration =
                defaultZooKeeperClientIntegrationServiceConfiguration.getSessionTimeoutConfiguration();
        final long sessionTimeoutMs =
                AppbaseNetUtil.convertTimeoutConfiguration(sessionTimeoutConfiguration, TimeUnit.MILLISECONDS);
        builder.sessionTimeoutMs((int) sessionTimeoutMs);


        final TimeoutConfiguration waitForShutdownTimeoutConfiguration =
                defaultZooKeeperClientIntegrationServiceConfiguration.getWaitForShutdownTimeoutConfiguration();
        final long waitForShutdownTimeoutMs =
                AppbaseNetUtil.convertTimeoutConfiguration(waitForShutdownTimeoutConfiguration, TimeUnit.MILLISECONDS);
        builder.waitForShutdownTimeoutMs((int) waitForShutdownTimeoutMs);


        final String defaultDataUtf8 = defaultZooKeeperClientIntegrationServiceConfiguration.getDefaultDataUtf8();
        if (defaultDataUtf8 != null) {
            final byte[] defaultData = defaultDataUtf8.getBytes(AppbaseCharset.UTF_8.getCharset());
            builder.defaultData(defaultData);
        }


        final String namespace = defaultZooKeeperClientIntegrationServiceConfiguration.getNamespace();
        if (namespace != null) {
            builder.namespace(namespace);
        }


        final int simulatedSessionExpirationPercent =
                defaultZooKeeperClientIntegrationServiceConfiguration.getSimulatedSessionExpirationPercent();
        builder.simulatedSessionExpirationPercent(simulatedSessionExpirationPercent);


        // Auth info
        final List<AuthInfoConfiguration> authInfoList =
                defaultZooKeeperClientIntegrationServiceConfiguration.getAuthInfoList();
        if (authInfoList != null && !authInfoList.isEmpty()) {
            final List<AuthInfo> authInfos = authInfoList.stream()
                    .map(a -> new AuthInfo(a.getScheme(), a.getAuth().getBytes(StandardCharsets.UTF_8)))
                    .collect(Collectors.toList());
            builder.authorization(authInfos);
        }

        // ACL provider
        final Class<?> aclProviderClass = defaultZooKeeperClientIntegrationServiceConfiguration.getAclProviderClass();
        if (aclProviderClass != null) {
            try {
                builder.aclProvider((ACLProvider) aclProviderClass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new AppbaseRuntimeException(
                        "Failed to instantiate ACL provider class: " + aclProviderClass.getName(), e);
            }
        }

        // Compression provider
        final Class<?> compressionProviderClass =
                defaultZooKeeperClientIntegrationServiceConfiguration.getCompressionProviderClass();
        if (compressionProviderClass != null) {
            try {
                builder.compressionProvider(
                        (CompressionProvider) compressionProviderClass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new AppbaseRuntimeException(
                        "Failed to instantiate compression provider class: " + compressionProviderClass.getName(), e);
            }
        }

        // TODO(Phase-2): connectionHandlingPolicy — no matching method on CuratorFrameworkFactory.Builder in Curator 5.1.0
        // TODO(Phase-2): sessionCreationTimeout — no matching method on CuratorFrameworkFactory.Builder in Curator 5.1.0
        // TODO(Phase-2): dnsResolverClass — no matching method on CuratorFrameworkFactory.Builder in Curator 5.1.0

        final CuratorFramework curatorFramework = builder.build();
        curatorFramework.start();

        // Curator has no driver-native metrics registry (unlike Aerospike/Cassandra/Kafka): bridge
        // the live ConnectionStateListener state onto the shared composite instead.
        bindConnectionStateMetrics(curatorFramework);

        if (!defaultZooKeeperClientIntegrationServiceConfiguration.isDisableBlockUntilConnected()) {
            final TimeoutConfiguration blockUntilConnectedTimeoutConfiguration =
                    defaultZooKeeperClientIntegrationServiceConfiguration.getBlockUntilConnectedTimeoutConfiguration();
            try {
                curatorFramework.blockUntilConnected(
                        (int) blockUntilConnectedTimeoutConfiguration.getTimeout(),
                        blockUntilConnectedTimeoutConfiguration.getTimeoutTimeUnit());
            } catch (InterruptedException ie) {
                throw new AppbaseRuntimeException("ZooKeeper connection 'block until connected' was interrupted", ie);
            }
        }

        curatorFrameworkRef.set(curatorFramework);
    }

    /**
     * Bridges the live Curator connection-state to a Micrometer gauge on the shared composite
     * registry (→ JMX). Curator has no driver-native metrics registry (unlike
     * Aerospike/Cassandra/Kafka), so this gauge is fed by a {@link ConnectionStateListener} rather
     * than polling a live counter -- the gauge simply reports the latest numeric state observed on
     * the listener. This is in addition to, not instead of, the spine's per-operation {@code
     * Timer}s. No-op when {@link #isDriverMetricsEnabled()} is {@code false}.
     *
     * <p>The gauge is tagged with this service's own {@link #appbaseClientKey}, stringified, so
     * that multiple keyed ZooKeeper clients sharing the same composite registry register DISTINCT
     * Micrometer Ids. Without this tag, every {@code DefaultZooKeeperClientIntegrationService}
     * instance would register the exact same gauge Id: Micrometer's {@code register()}
     * dedup-by-Id would silently drop the second-and-later keyed clients' gauges, and {@link
     * #unbindConnectionStateMetrics()} on one keyed service would remove a still-live sibling's
     * gauge. The composite registry itself is MetricService-owned -- it is never shut down here.
     *
     * @param client the live Curator client whose connection-state changes should be observed
     */
    protected void bindConnectionStateMetrics(final CuratorFramework client) {
        if (!isDriverMetricsEnabled()) {
            return;
        }

        // Defensive: a re-bind without an intervening shutdown (e.g. re-init/repair) still
        // refreshes against the new client rather than being silently dropped by Micrometer's
        // register() dedup-by-Id, and stops the stale listener from further updating the gauge.
        unbindConnectionStateMetrics();

        // Reset to the unknown/initial state so a rebound gauge does not keep reporting the
        // previous client's last-observed state until the first new connection event fires.
        connectionStateCode.set(0);

        final MeterRegistry registry = appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
        final Tags keyTag = Tags.of("key", appbaseClientKey.toString());

        this.connectionStateListener = (c, newState) -> connectionStateCode.set(stateCode(newState));
        client.getConnectionStateListenable().addListener(connectionStateListener);

        this.connectionStateGauge = Gauge.builder("zookeeper.connection.state", connectionStateCode, AtomicInteger::get)
                .tags(keyTag)
                .register(registry);
    }

    /**
     * Removes this service's own {@code zookeeper.connection.state} gauge and {@link
     * ConnectionStateListener} so that a later re-init (repair/reconnect) re-registers fresh
     * against the new client, instead of Micrometer's {@code register()} dedup-by-Id silently
     * discarding the re-bind and leaving the old gauge frozen at a stale state. This does NOT shut
     * down the shared composite registry itself -- that is MetricService-owned. No-op if no gauge
     * or listener is currently bound.
     */
    protected void unbindConnectionStateMetrics() {
        if (connectionStateGauge != null) {
            final MeterRegistry registry =
                    appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry();
            registry.remove(connectionStateGauge);
            connectionStateGauge = null;
        }

        if (connectionStateListener != null) {
            // Defensive: getClient() may already be null during shutdown (curatorFrameworkRef is
            // nulled after unbind, but a repair/rebind race could still leave no live client).
            final CuratorFramework client = getClient();
            if (client != null) {
                client.getConnectionStateListenable().removeListener(connectionStateListener);
            }
            connectionStateListener = null;
        }
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return defaultZooKeeperClientIntegrationServiceConfiguration != null
                && defaultZooKeeperClientIntegrationServiceConfiguration.isDriverMetricsEnabled();
    }

    /**
     * Maps a Curator {@link ConnectionState} to the numeric code reported by the connection-state
     * gauge: {@code CONNECTED}/{@code RECONNECTED} → 1, {@code SUSPENDED} → 2, {@code LOST} → 3,
     * {@code READ_ONLY} → 4, and 0 for the unknown/initial state.
     *
     * @param connectionState the Curator connection state
     * @return the numeric gauge code for {@code connectionState}
     */
    private static int stateCode(final ConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTED:
            case RECONNECTED:
                return 1;
            case SUSPENDED:
                return 2;
            case LOST:
                return 3;
            case READ_ONLY:
                return 4;
            default:
                return 0;
        }
    }

    private RetryPolicy retryPolicy(final RetryPolicyConfiguration retryPolicyConfiguration) {
        AppbaseUtil.notNull(retryPolicyConfiguration, "retryPolicyConfiguration");

        if (SessionFailedRetryConfiguration.class.isAssignableFrom(retryPolicyConfiguration.getClass())) {
            final SessionFailedRetryConfiguration sessionFailedRetryConfiguration =
                    (SessionFailedRetryConfiguration) retryPolicyConfiguration;
            final RetryPolicyConfiguration delegateRetryPolicyConfiguration =
                    sessionFailedRetryConfiguration.getDelegateRetryPolicyConfiguration();
            final RetryPolicy retryPolicy = nonDelegatingRetryPolicy(delegateRetryPolicyConfiguration);
            return new SessionFailedRetryPolicy(retryPolicy);
        }

        return nonDelegatingRetryPolicy(retryPolicyConfiguration);
    }

    @SuppressWarnings({"checkstyle:LeftCurly", "checkstyle:RightCurly"})
    private RetryPolicy nonDelegatingRetryPolicy(final RetryPolicyConfiguration retryPolicyConfiguration) {
        AppbaseUtil.notNull(retryPolicyConfiguration, "retryPolicyConfiguration");

        final Class<? extends RetryPolicyConfiguration> retryPolicyConfigurationType = retryPolicyConfiguration.getClass();
        if (BoundedExponentialBackoffRetryConfiguration.class.isAssignableFrom(retryPolicyConfigurationType))
        {
            final BoundedExponentialBackoffRetryConfiguration boundedExponentialBackoffRetryConfiguration =
                    (BoundedExponentialBackoffRetryConfiguration) retryPolicyConfiguration;
            return new BoundedExponentialBackoffRetry(
                    boundedExponentialBackoffRetryConfiguration.getBaseSleepTimeMillis(),
                    boundedExponentialBackoffRetryConfiguration.getMaxSleepTimeMillis(),
                    boundedExponentialBackoffRetryConfiguration.getMaxRetries());
        }
        else if (ExponentialBackoffRetryConfiguration.class.isAssignableFrom(retryPolicyConfigurationType))
        {
            final ExponentialBackoffRetryConfiguration exponentialBackoffRetryConfiguration =
                    (ExponentialBackoffRetryConfiguration) retryPolicyConfiguration;
            return new ExponentialBackoffRetry(
                    exponentialBackoffRetryConfiguration.getBaseSleepTimeMillis(),
                    exponentialBackoffRetryConfiguration.getMaxRetries(),
                    exponentialBackoffRetryConfiguration.getMaxSleepTimeMillis());
        }
        else if (RetryForeverConfiguration.class.isAssignableFrom(retryPolicyConfigurationType))
        {
            final RetryForeverConfiguration retryForeverConfiguration =
                    (RetryForeverConfiguration) retryPolicyConfiguration;
            return new RetryForever(retryForeverConfiguration.getRetryIntervalMillis());
        }
        else if (RetryNTimesConfiguration.class.isAssignableFrom(retryPolicyConfigurationType))
        {
            final RetryNTimesConfiguration retryNTimesConfiguration = (RetryNTimesConfiguration) retryPolicyConfiguration;
            return new RetryNTimes(
                    retryNTimesConfiguration.getNumberOfTimes(),
                    retryNTimesConfiguration.getSleepMillisBetweenRetries());
        }
        else if (RetryOneTimeConfiguration.class.isAssignableFrom(retryPolicyConfigurationType))
        {
            final RetryOneTimeConfiguration retryOneTimeConfiguration = (RetryOneTimeConfiguration) retryPolicyConfiguration;
            return new RetryOneTime(retryOneTimeConfiguration.getSleepMillisBetweenRetry());
        }
        else if (RetryUntilElapsedConfiguration.class.isAssignableFrom(retryPolicyConfigurationType))
        {
            final RetryUntilElapsedConfiguration retryUntilElapsedConfiguration =
                    (RetryUntilElapsedConfiguration) retryPolicyConfiguration;
            return new RetryUntilElapsed(
                    retryUntilElapsedConfiguration.getMaxElapsedTimeMillis(),
                    retryUntilElapsedConfiguration.getSleepMillisBetweenRetries());
        }

        throw new AppbaseRuntimeException("Unsupported " + RetryPolicyConfiguration.class.getName() +
                " implementation type: " + retryPolicyConfigurationType);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultZooKeeperClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        unbindConnectionStateMetrics();

        final CuratorFramework curatorFramework = getClient();
        if (curatorFramework != null) {
            curatorFramework.close();
        }

        curatorFrameworkRef.set(null);
    }

    @Override
    public CuratorFramework getClient() {
        return curatorFrameworkRef.get();
    }

    @Override
    public boolean isHealthy() {
        final CuratorFramework curatorFramework = getClient();
        if (curatorFramework == null) {
            return false;
        }

        final CuratorFrameworkState curatorFrameworkState = curatorFramework.getState();
        return curatorFrameworkState == CuratorFrameworkState.STARTED;
    }
}
