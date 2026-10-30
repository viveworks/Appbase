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

package com.viveworks.appbase.client.service.aerospike.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of {@link AerospikeClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAerospikeClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultAerospikeClientIntegrationServiceConfiguration
        implements AerospikeClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    @JsonSerialize(contentAs = AerospikeNodeConfiguration.class)
    @JsonDeserialize(contentAs = AerospikeNodeConfiguration.class)
    private final Set<AerospikeNodeConfiguration> aerospikeNodeConfigurations;

    // Connection
    private final Integer timeout;
    private final Integer loginTimeout;
    private final Integer maxConnsPerNode;
    private final Integer minConnsPerNode;
    private final Integer connPoolsPerNode;
    private final Integer maxSocketIdle;
    private final Integer tendInterval;
    private final Boolean failIfNotConnected;

    // Auth
    private final String user;
    private final String password;
    private final AuthMode authMode;

    // TLS
    private final Boolean tlsEnabled;

    // Read policies
    private final ReadModeSC readModeSC;
    private final ReadModeAP readModeAP;
    private final ReplicaPolicy replicaPolicy;

    // Rack awareness
    private final Boolean rackAware;
    private final List<Integer> rackIds;

    // Metrics
    private final boolean driverMetricsEnabled;

    private DefaultAerospikeClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(
                builder.clusterIdValueKeyConfiguration, "builder.clusterIdValueKeyConfiguration");
        AppbaseUtil.notNull(builder.aerospikeNodeConfigurations, "builder.aerospikeNodeConfigurations");
        AppbaseUtil.notEmpty(builder.aerospikeNodeConfigurations, "builder.aerospikeNodeConfigurations");

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        aerospikeNodeConfigurations = builder.aerospikeNodeConfigurations;
        timeout = builder.timeout;
        loginTimeout = builder.loginTimeout;
        maxConnsPerNode = builder.maxConnsPerNode;
        minConnsPerNode = builder.minConnsPerNode;
        connPoolsPerNode = builder.connPoolsPerNode;
        maxSocketIdle = builder.maxSocketIdle;
        tendInterval = builder.tendInterval;
        failIfNotConnected = builder.failIfNotConnected;
        user = builder.user;
        password = builder.password;
        authMode = builder.authMode;
        tlsEnabled = builder.tlsEnabled;
        readModeSC = builder.readModeSC;
        readModeAP = builder.readModeAP;
        replicaPolicy = builder.replicaPolicy;
        rackAware = builder.rackAware;
        rackIds = builder.rackIds;
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

    /**
     * Get the cluster ID value key configuration.
     *
     * @return cluster ID value key configuration
     */
    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    /**
     * Get the Aerospike node configurations.
     *
     * @return set of node configurations
     */
    public Set<AerospikeNodeConfiguration> getAerospikeNodeConfigurations() {
        return aerospikeNodeConfigurations;
    }

    /**
     * Initial host connection timeout in milliseconds. When null, the Aerospike default (1000ms) is used.
     *
     * @return timeout or null
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Login timeout in milliseconds. When null, the Aerospike default (5000ms) is used.
     *
     * @return login timeout or null
     */
    public Integer getLoginTimeout() {
        return loginTimeout;
    }

    /**
     * Maximum number of synchronous connections allowed per server node.
     * When null, the Aerospike default (300) is used.
     *
     * @return max connections per node or null
     */
    public Integer getMaxConnsPerNode() {
        return maxConnsPerNode;
    }

    /**
     * Minimum number of synchronous connections allowed per server node.
     * When null, the Aerospike default (0) is used.
     *
     * @return min connections per node or null
     */
    public Integer getMinConnsPerNode() {
        return minConnsPerNode;
    }

    /**
     * Number of synchronous connection pools used for each node.
     * When null, the Aerospike default (1) is used.
     *
     * @return connection pools per node or null
     */
    public Integer getConnPoolsPerNode() {
        return connPoolsPerNode;
    }

    /**
     * Maximum socket idle in seconds.
     * When null, the Aerospike default (55 seconds) is used.
     *
     * @return max socket idle or null
     */
    public Integer getMaxSocketIdle() {
        return maxSocketIdle;
    }

    /**
     * Interval in milliseconds between cluster tends by the maintenance thread.
     * When null, the Aerospike default (1000ms) is used.
     *
     * @return tend interval or null
     */
    public Integer getTendInterval() {
        return tendInterval;
    }

    /**
     * Whether to throw an exception if all seed connections fail on cluster instantiation.
     * When null, the Aerospike default (true) is used.
     *
     * @return fail-if-not-connected flag or null
     */
    public Boolean getFailIfNotConnected() {
        return failIfNotConnected;
    }

    /**
     * Username for cluster authentication. Leave null for clusters without restricted access.
     *
     * @return user or null
     */
    public String getUser() {
        return user;
    }

    /**
     * Password for cluster authentication. Leave null for clusters without restricted access.
     *
     * @return password or null
     */
    public String getPassword() {
        return password;
    }

    /**
     * Authentication mode. When null, the Aerospike default (INTERNAL) is used.
     *
     * @return auth mode or null
     */
    public AuthMode getAuthMode() {
        return authMode;
    }

    /**
     * Whether TLS is enabled. When null or false, plain sockets are used.
     *
     * @return TLS enabled flag or null
     */
    public Boolean getTlsEnabled() {
        return tlsEnabled;
    }

    /**
     * Read policy for SC (strong consistency) namespaces.
     * When null, the Aerospike default (SESSION) is used.
     *
     * @return read mode SC or null
     */
    public ReadModeSC getReadModeSC() {
        return readModeSC;
    }

    /**
     * Read policy for AP (availability) namespaces.
     * When null, the Aerospike default (ONE) is used.
     *
     * @return read mode AP or null
     */
    public ReadModeAP getReadModeAP() {
        return readModeAP;
    }

    /**
     * Replica algorithm used to determine the target node for a command.
     * When null, the Aerospike default (SEQUENCE) is used.
     *
     * @return replica policy or null
     */
    public ReplicaPolicy getReplicaPolicy() {
        return replicaPolicy;
    }

    /**
     * Whether rack awareness is enabled to direct reads to the same rack as the client.
     * When null, the Aerospike default (false) is used.
     *
     * @return rack aware flag or null
     */
    public Boolean getRackAware() {
        return rackAware;
    }

    /**
     * Rack IDs where this client instance resides. The first element is applied as the primary rack ID.
     * When null, the Aerospike default rack (0) is used.
     *
     * @return rack IDs or null
     */
    public List<Integer> getRackIds() {
        return rackIds;
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

        final DefaultAerospikeClientIntegrationServiceConfiguration that =
                (DefaultAerospikeClientIntegrationServiceConfiguration) o;
        return Objects.equals(
                clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(aerospikeNodeConfigurations, that.aerospikeNodeConfigurations) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(loginTimeout, that.loginTimeout) &&
                Objects.equals(maxConnsPerNode, that.maxConnsPerNode) &&
                Objects.equals(minConnsPerNode, that.minConnsPerNode) &&
                Objects.equals(connPoolsPerNode, that.connPoolsPerNode) &&
                Objects.equals(maxSocketIdle, that.maxSocketIdle) &&
                Objects.equals(tendInterval, that.tendInterval) &&
                Objects.equals(failIfNotConnected, that.failIfNotConnected) &&
                Objects.equals(user, that.user) &&
                Objects.equals(password, that.password) &&
                authMode == that.authMode &&
                Objects.equals(tlsEnabled, that.tlsEnabled) &&
                readModeSC == that.readModeSC &&
                readModeAP == that.readModeAP &&
                replicaPolicy == that.replicaPolicy &&
                Objects.equals(rackAware, that.rackAware) &&
                Objects.equals(rackIds, that.rackIds) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueKeyConfiguration,
                aerospikeNodeConfigurations,
                timeout,
                loginTimeout,
                maxConnsPerNode,
                minConnsPerNode,
                connPoolsPerNode,
                maxSocketIdle,
                tendInterval,
                failIfNotConnected,
                user,
                password,
                authMode,
                tlsEnabled,
                readModeSC,
                readModeAP,
                replicaPolicy,
                rackAware,
                rackIds,
                driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultAerospikeClientIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", aerospikeNodeConfigurations=" + aerospikeNodeConfigurations +
                ", timeout=" + timeout +
                ", loginTimeout=" + loginTimeout +
                ", maxConnsPerNode=" + maxConnsPerNode +
                ", minConnsPerNode=" + minConnsPerNode +
                ", connPoolsPerNode=" + connPoolsPerNode +
                ", maxSocketIdle=" + maxSocketIdle +
                ", tendInterval=" + tendInterval +
                ", failIfNotConnected=" + failIfNotConnected +
                ", user=" + user +
                ", password='[REDACTED]'" +
                ", authMode=" + authMode +
                ", tlsEnabled=" + tlsEnabled +
                ", readModeSC=" + readModeSC +
                ", readModeAP=" + readModeAP +
                ", replicaPolicy=" + replicaPolicy +
                ", rackAware=" + rackAware +
                ", rackIds=" + rackIds +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }

    /**
     * Builder for {@link DefaultAerospikeClientIntegrationServiceConfiguration}.
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
        private static final Set<AerospikeNodeConfiguration> DEFAULT_AEROSPIKE_NODE_CONFIGURATIONS = Set.of(
                AerospikeNodeConfiguration
                        .builder()
                        .hostnameOrIp("aerospike.local")
                        .port(3000)
                        .build());


        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration = DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;

        @JsonSerialize(contentAs = AerospikeNodeConfiguration.class)
        @JsonDeserialize(contentAs = AerospikeNodeConfiguration.class)
        private Set<AerospikeNodeConfiguration> aerospikeNodeConfigurations = DEFAULT_AEROSPIKE_NODE_CONFIGURATIONS;

        // Connection
        private Integer timeout = null;
        private Integer loginTimeout = null;
        private Integer maxConnsPerNode = null;
        private Integer minConnsPerNode = null;
        private Integer connPoolsPerNode = null;
        private Integer maxSocketIdle = null;
        private Integer tendInterval = null;
        private Boolean failIfNotConnected = null;

        // Auth
        private String user = null;
        private String password = null;
        private AuthMode authMode = null;

        // TLS
        private Boolean tlsEnabled = null;

        // Read policies
        private ReadModeSC readModeSC = null;
        private ReadModeAP readModeAP = null;
        private ReplicaPolicy replicaPolicy = null;

        // Rack awareness
        private Boolean rackAware = null;
        private List<Integer> rackIds = null;

        // Metrics
        private boolean driverMetricsEnabled = true;

        private Builder() {
        }

        /**
         * Set the cluster ID value key configuration.
         *
         * @param clusterIdValueKeyConfiguration cluster ID value key configuration
         * @return this builder
         */
        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        /**
         * Set the Aerospike node configurations.
         *
         * @param aerospikeNodeConfigurations set of node configurations
         * @return this builder
         */
        public Builder aerospikeNodeConfigurations(
                @JsonSerialize(contentAs = AerospikeNodeConfiguration.class)
                @JsonDeserialize(contentAs = AerospikeNodeConfiguration.class)
                final Set<AerospikeNodeConfiguration> aerospikeNodeConfigurations)
        {
            this.aerospikeNodeConfigurations = aerospikeNodeConfigurations;
            return this;
        }

        /**
         * Set the initial host connection timeout in milliseconds.
         *
         * @param timeout connection timeout in milliseconds
         * @return this builder
         */
        public Builder timeout(final Integer timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Set the login timeout in milliseconds.
         *
         * @param loginTimeout login timeout in milliseconds
         * @return this builder
         */
        public Builder loginTimeout(final Integer loginTimeout) {
            this.loginTimeout = loginTimeout;
            return this;
        }

        /**
         * Set the maximum number of synchronous connections per server node.
         *
         * @param maxConnsPerNode max connections per node
         * @return this builder
         */
        public Builder maxConnsPerNode(final Integer maxConnsPerNode) {
            this.maxConnsPerNode = maxConnsPerNode;
            return this;
        }

        /**
         * Set the minimum number of synchronous connections per server node.
         *
         * @param minConnsPerNode min connections per node
         * @return this builder
         */
        public Builder minConnsPerNode(final Integer minConnsPerNode) {
            this.minConnsPerNode = minConnsPerNode;
            return this;
        }

        /**
         * Set the number of synchronous connection pools per node.
         *
         * @param connPoolsPerNode connection pools per node
         * @return this builder
         */
        public Builder connPoolsPerNode(final Integer connPoolsPerNode) {
            this.connPoolsPerNode = connPoolsPerNode;
            return this;
        }

        /**
         * Set the maximum socket idle in seconds.
         *
         * @param maxSocketIdle max socket idle in seconds
         * @return this builder
         */
        public Builder maxSocketIdle(final Integer maxSocketIdle) {
            this.maxSocketIdle = maxSocketIdle;
            return this;
        }

        /**
         * Set the interval in milliseconds between cluster tends.
         *
         * @param tendInterval tend interval in milliseconds
         * @return this builder
         */
        public Builder tendInterval(final Integer tendInterval) {
            this.tendInterval = tendInterval;
            return this;
        }

        /**
         * Set whether to throw an exception if all seed connections fail on cluster instantiation.
         *
         * @param failIfNotConnected fail-if-not-connected flag
         * @return this builder
         */
        public Builder failIfNotConnected(final Boolean failIfNotConnected) {
            this.failIfNotConnected = failIfNotConnected;
            return this;
        }

        /**
         * Set the username for cluster authentication.
         *
         * @param user username
         * @return this builder
         */
        public Builder user(final String user) {
            this.user = user;
            return this;
        }

        /**
         * Set the password for cluster authentication.
         *
         * @param password password
         * @return this builder
         */
        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        /**
         * Set the authentication mode.
         *
         * @param authMode auth mode
         * @return this builder
         */
        public Builder authMode(final AuthMode authMode) {
            this.authMode = authMode;
            return this;
        }

        /**
         * Set whether TLS is enabled for connections.
         *
         * @param tlsEnabled TLS enabled flag
         * @return this builder
         */
        public Builder tlsEnabled(final Boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
            return this;
        }

        /**
         * Set the read policy for SC (strong consistency) namespaces.
         *
         * @param readModeSC read mode SC
         * @return this builder
         */
        public Builder readModeSC(final ReadModeSC readModeSC) {
            this.readModeSC = readModeSC;
            return this;
        }

        /**
         * Set the read policy for AP (availability) namespaces.
         *
         * @param readModeAP read mode AP
         * @return this builder
         */
        public Builder readModeAP(final ReadModeAP readModeAP) {
            this.readModeAP = readModeAP;
            return this;
        }

        /**
         * Set the replica algorithm used to determine the target node for a command.
         *
         * @param replicaPolicy replica policy
         * @return this builder
         */
        public Builder replicaPolicy(final ReplicaPolicy replicaPolicy) {
            this.replicaPolicy = replicaPolicy;
            return this;
        }

        /**
         * Set whether rack awareness is enabled.
         *
         * @param rackAware rack aware flag
         * @return this builder
         */
        public Builder rackAware(final Boolean rackAware) {
            this.rackAware = rackAware;
            return this;
        }

        /**
         * Set the rack IDs where this client instance resides.
         * The first element is applied as the primary rack ID to the Aerospike client policy.
         *
         * @param rackIds list of rack IDs
         * @return this builder
         */
        public Builder rackIds(final List<Integer> rackIds) {
            this.rackIds = rackIds;
            return this;
        }

        /**
         * Set whether Aerospike driver-native metrics should be routed onto Appbase's shared
         * Micrometer composite registry (→ JMX). Defaults to {@code true}.
         *
         * @param driverMetricsEnabled driver metrics enabled flag
         * @return this builder
         */
        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        /**
         * Build the configuration.
         *
         * @return built configuration
         */
        public DefaultAerospikeClientIntegrationServiceConfiguration build() {
            return new DefaultAerospikeClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
