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

package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.jdbc.configuration.DefaultJdbcClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.HikariDataSourceConfiguration;
import com.viveworks.appbase.client.service.jdbc.configuration.JdbcTransactionIsolation;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.SQLExceptionOverride;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link JdbcClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultJdbcClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <DataSource, TextValue, DefaultJdbcClientIntegrationServiceConfiguration>
        implements JdbcClientIntegrationService
{
    private static final String SERVER_NAME_PROPERTY = "serverName";
    private static final String PORT_PROPERTY = "port";
    private static final String DATABASE_NAME_PROPERTY = "databaseName";


    private final AtomicReference<DataSource> dataSourceRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultJdbcClientIntegrationServiceConfiguration defaultJdbcClientIntegrationServiceConfiguration;


    public DefaultJdbcClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<DataSource, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DataSource dataSource)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, DataSource.class);

        AppbaseUtil.notNull(dataSource, "dataSource");

        this.dataSourceRef = new AtomicReference<>(dataSource);
        this.defaultJdbcClientIntegrationServiceConfiguration = null;
    }

    public DefaultJdbcClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<DataSource, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultJdbcClientIntegrationServiceConfiguration defaultJdbcClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, DataSource.class);

        AppbaseUtil.notNull(
                defaultJdbcClientIntegrationServiceConfiguration, "defaultJdbcClientIntegrationServiceConfiguration");

        this.dataSourceRef = new AtomicReference<>();
        this.defaultJdbcClientIntegrationServiceConfiguration = defaultJdbcClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultJdbcClientIntegrationServiceConfiguration, "defaultJdbcClientIntegrationServiceConfiguration");

        final DataSource dataSource;

        final HikariDataSourceConfiguration hikariDataSourceConfiguration =
                defaultJdbcClientIntegrationServiceConfiguration.getHikariDataSourceConfiguration();

        final HikariConfig hikariConfig = new HikariConfig();

        final Class<? extends DataSource> dataSourceType = hikariDataSourceConfiguration.getDataSourceType();
        hikariConfig.setDataSourceClassName(dataSourceType.getName());


        final HostPortConfiguration serverHostPortConfiguration =
                hikariDataSourceConfiguration.getServerHostPortConfiguration();
        hikariConfig.addDataSourceProperty(SERVER_NAME_PROPERTY, serverHostPortConfiguration.getHost());
        hikariConfig.addDataSourceProperty(PORT_PROPERTY, serverHostPortConfiguration.getPort());


        final String databaseName = hikariDataSourceConfiguration.getDatabaseName();
        hikariConfig.addDataSourceProperty(DATABASE_NAME_PROPERTY, databaseName);


        final String username = hikariDataSourceConfiguration.getUsername();
        if (username != null) {
            hikariConfig.setUsername(username);
        }


        final String password = hikariDataSourceConfiguration.getPassword();
        if (password != null) {
            hikariConfig.setPassword(password);
        }


        final TimeoutConfiguration connectionTimeoutConfiguration =
                hikariDataSourceConfiguration.getConnectionTimeoutConfiguration();
        if (connectionTimeoutConfiguration != null && connectionTimeoutConfiguration.isEnabled()) {
            hikariConfig.setConnectionTimeout(connectionTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(connectionTimeoutConfiguration.getTimeout()));
        }


        final TimeoutConfiguration initializationFailTimeoutConfiguration =
                hikariDataSourceConfiguration.getInitializationFailTimeoutConfiguration();
        if (initializationFailTimeoutConfiguration != null && initializationFailTimeoutConfiguration.isEnabled()) {
            hikariConfig.setInitializationFailTimeout(initializationFailTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(initializationFailTimeoutConfiguration.getTimeout()));
        }


        final TimeoutConfiguration idleTimeoutConfiguration = hikariDataSourceConfiguration.getIdleTimeoutConfiguration();
        if (idleTimeoutConfiguration != null && idleTimeoutConfiguration.isEnabled()) {
            hikariConfig.setIdleTimeout(idleTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(idleTimeoutConfiguration.getTimeout()));
        }


        final TimeUnitConfiguration keepAliveTimeConfiguration =
                hikariDataSourceConfiguration.getKeepAliveTimeConfiguration();
        if (keepAliveTimeConfiguration != null && keepAliveTimeConfiguration.isEnabled()) {
            hikariConfig.setKeepaliveTime(keepAliveTimeConfiguration
                    .getTimeUnit()
                    .toMillis(keepAliveTimeConfiguration.getTime()));
        }


        final TimeUnitConfiguration leakDetectionThresholdConfiguration =
                hikariDataSourceConfiguration.getLeakDetectionThresholdConfiguration();
        if (leakDetectionThresholdConfiguration != null && leakDetectionThresholdConfiguration.isEnabled()) {
            hikariConfig.setLeakDetectionThreshold(leakDetectionThresholdConfiguration
                    .getTimeUnit()
                    .toMillis(leakDetectionThresholdConfiguration.getTime()));
        }


        final TimeUnitConfiguration maxLifetimeConfiguration = hikariDataSourceConfiguration.getMaxLifetimeConfiguration();
        if (maxLifetimeConfiguration != null && maxLifetimeConfiguration.isEnabled()) {
            hikariConfig.setMaxLifetime(maxLifetimeConfiguration
                    .getTimeUnit()
                    .toMillis(maxLifetimeConfiguration.getTime()));
        }


        final TimeoutConfiguration validationTimeoutConfiguration =
                hikariDataSourceConfiguration.getValidationTimeoutConfiguration();
        if (validationTimeoutConfiguration != null && validationTimeoutConfiguration.isEnabled()) {
            hikariConfig.setValidationTimeout(validationTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(validationTimeoutConfiguration.getTimeout()));
        }


        final String connectionInitSql = hikariDataSourceConfiguration.getConnectionInitSql();
        if (connectionInitSql != null) {
            hikariConfig.setConnectionInitSql(connectionInitSql);
        }


        final String connectionTestQuery = hikariDataSourceConfiguration.getConnectionTestSql();
        if (connectionTestQuery != null) {
            hikariConfig.setConnectionTestQuery(connectionTestQuery);
        }


        final JdbcTransactionIsolation jdbcTransactionIsolation =
                hikariDataSourceConfiguration.getJdbcTransactionIsolation();
        if (jdbcTransactionIsolation != null) {
            hikariConfig.setTransactionIsolation(jdbcTransactionIsolation.getTransactionName());
        }


        final Integer maximumPoolSize = hikariDataSourceConfiguration.getMaximumPoolSize();
        if (maximumPoolSize != null) {
            hikariConfig.setMaximumPoolSize(maximumPoolSize);
        }


        final Integer minimumIdleConnections = hikariDataSourceConfiguration.getMinimumIdleConnections();
        if (minimumIdleConnections != null) {
            hikariConfig.setMinimumIdle(minimumIdleConnections);
        }


        final String poolName = hikariDataSourceConfiguration.getPoolName();
        if (poolName != null) {
            hikariConfig.setPoolName(poolName);
        }


        final String schema = hikariDataSourceConfiguration.getSchema();
        if (schema != null) {
            hikariConfig.setSchema(schema);
        }


        final String jdbcUrl = hikariDataSourceConfiguration.getJdbcUrl();
        if (jdbcUrl != null) {
            hikariConfig.setJdbcUrl(jdbcUrl);
        }


        final String catalog = hikariDataSourceConfiguration.getCatalog();
        if (catalog != null) {
            hikariConfig.setCatalog(catalog);
        }


        final Map<String, String> dataSourceProperties = hikariDataSourceConfiguration.getDataSourceProperties();
        if (dataSourceProperties != null) {
            dataSourceProperties.forEach(hikariConfig::addDataSourceProperty);
        }


        final Class<? extends SQLExceptionOverride> sqlExceptionOverrideType =
                hikariDataSourceConfiguration.getSqlExceptionOverrideType();
        if (sqlExceptionOverrideType != null) {
            hikariConfig.setExceptionOverrideClassName(sqlExceptionOverrideType.getName());
        }


        final Boolean allowPoolSuspension = hikariDataSourceConfiguration.isAllowPoolSuspension();
        if (allowPoolSuspension != null) {
            hikariConfig.setAllowPoolSuspension(allowPoolSuspension);
        }


        final Boolean autoCommit = hikariDataSourceConfiguration.isAutoCommit();
        if (autoCommit != null) {
            hikariConfig.setAutoCommit(autoCommit);
        }


        final Boolean isolateInternalQueries = hikariDataSourceConfiguration.isIsolateInternalQueries();
        if (isolateInternalQueries != null) {
            hikariConfig.setIsolateInternalQueries(isolateInternalQueries);
        }


        final Boolean readOnly = hikariDataSourceConfiguration.isReadOnly();
        if (readOnly != null) {
            hikariConfig.setReadOnly(readOnly);
        }


        final Boolean registerMBeans = hikariDataSourceConfiguration.isRegisterMBeans();
        if (registerMBeans != null) {
            hikariConfig.setRegisterMbeans(registerMBeans);
        }


        final ThreadFactoryConfiguration threadFactoryConfiguration =
                hikariDataSourceConfiguration.getThreadFactoryConfiguration();
        if (threadFactoryConfiguration != null) {
            final ThreadFactory threadFactory =
                    AppbaseUtil.newThreadFactory(
                            threadFactoryConfiguration, AppbaseUtil.defaultUncaughtExceptionHandler(logger));
            hikariConfig.setThreadFactory(threadFactory);
        }


        final Boolean useSharedScheduledExecutorService =
                hikariDataSourceConfiguration.isUseSharedScheduledExecutorService();
        final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration =
                hikariDataSourceConfiguration.getScheduledExecutorServiceConfiguration();
        if (useSharedScheduledExecutorService != null && useSharedScheduledExecutorService) {
            hikariConfig.setScheduledExecutor(appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .getSharedScheduledExecutorService());
        } else if (scheduledExecutorServiceConfiguration != null) {
            final ScheduledExecutorService scheduledExecutorService =
                    ScheduledExecutorServiceFactory.newScheduledExecutorService(
                            scheduledExecutorServiceConfiguration, AppbaseUtil.defaultUncaughtExceptionHandler(logger));
            hikariConfig.setScheduledExecutor(scheduledExecutorService);
        }


        final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        this.dataSourceRef.set(hikariDataSource);
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultJdbcClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final DataSource dataSource = getClient();

        if (dataSource != null && HikariDataSource.class.isAssignableFrom(dataSource.getClass())) {
            ((HikariDataSource) dataSource).close();
        }

        dataSourceRef.set(null);
    }

    @Override
    public DataSource getClient() {
        return dataSourceRef.get();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public boolean isHealthy() {
        final DataSource dataSource = getClient();

        if (dataSource == null) {
            return false;
        }

        if (HikariDataSource.class.isAssignableFrom(dataSource.getClass())) {
            return !((HikariDataSource) dataSource).isClosed();
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (Exception ex) {
            return false;
        }
    }
}
