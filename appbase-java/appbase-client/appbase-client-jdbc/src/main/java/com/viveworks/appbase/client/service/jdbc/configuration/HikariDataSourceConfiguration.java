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

package com.viveworks.appbase.client.service.jdbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.zaxxer.hikari.SQLExceptionOverride;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.annotation.concurrent.Immutable;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents {@code HikariCP} {@link DataSource} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = HikariDataSourceConfiguration.Builder.class)
public final class HikariDataSourceConfiguration {
    private final Class<? extends DataSource> dataSourceType;

    private final HostPortConfiguration serverHostPortConfiguration;

    private final String databaseName;

    private final String username;

    private final String password;

    private final TimeoutConfiguration initializationFailTimeoutConfiguration;

    private final TimeoutConfiguration connectionTimeoutConfiguration;

    private final TimeoutConfiguration validationTimeoutConfiguration;

    private final TimeoutConfiguration idleTimeoutConfiguration;

    private final TimeUnitConfiguration keepAliveTimeConfiguration;

    private final TimeUnitConfiguration leakDetectionThresholdConfiguration;

    private final TimeUnitConfiguration maxLifetimeConfiguration;

    private final Integer maximumPoolSize;

    private final Integer minimumIdleConnections;

    private final String connectionInitSql;

    private final String connectionTestSql;

    private final Class<? extends SQLExceptionOverride> sqlExceptionOverrideType;

    private final String poolName;

    private final String schema;

    private final JdbcTransactionIsolation jdbcTransactionIsolation;

    private final Boolean autoCommit;

    private final Boolean readOnly;

    private final Boolean isolateInternalQueries;

    private final Boolean registerMBeans;

    private final Boolean allowPoolSuspension;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> dataSourceProperties;

    private final ThreadFactoryConfiguration threadFactoryConfiguration;

    private final Boolean useSharedScheduledExecutorService;

    private final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration;

    private final String jdbcUrl;

    private final String catalog;



