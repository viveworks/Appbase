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

package com.viveworks.appbase.client.service.kafka.consumer.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.kafka.configuration.AutoOffsetReset;
import com.viveworks.appbase.client.service.kafka.configuration.ClientDnsLookup;
import com.viveworks.appbase.client.service.kafka.configuration.GroupProtocol;
import com.viveworks.appbase.client.service.kafka.configuration.IsolationLevel;
import com.viveworks.appbase.client.service.kafka.configuration.MetricRecordingLevel;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.service.kafka.consumer.DefaultKafkaConsumerClientIntegrationService;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultKafkaConsumerClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 * @see <a href="https://kafka.apache.org/081/documentation.html#consumerconfigs">Kafka Consumer Properties</a>
 */
@Immutable
@JsonDeserialize(builder = DefaultKafkaConsumerClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultKafkaConsumerClientIntegrationServiceConfiguration
        implements KafkaConsumerClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueConfiguration;

    private final Class<? extends Deserializer> keyDeserializerType;

    private final Class<? extends Deserializer> valueDeserializerType;

    @JsonSerialize(contentAs = HostPortConfiguration.class)
    @JsonDeserialize(contentAs = HostPortConfiguration.class)
    private final Set<HostPortConfiguration> bootstrapServerConfigurations;

    private final String groupId;

    @Nullable
    private final DataSizeConfiguration fetchMinDataSizeConfiguration;

    @Nullable
    private final TimeUnitConfiguration heartbeatIntervalConfiguration;

    @Nullable
    private final DataSizeConfiguration maxPartitionFetchSizeConfiguration;

    @Nullable
    private final TimeoutConfiguration sessionTimeoutConfiguration;

    @Nullable
    private final Boolean allowAutoCreateTopics;

    @Nullable
    private final AutoOffsetReset autoOffsetReset;

    @Nullable
    private final ClientDnsLookup clientDnsLookup;

    @Nullable
    private final TimeUnitConfiguration connectionsMaxIdleConfiguration;

    @Nullable
    private final TimeoutConfiguration defaultApiTimeoutConfiguration;

    @Nullable
    private final Boolean enableAutoCommit;

    @Nullable
    private final Boolean excludeInternalTopics;

    @Nullable
    private final DataSizeConfiguration fetchMaxDataSizeConfiguration;

    @Nullable
    private final IsolationLevel isolationLevel;

    @Nullable
    private final TimeUnitConfiguration maxPollIntervalConfiguration;

    @Nullable
    private final Integer maxPollRecords;

    @Nullable
    @JsonSerialize(contentAs = Class.class)
    @JsonDeserialize(contentAs = Class.class)
    private final List<Class<? extends ConsumerPartitionAssignor>> partitionAssignmentStrategyTypes;

    @Nullable
    private final DataSizeConfiguration receiveBufferSizeConfiguration;

    @Nullable
    private final TimeoutConfiguration requestTimeoutConfiguration;

    @Nullable
    private final DataSizeConfiguration sendBufferSizeConfiguration;

    @Nullable
    private final TimeUnitConfiguration autoCommitIntervalConfiguration;

    @Nullable
    private final Boolean checkCrcs;

    @Nullable
    private final TimeUnitConfiguration fetchMaxWaitConfiguration;

    @Nullable
    @JsonSerialize(contentAs = Class.class)
    @JsonDeserialize(contentAs = Class.class)
    private final List<Class<? extends ConsumerInterceptor>> interceptorTypes;

    @Nullable
    private final TimeUnitConfiguration metadataMaxAgeConfiguration;

    @Nullable
    @JsonSerialize(contentAs = Class.class)
    @JsonDeserialize(contentAs = Class.class)
    private final List<Class<? extends MetricsReporter>> metricsReporterTypes;

    @Nullable
    private final Integer metricsNumSamples;

    @Nullable
    private final MetricRecordingLevel metricsRecordingLevel;

    @Nullable
    private final TimeUnitConfiguration metricsSampleWindowConfiguration;

    @Nullable
    private final TimeUnitConfiguration reconnectBackoffMaxConfiguration;

    @Nullable
    private final TimeUnitConfiguration reconnectBackoffConfiguration;

    @Nullable
    private final TimeUnitConfiguration retryBackoffConfiguration;

    @Nullable
    private final String clientId;

    @Nullable
    private final String clientRack;

    @Nullable
    private final String groupInstanceId;

    @Nullable
    private final Duration closeDuration;

    @Nullable
    private final SecurityProtocol securityProtocol;

    @Nullable
    private final Long socketConnectionSetupTimeoutMs;

    @Nullable
    private final Long socketConnectionSetupTimeoutMaxMs;

    @Nullable
    private final GroupProtocol groupProtocol;

    @Nullable
    private final Long retryBackoffMaxMs;

    @Nullable
    private final List<String> subscriptionTopics;

    private final boolean driverMetricsEnabled;



    private DefaultKafkaConsumerClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.clusterIdValueConfiguration, "builder.clusterIdValueConfiguration");
        AppbaseUtil.notNull(builder.keyDeserializerType, "builder.keyDeserializerType");
        AppbaseUtil.notNull(builder.valueDeserializerType, "builder.valueDeserializerType");
        AppbaseUtil.notNull(builder.bootstrapServerConfigurations, "builder.bootstrapServerConfigurations");
        AppbaseUtil.notEmpty(builder.bootstrapServerConfigurations, "builder.bootstrapServerConfigurations");
        AppbaseUtil.notNull(builder.groupId, "builder.groupId");

        clusterIdValueConfiguration = builder.clusterIdValueConfiguration;
        keyDeserializerType = builder.keyDeserializerType;
        valueDeserializerType = builder.valueDeserializerType;
        bootstrapServerConfigurations = builder.bootstrapServerConfigurations;
        fetchMinDataSizeConfiguration = builder.fetchMinDataSizeConfiguration;
        groupId = builder.groupId;
        heartbeatIntervalConfiguration = builder.heartbeatIntervalConfiguration;
        maxPartitionFetchSizeConfiguration = builder.maxPartitionFetchSizeConfiguration;
        sessionTimeoutConfiguration = builder.sessionTimeoutConfiguration;
        allowAutoCreateTopics = builder.allowAutoCreateTopics;
        autoOffsetReset = builder.autoOffsetReset;
        clientDnsLookup = builder.clientDnsLookup;
        connectionsMaxIdleConfiguration = builder.connectionsMaxIdleConfiguration;
        defaultApiTimeoutConfiguration = builder.defaultApiTimeoutConfiguration;
        enableAutoCommit = builder.enableAutoCommit;
        excludeInternalTopics = builder.excludeInternalTopics;
        fetchMaxDataSizeConfiguration = builder.fetchMaxDataSizeConfiguration;
        isolationLevel = builder.isolationLevel;
        maxPollIntervalConfiguration = builder.maxPollIntervalConfiguration;
        maxPollRecords = builder.maxPollRecords;
        partitionAssignmentStrategyTypes = builder.partitionAssignmentStrategyTypes;
        receiveBufferSizeConfiguration = builder.receiveBufferSizeConfiguration;
        requestTimeoutConfiguration = builder.requestTimeoutConfiguration;
        sendBufferSizeConfiguration = builder.sendBufferSizeConfiguration;
        autoCommitIntervalConfiguration = builder.autoCommitIntervalConfiguration;
        checkCrcs = builder.checkCrcs;
        fetchMaxWaitConfiguration = builder.fetchMaxWaitConfiguration;
        interceptorTypes = builder.interceptorTypes;
        metadataMaxAgeConfiguration = builder.metadataMaxAgeConfiguration;
        metricsReporterTypes = builder.metricsReporterTypes;
        metricsNumSamples = builder.metricsNumSamples;
        metricsRecordingLevel = builder.metricsRecordingLevel;
        metricsSampleWindowConfiguration = builder.metricsSampleWindowConfiguration;
        reconnectBackoffMaxConfiguration = builder.reconnectBackoffMaxConfiguration;
        reconnectBackoffConfiguration = builder.reconnectBackoffConfiguration;
        retryBackoffConfiguration = builder.retryBackoffConfiguration;
        clientId = builder.clientId;
        clientRack = builder.clientRack;
        groupInstanceId = builder.groupInstanceId;
        closeDuration = builder.closeDuration;
        securityProtocol = builder.securityProtocol;
        socketConnectionSetupTimeoutMs = builder.socketConnectionSetupTimeoutMs;
        socketConnectionSetupTimeoutMaxMs = builder.socketConnectionSetupTimeoutMaxMs;
        groupProtocol = builder.groupProtocol;
        retryBackoffMaxMs = builder.retryBackoffMaxMs;
        subscriptionTopics = builder.subscriptionTopics;
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

    public ClusterIdValueConfiguration getClusterIdValueConfiguration() {
        return clusterIdValueConfiguration;
    }

    public Class<? extends Deserializer> getKeyDeserializerType() {
        return keyDeserializerType;
    }

    public Class<? extends Deserializer> getValueDeserializerType() {
        return valueDeserializerType;
    }

    public Set<HostPortConfiguration> getBootstrapServerConfigurations() {
        return bootstrapServerConfigurations;
    }

    public String getGroupId() {
        return groupId;
    }

    @Nullable
    public DataSizeConfiguration getFetchMinDataSizeConfiguration() {
        return fetchMinDataSizeConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getHeartbeatIntervalConfiguration() {
        return heartbeatIntervalConfiguration;
    }

    @Nullable
    public DataSizeConfiguration getMaxPartitionFetchSizeConfiguration() {
        return maxPartitionFetchSizeConfiguration;
    }

    @Nullable
    public TimeoutConfiguration getSessionTimeoutConfiguration() {
        return sessionTimeoutConfiguration;
    }

    @Nullable
    public Boolean getAllowAutoCreateTopics() {
        return allowAutoCreateTopics;
    }

    @Nullable
    public AutoOffsetReset getAutoOffsetReset() {
        return autoOffsetReset;
    }

    @Nullable
    public ClientDnsLookup getClientDnsLookup() {
        return clientDnsLookup;
    }

    @Nullable
    public TimeUnitConfiguration getConnectionsMaxIdleConfiguration() {
        return connectionsMaxIdleConfiguration;
    }

    @Nullable
    public TimeoutConfiguration getDefaultApiTimeoutConfiguration() {
        return defaultApiTimeoutConfiguration;
    }

    @Nullable
    public Boolean getEnableAutoCommit() {
        return enableAutoCommit;
    }

    @Nullable
    public Boolean getExcludeInternalTopics() {
        return excludeInternalTopics;
    }

    @Nullable
    public DataSizeConfiguration getFetchMaxDataSizeConfiguration() {
        return fetchMaxDataSizeConfiguration;
    }

    @Nullable
    public IsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    @Nullable
    public TimeUnitConfiguration getMaxPollIntervalConfiguration() {
        return maxPollIntervalConfiguration;
    }

    @Nullable
    public Integer getMaxPollRecords() {
        return maxPollRecords;
    }

    @Nullable
    public List<Class<? extends ConsumerPartitionAssignor>> getPartitionAssignmentStrategyTypes() {
        return partitionAssignmentStrategyTypes;
    }

    @Nullable
    public DataSizeConfiguration getReceiveBufferSizeConfiguration() {
        return receiveBufferSizeConfiguration;
    }

    @Nullable
    public TimeoutConfiguration getRequestTimeoutConfiguration() {
        return requestTimeoutConfiguration;
    }

    @Nullable
    public DataSizeConfiguration getSendBufferSizeConfiguration() {
        return sendBufferSizeConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getAutoCommitIntervalConfiguration() {
        return autoCommitIntervalConfiguration;
    }

    @Nullable
    public Boolean getCheckCrcs() {
        return checkCrcs;
    }

    @Nullable
    public TimeUnitConfiguration getFetchMaxWaitConfiguration() {
        return fetchMaxWaitConfiguration;
    }

    @Nullable
    public List<Class<? extends ConsumerInterceptor>> getInterceptorTypes() {
        return interceptorTypes;
    }

    @Nullable
    public TimeUnitConfiguration getMetadataMaxAgeConfiguration() {
        return metadataMaxAgeConfiguration;
    }

    @Nullable
    public List<Class<? extends MetricsReporter>> getMetricsReporterTypes() {
        return metricsReporterTypes;
    }

    @Nullable
    public Integer getMetricsNumSamples() {
        return metricsNumSamples;
    }

    @Nullable
    public MetricRecordingLevel getMetricsRecordingLevel() {
        return metricsRecordingLevel;
    }

    @Nullable
    public TimeUnitConfiguration getMetricsSampleWindowConfiguration() {
        return metricsSampleWindowConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getReconnectBackoffMaxConfiguration() {
        return reconnectBackoffMaxConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getReconnectBackoffConfiguration() {
        return reconnectBackoffConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getRetryBackoffConfiguration() {
        return retryBackoffConfiguration;
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }

    @Nullable
    public String getClientRack() {
        return clientRack;
    }

    @Nullable
    public String getGroupInstanceId() {
        return groupInstanceId;
    }

    @Nullable
    public Duration getCloseDuration() {
        return closeDuration;
    }

    @Nullable
    public SecurityProtocol getSecurityProtocol() {
        return securityProtocol;
    }

    @Nullable
    public Long getSocketConnectionSetupTimeoutMs() {
        return socketConnectionSetupTimeoutMs;
    }

    @Nullable
    public Long getSocketConnectionSetupTimeoutMaxMs() {
        return socketConnectionSetupTimeoutMaxMs;
    }

    @Nullable
    public GroupProtocol getGroupProtocol() {
        return groupProtocol;
    }

    @Nullable
    public Long getRetryBackoffMaxMs() {
        return retryBackoffMaxMs;
    }

    /**
     * The topics the reactive receiver subscribes to. Nullable (defaults to {@code null}); consumed
     * by the client service when building the reactor-kafka {@code ReceiverOptions.subscription(...)}
     * for the reactive accessor. Not part of the synchronous driver {@link java.util.Properties} (topic
     * subscription is an API call on the consumer, not a config property).
     *
     * @return Subscription topics, or {@code null} if none configured
     */
    @Override
    @Nullable
    public List<String> getSubscriptionTopics() {
        return subscriptionTopics;
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

        final DefaultKafkaConsumerClientIntegrationServiceConfiguration that =
                (DefaultKafkaConsumerClientIntegrationServiceConfiguration) o;
        return Objects.equals(clusterIdValueConfiguration, that.clusterIdValueConfiguration) &&
                Objects.equals(keyDeserializerType, that.keyDeserializerType) &&
                Objects.equals(valueDeserializerType, that.valueDeserializerType) &&
                Objects.equals(bootstrapServerConfigurations, that.bootstrapServerConfigurations) &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(fetchMinDataSizeConfiguration, that.fetchMinDataSizeConfiguration) &&
                Objects.equals(heartbeatIntervalConfiguration, that.heartbeatIntervalConfiguration) &&
                Objects.equals(maxPartitionFetchSizeConfiguration, that.maxPartitionFetchSizeConfiguration) &&
                Objects.equals(sessionTimeoutConfiguration, that.sessionTimeoutConfiguration) &&
                Objects.equals(allowAutoCreateTopics, that.allowAutoCreateTopics) &&
                autoOffsetReset == that.autoOffsetReset &&
                clientDnsLookup == that.clientDnsLookup &&
                Objects.equals(connectionsMaxIdleConfiguration, that.connectionsMaxIdleConfiguration) &&
                Objects.equals(defaultApiTimeoutConfiguration, that.defaultApiTimeoutConfiguration) &&
                Objects.equals(enableAutoCommit, that.enableAutoCommit) &&
                Objects.equals(excludeInternalTopics, that.excludeInternalTopics) &&
                Objects.equals(fetchMaxDataSizeConfiguration, that.fetchMaxDataSizeConfiguration) &&
                isolationLevel == that.isolationLevel &&
                Objects.equals(maxPollIntervalConfiguration, that.maxPollIntervalConfiguration) &&
                Objects.equals(maxPollRecords, that.maxPollRecords) &&
                Objects.equals(partitionAssignmentStrategyTypes, that.partitionAssignmentStrategyTypes) &&
                Objects.equals(receiveBufferSizeConfiguration, that.receiveBufferSizeConfiguration) &&
                Objects.equals(requestTimeoutConfiguration, that.requestTimeoutConfiguration) &&
                Objects.equals(sendBufferSizeConfiguration, that.sendBufferSizeConfiguration) &&
                Objects.equals(autoCommitIntervalConfiguration, that.autoCommitIntervalConfiguration) &&
                Objects.equals(checkCrcs, that.checkCrcs) &&
                Objects.equals(fetchMaxWaitConfiguration, that.fetchMaxWaitConfiguration) &&
                Objects.equals(interceptorTypes, that.interceptorTypes) &&
                Objects.equals(metadataMaxAgeConfiguration, that.metadataMaxAgeConfiguration) &&
                Objects.equals(metricsReporterTypes, that.metricsReporterTypes) &&
                Objects.equals(metricsNumSamples, that.metricsNumSamples) &&
                metricsRecordingLevel == that.metricsRecordingLevel &&
                Objects.equals(metricsSampleWindowConfiguration, that.metricsSampleWindowConfiguration) &&
                Objects.equals(reconnectBackoffMaxConfiguration, that.reconnectBackoffMaxConfiguration) &&
                Objects.equals(reconnectBackoffConfiguration, that.reconnectBackoffConfiguration) &&
                Objects.equals(retryBackoffConfiguration, that.retryBackoffConfiguration) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(clientRack, that.clientRack) &&
                Objects.equals(groupInstanceId, that.groupInstanceId) &&
                Objects.equals(closeDuration, that.closeDuration) &&
                securityProtocol == that.securityProtocol &&
                Objects.equals(socketConnectionSetupTimeoutMs, that.socketConnectionSetupTimeoutMs) &&
                Objects.equals(socketConnectionSetupTimeoutMaxMs, that.socketConnectionSetupTimeoutMaxMs) &&
                groupProtocol == that.groupProtocol &&
                Objects.equals(retryBackoffMaxMs, that.retryBackoffMaxMs) &&
                Objects.equals(subscriptionTopics, that.subscriptionTopics) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueConfiguration,
                keyDeserializerType,
                valueDeserializerType,
                bootstrapServerConfigurations,
                groupId,
                fetchMinDataSizeConfiguration,
                heartbeatIntervalConfiguration,
                maxPartitionFetchSizeConfiguration,
                sessionTimeoutConfiguration,
                allowAutoCreateTopics,
                autoOffsetReset,
                clientDnsLookup,
                connectionsMaxIdleConfiguration,
                defaultApiTimeoutConfiguration,
                enableAutoCommit,
                excludeInternalTopics,
                fetchMaxDataSizeConfiguration,
                isolationLevel,
                maxPollIntervalConfiguration,
                maxPollRecords,
                partitionAssignmentStrategyTypes,
                receiveBufferSizeConfiguration,
                requestTimeoutConfiguration,
                sendBufferSizeConfiguration,
                autoCommitIntervalConfiguration,
                checkCrcs,
                fetchMaxWaitConfiguration,
                interceptorTypes,
                metadataMaxAgeConfiguration,
                metricsReporterTypes,
                metricsNumSamples,
                metricsRecordingLevel,
                metricsSampleWindowConfiguration,
                reconnectBackoffMaxConfiguration,
                reconnectBackoffConfiguration,
                retryBackoffConfiguration,
                clientId,
                clientRack,
                groupInstanceId,
                closeDuration,
                securityProtocol,
                socketConnectionSetupTimeoutMs,
                socketConnectionSetupTimeoutMaxMs,
                groupProtocol,
                retryBackoffMaxMs,
                subscriptionTopics,
                driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultKafkaConsumerClientIntegrationServiceConfiguration{" +
                "clusterIdValueConfiguration=" + clusterIdValueConfiguration +
                ", keyDeserializerType=" + keyDeserializerType +
                ", valueDeserializerType=" + valueDeserializerType +
                ", bootstrapServerConfigurations=" + bootstrapServerConfigurations +
                ", groupId='" + groupId + '\'' +
                ", fetchMinDataSizeConfiguration=" + fetchMinDataSizeConfiguration +
                ", heartbeatIntervalConfiguration=" + heartbeatIntervalConfiguration +
                ", maxPartitionFetchSizeConfiguration=" + maxPartitionFetchSizeConfiguration +
                ", sessionTimeoutConfiguration=" + sessionTimeoutConfiguration +
                ", allowAutoCreateTopics=" + allowAutoCreateTopics +
                ", autoOffsetReset=" + autoOffsetReset +
                ", clientDnsLookup=" + clientDnsLookup +
                ", connectionsMaxIdleConfiguration=" + connectionsMaxIdleConfiguration +
                ", defaultApiTimeoutConfiguration=" + defaultApiTimeoutConfiguration +
                ", enableAutoCommit=" + enableAutoCommit +
                ", excludeInternalTopics=" + excludeInternalTopics +
                ", fetchMaxDataSizeConfiguration=" + fetchMaxDataSizeConfiguration +
                ", isolationLevel=" + isolationLevel +
                ", maxPollIntervalConfiguration=" + maxPollIntervalConfiguration +
                ", maxPollRecords=" + maxPollRecords +
                ", partitionAssignmentStrategyTypes=" + partitionAssignmentStrategyTypes +
                ", receiveBufferSizeConfiguration=" + receiveBufferSizeConfiguration +
                ", requestTimeoutConfiguration=" + requestTimeoutConfiguration +
                ", sendBufferSizeConfiguration=" + sendBufferSizeConfiguration +
                ", autoCommitIntervalConfiguration=" + autoCommitIntervalConfiguration +
                ", checkCrcs=" + checkCrcs +
                ", fetchMaxWaitConfiguration=" + fetchMaxWaitConfiguration +
                ", interceptorTypes=" + interceptorTypes +
                ", metadataMaxAgeConfiguration=" + metadataMaxAgeConfiguration +
                ", metricsReporterTypes=" + metricsReporterTypes +
                ", metricsNumSamples=" + metricsNumSamples +
                ", metricsRecordingLevel=" + metricsRecordingLevel +
                ", metricsSampleWindowConfiguration=" + metricsSampleWindowConfiguration +
                ", reconnectBackoffMaxConfiguration=" + reconnectBackoffMaxConfiguration +
                ", reconnectBackoffConfiguration=" + reconnectBackoffConfiguration +
                ", retryBackoffConfiguration=" + retryBackoffConfiguration +
                ", clientId='" + clientId + '\'' +
                ", clientRack='" + clientRack + '\'' +
                ", groupInstanceId='" + groupInstanceId + '\'' +
                ", closeDuration=" + closeDuration +
                ", securityProtocol=" + securityProtocol +
                ", socketConnectionSetupTimeoutMs=" + socketConnectionSetupTimeoutMs +
                ", socketConnectionSetupTimeoutMaxMs=" + socketConnectionSetupTimeoutMaxMs +
                ", groupProtocol=" + groupProtocol +
                ", retryBackoffMaxMs=" + retryBackoffMaxMs +
                ", subscriptionTopics=" + subscriptionTopics +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }



    /**
     * Builder for {@link DefaultKafkaConsumerClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final ClusterIdValueConfiguration DEFAULT_CLUSTER_ID_VALUE_CONFIGURATION =
                ClusterIdValueConfiguration.PRIMARY;
        private static final Class<? extends Deserializer> DEFAULT_KEY_DESERIALIZER_TYPE = StringDeserializer.class;
        private static final Class<? extends Deserializer> DEFAULT_VALUE_DESERIALIZER_TYPE = StringDeserializer.class;
        private static final Set<HostPortConfiguration> DEFAULT_BOOTSTRAP_SERVER_CONFIGURATIONS = Set.of(
                HostPortConfiguration
                        .builder()
                        .host("kafka.local")
                        .port(9092)
                        .build());
        private static final DataSizeConfiguration DEFAULT_FETCH_MIN_DATA_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(1)
                .unit(DataSizeConfiguration.Unit.BYTES)
                .build();
        private static final String DEFAULT_GROUP_ID = "appbase-kafka-client-group";
        private static final TimeUnitConfiguration DEFAULT_HEARTBEAT_INTERVAL_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(3)
                .timeUnit(TimeUnit.SECONDS)
                .build();
        private static final DataSizeConfiguration DEFAULT_MAX_PARTITION_FETCH_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(1)
                .unit(DataSizeConfiguration.Unit.MEGABYTES)
                .build();
        private static final TimeoutConfiguration DEFAULT_SESSION_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(10)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final boolean DEFAULT_ALLOW_AUTO_CREATE_TOPICS = true;
        private static final AutoOffsetReset DEFAULT_AUTO_OFFSET_RESET = AutoOffsetReset.LATEST;
        private static final ClientDnsLookup DEFAULT_CLIENT_DNS_LOOKUP = ClientDnsLookup.USE_ALL_DNS_IPS;
        private static final TimeUnitConfiguration DEFAULT_CONNECTIONS_MAX_IDLE_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(9)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final TimeoutConfiguration DEFAULT_DEFAULT_API_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(1)
                .timeoutTimeUnit(TimeUnit.MINUTES)
                .build();
        private static final boolean DEFAULT_ENABLE_AUTO_COMMIT = true;
        private static final boolean DEFAULT_EXCLUDE_INTERNAL_TOPICS = true;
        private static final DataSizeConfiguration DEFAULT_FETCH_MAX_DATA_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(50)
                .unit(DataSizeConfiguration.Unit.MEGABYTES)
                .build();
        private static final IsolationLevel DEFAULT_ISOLATION_LEVEL = IsolationLevel.READ_UNCOMMITTED;
        private static final TimeUnitConfiguration DEFAULT_MAX_POLL_INTERVAL_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(5)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final int DEFAULT_MAX_POLL_RECORDS = 500;
        private static final List<Class<? extends ConsumerPartitionAssignor>> DEFAULT_PARTITION_ASSIGNMENT_STRATEGY_TYPES =
                List.of(RangeAssignor.class);
        private static final DataSizeConfiguration DEFAULT_RECEIVE_BUFFER_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(64)
                .unit(DataSizeConfiguration.Unit.KILOBYTES)
                .build();
        private static final TimeoutConfiguration DEFAULT_REQUEST_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(30)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final DataSizeConfiguration DEFAULT_SEND_BUFFER_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(128)
                .unit(DataSizeConfiguration.Unit.KILOBYTES)
                .build();
        private static final TimeUnitConfiguration DEFAULT_AUTO_COMMIT_INTERVAL_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(5)
                .timeUnit(TimeUnit.SECONDS)
                .build();
        private static final boolean DEFAULT_CHECK_CRCS = true;
        private static final TimeUnitConfiguration DEFAULT_FETCH_MAX_WAIT_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(500)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        private static final List<Class<? extends ConsumerInterceptor>> DEFAULT_INTERCEPTOR_TYPES =
                List.of();
        private static final TimeUnitConfiguration DEFAULT_METADATA_MAX_AGE_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(5)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final List<Class<? extends MetricsReporter>> DEFAULT_METRICS_REPORTER_TYPES = List.of();
        private static final int DEFAULT_METRICS_NUM_SAMPLES = 2;
        private static final MetricRecordingLevel DEFAULT_METRICS_RECORDING_LEVEL = MetricRecordingLevel.INFO;
        private static final TimeUnitConfiguration DEFAULT_METRICS_SAMPLE_WINDOW_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(30)
                .timeUnit(TimeUnit.SECONDS)
                .build();
        private static final TimeUnitConfiguration DEFAULT_RECONNECT_BACKOFF_MAX_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(1)
                .timeUnit(TimeUnit.SECONDS)
                .build();
        private static final TimeUnitConfiguration DEFAULT_RECONNECT_BACKOFF_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(50)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        private static final TimeUnitConfiguration DEFAULT_RETRY_BACKOFF_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(100)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        private static final String DEFAULT_CLIENT_ID = AppbaseNetUtil.hostname();
        private static final String DEFAULT_CLIENT_RACK = null;
        private static final String DEFAULT_GROUP_INSTANCE_ID = null;
        private static final Duration DEFAULT_CLOSE_DURATION = Duration.ofMillis(2000);



        private ClusterIdValueConfiguration clusterIdValueConfiguration = DEFAULT_CLUSTER_ID_VALUE_CONFIGURATION;

        private Class<? extends Deserializer> keyDeserializerType = DEFAULT_KEY_DESERIALIZER_TYPE;

        private Class<? extends Deserializer> valueDeserializerType = DEFAULT_VALUE_DESERIALIZER_TYPE;

        @JsonSerialize(contentAs = HostPortConfiguration.class)
        @JsonDeserialize(contentAs = HostPortConfiguration.class)
        private Set<HostPortConfiguration> bootstrapServerConfigurations = DEFAULT_BOOTSTRAP_SERVER_CONFIGURATIONS;

        private String groupId = DEFAULT_GROUP_ID;

        @Nullable
        private DataSizeConfiguration fetchMinDataSizeConfiguration = DEFAULT_FETCH_MIN_DATA_SIZE_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration heartbeatIntervalConfiguration = DEFAULT_HEARTBEAT_INTERVAL_CONFIGURATION;

        @Nullable
        private DataSizeConfiguration maxPartitionFetchSizeConfiguration = DEFAULT_MAX_PARTITION_FETCH_SIZE_CONFIGURATION;

        @Nullable
        private TimeoutConfiguration sessionTimeoutConfiguration = DEFAULT_SESSION_TIMEOUT_CONFIGURATION;

        @Nullable
        private Boolean allowAutoCreateTopics = DEFAULT_ALLOW_AUTO_CREATE_TOPICS;

        @Nullable
        private AutoOffsetReset autoOffsetReset = DEFAULT_AUTO_OFFSET_RESET;

        @Nullable
        private ClientDnsLookup clientDnsLookup = DEFAULT_CLIENT_DNS_LOOKUP;

        @Nullable
        private TimeUnitConfiguration connectionsMaxIdleConfiguration = DEFAULT_CONNECTIONS_MAX_IDLE_CONFIGURATION;

        @Nullable
        private TimeoutConfiguration defaultApiTimeoutConfiguration = DEFAULT_DEFAULT_API_TIMEOUT_CONFIGURATION;

        @Nullable
        private Boolean enableAutoCommit = DEFAULT_ENABLE_AUTO_COMMIT;

        @Nullable
        private Boolean excludeInternalTopics = DEFAULT_EXCLUDE_INTERNAL_TOPICS;

        @Nullable
        private DataSizeConfiguration fetchMaxDataSizeConfiguration = DEFAULT_FETCH_MAX_DATA_SIZE_CONFIGURATION;

        @Nullable
        private IsolationLevel isolationLevel = DEFAULT_ISOLATION_LEVEL;

        @Nullable
        private TimeUnitConfiguration maxPollIntervalConfiguration = DEFAULT_MAX_POLL_INTERVAL_CONFIGURATION;

        @Nullable
        private Integer maxPollRecords = DEFAULT_MAX_POLL_RECORDS;

        @Nullable
        @JsonSerialize(contentAs = Class.class)
        @JsonDeserialize(contentAs = Class.class)
        private List<Class<? extends ConsumerPartitionAssignor>> partitionAssignmentStrategyTypes =
                DEFAULT_PARTITION_ASSIGNMENT_STRATEGY_TYPES;

        @Nullable
        private DataSizeConfiguration receiveBufferSizeConfiguration = DEFAULT_RECEIVE_BUFFER_SIZE_CONFIGURATION;

        @Nullable
        private TimeoutConfiguration requestTimeoutConfiguration = DEFAULT_REQUEST_TIMEOUT_CONFIGURATION;

        @Nullable
        private DataSizeConfiguration sendBufferSizeConfiguration = DEFAULT_SEND_BUFFER_SIZE_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration autoCommitIntervalConfiguration = DEFAULT_AUTO_COMMIT_INTERVAL_CONFIGURATION;

        @Nullable
        private Boolean checkCrcs = DEFAULT_CHECK_CRCS;

        @Nullable
        private TimeUnitConfiguration fetchMaxWaitConfiguration = DEFAULT_FETCH_MAX_WAIT_CONFIGURATION;

        @Nullable
        @JsonSerialize(contentAs = Class.class)
        @JsonDeserialize(contentAs = Class.class)
        private List<Class<? extends ConsumerInterceptor>> interceptorTypes = DEFAULT_INTERCEPTOR_TYPES;

        @Nullable
        private TimeUnitConfiguration metadataMaxAgeConfiguration = DEFAULT_METADATA_MAX_AGE_CONFIGURATION;

        @Nullable
        @JsonSerialize(contentAs = Class.class)
        @JsonDeserialize(contentAs = Class.class)
        private List<Class<? extends MetricsReporter>> metricsReporterTypes = DEFAULT_METRICS_REPORTER_TYPES;

        @Nullable
        private Integer metricsNumSamples = DEFAULT_METRICS_NUM_SAMPLES;

        @Nullable
        private MetricRecordingLevel metricsRecordingLevel = DEFAULT_METRICS_RECORDING_LEVEL;

        @Nullable
        private TimeUnitConfiguration metricsSampleWindowConfiguration = DEFAULT_METRICS_SAMPLE_WINDOW_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration reconnectBackoffMaxConfiguration = DEFAULT_RECONNECT_BACKOFF_MAX_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration reconnectBackoffConfiguration = DEFAULT_RECONNECT_BACKOFF_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration retryBackoffConfiguration = DEFAULT_RETRY_BACKOFF_CONFIGURATION;

        @Nullable
        private String clientId = DEFAULT_CLIENT_ID;

        @Nullable
        private String clientRack = DEFAULT_CLIENT_RACK;

        @Nullable
        private String groupInstanceId = DEFAULT_GROUP_INSTANCE_ID;

        @Nullable
        private Duration closeDuration = DEFAULT_CLOSE_DURATION;

        @Nullable
        private SecurityProtocol securityProtocol = null;

        @Nullable
        private Long socketConnectionSetupTimeoutMs = null;

        @Nullable
        private Long socketConnectionSetupTimeoutMaxMs = null;

        @Nullable
        private GroupProtocol groupProtocol = null;

        @Nullable
        private Long retryBackoffMaxMs = null;

        @Nullable
        private List<String> subscriptionTopics = null;

        private boolean driverMetricsEnabled = true;



        private Builder() {
        }

        public Builder clusterIdValueConfiguration(final ClusterIdValueConfiguration clusterIdValueConfiguration) {
            this.clusterIdValueConfiguration = clusterIdValueConfiguration;
            return this;
        }

        public Builder keyDeserializerType(final Class<? extends Deserializer> keyDeserializerType) {
            this.keyDeserializerType = keyDeserializerType;
            return this;
        }

        public Builder valueDeserializerType(final Class<? extends Deserializer> valueDeserializerType) {
            this.valueDeserializerType = valueDeserializerType;
            return this;
        }

        public Builder bootstrapServerConfigurations(
                @JsonSerialize(contentAs = HostPortConfiguration.class)
                @JsonDeserialize(contentAs = HostPortConfiguration.class)
                final Set<HostPortConfiguration> bootstrapServerConfigurations)
        {
            this.bootstrapServerConfigurations = bootstrapServerConfigurations;
            return this;
        }

        public Builder groupId(final String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder fetchMinDataSizeConfiguration(final DataSizeConfiguration fetchMinDataSizeConfiguration) {
            this.fetchMinDataSizeConfiguration = fetchMinDataSizeConfiguration;
            return this;
        }

        public Builder heartbeatIntervalConfiguration(final TimeUnitConfiguration heartbeatIntervalConfiguration) {
            this.heartbeatIntervalConfiguration = heartbeatIntervalConfiguration;
            return this;
        }

        public Builder maxPartitionFetchSizeConfiguration(final DataSizeConfiguration maxPartitionFetchSizeConfiguration) {
            this.maxPartitionFetchSizeConfiguration = maxPartitionFetchSizeConfiguration;
            return this;
        }

        public Builder sessionTimeoutConfiguration(final TimeoutConfiguration sessionTimeoutConfiguration) {
            this.sessionTimeoutConfiguration = sessionTimeoutConfiguration;
            return this;
        }

        public Builder allowAutoCreateTopics(final Boolean allowAutoCreateTopics) {
            this.allowAutoCreateTopics = allowAutoCreateTopics;
            return this;
        }

        public Builder autoOffsetReset(final AutoOffsetReset autoOffsetReset) {
            this.autoOffsetReset = autoOffsetReset;
            return this;
        }

        public Builder clientDnsLookup(final ClientDnsLookup clientDnsLookup) {
            this.clientDnsLookup = clientDnsLookup;
            return this;
        }

        public Builder connectionsMaxIdleConfiguration(final TimeUnitConfiguration connectionsMaxIdleConfiguration) {
            this.connectionsMaxIdleConfiguration = connectionsMaxIdleConfiguration;
            return this;
        }

        public Builder defaultApiTimeoutConfiguration(final TimeoutConfiguration defaultApiTimeoutConfiguration) {
            this.defaultApiTimeoutConfiguration = defaultApiTimeoutConfiguration;
            return this;
        }

        public Builder enableAutoCommit(final Boolean enableAutoCommit) {
            this.enableAutoCommit = enableAutoCommit;
            return this;
        }

        public Builder excludeInternalTopics(final Boolean excludeInternalTopics) {
            this.excludeInternalTopics = excludeInternalTopics;
            return this;
        }

        public Builder fetchMaxDataSizeConfiguration(final DataSizeConfiguration fetchMaxDataSizeConfiguration) {
            this.fetchMaxDataSizeConfiguration = fetchMaxDataSizeConfiguration;
            return this;
        }

        public Builder isolationLevel(final IsolationLevel isolationLevel) {
            this.isolationLevel = isolationLevel;
            return this;
        }

        public Builder maxPollIntervalConfiguration(final TimeUnitConfiguration maxPollIntervalConfiguration) {
            this.maxPollIntervalConfiguration = maxPollIntervalConfiguration;
            return this;
        }

        public Builder maxPollRecords(final Integer maxPollRecords) {
            this.maxPollRecords = maxPollRecords;
            return this;
        }

        public Builder partitionAssignmentStrategyTypes(
                @JsonSerialize(contentAs = Class.class)
                @JsonDeserialize(contentAs = Class.class)
                final List<Class<? extends ConsumerPartitionAssignor>> partitionAssignmentStrategyTypes)
        {
            this.partitionAssignmentStrategyTypes = partitionAssignmentStrategyTypes;
            return this;
        }

        public Builder receiveBufferSizeConfiguration(final DataSizeConfiguration receiveBufferSizeConfiguration) {
            this.receiveBufferSizeConfiguration = receiveBufferSizeConfiguration;
            return this;
        }

        public Builder requestTimeoutConfiguration(final TimeoutConfiguration requestTimeoutConfiguration) {
            this.requestTimeoutConfiguration = requestTimeoutConfiguration;
            return this;
        }

        public Builder sendBufferSizeConfiguration(final DataSizeConfiguration sendBufferSizeConfiguration) {
            this.sendBufferSizeConfiguration = sendBufferSizeConfiguration;
            return this;
        }

        public Builder autoCommitIntervalConfiguration(final TimeUnitConfiguration autoCommitIntervalConfiguration) {
            this.autoCommitIntervalConfiguration = autoCommitIntervalConfiguration;
            return this;
        }

        public Builder checkCrcs(final Boolean checkCrcs) {
            this.checkCrcs = checkCrcs;
            return this;
        }

        public Builder fetchMaxWaitConfiguration(final TimeUnitConfiguration fetchMaxWaitConfiguration) {
            this.fetchMaxWaitConfiguration = fetchMaxWaitConfiguration;
            return this;
        }

        public Builder interceptorTypes(
                @JsonSerialize(contentAs = Class.class)
                @JsonDeserialize(contentAs = Class.class)
                final List<Class<? extends ConsumerInterceptor>> interceptorTypes)
        {
            this.interceptorTypes = interceptorTypes;
            return this;
        }

        public Builder metadataMaxAgeConfiguration(final TimeUnitConfiguration metadataMaxAgeConfiguration) {
            this.metadataMaxAgeConfiguration = metadataMaxAgeConfiguration;
            return this;
        }

        public Builder metricsReporterTypes(
                @JsonSerialize(contentAs = Class.class)
                @JsonDeserialize(contentAs = Class.class)
                final List<Class<? extends MetricsReporter>> metricsReporterTypes)
        {
            this.metricsReporterTypes = metricsReporterTypes;
            return this;
        }

        public Builder metricsNumSamples(final Integer metricsNumSamples) {
            this.metricsNumSamples = metricsNumSamples;
            return this;
        }

        public Builder metricsRecordingLevel(final MetricRecordingLevel metricsRecordingLevel) {
            this.metricsRecordingLevel = metricsRecordingLevel;
            return this;
        }

        public Builder metricsSampleWindowConfiguration(final TimeUnitConfiguration metricsSampleWindowConfiguration) {
            this.metricsSampleWindowConfiguration = metricsSampleWindowConfiguration;
            return this;
        }

        public Builder reconnectBackoffMaxConfiguration(final TimeUnitConfiguration reconnectBackoffMaxConfiguration) {
            this.reconnectBackoffMaxConfiguration = reconnectBackoffMaxConfiguration;
            return this;
        }

        public Builder reconnectBackoffConfiguration(final TimeUnitConfiguration reconnectBackoffConfiguration) {
            this.reconnectBackoffConfiguration = reconnectBackoffConfiguration;
            return this;
        }

        public Builder retryBackoffConfiguration(final TimeUnitConfiguration retryBackoffConfiguration) {
            this.retryBackoffConfiguration = retryBackoffConfiguration;
            return this;
        }

        public Builder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientRack(final String clientRack) {
            this.clientRack = clientRack;
            return this;
        }

        public Builder groupInstanceId(final String groupInstanceId) {
            this.groupInstanceId = groupInstanceId;
            return this;
        }

        public Builder closeDuration(final Duration closeDuration) {
            this.closeDuration = closeDuration;
            return this;
        }

        public Builder securityProtocol(final SecurityProtocol securityProtocol) {
            this.securityProtocol = securityProtocol;
            return this;
        }

        public Builder socketConnectionSetupTimeoutMs(final Long socketConnectionSetupTimeoutMs) {
            this.socketConnectionSetupTimeoutMs = socketConnectionSetupTimeoutMs;
            return this;
        }

        public Builder socketConnectionSetupTimeoutMaxMs(final Long socketConnectionSetupTimeoutMaxMs) {
            this.socketConnectionSetupTimeoutMaxMs = socketConnectionSetupTimeoutMaxMs;
            return this;
        }

        public Builder groupProtocol(final GroupProtocol groupProtocol) {
            this.groupProtocol = groupProtocol;
            return this;
        }

        public Builder retryBackoffMaxMs(final Long retryBackoffMaxMs) {
            this.retryBackoffMaxMs = retryBackoffMaxMs;
            return this;
        }

        public Builder subscriptionTopics(final List<String> subscriptionTopics) {
            this.subscriptionTopics = subscriptionTopics;
            return this;
        }

        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        public DefaultKafkaConsumerClientIntegrationServiceConfiguration build() {
            return new DefaultKafkaConsumerClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
