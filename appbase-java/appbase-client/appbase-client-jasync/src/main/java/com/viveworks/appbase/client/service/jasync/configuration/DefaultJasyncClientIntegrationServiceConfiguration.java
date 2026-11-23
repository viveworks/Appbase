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

package com.viveworks.appbase.client.service.jasync.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.jasync.DefaultJasyncClientIntegrationService;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultJasyncClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJasyncClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultJasyncClientIntegrationServiceConfiguration
        implements JasyncClientIntegrationServiceConfiguration
{
    private final TextValueConfiguration textValueKeyConfiguration;

    private final JasyncClientServiceConfiguration.DatabaseType databaseType;

    private final String host;

    private final int port;

    private final String database;

    private final String user;

    @Nullable
    private final String password;

    private final int maxActiveConnections;

    private final TimeUnitConfiguration maxIdleTimeConfiguration;

    private final int maxPendingQueries;

    private final TimeUnitConfiguration connectionValidationIntervalConfiguration;

    private final TimeoutConfiguration connectionCreateTimeoutConfiguration;

    private final TimeoutConfiguration connectionTestTimeoutConfiguration;

    private final TimeoutConfiguration queryTimeoutConfiguration;

    private final TimeUnitConfiguration maxConnectionTtlConfiguration;

    @Nullable
    private final String applicationName;

    // Metrics
    private final boolean driverMetricsEnabled;



    private DefaultJasyncClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.textValueKeyConfiguration, "builder.textValueKeyConfiguration");
        AppbaseUtil.notNull(builder.databaseType, "builder.databaseType");
        AppbaseUtil.notNull(builder.host, "builder.host");
        AppbaseUtil.positiveOrZero(builder.port, "builder.port");
        AppbaseUtil.notNull(builder.user, "builder.user");
        AppbaseUtil.positiveOrZero(builder.maxActiveConnections, "builder.maxActiveConnections");
        AppbaseUtil.notNull(builder.maxIdleTimeConfiguration, "builder.maxIdleTimeConfiguration");
        AppbaseUtil.positiveOrZero(builder.maxPendingQueries, "builder.maxPendingQueries");
        AppbaseUtil.notNull(
                builder.connectionValidationIntervalConfiguration,
                "builder.connectionValidationIntervalConfiguration");
        AppbaseUtil.notNull(builder.connectionCreateTimeoutConfiguration, "builder.connectionCreateTimeoutConfiguration");
        AppbaseUtil.notNull(builder.connectionTestTimeoutConfiguration, "builder.connectionTestTimeoutConfiguration");
        AppbaseUtil.notNull(builder.queryTimeoutConfiguration, "builder.queryTimeoutConfiguration");
        AppbaseUtil.notNull(builder.maxConnectionTtlConfiguration, "builder.maxConnectionTtlConfiguration");


        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        databaseType = builder.databaseType;
        host = builder.host;
        port = builder.port;
        database = builder.database;
        user = builder.user;
        password = builder.password;
        maxActiveConnections = builder.maxActiveConnections;
        maxIdleTimeConfiguration = builder.maxIdleTimeConfiguration;
        maxPendingQueries = builder.maxPendingQueries;
        connectionValidationIntervalConfiguration = builder.connectionValidationIntervalConfiguration;
        connectionCreateTimeoutConfiguration = builder.connectionCreateTimeoutConfiguration;
        connectionTestTimeoutConfiguration = builder.connectionTestTimeoutConfiguration;
        queryTimeoutConfiguration = builder.queryTimeoutConfiguration;
        maxConnectionTtlConfiguration = builder.maxConnectionTtlConfiguration;
        applicationName = builder.applicationName;
        driverMetricsEnabled = builder.driverMetricsEnabled;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public JasyncClientServiceConfiguration.DatabaseType getDatabaseType() {
        return databaseType;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public int getMaxActiveConnections() {
        return maxActiveConnections;
    }

    public TimeUnitConfiguration getMaxIdleTimeConfiguration() {
        return maxIdleTimeConfiguration;
    }

    public int getMaxPendingQueries() {
        return maxPendingQueries;
    }

    public TimeUnitConfiguration getConnectionValidationIntervalConfiguration() {
        return connectionValidationIntervalConfiguration;
    }

    public TimeoutConfiguration getConnectionCreateTimeoutConfiguration() {
        return connectionCreateTimeoutConfiguration;
    }

    public TimeoutConfiguration getConnectionTestTimeoutConfiguration() {
        return connectionTestTimeoutConfiguration;
    }

    public TimeoutConfiguration getQueryTimeoutConfiguration() {
        return queryTimeoutConfiguration;
    }

    public TimeUnitConfiguration getMaxConnectionTtlConfiguration() {
        return maxConnectionTtlConfiguration;
    }

    @Nullable
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return driverMetricsEnabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultJasyncClientIntegrationServiceConfiguration that =
                (DefaultJasyncClientIntegrationServiceConfiguration) o;
        return port == that.port &&
                maxActiveConnections == that.maxActiveConnections &&
                maxPendingQueries == that.maxPendingQueries &&
                databaseType == that.databaseType &&
                Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(host, that.host) &&
                Objects.equals(database, that.database) &&
                Objects.equals(user, that.user) &&
                Objects.equals(password, that.password) &&
                Objects.equals(maxIdleTimeConfiguration, that.maxIdleTimeConfiguration) &&
                Objects.equals(connectionValidationIntervalConfiguration, that.connectionValidationIntervalConfiguration) &&
                Objects.equals(connectionCreateTimeoutConfiguration, that.connectionCreateTimeoutConfiguration) &&
                Objects.equals(connectionTestTimeoutConfiguration, that.connectionTestTimeoutConfiguration) &&
                Objects.equals(queryTimeoutConfiguration, that.queryTimeoutConfiguration) &&
                Objects.equals(maxConnectionTtlConfiguration, that.maxConnectionTtlConfiguration) &&
                Objects.equals(applicationName, that.applicationName) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                textValueKeyConfiguration,
                databaseType,
                host,
                port,
                database,
                user,
                password,
                maxActiveConnections,
                maxIdleTimeConfiguration,
                maxPendingQueries,
                connectionValidationIntervalConfiguration,
                connectionCreateTimeoutConfiguration,
                connectionTestTimeoutConfiguration,
                queryTimeoutConfiguration,
                maxConnectionTtlConfiguration,
                applicationName,
                driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultJasyncClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", databaseType=" + databaseType +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", user='" + user + '\'' +
                ", password='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", maxActiveConnections=" + maxActiveConnections +
                ", maxIdleTimeConfiguration=" + maxIdleTimeConfiguration +
                ", maxPendingQueries=" + maxPendingQueries +
                ", connectionValidationIntervalConfiguration=" + connectionValidationIntervalConfiguration +
                ", connectionCreateTimeoutConfiguration=" + connectionCreateTimeoutConfiguration +
                ", connectionTestTimeoutConfiguration=" + connectionTestTimeoutConfiguration +
                ", queryTimeoutConfiguration=" + queryTimeoutConfiguration +
                ", maxConnectionTtlConfiguration=" + maxConnectionTtlConfiguration +
                ", applicationName='" + applicationName + '\'' +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }


    /**
     * Builder for {@link DefaultJasyncClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private final TextValueConfiguration DEFAULT_TEXT_VALUE_KEY_CONFIGURATION = TextValueConfiguration.PRIMARY;
        private static final JasyncClientServiceConfiguration.DatabaseType DEFAULT_DATABASE_TYPE =
                JasyncClientServiceConfiguration.DatabaseType.MYSQL_OR_MARIADB;
        private static final String DEFAULT_HOST = "mariadb.local";
        private static final int DEFAULT_PORT = 3306;
        private static final String DEFAULT_DATABASE = "appbase-test-mariadb-db";
        private static final String DEFAULT_USER = "root";
        private static final String DEFAULT_PASSWORD = "password";
        private static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 1;
        private static final TimeUnitConfiguration DEFAULT_MAX_IDLE_TIME_CONFIGURATION =
                TimeUnitConfiguration.builder().enabled(true).time(10).timeUnit(TimeUnit.MILLISECONDS).build();
        private static final int DEFAULT_MAX_PENDING_QUERIES = Integer.MAX_VALUE;
        private static final TimeUnitConfiguration DEFAULT_CONNECTION_VALIDATION_INTERVAL_CONFIGURATION =
                TimeUnitConfiguration.builder().enabled(true).time(5).timeUnit(TimeUnit.SECONDS).build();
        private static final TimeoutConfiguration DEFAULT_CONNECTION_CREATE_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(5).timeoutTimeUnit(TimeUnit.SECONDS).build();
        private static final TimeoutConfiguration DEFAULT_CONNECTION_TEST_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(5).timeoutTimeUnit(TimeUnit.SECONDS).build();
        private static final TimeoutConfiguration DEFAULT_QUERY_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(false).build();
        private static final TimeUnitConfiguration DEFAULT_MAX_CONNECTION_TTL_CONFIGURATION =
                TimeUnitConfiguration.builder().enabled(false).build();



        private TextValueConfiguration textValueKeyConfiguration = DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;

        private JasyncClientServiceConfiguration.DatabaseType databaseType = DEFAULT_DATABASE_TYPE;

        private String host = DEFAULT_HOST;

        private int port = DEFAULT_PORT;

        private String database = DEFAULT_DATABASE;

        private String user = DEFAULT_USER;

        @Nullable
        private String password = DEFAULT_PASSWORD;

        private int maxActiveConnections = DEFAULT_MAX_ACTIVE_CONNECTIONS;

        private TimeUnitConfiguration maxIdleTimeConfiguration = DEFAULT_MAX_IDLE_TIME_CONFIGURATION;

        private int maxPendingQueries = DEFAULT_MAX_PENDING_QUERIES;

        private TimeUnitConfiguration connectionValidationIntervalConfiguration =
                DEFAULT_CONNECTION_VALIDATION_INTERVAL_CONFIGURATION;

        private TimeoutConfiguration connectionCreateTimeoutConfiguration =
                DEFAULT_CONNECTION_CREATE_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration connectionTestTimeoutConfiguration =
                DEFAULT_CONNECTION_TEST_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration queryTimeoutConfiguration = DEFAULT_QUERY_TIMEOUT_CONFIGURATION;

        private TimeUnitConfiguration maxConnectionTtlConfiguration = DEFAULT_MAX_CONNECTION_TTL_CONFIGURATION;

        @Nullable
        private String applicationName;

        // Metrics
        private boolean driverMetricsEnabled = true;



        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder databaseType(final JasyncClientServiceConfiguration.DatabaseType databaseType) {
            this.databaseType = databaseType;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder database(final String database) {
            this.database = database;
            return this;
        }

        public Builder user(final String user) {
            this.user = user;
            return this;
        }

        public Builder password(@Nullable final String password) {
            this.password = password;
            return this;
        }

        public Builder maxActiveConnections(final int maxActiveConnections) {
            this.maxActiveConnections = maxActiveConnections;
            return this;
        }

        public Builder maxIdleTimeConfiguration(final TimeUnitConfiguration maxIdleTimeConfiguration) {
            this.maxIdleTimeConfiguration = maxIdleTimeConfiguration;
            return this;
        }

        public Builder maxPendingQueries(final int maxPendingQueries) {
            this.maxPendingQueries = maxPendingQueries;
            return this;
        }

        public Builder connectionValidationIntervalConfiguration(
                final TimeUnitConfiguration connectionValidationIntervalConfiguration)
        {
            this.connectionValidationIntervalConfiguration = connectionValidationIntervalConfiguration;
            return this;
        }

        public Builder connectionCreateTimeoutConfiguration(
                final TimeoutConfiguration connectionCreateTimeoutConfiguration)
        {
            this.connectionCreateTimeoutConfiguration = connectionCreateTimeoutConfiguration;
            return this;
        }

        public Builder connectionTestTimeoutConfiguration(final TimeoutConfiguration connectionTestTimeoutConfiguration) {
            this.connectionTestTimeoutConfiguration = connectionTestTimeoutConfiguration;
            return this;
        }

        public Builder queryTimeoutConfiguration(final TimeoutConfiguration queryTimeoutConfiguration) {
            this.queryTimeoutConfiguration = queryTimeoutConfiguration;
            return this;
        }

        public Builder maxConnectionTtlConfiguration(final TimeUnitConfiguration maxConnectionTtlConfiguration) {
            this.maxConnectionTtlConfiguration = maxConnectionTtlConfiguration;
            return this;
        }

        public Builder applicationName(@Nullable final String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        /**
         * Set whether jasync connection-pool size counters should be bridged onto Appbase's
         * shared Micrometer composite registry (→ JMX). Defaults to {@code true}.
         *
         * @param driverMetricsEnabled driver metrics enabled flag
         * @return this builder
         */
        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        public DefaultJasyncClientIntegrationServiceConfiguration build() {
            return new DefaultJasyncClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
