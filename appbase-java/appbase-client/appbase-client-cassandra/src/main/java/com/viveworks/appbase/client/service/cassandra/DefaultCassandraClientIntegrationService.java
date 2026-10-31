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

package com.viveworks.appbase.client.service.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.cassandra.configuration.DefaultCassandraClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.cassandra.configuration.DriverConfigLoaderConfiguration;
import com.viveworks.appbase.client.service.cassandra.configuration.LocalDatacenterConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;

/**
 * Default implementation of {@link CassandraClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultCassandraClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <CqlSession, ClusterIdValue, DefaultCassandraClientIntegrationServiceConfiguration>
        implements CassandraClientIntegrationService
{
    /**
     * Fully-qualified name of the DataStax driver's Micrometer metrics factory. Referenced by string because it lives
     * in an {@code internal} package that must not be imported.
     */
    private static final String MICROMETER_METRICS_FACTORY_CLASS_NAME =
            "com.datastax.oss.driver.internal.metrics.micrometer.MicrometerMetricsFactory";

    /**
     * Session-level driver metrics enabled when routing driver-native metrics onto Appbase's shared composite. Must be
     * non-empty for the driver to emit any metrics.
     */
    private static final List<String> DEFAULT_ENABLED_SESSION_METRICS =
            List.of("cql-requests", "bytes-sent", "bytes-received");

    private final AtomicReference<CqlSession> cqlSessionRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultCassandraClientIntegrationServiceConfiguration
            defaultCassandraClientIntegrationServiceConfiguration;



    public DefaultCassandraClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<CqlSession, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final CqlSession cqlSession)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, CqlSession.class);

        AppbaseUtil.notNull(cqlSession, "cqlSession");

        this.cqlSessionRef = new AtomicReference<>(cqlSession);
        this.defaultCassandraClientIntegrationServiceConfiguration = null;
    }

    public DefaultCassandraClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<CqlSession, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultCassandraClientIntegrationServiceConfiguration
                    defaultCassandraClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, CqlSession.class);

        AppbaseUtil.notNull(
                defaultCassandraClientIntegrationServiceConfiguration,
                "defaultCassandraClientIntegrationServiceConfiguration");

        this.cqlSessionRef = new AtomicReference<>();
        this.defaultCassandraClientIntegrationServiceConfiguration = defaultCassandraClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultCassandraClientIntegrationServiceConfiguration,
                "defaultCassandraClientIntegrationServiceConfiguration");

        // TODO Phase-2: Building NettyOptions used shared event loop OR just using CqlSessionBuilder must be configurable
        final AppbaseCassandraSessionBuilder sessionBuilder = new AppbaseCassandraSessionBuilder(appbaseClientCore);


        final Set<HostPortConfiguration> hostPortConfigurations =
                defaultCassandraClientIntegrationServiceConfiguration.getHostPortConfigurations();
        AppbaseUtil.notNull(hostPortConfigurations, "hostPortConfigurations");

        hostPortConfigurations.forEach(hostPortConfiguration -> {
            final String host = hostPortConfiguration.getHost();
            final int port = hostPortConfiguration.getPort();
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
            sessionBuilder.addContactPoint(inetSocketAddress);
        });


        final String keyspace = defaultCassandraClientIntegrationServiceConfiguration.getKeyspace();
        if (keyspace != null) {
            sessionBuilder.withKeyspace(keyspace);
        }


        final LocalDatacenterConfiguration localDatacenterConfiguration =
                defaultCassandraClientIntegrationServiceConfiguration.getLocalDatacenterConfiguration();
        if (localDatacenterConfiguration != null && localDatacenterConfiguration.isEnabled()) {
            sessionBuilder.withLocalDatacenter(localDatacenterConfiguration.getLocalDatacenter());
        }


        final DriverConfigLoaderConfiguration driverConfigLoaderConfiguration =
                defaultCassandraClientIntegrationServiceConfiguration.getDriverConfigLoaderConfiguration();
        if (driverConfigLoaderConfiguration != null) {
            final String filename = driverConfigLoaderConfiguration.getFilename();
            final String resourceBaseName = driverConfigLoaderConfiguration.getResourceBaseName();
            final String url = driverConfigLoaderConfiguration.getUrl();

            DriverConfigLoader driverConfigLoader = null;
            if (url != null) {
                try {
                    final URL parsedUrl = new URL(url);
                    driverConfigLoader = DriverConfigLoader.fromUrl(parsedUrl);
                } catch (MalformedURLException mue) {
                    throw new AppbaseRuntimeException(
                            "Error parsing URL string " + url + " from configuration " +
                                    defaultCassandraClientIntegrationServiceConfiguration);
                }
            } else if (filename != null) {
                final File file = new File(filename);
                driverConfigLoader = DriverConfigLoader.fromFile(file);
            } else if (resourceBaseName != null) {
                driverConfigLoader = DriverConfigLoader.fromClasspath(resourceBaseName);
            }

            if (driverConfigLoader == null) {
                throw new AppbaseRuntimeException(
                        "Failed to construct " + DriverConfigLoader.class.getName() +
                                " from configuration: " + driverConfigLoaderConfiguration);
            }
            sessionBuilder.withConfigLoader(driverConfigLoader);
        }


        // Auth credentials
        final String username = defaultCassandraClientIntegrationServiceConfiguration.getUsername();
        final String password = defaultCassandraClientIntegrationServiceConfiguration.getPassword();
        if (username != null && password != null) {
            sessionBuilder.withAuthCredentials(username, password);
        }

        // SSL context — withSslContext() delegates to DefaultSslEngineFactory which enables
        // hostname verification by default (datastax-java-driver 4.x), preventing MITM attacks.
        // Callers needing a custom trust store must supply a DriverConfigLoaderConfiguration
        // with datastax-java-driver.advanced.ssl-engine-factory settings instead.
        if (Boolean.TRUE.equals(defaultCassandraClientIntegrationServiceConfiguration.isSslEnabled())) {
            try {
                sessionBuilder.withSslContext(SSLContext.getDefault());
            } catch (NoSuchAlgorithmException e) {
                throw new AppbaseRuntimeException("Failed to obtain default SSL context", e);
            }
        }

        // Astra cloud secure connect bundle
        final String cloudBundle = defaultCassandraClientIntegrationServiceConfiguration.getCloudSecureConnectBundlePath();
        if (cloudBundle != null) {
            sessionBuilder.withCloudSecureConnectBundle(new File(cloudBundle).toPath());
        }

        // TODO(Phase-2): loadBalancingPolicyClass requires DefaultDriverOption.LOAD_BALANCING_POLICY_CLASS
        // via programmatic config loader; combining with file-based DriverConfigLoaderConfiguration is complex.
        // TODO(Phase-2): retryPolicyClass requires DefaultDriverOption.RETRY_POLICY_CLASS via programmatic config loader.

        // Programmatic request-timeout + config-gated driver metrics (bind Appbase's shared Micrometer composite).
        applyProgrammaticDriverConfig(sessionBuilder, defaultCassandraClientIntegrationServiceConfiguration);

        // TODO Phase-2 Configure and custom-build more options

        final CqlSession cqlSession = sessionBuilder.build();
        this.cqlSessionRef.set(cqlSession);
    }

    /**
     * Apply programmatic driver options (request timeout and, when enabled, DataStax driver-native metrics) and bind
     * Appbase's shared Micrometer composite registry onto the session builder.
     *
     * <p>Programmatic options are only applied when no external/file-based {@code DriverConfigLoaderConfiguration} is
     * supplied — combining programmatic options with a file-based loader is a known limitation (see the Phase-2 TODOs).
     * When driver metrics are enabled the driver's {@code MicrometerMetricsFactory} is configured and Appbase's shared
     * composite {@code MeterRegistry} (owned by {@code MetricService}, auto-exported to JMX) is bound so CQL-request and
     * byte metrics are recorded onto it. The registry's lifecycle is owned by {@code MetricService}; this session never
     * creates nor shuts down a registry.</p>
     *
     * <p>Package-private so it can be unit-tested without a live {@code build()} (which connects to Cassandra).</p>
     *
     * @param sessionBuilder the session builder to configure (state-only mutation; no connection is opened)
     * @param cfg the resolved Cassandra client configuration
     */
    void applyProgrammaticDriverConfig(
            final AppbaseCassandraSessionBuilder sessionBuilder,
            final DefaultCassandraClientIntegrationServiceConfiguration cfg)
    {
        final DriverConfigLoaderConfiguration fileLoaderCfg = cfg.getDriverConfigLoaderConfiguration();

        // Programmatic options (request-timeout + driver metrics) only when no external/file config loader is supplied.
        // (Combining programmatic options with a file-based loader is a known limitation — see the Phase-2 TODOs.)
        if (fileLoaderCfg == null) {
            final ProgrammaticDriverConfigLoaderBuilder pb = DriverConfigLoader.programmaticBuilder();
            boolean anyProgrammatic = false;

            final Duration requestTimeout = cfg.getRequestTimeout();
            if (requestTimeout != null) {
                pb.withDuration(DefaultDriverOption.REQUEST_TIMEOUT, requestTimeout);
                anyProgrammatic = true;
            }

            if (cfg.isDriverMetricsEnabled()) {
                pb.withString(DefaultDriverOption.METRICS_FACTORY_CLASS, MICROMETER_METRICS_FACTORY_CLASS_NAME)
                        .withStringList(DefaultDriverOption.METRICS_SESSION_ENABLED, DEFAULT_ENABLED_SESSION_METRICS);
                anyProgrammatic = true;
            }

            if (anyProgrammatic) {
                sessionBuilder.withConfigLoader(pb.build());
            }
        }

        // Bind Appbase's shared Micrometer composite so the driver's MicrometerMetricsFactory records onto it (→ JMX).
        // Gated on the programmatic path: only when we actually configured the Micrometer factory above, so we never
        // hand a Micrometer registry to a differently-configured (file-loader) factory and trip build()'s type check.
        if (cfg.isDriverMetricsEnabled() && fileLoaderCfg == null) {
            sessionBuilder.withMetricRegistry(
                    appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry());
        }
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultCassandraClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final CqlSession cqlSession = getClient();
        if (cqlSession != null) {
            cqlSession.close();
        }

        cqlSessionRef.set(null);
    }

    @Override
    public CqlSession getClient() {
        return cqlSessionRef.get();
    }

    @Override
    public boolean isHealthy() {
        final CqlSession cqlSession = getClient();
        if (cqlSession == null) {
            return false;
        }
        return !cqlSession.isClosed();
    }
}
