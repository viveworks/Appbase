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

package com.viveworks.appbase.client.service.cassandra.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of {@link CassandraClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultCassandraClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultCassandraClientIntegrationServiceConfiguration
        implements CassandraClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    private final Set<HostPortConfiguration> hostPortConfigurations;

    private final LocalDatacenterConfiguration localDatacenterConfiguration;

    @Nullable
    private final String keyspace;

    @Nullable
    private final DriverConfigLoaderConfiguration driverConfigLoaderConfiguration;

    @Nullable
    private final String username;

    @Nullable
    private final String password;

    @Nullable
    private final Duration requestTimeout;

    @Nullable
    private final Boolean sslEnabled;

    @Nullable
    private final String cloudSecureConnectBundlePath;

    @Nullable
    private final Class<?> loadBalancingPolicyClass;

    @Nullable
    private final Class<?> retryPolicyClass;

    private final boolean driverMetricsEnabled;



    private DefaultCassandraClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.clusterIdValueKeyConfiguration, "builder.clusterIdValueKeyConfiguration");
        AppbaseUtil.notEmpty(builder.hostPortConfigurations, "builder.hostPortConfigurations");
        AppbaseUtil.notNull(builder.localDatacenterConfiguration, "builder.localDatacenterConfiguration");

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        hostPortConfigurations = builder.hostPortConfigurations;
        localDatacenterConfiguration = builder.localDatacenterConfiguration;
        keyspace = builder.keyspace;
        driverConfigLoaderConfiguration = builder.driverConfigLoaderConfiguration;
        username = builder.username;
        password = builder.password;
        requestTimeout = builder.requestTimeout;
        sslEnabled = builder.sslEnabled;
        cloudSecureConnectBundlePath = builder.cloudSecureConnectBundlePath;
        loadBalancingPolicyClass = builder.loadBalancingPolicyClass;
        retryPolicyClass = builder.retryPolicyClass;
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

    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    public Set<HostPortConfiguration> getHostPortConfigurations() {
        return hostPortConfigurations;
    }

    public LocalDatacenterConfiguration getLocalDatacenterConfiguration() {
        return localDatacenterConfiguration;
    }

    @Nullable
    public String getKeyspace() {
        return keyspace;
    }

    @Nullable
    public DriverConfigLoaderConfiguration getDriverConfigLoaderConfiguration() {
        return driverConfigLoaderConfiguration;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    @Nullable
    public Boolean isSslEnabled() {
        return sslEnabled;
    }

    @Nullable
    public String getCloudSecureConnectBundlePath() {
        return cloudSecureConnectBundlePath;
    }

    @Nullable
    public Class<?> getLoadBalancingPolicyClass() {
        return loadBalancingPolicyClass;
    }

    @Nullable
    public Class<?> getRetryPolicyClass() {
        return retryPolicyClass;
    }

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

        final DefaultCassandraClientIntegrationServiceConfiguration that =
                (DefaultCassandraClientIntegrationServiceConfiguration) o;
        return Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(hostPortConfigurations, that.hostPortConfigurations) &&
                Objects.equals(localDatacenterConfiguration, that.localDatacenterConfiguration) &&
                Objects.equals(keyspace, that.keyspace) &&
                Objects.equals(driverConfigLoaderConfiguration, that.driverConfigLoaderConfiguration) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(requestTimeout, that.requestTimeout) &&
                Objects.equals(sslEnabled, that.sslEnabled) &&
                Objects.equals(cloudSecureConnectBundlePath, that.cloudSecureConnectBundlePath) &&
                Objects.equals(loadBalancingPolicyClass, that.loadBalancingPolicyClass) &&
                Objects.equals(retryPolicyClass, that.retryPolicyClass) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueKeyConfiguration,
                hostPortConfigurations,
                localDatacenterConfiguration,
                keyspace,
                driverConfigLoaderConfiguration,
                username,
                password,
                requestTimeout,
                sslEnabled,
                cloudSecureConnectBundlePath,
                loadBalancingPolicyClass,
                retryPolicyClass,
                driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultCassandraClientIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", hostPortConfigurations=" + hostPortConfigurations +
                ", localDatacenterConfiguration=" + localDatacenterConfiguration +
                ", keyspace='" + keyspace + '\'' +
                ", driverConfigLoaderConfiguration=" + driverConfigLoaderConfiguration +
                ", username=" + username +
                ", password=" + (password != null ? "[REDACTED]" : null) +
                ", requestTimeout=" + requestTimeout +
                ", sslEnabled=" + sslEnabled +
                ", cloudSecureConnectBundlePath=" + cloudSecureConnectBundlePath +
                ", loadBalancingPolicyClass=" + loadBalancingPolicyClass +
                ", retryPolicyClass=" + retryPolicyClass +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }


    /**
     * Builder for {@link DefaultCassandraClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final ClusterIdValueConfiguration DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION =
                ClusterIdValueConfiguration.builder().build();
        private static final Set<HostPortConfiguration> DEFAULT_HOST_PORT_CONFIGURATIONS = Set.of(
                HostPortConfiguration
                        .builder()
                        .host("docker.for.mac.localhost")
                        .port(9042)
                        .build());
        private static final LocalDatacenterConfiguration DEFAULT_LOCAL_DATA_CENTER_CONFIGURATION =
                LocalDatacenterConfiguration.builder().build();



        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration = DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;

        private Set<HostPortConfiguration> hostPortConfigurations = DEFAULT_HOST_PORT_CONFIGURATIONS;

        private LocalDatacenterConfiguration localDatacenterConfiguration = DEFAULT_LOCAL_DATA_CENTER_CONFIGURATION;

        @Nullable
        private String keyspace;

        @Nullable
        private DriverConfigLoaderConfiguration driverConfigLoaderConfiguration;

        @Nullable
        private String username;

        @Nullable
        private String password;

        @Nullable
        private Duration requestTimeout;

        @Nullable
        private Boolean sslEnabled;

        @Nullable
        private String cloudSecureConnectBundlePath;

        @Nullable
        private Class<?> loadBalancingPolicyClass;

        @Nullable
        private Class<?> retryPolicyClass;

        private boolean driverMetricsEnabled = true;



        private Builder() {
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public Builder hostPortConfigurations(final Set<HostPortConfiguration> hostPortConfigurations) {
            this.hostPortConfigurations = hostPortConfigurations;
            return this;
        }

        public Builder localDatacenterConfiguration(final LocalDatacenterConfiguration localDatacenterConfiguration) {
            this.localDatacenterConfiguration = localDatacenterConfiguration;
            return this;
        }

        public Builder keyspace(@Nullable final String keyspace) {
            this.keyspace = keyspace;
            return this;
        }

        public Builder driverConfigLoaderConfiguration(
                @Nullable final DriverConfigLoaderConfiguration driverConfigLoaderConfiguration)
        {
            this.driverConfigLoaderConfiguration = driverConfigLoaderConfiguration;
            return this;
        }

        public Builder username(@Nullable final String username) {
            this.username = username;
            return this;
        }

        public Builder password(@Nullable final String password) {
            this.password = password;
            return this;
        }

        public Builder requestTimeout(@Nullable final Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder sslEnabled(@Nullable final Boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
            return this;
        }

        public Builder cloudSecureConnectBundlePath(@Nullable final String cloudSecureConnectBundlePath) {
            this.cloudSecureConnectBundlePath = cloudSecureConnectBundlePath;
            return this;
        }

        public Builder loadBalancingPolicyClass(@Nullable final Class<?> loadBalancingPolicyClass) {
            this.loadBalancingPolicyClass = loadBalancingPolicyClass;
            return this;
        }

        public Builder retryPolicyClass(@Nullable final Class<?> retryPolicyClass) {
            this.retryPolicyClass = retryPolicyClass;
            return this;
        }

        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        public DefaultCassandraClientIntegrationServiceConfiguration build() {
            return new DefaultCassandraClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
