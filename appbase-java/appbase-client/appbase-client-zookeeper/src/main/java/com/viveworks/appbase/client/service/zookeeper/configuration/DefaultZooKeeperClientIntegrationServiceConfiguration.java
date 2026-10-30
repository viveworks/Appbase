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

package com.viveworks.appbase.client.service.zookeeper.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryForeverConfiguration;
import com.viveworks.appbase.client.service.zookeeper.configuration.retry.RetryPolicyConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NotNullWhenEnabled;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link ZooKeeperClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultZooKeeperClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultZooKeeperClientIntegrationServiceConfiguration
        implements AppbaseClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    private final Set<HostPortConfiguration> hostPortConfigurations;

    private final boolean disableBlockUntilConnected;

    @NotNullWhenEnabled(dependsOn = "disableBlockUntilConnected")
    private final TimeoutConfiguration blockUntilConnectedTimeoutConfiguration;

    private final TimeoutConfiguration connectionTimeoutConfiguration;

    private final TimeoutConfiguration sessionTimeoutConfiguration;

    private final TimeoutConfiguration maxCloseWaitTimeoutConfiguration;

    private final TimeoutConfiguration waitForShutdownTimeoutConfiguration;

    private final RetryPolicyConfiguration retryPolicyConfiguration;

    private final boolean canBeReadOnly;

    private final int simulatedSessionExpirationPercent;

    private final boolean ensembleTracker;

    private final boolean useContainerParents;

    @Nullable
    private final String namespace;

    @Nullable
    private final String defaultDataUtf8;

    @JsonSerialize(contentAs = AuthInfoConfiguration.class)
    @JsonDeserialize(contentAs = AuthInfoConfiguration.class)
    @Nullable
    private final List<AuthInfoConfiguration> authInfoList;

    @Nullable
    private final Class<?> aclProviderClass;

    @Nullable
    private final Class<?> compressionProviderClass;

    @Nullable
    private final Class<?> dnsResolverClass;

    @Nullable
    private final ConnectionHandlingPolicyType connectionHandlingPolicy;

    @Nullable
    private final Duration sessionCreationTimeout;

    // Metrics
    private final boolean driverMetricsEnabled;



    private DefaultZooKeeperClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.clusterIdValueKeyConfiguration, "builder.clusterIdValueKeyConfiguration");
        AppbaseUtil.notEmpty(builder.hostPortConfigurations, "builder.hostPortConfigurations");
        AppbaseUtil.notNull(builder.connectionTimeoutConfiguration, "builder.connectionTimeoutConfiguration");
        AppbaseUtil.notNull(builder.sessionTimeoutConfiguration, "builder.sessionTimeoutConfiguration");
        AppbaseUtil.notNull(builder.maxCloseWaitTimeoutConfiguration, "builder.maxCloseWaitTimeoutConfiguration");
        AppbaseUtil.notNull(builder.waitForShutdownTimeoutConfiguration, "builder.waitForShutdownTimeoutConfiguration");
        AppbaseUtil.notNull(builder.retryPolicyConfiguration, "builder.retryPolicyConfiguration");

        if (!builder.disableBlockUntilConnected) {
            AppbaseUtil.notNull(
                    builder.blockUntilConnectedTimeoutConfiguration,
                    "builder.blockUntilConnectedTimeoutConfiguration");
        }

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        hostPortConfigurations = builder.hostPortConfigurations;
        disableBlockUntilConnected = builder.disableBlockUntilConnected;
        blockUntilConnectedTimeoutConfiguration = builder.blockUntilConnectedTimeoutConfiguration;
        connectionTimeoutConfiguration = builder.connectionTimeoutConfiguration;
        sessionTimeoutConfiguration = builder.sessionTimeoutConfiguration;
        maxCloseWaitTimeoutConfiguration = builder.maxCloseWaitTimeoutConfiguration;
        waitForShutdownTimeoutConfiguration = builder.waitForShutdownTimeoutConfiguration;
        retryPolicyConfiguration = builder.retryPolicyConfiguration;
        canBeReadOnly = builder.canBeReadOnly;
        simulatedSessionExpirationPercent = builder.simulatedSessionExpirationPercent;
        ensembleTracker = builder.ensembleTracker;
        useContainerParents = builder.useContainerParents;
        namespace = builder.namespace;
        defaultDataUtf8 = builder.defaultDataUtf8;
        authInfoList = builder.authInfoList;
        aclProviderClass = builder.aclProviderClass;
        compressionProviderClass = builder.compressionProviderClass;
        dnsResolverClass = builder.dnsResolverClass;
        connectionHandlingPolicy = builder.connectionHandlingPolicy;
        sessionCreationTimeout = builder.sessionCreationTimeout;
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

    public boolean isDisableBlockUntilConnected() {
        return disableBlockUntilConnected;
    }

    @NotNullWhenEnabled(dependsOn = "disableBlockUntilConnected")
    public TimeoutConfiguration getBlockUntilConnectedTimeoutConfiguration() {
        return blockUntilConnectedTimeoutConfiguration;
    }

    public TimeoutConfiguration getConnectionTimeoutConfiguration() {
        return connectionTimeoutConfiguration;
    }

    public TimeoutConfiguration getSessionTimeoutConfiguration() {
        return sessionTimeoutConfiguration;
    }

    public TimeoutConfiguration getMaxCloseWaitTimeoutConfiguration() {
        return maxCloseWaitTimeoutConfiguration;
    }

    public TimeoutConfiguration getWaitForShutdownTimeoutConfiguration() {
        return waitForShutdownTimeoutConfiguration;
    }

    public RetryPolicyConfiguration getRetryPolicyConfiguration() {
        return retryPolicyConfiguration;
    }

    public boolean isCanBeReadOnly() {
        return canBeReadOnly;
    }

    public int getSimulatedSessionExpirationPercent() {
        return simulatedSessionExpirationPercent;
    }

    public boolean isEnsembleTracker() {
        return ensembleTracker;
    }

    public boolean isUseContainerParents() {
        return useContainerParents;
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @Nullable
    public String getDefaultDataUtf8() {
        return defaultDataUtf8;
    }

    @Nullable
    public List<AuthInfoConfiguration> getAuthInfoList() {
        return authInfoList;
    }

    @Nullable
    public Class<?> getAclProviderClass() {
        return aclProviderClass;
    }

    @Nullable
    public Class<?> getCompressionProviderClass() {
        return compressionProviderClass;
    }

    @Nullable
    public Class<?> getDnsResolverClass() {
        return dnsResolverClass;
    }

    @Nullable
    public ConnectionHandlingPolicyType getConnectionHandlingPolicy() {
        return connectionHandlingPolicy;
    }

    @Nullable
    public Duration getSessionCreationTimeout() {
        return sessionCreationTimeout;
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

        final DefaultZooKeeperClientIntegrationServiceConfiguration that =
                (DefaultZooKeeperClientIntegrationServiceConfiguration) o;
        return canBeReadOnly == that.canBeReadOnly &&
                simulatedSessionExpirationPercent == that.simulatedSessionExpirationPercent &&
                ensembleTracker == that.ensembleTracker &&
                useContainerParents == that.useContainerParents &&
                disableBlockUntilConnected == that.disableBlockUntilConnected &&
                Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(hostPortConfigurations, that.hostPortConfigurations) &&
                Objects.equals(blockUntilConnectedTimeoutConfiguration, that.blockUntilConnectedTimeoutConfiguration) &&
                Objects.equals(connectionTimeoutConfiguration, that.connectionTimeoutConfiguration) &&
                Objects.equals(sessionTimeoutConfiguration, that.sessionTimeoutConfiguration) &&
                Objects.equals(maxCloseWaitTimeoutConfiguration, that.maxCloseWaitTimeoutConfiguration) &&
                Objects.equals(waitForShutdownTimeoutConfiguration, that.waitForShutdownTimeoutConfiguration) &&
                Objects.equals(retryPolicyConfiguration, that.retryPolicyConfiguration) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(defaultDataUtf8, that.defaultDataUtf8) &&
                Objects.equals(authInfoList, that.authInfoList) &&
                Objects.equals(aclProviderClass, that.aclProviderClass) &&
                Objects.equals(compressionProviderClass, that.compressionProviderClass) &&
                Objects.equals(dnsResolverClass, that.dnsResolverClass) &&
                Objects.equals(connectionHandlingPolicy, that.connectionHandlingPolicy) &&
                Objects.equals(sessionCreationTimeout, that.sessionCreationTimeout) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueKeyConfiguration,
                hostPortConfigurations,
                blockUntilConnectedTimeoutConfiguration,
                disableBlockUntilConnected,
                connectionTimeoutConfiguration,
                sessionTimeoutConfiguration,
                maxCloseWaitTimeoutConfiguration,
                waitForShutdownTimeoutConfiguration,
                retryPolicyConfiguration,
                canBeReadOnly,
                simulatedSessionExpirationPercent,
                ensembleTracker,
                useContainerParents,
                namespace,
                defaultDataUtf8,
                authInfoList,
                aclProviderClass,
                compressionProviderClass,
                dnsResolverClass,
                connectionHandlingPolicy,
                sessionCreationTimeout,
                driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultZooKeeperClientIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", hostPortConfigurations=" + hostPortConfigurations +
                ", disableBlockUntilConnected=" + disableBlockUntilConnected +
                ", blockUntilConnectedTimeoutConfiguration=" + blockUntilConnectedTimeoutConfiguration +
                ", connectionTimeoutConfiguration=" + connectionTimeoutConfiguration +
                ", sessionTimeoutConfiguration=" + sessionTimeoutConfiguration +
                ", maxCloseWaitTimeoutConfiguration=" + maxCloseWaitTimeoutConfiguration +
                ", waitForShutdownTimeoutConfiguration=" + waitForShutdownTimeoutConfiguration +
                ", retryPolicyConfiguration=" + retryPolicyConfiguration +
                ", canBeReadOnly=" + canBeReadOnly +
                ", simulatedSessionExpirationPercent=" + simulatedSessionExpirationPercent +
                ", ensembleTracker=" + ensembleTracker +
                ", useContainerParents=" + useContainerParents +
                ", namespace='" + namespace + '\'' +
                ", defaultDataUtf8='" + defaultDataUtf8 + '\'' +
                ", authInfoList=" + authInfoList +
                ", aclProviderClass=" + aclProviderClass +
                ", compressionProviderClass=" + compressionProviderClass +
                ", dnsResolverClass=" + dnsResolverClass +
                ", connectionHandlingPolicy=" + connectionHandlingPolicy +
                ", sessionCreationTimeout=" + sessionCreationTimeout +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }

    /**
     * Builder for {@link DefaultZooKeeperClientIntegrationServiceConfiguration}.
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
        private static final Set<HostPortConfiguration> DEFAULT_HOST_PORT_CONFIGURATIONS =
                Set.of(HostPortConfiguration.builder().host("zookeeper.local").port(2181).build());
        private static final boolean DEFAULT_DISABLE_BLOCK_UNTIL_CONNECTED = false;
        private static final TimeoutConfiguration DEFAULT_BLOCK_UNTIL_CONNECTED_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(120).timeoutTimeUnit(TimeUnit.SECONDS).build();
        private static final TimeoutConfiguration DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(15).timeoutTimeUnit(TimeUnit.SECONDS).build();
        private static final TimeoutConfiguration DEFAULT_SESSION_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(60).timeoutTimeUnit(TimeUnit.SECONDS).build();
        private static final TimeoutConfiguration DEFAULT_MAX_CLOSE_WAIT_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(1).timeoutTimeUnit(TimeUnit.MILLISECONDS).build();
        private static final TimeoutConfiguration DEFAULT_WAIT_FOR_SHUTDOWN_TIMEOUT_CONFIGURATION =
                TimeoutConfiguration.builder().enabled(true).timeout(0).timeoutTimeUnit(TimeUnit.MILLISECONDS).build();
        private static final RetryPolicyConfiguration DEFAULT_RETRY_POLICY_CONFIGURATION =
                RetryForeverConfiguration.builder().retryIntervalMillis(5000).build();
        private static final boolean DEFAULT_CAN_BE_READ_ONLY = true;
        private static final int DEFAULT_SIMULATED_SESSION_EXPIRATION_PERCENT = 100;
        private static final boolean DEFAULT_ENSEMBLE_TRACKER = true;
        private static final boolean DEFAULT_USE_CONTAINER_PARENTS = false;



        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration = DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;

        private Set<HostPortConfiguration> hostPortConfigurations = DEFAULT_HOST_PORT_CONFIGURATIONS;

        private boolean disableBlockUntilConnected = DEFAULT_DISABLE_BLOCK_UNTIL_CONNECTED;

        @NotNullWhenEnabled(dependsOn = "disableBlockUntilConnected")
        private TimeoutConfiguration blockUntilConnectedTimeoutConfiguration =
                DEFAULT_BLOCK_UNTIL_CONNECTED_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration connectionTimeoutConfiguration = DEFAULT_CONNECTION_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration sessionTimeoutConfiguration = DEFAULT_SESSION_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration maxCloseWaitTimeoutConfiguration = DEFAULT_MAX_CLOSE_WAIT_TIMEOUT_CONFIGURATION;

        private TimeoutConfiguration waitForShutdownTimeoutConfiguration = DEFAULT_WAIT_FOR_SHUTDOWN_TIMEOUT_CONFIGURATION;

        private RetryPolicyConfiguration retryPolicyConfiguration = DEFAULT_RETRY_POLICY_CONFIGURATION;

        private boolean canBeReadOnly = DEFAULT_CAN_BE_READ_ONLY;

        private int simulatedSessionExpirationPercent = DEFAULT_SIMULATED_SESSION_EXPIRATION_PERCENT;

        private boolean ensembleTracker = DEFAULT_ENSEMBLE_TRACKER;

        private boolean useContainerParents = DEFAULT_USE_CONTAINER_PARENTS;

        @Nullable
        private String namespace;

        @Nullable
        private String defaultDataUtf8;

        @JsonSerialize(contentAs = AuthInfoConfiguration.class)
        @JsonDeserialize(contentAs = AuthInfoConfiguration.class)
        @Nullable
        private List<AuthInfoConfiguration> authInfoList;

        @Nullable
        private Class<?> aclProviderClass;

        @Nullable
        private Class<?> compressionProviderClass;

        @Nullable
        private Class<?> dnsResolverClass;

        @Nullable
        private ConnectionHandlingPolicyType connectionHandlingPolicy;

        @Nullable
        private Duration sessionCreationTimeout;

        // Metrics
        private boolean driverMetricsEnabled = true;



        private Builder() {
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public Builder disableBlockUntilConnected(final boolean disableBlockUntilConnected) {
            this.disableBlockUntilConnected = disableBlockUntilConnected;
            return this;
        }

        public Builder blockUntilConnectedTimeoutConfiguration(
                @NotNullWhenEnabled(dependsOn = "disableBlockUntilConnected")
                final TimeoutConfiguration blockUntilConnectedTimeoutConfiguration)
        {
            this.blockUntilConnectedTimeoutConfiguration = blockUntilConnectedTimeoutConfiguration;
            return this;
        }

        public Builder hostPortConfigurations(final Set<HostPortConfiguration> hostPortConfigurations) {
            this.hostPortConfigurations = hostPortConfigurations;
            return this;
        }

        public Builder connectionTimeoutConfiguration(final TimeoutConfiguration connectionTimeoutConfiguration) {
            this.connectionTimeoutConfiguration = connectionTimeoutConfiguration;
            return this;
        }

        public Builder sessionTimeoutConfiguration(final TimeoutConfiguration sessionTimeoutConfiguration) {
            this.sessionTimeoutConfiguration = sessionTimeoutConfiguration;
            return this;
        }

        public Builder maxCloseWaitTimeoutConfiguration(final TimeoutConfiguration maxCloseWaitTimeoutConfiguration) {
            this.maxCloseWaitTimeoutConfiguration = maxCloseWaitTimeoutConfiguration;
            return this;
        }

        public Builder waitForShutdownTimeoutConfiguration(final TimeoutConfiguration waitForShutdownTimeoutConfiguration) {
            this.waitForShutdownTimeoutConfiguration = waitForShutdownTimeoutConfiguration;
            return this;
        }

        public Builder retryPolicyConfiguration(final RetryPolicyConfiguration retryPolicyConfiguration) {
            this.retryPolicyConfiguration = retryPolicyConfiguration;
            return this;
        }

        public Builder canBeReadOnly(final boolean canBeReadOnly) {
            this.canBeReadOnly = canBeReadOnly;
            return this;
        }

        public Builder simulatedSessionExpirationPercent(final int simulatedSessionExpirationPercent) {
            this.simulatedSessionExpirationPercent = simulatedSessionExpirationPercent;
            return this;
        }

        public Builder ensembleTracker(final boolean ensembleTracker) {
            this.ensembleTracker = ensembleTracker;
            return this;
        }

        public Builder useContainerParents(final boolean useContainerParents) {
            this.useContainerParents = useContainerParents;
            return this;
        }

        public Builder namespace(@Nullable final String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder defaultDataUtf8(@Nullable final String defaultDataUtf8) {
            this.defaultDataUtf8 = defaultDataUtf8;
            return this;
        }

        public Builder authInfoList(@Nullable final List<AuthInfoConfiguration> authInfoList) {
            this.authInfoList = authInfoList;
            return this;
        }

        public Builder aclProviderClass(@Nullable final Class<?> aclProviderClass) {
            this.aclProviderClass = aclProviderClass;
            return this;
        }

        public Builder compressionProviderClass(@Nullable final Class<?> compressionProviderClass) {
            this.compressionProviderClass = compressionProviderClass;
            return this;
        }

        public Builder dnsResolverClass(@Nullable final Class<?> dnsResolverClass) {
            this.dnsResolverClass = dnsResolverClass;
            return this;
        }

        public Builder connectionHandlingPolicy(@Nullable final ConnectionHandlingPolicyType connectionHandlingPolicy) {
            this.connectionHandlingPolicy = connectionHandlingPolicy;
            return this;
        }

        public Builder sessionCreationTimeout(@Nullable final Duration sessionCreationTimeout) {
            this.sessionCreationTimeout = sessionCreationTimeout;
            return this;
        }

        /**
         * Set whether the Curator connection-state should be bridged onto Appbase's shared
         * Micrometer composite registry (→ JMX). Defaults to {@code true}.
         *
         * @param driverMetricsEnabled driver metrics enabled flag
         * @return this builder
         */
        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        public DefaultZooKeeperClientIntegrationServiceConfiguration build() {
            return new DefaultZooKeeperClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