    private HikariDataSourceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.dataSourceType, "builder.dataSourceType");
        AppbaseUtil.notNull(builder.serverHostPortConfiguration, "builder.serverHostPortConfiguration");
        AppbaseUtil.notNull(builder.databaseName, "builder.databaseName");

        dataSourceType = builder.dataSourceType;
        serverHostPortConfiguration = builder.serverHostPortConfiguration;
        databaseName = builder.databaseName;
        username = builder.username;
        password = builder.password;
        connectionTimeoutConfiguration = builder.connectionTimeoutConfiguration;
        initializationFailTimeoutConfiguration = builder.initializationFailTimeoutConfiguration;
        validationTimeoutConfiguration = builder.validationTimeoutConfiguration;
        idleTimeoutConfiguration = builder.idleTimeoutConfiguration;
        keepAliveTimeConfiguration = builder.keepAliveTimeConfiguration;
        leakDetectionThresholdConfiguration = builder.leakDetectionThresholdConfiguration;
        maxLifetimeConfiguration = builder.maxLifetimeConfiguration;
        maximumPoolSize = builder.maximumPoolSize;
        minimumIdleConnections = builder.minimumIdleConnections;
        connectionInitSql = builder.connectionInitSql;
        connectionTestSql = builder.connectionTestSql;
        sqlExceptionOverrideType = builder.sqlExceptionOverrideType;
        poolName = builder.poolName;
        schema = builder.schema;
        jdbcTransactionIsolation = builder.jdbcTransactionIsolation;
        autoCommit = builder.autoCommit;
        readOnly = builder.readOnly;
        isolateInternalQueries = builder.isolateInternalQueries;
        registerMBeans = builder.registerMBeans;
        allowPoolSuspension = builder.allowPoolSuspension;
        dataSourceProperties = builder.dataSourceProperties;
        threadFactoryConfiguration = builder.threadFactoryConfiguration;
        useSharedScheduledExecutorService = builder.useSharedScheduledExecutorService;
        scheduledExecutorServiceConfiguration = builder.scheduledExecutorServiceConfiguration;
        jdbcUrl = builder.jdbcUrl;
        catalog = builder.catalog;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Class<? extends DataSource> getDataSourceType() {
        return dataSourceType;
    }

    public HostPortConfiguration getServerHostPortConfiguration() {
        return serverHostPortConfiguration;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public TimeoutConfiguration getConnectionTimeoutConfiguration() {
        return connectionTimeoutConfiguration;
    }

    public TimeoutConfiguration getInitializationFailTimeoutConfiguration() {
        return initializationFailTimeoutConfiguration;
    }

    public TimeoutConfiguration getValidationTimeoutConfiguration() {
        return validationTimeoutConfiguration;
    }

    public TimeoutConfiguration getIdleTimeoutConfiguration() {
        return idleTimeoutConfiguration;
    }

    public TimeUnitConfiguration getKeepAliveTimeConfiguration() {
        return keepAliveTimeConfiguration;
    }

    public TimeUnitConfiguration getLeakDetectionThresholdConfiguration() {
        return leakDetectionThresholdConfiguration;
    }

    public TimeUnitConfiguration getMaxLifetimeConfiguration() {
        return maxLifetimeConfiguration;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public Integer getMinimumIdleConnections() {
        return minimumIdleConnections;
    }

    public String getConnectionInitSql() {
        return connectionInitSql;
    }

    public String getConnectionTestSql() {
        return connectionTestSql;
    }

    public Class<? extends SQLExceptionOverride> getSqlExceptionOverrideType() {
        return sqlExceptionOverrideType;
    }

    public String getPoolName() {
        return poolName;
    }

    public String getSchema() {
        return schema;
    }

    public JdbcTransactionIsolation getJdbcTransactionIsolation() {
        return jdbcTransactionIsolation;
    }

    public Boolean isAutoCommit() {
        return autoCommit;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public Boolean isIsolateInternalQueries() {
        return isolateInternalQueries;
    }

    public Boolean isRegisterMBeans() {
        return registerMBeans;
    }

    public Boolean isAllowPoolSuspension() {
        return allowPoolSuspension;
    }

    public Map<String, String> getDataSourceProperties() {
        return dataSourceProperties;
    }

    public ThreadFactoryConfiguration getThreadFactoryConfiguration() {
        return threadFactoryConfiguration;
    }

    public Boolean isUseSharedScheduledExecutorService() {
        return useSharedScheduledExecutorService;
    }

    public ScheduledExecutorServiceConfiguration getScheduledExecutorServiceConfiguration() {
        return scheduledExecutorServiceConfiguration;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getCatalog() {
        return catalog;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HikariDataSourceConfiguration that = (HikariDataSourceConfiguration) o;
        return Objects.equals(maximumPoolSize, that.maximumPoolSize) &&
                Objects.equals(minimumIdleConnections, that.minimumIdleConnections) &&
                autoCommit == that.autoCommit &&
                readOnly == that.readOnly &&
                isolateInternalQueries == that.isolateInternalQueries &&
                registerMBeans == that.registerMBeans &&
                allowPoolSuspension == that.allowPoolSuspension &&
                useSharedScheduledExecutorService == that.useSharedScheduledExecutorService &&
                Objects.equals(dataSourceType, that.dataSourceType) &&
                Objects.equals(serverHostPortConfiguration, that.serverHostPortConfiguration) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(connectionTimeoutConfiguration, that.connectionTimeoutConfiguration) &&
                Objects.equals(initializationFailTimeoutConfiguration, that.initializationFailTimeoutConfiguration) &&
                Objects.equals(validationTimeoutConfiguration, that.validationTimeoutConfiguration) &&
                Objects.equals(idleTimeoutConfiguration, that.idleTimeoutConfiguration) &&
                Objects.equals(keepAliveTimeConfiguration, that.keepAliveTimeConfiguration) &&
                Objects.equals(leakDetectionThresholdConfiguration, that.leakDetectionThresholdConfiguration) &&
                Objects.equals(maxLifetimeConfiguration, that.maxLifetimeConfiguration) &&
                Objects.equals(connectionInitSql, that.connectionInitSql) &&
                Objects.equals(connectionTestSql, that.connectionTestSql) &&
                Objects.equals(sqlExceptionOverrideType, that.sqlExceptionOverrideType) &&
                Objects.equals(poolName, that.poolName) &&
                Objects.equals(schema, that.schema) &&
                jdbcTransactionIsolation == that.jdbcTransactionIsolation &&
                Objects.equals(dataSourceProperties, that.dataSourceProperties) &&
                Objects.equals(threadFactoryConfiguration, that.threadFactoryConfiguration) &&
                Objects.equals(scheduledExecutorServiceConfiguration, that.scheduledExecutorServiceConfiguration) &&
                Objects.equals(jdbcUrl, that.jdbcUrl) &&
                Objects.equals(catalog, that.catalog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                dataSourceType,
                serverHostPortConfiguration,
                databaseName,
                username,
                password,
                connectionTimeoutConfiguration,
                initializationFailTimeoutConfiguration,
                validationTimeoutConfiguration,
                idleTimeoutConfiguration,
                keepAliveTimeConfiguration,
                leakDetectionThresholdConfiguration,
                maxLifetimeConfiguration,
                maximumPoolSize,
                minimumIdleConnections,
                connectionInitSql,
                connectionTestSql,
                sqlExceptionOverrideType,
                poolName,
                schema,
                jdbcTransactionIsolation,
                autoCommit,
                readOnly,
                isolateInternalQueries,
                registerMBeans,
                allowPoolSuspension,
                dataSourceProperties,
                threadFactoryConfiguration,
                useSharedScheduledExecutorService,
                scheduledExecutorServiceConfiguration,
                jdbcUrl,
                catalog);
    }

    @Override
    public String toString() {
        return "HikariPoolConfiguration{" +
                "dataSourceType=" + dataSourceType +
                ", serverHostPortConfiguration=" + serverHostPortConfiguration +
                ", databaseName='" + databaseName + '\'' +
                ", username='" + username + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", connectionTimeoutConfiguration=" + connectionTimeoutConfiguration +
                ", initializationFailTimeoutConfiguration=" + initializationFailTimeoutConfiguration +
                ", validationTimeoutConfiguration=" + validationTimeoutConfiguration +
                ", idleTimeoutConfiguration=" + idleTimeoutConfiguration +
                ", keepAliveTimeConfiguration=" + keepAliveTimeConfiguration +
                ", leakDetectionThresholdConfiguration=" + leakDetectionThresholdConfiguration +
                ", maxLifetimeConfiguration=" + maxLifetimeConfiguration +
                ", maximumPoolSize=" + maximumPoolSize +
                ", minimumIdleConnections=" + minimumIdleConnections +
                ", connectionInitSql='" + connectionInitSql + '\'' +
                ", connectionTestSql='" + connectionTestSql + '\'' +
                ", sqlExceptionOverrideType=" + sqlExceptionOverrideType +
                ", poolName='" + poolName + '\'' +
                ", schema='" + schema + '\'' +
                ", jdbcTransactionIsolation=" + jdbcTransactionIsolation +
                ", autoCommit=" + autoCommit +
                ", readOnly=" + readOnly +
                ", isolateInternalQueries=" + isolateInternalQueries +
                ", registerMBeans=" + registerMBeans +
                ", allowPoolSuspension=" + allowPoolSuspension +
                ", properties=" + AppbaseUtil.OBFUSCATE +
                ", threadFactoryConfiguration=" + threadFactoryConfiguration +
                ", useSharedScheduledExecutorService=" + useSharedScheduledExecutorService +
                ", scheduledExecutorServiceConfiguration=" + scheduledExecutorServiceConfiguration +
                ", jdbcUrl='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", catalog='" + catalog + '\'' +
                '}';
    }



    /**
     * Builder for {@link HikariDataSourceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final Class<? extends DataSource> DEFAULT_DATA_SOURCE_TYPE = MariaDbDataSource.class;
        private static final HostPortConfiguration DEFAULT_HOST_PORT_CONFIGURATION = HostPortConfiguration
                .builder()
                .host("mariadb.local")
                .port(3306)
                .build();
        private static final String DEFAULT_DATABASE_NAME = "appbase-test-mariadb-db";
        private static final String DEFAULT_USERNAME = "root";
        private static final String DEFAULT_PASSWORD = "password";
        private static final TimeoutConfiguration DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(30)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final TimeoutConfiguration DEFAULT_INITIALIZATION_FAIL_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(false)
                .build();
        private static final TimeoutConfiguration DEFAULT_VALIDATION_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(5)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final TimeoutConfiguration DEFAULT_IDLE_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(false)
                .build();
        private static final TimeUnitConfiguration DEFAULT_KEEP_ALIVE_TIME_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(false)
                .build();
        private static final TimeUnitConfiguration DEFAULT_LEAK_DETECTION_THRESHOLD_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(false)
                .build();
        private static final TimeUnitConfiguration DEFAULT_MAX_LIFETIME_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(1)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;
        private static final int DEFAULT_MINIMUM_IDLE_CONNECTIONS = DEFAULT_MAXIMUM_POOL_SIZE;
        private static final String DEFAULT_POOL_NAME = "appbase-hikari-jdbc-pool";
        private static final JdbcTransactionIsolation DEFAULT_JDBC_TRANSACTION_ISOLATION =
                JdbcTransactionIsolation.TRANSACTION_REPEATABLE_READ;
        private static final boolean DEFAULT_AUTO_COMMIT = true;
        private static final boolean DEFAULT_READ_ONLY = false;
        private static final boolean DEFAULT_ISOLATE_INTERNAL_QUERIES = false;
        private static final boolean DEFAULT_REGISTER_MBEANS = true;
        private static final boolean DEFAULT_ALLOW_POOL_SUSPENSION = false;
        private static final Map<String, String> DEFAULT_DATA_SOURCE_PROPERTIES = Map.of();
        private static final ThreadFactoryConfiguration DEFAULT_THREAD_FACTORY_CONFIGURATION = ThreadFactoryConfiguration
                .builder()
                .enableThreadAffinity(true)
                .daemon(false)
                .nameFormat("appbase-hikari-jdbc-thread-%d")
                .build();
        private static final boolean DEFAULT_USE_SHARED_SCHEDULED_EXECUTOR_SERVICE = true;



        private Class<? extends DataSource> dataSourceType = DEFAULT_DATA_SOURCE_TYPE;

        private HostPortConfiguration serverHostPortConfiguration = DEFAULT_HOST_PORT_CONFIGURATION;

        private String databaseName = DEFAULT_DATABASE_NAME;

        private String username = DEFAULT_USERNAME;

        private String password = DEFAULT_PASSWORD;

        private TimeoutConfiguration connectionTimeoutConfiguration = DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration initializationFailTimeoutConfiguration =
                DEFAULT_INITIALIZATION_FAIL_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration validationTimeoutConfiguration = DEFAULT_VALIDATION_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration idleTimeoutConfiguration = DEFAULT_IDLE_TIMEOUT_CONFIGURATION;

        private TimeUnitConfiguration keepAliveTimeConfiguration = DEFAULT_KEEP_ALIVE_TIME_CONFIGURATION;

        private TimeUnitConfiguration leakDetectionThresholdConfiguration = DEFAULT_LEAK_DETECTION_THRESHOLD_CONFIGURATION;

        private TimeUnitConfiguration maxLifetimeConfiguration = DEFAULT_MAX_LIFETIME_CONFIGURATION;

        private Integer maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;

        private Integer minimumIdleConnections = DEFAULT_MINIMUM_IDLE_CONNECTIONS;

        private String connectionInitSql;

        private String connectionTestSql;

        private Class<? extends SQLExceptionOverride> sqlExceptionOverrideType;

        private String poolName = DEFAULT_POOL_NAME;

        private String schema;

        private JdbcTransactionIsolation jdbcTransactionIsolation = DEFAULT_JDBC_TRANSACTION_ISOLATION;

        private Boolean autoCommit = DEFAULT_AUTO_COMMIT;

        private Boolean readOnly = DEFAULT_READ_ONLY;

        private Boolean isolateInternalQueries = DEFAULT_ISOLATE_INTERNAL_QUERIES;

        private Boolean registerMBeans = DEFAULT_REGISTER_MBEANS;

        private Boolean allowPoolSuspension = DEFAULT_ALLOW_POOL_SUSPENSION;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> dataSourceProperties = DEFAULT_DATA_SOURCE_PROPERTIES;

        private ThreadFactoryConfiguration threadFactoryConfiguration = DEFAULT_THREAD_FACTORY_CONFIGURATION;

        private Boolean useSharedScheduledExecutorService = DEFAULT_USE_SHARED_SCHEDULED_EXECUTOR_SERVICE;

        private ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration;

        private String jdbcUrl;

        private String catalog;



        private Builder() {
        }

        public Builder dataSourceType(final Class<? extends DataSource> dataSourceType) {
            this.dataSourceType = dataSourceType;
            return this;
        }

        public Builder databaseName(final String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder serverHostPortConfiguration(final HostPortConfiguration serverHostPortConfiguration) {
            this.serverHostPortConfiguration = serverHostPortConfiguration;
            return this;
        }

        public Builder portNumber(final String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder connectionTimeoutConfiguration(final TimeoutConfiguration connectionTimeoutConfiguration) {
            this.connectionTimeoutConfiguration = connectionTimeoutConfiguration;
            return this;
        }

        public Builder initializationFailTimeoutConfiguration(
                final TimeoutConfiguration initializationFailTimeoutConfiguration)
        {
            this.initializationFailTimeoutConfiguration = initializationFailTimeoutConfiguration;
            return this;
        }

        public Builder validationTimeoutConfiguration(final TimeoutConfiguration validationTimeoutConfiguration) {
            this.validationTimeoutConfiguration = validationTimeoutConfiguration;
            return this;
        }

        public Builder idleTimeoutConfiguration(final TimeoutConfiguration idleTimeoutConfiguration) {
            this.idleTimeoutConfiguration = idleTimeoutConfiguration;
            return this;
        }

        public Builder keepAliveTimeConfiguration(final TimeUnitConfiguration keepAliveTimeConfiguration) {
            this.keepAliveTimeConfiguration = keepAliveTimeConfiguration;
            return this;
        }

        public Builder leakDetectionThresholdConfiguration(
                final TimeUnitConfiguration leakDetectionThresholdConfiguration)
        {
            this.leakDetectionThresholdConfiguration = leakDetectionThresholdConfiguration;
            return this;
        }

        public Builder maxLifetimeConfiguration(final TimeUnitConfiguration maxLifetimeConfiguration) {
            this.maxLifetimeConfiguration = maxLifetimeConfiguration;
            return this;
        }

        public Builder maximumPoolSize(final Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder minimumIdleConnections(final Integer minimumIdleConnections) {
            this.minimumIdleConnections = minimumIdleConnections;
            return this;
        }

        public Builder connectionInitSql(final String connectionInitSql) {
            this.connectionInitSql = connectionInitSql;
            return this;
        }

        public Builder connectionTestSql(final String connectionTestSql) {
            this.connectionTestSql = connectionTestSql;
            return this;
        }

        public Builder sqlExceptionOverrideType(final Class<? extends SQLExceptionOverride> sqlExceptionOverrideType) {
            this.sqlExceptionOverrideType = sqlExceptionOverrideType;
            return this;
        }

        public Builder poolName(final String poolName) {
            this.poolName = poolName;
            return this;
        }

        public Builder schema(final String schema) {
            this.schema = schema;
            return this;
        }

        public Builder jdbcTransactionIsolation(final JdbcTransactionIsolation jdbcTransactionIsolation) {
            this.jdbcTransactionIsolation = jdbcTransactionIsolation;
            return this;
        }

        public Builder autoCommit(final Boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder readOnly(final Boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder isolateInternalQueries(final Boolean isolateInternalQueries) {
            this.isolateInternalQueries = isolateInternalQueries;
            return this;
        }

        public Builder registerMBeans(final Boolean registerMBeans) {
            this.registerMBeans = registerMBeans;
            return this;
        }

        public Builder allowPoolSuspension(final Boolean allowPoolSuspension) {
            this.allowPoolSuspension = allowPoolSuspension;
            return this;
        }

        public Builder dataSourceProperties(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> dataSourceProperties)
        {
            this.dataSourceProperties = dataSourceProperties;
            return this;
        }

        public Builder threadFactoryConfiguration(final ThreadFactoryConfiguration threadFactoryConfiguration) {
            this.threadFactoryConfiguration = threadFactoryConfiguration;
            return this;
        }

        public Builder useSharedScheduledExecutorService(final Boolean useSharedScheduledExecutorService) {
            this.useSharedScheduledExecutorService = useSharedScheduledExecutorService;
            return this;
        }

        public Builder scheduledExecutorServiceConfiguration(
                final ScheduledExecutorServiceConfiguration scheduledExecutorServiceConfiguration)
        {
            this.scheduledExecutorServiceConfiguration = scheduledExecutorServiceConfiguration;
            return this;
        }

        public Builder jdbcUrl(final String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder catalog(final String catalog) {
            this.catalog = catalog;
            return this;
        }

        public HikariDataSourceConfiguration build() {
            return new HikariDataSourceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
