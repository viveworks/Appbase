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

package com.viveworks.appbase.client.service.kafka.producer.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.kafka.configuration.Acks;
import com.viveworks.appbase.client.service.kafka.configuration.ClientDnsLookup;
import com.viveworks.appbase.client.service.kafka.configuration.CompressionType;
import com.viveworks.appbase.client.service.kafka.configuration.MetricRecordingLevel;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.service.kafka.producer.DefaultKafkaProducerClientIntegrationService;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultKafkaProducerClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @see <a href="https://kafka.apache.org/081/documentation.html#producerconfigs">Kafka Producer Properties</a>
 * @see <a href="https://kafka.apache.org/081/documentation.html#newproducerconfigs">Kafka New Producer Properties</a>
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultKafkaProducerClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultKafkaProducerClientIntegrationServiceConfiguration
        implements KafkaProducerClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueConfiguration;

    private final Class<? extends Serializer> keySerializerType;

    private final Class<? extends Serializer> valueSerializerType;

    private final Acks acks;

    @JsonSerialize(contentAs = HostPortConfiguration.class)
    @JsonDeserialize(contentAs = HostPortConfiguration.class)
    private final Set<HostPortConfiguration> bootstrapServerConfigurations;

    @Nullable
    private final DataSizeConfiguration bufferMemorySizeConfiguration;

    @Nullable
    private final CompressionType compressionType;

    @Nullable
    private final Integer retries;

    @Nullable
    private final Integer batchSize;

    @Nullable
    private final ClientDnsLookup clientDnsLookup;

    @Nullable
    private final TimeUnitConfiguration connectionsMaxIdleConfiguration;

    @Nullable
    private final TimeoutConfiguration deliveryTimeoutConfiguration;

    @Nullable
    private final TimeUnitConfiguration lingerTimeConfiguration;

    @Nullable
    private final TimeUnitConfiguration maxBlockTimeConfiguration;

    @Nullable
    private final DataSizeConfiguration maxRequestSizeConfiguration;

    @Nullable
    private final Class<? extends Partitioner> partitionerType;

    @Nullable
    private final DataSizeConfiguration receiveBufferSizeConfiguration;

    @Nullable
    private final TimeoutConfiguration requestTimeoutConfiguration;

    @Nullable
    private final DataSizeConfiguration sendBufferSizeConfiguration;

    @Nullable
    private final Boolean enableIdempotence;

    @Nullable
    @JsonSerialize(contentAs = Class.class)
    @JsonDeserialize(contentAs = Class.class)
    private final List<Class<? extends ProducerInterceptor>> interceptorTypes;

    @Nullable
    private final Integer maxInFlightRequestsPerConnection;

    @Nullable
    private final TimeUnitConfiguration metadataMaxAgeConfiguration;

    @Nullable
    private final TimeUnitConfiguration metadataMaxIdleConfiguration;

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
    private final TimeoutConfiguration transactionTimeoutConfiguration;

    @Nullable
    private final String clientId;

    @Nullable
    private final String transactionalId;

    @Nullable
    private final Duration closeDuration;

    @Nullable
    private final SecurityProtocol securityProtocol;

    @Nullable
    private final Long socketConnectionSetupTimeoutMs;

    @Nullable
    private final Long socketConnectionSetupTimeoutMaxMs;

    @Nullable
    private final Long partitionerAvailabilityTimeoutMs;

    @Nullable
    private final Boolean partitionerIgnoreKeys;

    private final boolean driverMetricsEnabled;



    private DefaultKafkaProducerClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.clusterIdValueConfiguration, "builder.clusterIdValueConfiguration");
        AppbaseUtil.notNull(builder.keySerializerType, "builder.keySerializerType");
        AppbaseUtil.notNull(builder.valueSerializerType, "builder.valueSerializerType");
        AppbaseUtil.notNull(builder.acks, "builder.acks");
        AppbaseUtil.notNull(builder.bootstrapServerConfigurations, "builder.bootstrapServerConfigurations");
        AppbaseUtil.notEmpty(builder.bootstrapServerConfigurations, "builder.bootstrapServerConfigurations");

        clusterIdValueConfiguration = builder.clusterIdValueConfiguration;
        keySerializerType = builder.keySerializerType;
        valueSerializerType = builder.valueSerializerType;
        acks = builder.acks;
        bootstrapServerConfigurations = builder.bootstrapServerConfigurations;
        bufferMemorySizeConfiguration = builder.bufferMemorySizeConfiguration;
        compressionType = builder.compressionType;
        retries = builder.retries;
        batchSize = builder.batchSize;
        clientDnsLookup = builder.clientDnsLookup;
        connectionsMaxIdleConfiguration = builder.connectionsMaxIdleConfiguration;
        deliveryTimeoutConfiguration = builder.deliveryTimeoutConfiguration;
        lingerTimeConfiguration = builder.lingerTimeConfiguration;
        maxBlockTimeConfiguration = builder.maxBlockTimeConfiguration;
        maxRequestSizeConfiguration = builder.maxRequestSizeConfiguration;
        partitionerType = builder.partitionerType;
        receiveBufferSizeConfiguration = builder.receiveBufferSizeConfiguration;
        requestTimeoutConfiguration = builder.requestTimeoutConfiguration;
        sendBufferSizeConfiguration = builder.sendBufferSizeConfiguration;
        enableIdempotence = builder.enableIdempotence;
        interceptorTypes = builder.interceptorTypes;
        maxInFlightRequestsPerConnection = builder.maxInFlightRequestsPerConnection;
        metadataMaxAgeConfiguration = builder.metadataMaxAgeConfiguration;
        metadataMaxIdleConfiguration = builder.metadataMaxIdleConfiguration;
        metricsReporterTypes = builder.metricsReporterTypes;
        metricsNumSamples = builder.metricsNumSamples;
        metricsRecordingLevel = builder.metricsRecordingLevel;
        metricsSampleWindowConfiguration = builder.metricsSampleWindowConfiguration;
        reconnectBackoffMaxConfiguration = builder.reconnectBackoffMaxConfiguration;
        reconnectBackoffConfiguration = builder.reconnectBackoffConfiguration;
        retryBackoffConfiguration = builder.retryBackoffConfiguration;
        transactionTimeoutConfiguration = builder.transactionTimeoutConfiguration;
        clientId = builder.clientId;
        transactionalId = builder.transactionalId;
        closeDuration = builder.closeDuration;
        securityProtocol = builder.securityProtocol;
        socketConnectionSetupTimeoutMs = builder.socketConnectionSetupTimeoutMs;
        socketConnectionSetupTimeoutMaxMs = builder.socketConnectionSetupTimeoutMaxMs;
        partitionerAvailabilityTimeoutMs = builder.partitionerAvailabilityTimeoutMs;
        partitionerIgnoreKeys = builder.partitionerIgnoreKeys;
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

    public Class<? extends Serializer> getKeySerializerType() {
        return keySerializerType;
    }

    public Class<? extends Serializer> getValueSerializerType() {
        return valueSerializerType;
    }

    public Acks getAcks() {
        return acks;
    }

    public Set<HostPortConfiguration> getBootstrapServerConfigurations() {
        return bootstrapServerConfigurations;
    }

    @Nullable
    public DataSizeConfiguration getBufferMemorySizeConfiguration() {
        return bufferMemorySizeConfiguration;
    }

    @Nullable
    public CompressionType getCompressionType() {
        return compressionType;
    }

    @Nullable
    public Integer getRetries() {
        return retries;
    }

    @Nullable
    public Integer getBatchSize() {
        return batchSize;
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
    public TimeoutConfiguration getDeliveryTimeoutConfiguration() {
        return deliveryTimeoutConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getLingerTimeConfiguration() {
        return lingerTimeConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getMaxBlockTimeConfiguration() {
        return maxBlockTimeConfiguration;
    }

    @Nullable
    public DataSizeConfiguration getMaxRequestSizeConfiguration() {
        return maxRequestSizeConfiguration;
    }

    @Nullable
    public Class<? extends Partitioner> getPartitionerType() {
        return partitionerType;
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
    public Boolean getEnableIdempotence() {
        return enableIdempotence;
    }

    @Nullable
    public List<Class<? extends ProducerInterceptor>> getInterceptorTypes() {
        return interceptorTypes;
    }

    @Nullable
    public Integer getMaxInFlightRequestsPerConnection() {
        return maxInFlightRequestsPerConnection;
    }

    @Nullable
    public TimeUnitConfiguration getMetadataMaxAgeConfiguration() {
        return metadataMaxAgeConfiguration;
    }

    @Nullable
    public TimeUnitConfiguration getMetadataMaxIdleConfiguration() {
        return metadataMaxIdleConfiguration;
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
    public TimeoutConfiguration getTransactionTimeoutConfiguration() {
        return transactionTimeoutConfiguration;
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }

    @Nullable
    public String getTransactionalId() {
        return transactionalId;
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
    public Long getPartitionerAvailabilityTimeoutMs() {
        return partitionerAvailabilityTimeoutMs;
    }

    @Nullable
    public Boolean getPartitionerIgnoreKeys() {
        return partitionerIgnoreKeys;
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

        final DefaultKafkaProducerClientIntegrationServiceConfiguration that =
                (DefaultKafkaProducerClientIntegrationServiceConfiguration) o;
        return Objects.equals(clusterIdValueConfiguration, that.clusterIdValueConfiguration) &&
                Objects.equals(keySerializerType, that.keySerializerType) &&
                Objects.equals(valueSerializerType, that.valueSerializerType) &&
                acks == that.acks &&
                Objects.equals(bootstrapServerConfigurations, that.bootstrapServerConfigurations) &&
                Objects.equals(bufferMemorySizeConfiguration, that.bufferMemorySizeConfiguration) &&
                compressionType == that.compressionType &&
                Objects.equals(retries, that.retries) &&
                Objects.equals(batchSize, that.batchSize) &&
                clientDnsLookup == that.clientDnsLookup &&
                Objects.equals(connectionsMaxIdleConfiguration, that.connectionsMaxIdleConfiguration) &&
                Objects.equals(deliveryTimeoutConfiguration, that.deliveryTimeoutConfiguration) &&
                Objects.equals(lingerTimeConfiguration, that.lingerTimeConfiguration) &&
                Objects.equals(maxBlockTimeConfiguration, that.maxBlockTimeConfiguration) &&
                Objects.equals(maxRequestSizeConfiguration, that.maxRequestSizeConfiguration) &&
                Objects.equals(partitionerType, that.partitionerType) &&
                Objects.equals(receiveBufferSizeConfiguration, that.receiveBufferSizeConfiguration) &&
                Objects.equals(requestTimeoutConfiguration, that.requestTimeoutConfiguration) &&
                Objects.equals(sendBufferSizeConfiguration, that.sendBufferSizeConfiguration) &&
                Objects.equals(enableIdempotence, that.enableIdempotence) &&
                Objects.equals(interceptorTypes, that.interceptorTypes) &&
                Objects.equals(maxInFlightRequestsPerConnection, that.maxInFlightRequestsPerConnection) &&
                Objects.equals(metadataMaxAgeConfiguration, that.metadataMaxAgeConfiguration) &&
                Objects.equals(metadataMaxIdleConfiguration, that.metadataMaxIdleConfiguration) &&
                Objects.equals(metricsReporterTypes, that.metricsReporterTypes) &&
                Objects.equals(metricsNumSamples, that.metricsNumSamples) &&
                metricsRecordingLevel == that.metricsRecordingLevel &&
                Objects.equals(metricsSampleWindowConfiguration, that.metricsSampleWindowConfiguration) &&
                Objects.equals(reconnectBackoffMaxConfiguration, that.reconnectBackoffMaxConfiguration) &&
                Objects.equals(reconnectBackoffConfiguration, that.reconnectBackoffConfiguration) &&
                Objects.equals(retryBackoffConfiguration, that.retryBackoffConfiguration) &&
                Objects.equals(transactionTimeoutConfiguration, that.transactionTimeoutConfiguration) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(transactionalId, that.transactionalId) &&
                Objects.equals(closeDuration, that.closeDuration) &&
                securityProtocol == that.securityProtocol &&
                Objects.equals(socketConnectionSetupTimeoutMs, that.socketConnectionSetupTimeoutMs) &&
                Objects.equals(socketConnectionSetupTimeoutMaxMs, that.socketConnectionSetupTimeoutMaxMs) &&
                Objects.equals(partitionerAvailabilityTimeoutMs, that.partitionerAvailabilityTimeoutMs) &&
                Objects.equals(partitionerIgnoreKeys, that.partitionerIgnoreKeys) &&
                driverMetricsEnabled == that.driverMetricsEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterIdValueConfiguration,
                keySerializerType,
                valueSerializerType,
                acks,
                bootstrapServerConfigurations,
                bufferMemorySizeConfiguration,
                compressionType,
                retries,
                batchSize,
                clientDnsLookup,
                connectionsMaxIdleConfiguration,
                deliveryTimeoutConfiguration,
                lingerTimeConfiguration,
                maxBlockTimeConfiguration,
                maxRequestSizeConfiguration,
                partitionerType,
                receiveBufferSizeConfiguration,
                requestTimeoutConfiguration,
                sendBufferSizeConfiguration,
                enableIdempotence,
                interceptorTypes,
                maxInFlightRequestsPerConnection,
                metadataMaxAgeConfiguration,
                metadataMaxIdleConfiguration,
                metricsReporterTypes,
                metricsNumSamples,
                metricsRecordingLevel,
                metricsSampleWindowConfiguration,
                reconnectBackoffMaxConfiguration,
                reconnectBackoffConfiguration,
                retryBackoffConfiguration,
                transactionTimeoutConfiguration,
                clientId,
                transactionalId,
                closeDuration,
                securityProtocol,
                socketConnectionSetupTimeoutMs,
                socketConnectionSetupTimeoutMaxMs,
                partitionerAvailabilityTimeoutMs,
                partitionerIgnoreKeys,
                driverMetricsEnabled);
    }

    @Override
    public String toString() {
        return "DefaultKafkaProducerClientIntegrationServiceConfiguration{" +
                "clusterIdValueConfiguration=" + clusterIdValueConfiguration +
                ", keySerializerType=" + keySerializerType +
                ", valueSerializerType=" + valueSerializerType +
                ", acks=" + acks +
                ", bootstrapServerConfigurations=" + bootstrapServerConfigurations +
                ", bufferMemorySizeConfiguration=" + bufferMemorySizeConfiguration +
                ", compressionType=" + compressionType +
                ", retries=" + retries +
                ", batchSize=" + batchSize +
                ", clientDnsLookup=" + clientDnsLookup +
                ", connectionsMaxIdleConfiguration=" + connectionsMaxIdleConfiguration +
                ", deliveryTimeoutConfiguration=" + deliveryTimeoutConfiguration +
                ", lingerTimeConfiguration=" + lingerTimeConfiguration +
                ", maxBlockTimeConfiguration=" + maxBlockTimeConfiguration +
                ", maxRequestSizeConfiguration=" + maxRequestSizeConfiguration +
                ", partitionerType=" + partitionerType +
                ", receiveBufferSizeConfiguration=" + receiveBufferSizeConfiguration +
                ", requestTimeoutConfiguration=" + requestTimeoutConfiguration +
                ", sendBufferSizeConfiguration=" + sendBufferSizeConfiguration +
                ", enableIdempotence=" + enableIdempotence +
                ", interceptorTypes=" + interceptorTypes +
                ", maxInFlightRequestsPerConnection=" + maxInFlightRequestsPerConnection +
                ", metadataMaxAgeConfiguration=" + metadataMaxAgeConfiguration +
                ", metadataMaxIdleConfiguration=" + metadataMaxIdleConfiguration +
                ", metricsReporterTypes=" + metricsReporterTypes +
                ", metricsNumSamples=" + metricsNumSamples +
                ", metricsRecordingLevel=" + metricsRecordingLevel +
                ", metricsSampleWindowConfiguration=" + metricsSampleWindowConfiguration +
                ", reconnectBackoffMaxConfiguration=" + reconnectBackoffMaxConfiguration +
                ", reconnectBackoffConfiguration=" + reconnectBackoffConfiguration +
                ", retryBackoffConfiguration=" + retryBackoffConfiguration +
                ", transactionTimeoutConfiguration=" + transactionTimeoutConfiguration +
                ", clientId='" + clientId + '\'' +
                ", transactionalId='" + transactionalId + '\'' +
                ", closeDuration=" + closeDuration +
                ", securityProtocol=" + securityProtocol +
                ", socketConnectionSetupTimeoutMs=" + socketConnectionSetupTimeoutMs +
                ", socketConnectionSetupTimeoutMaxMs=" + socketConnectionSetupTimeoutMaxMs +
                ", partitionerAvailabilityTimeoutMs=" + partitionerAvailabilityTimeoutMs +
                ", partitionerIgnoreKeys=" + partitionerIgnoreKeys +
                ", driverMetricsEnabled=" + driverMetricsEnabled +
                '}';
    }



    /**
     * Builder for {@link DefaultKafkaProducerClientIntegrationServiceConfiguration}.
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
        private static final Class<? extends Serializer> DEFAULT_KEY_SERIALIZER_TYPE = StringSerializer.class;
        private static final Class<? extends Serializer> DEFAULT_VALUE_SERIALIZER_TYPE = StringSerializer.class;
        private static final Acks DEFAULT_ACKS = Acks.ONE;
        private static final Set<HostPortConfiguration> DEFAULT_BOOTSTRAP_SERVER_CONFIGURATIONS = Set.of(
                HostPortConfiguration
                        .builder()
                        .host("kafka.local")
                        .port(9092)
                        .build());
        private static final DataSizeConfiguration DEFAULT_BUFFER_MEMORY_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(32)
                .unit(DataSizeConfiguration.Unit.MEGABYTES)
                .build();
        private static final CompressionType DEFAULT_COMPRESSION_TYPE = CompressionType.NONE;
        private static final int DEFAULT_RETRIES = Integer.MAX_VALUE;
        private static final int DEFAULT_BATCH_SIZE = 16384;
        private static final ClientDnsLookup DEFAULT_CLIENT_DNS_LOOKUP = ClientDnsLookup.USE_ALL_DNS_IPS;
        private static final TimeUnitConfiguration DEFAULT_CONNECTIONS_MAX_IDLE_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(9)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final TimeoutConfiguration DEFAULT_DELIVERY_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(2)
                .timeoutTimeUnit(TimeUnit.MINUTES)
                .build();
        private static final TimeUnitConfiguration DEFAULT_LINGER_TIME_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(0)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
        private static final TimeUnitConfiguration DEFAULT_MAX_BLOCK_TIME_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(1)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final DataSizeConfiguration DEFAULT_MAX_REQUEST_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(1)
                .unit(DataSizeConfiguration.Unit.MEGABYTES)
                .build();
        private static final Class<? extends Partitioner> DEFAULT_PARTITIONER_TYPE = DefaultPartitioner.class;
        private static final DataSizeConfiguration DEFAULT_RECEIVE_BUFFER_SIZE_CONFIGURATION = DataSizeConfiguration
                .builder()
                .enabled(true)
                .dataSize(32)
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
        private static final boolean DEFAULT_ENABLE_IDEMPOTENCE = false;
        private static final List<Class<? extends ProducerInterceptor>> DEFAULT_INTERCEPTOR_TYPES = List.of();
        private static final int DEFAULT_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 5;
        private static final TimeUnitConfiguration DEFAULT_METADATA_MAX_AGE_CONFIGURATION = TimeUnitConfiguration
                .builder()
                .enabled(true)
                .time(5)
                .timeUnit(TimeUnit.MINUTES)
                .build();
        private static final TimeUnitConfiguration DEFAULT_METADATA_MAX_IDLE_CONFIGURATION = TimeUnitConfiguration
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
        private static final TimeoutConfiguration DEFAULT_TRANSACTION_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(1)
                .timeoutTimeUnit(TimeUnit.MINUTES)
                .build();
        private static final String DEFAULT_CLIENT_ID =  AppbaseNetUtil.hostname();
        private static final String DEFAULT_TRANSACTIONAL_ID = null;
        private static final Duration DEFAULT_CLOSE_DURATION = Duration.ofMillis(2000);



        private ClusterIdValueConfiguration clusterIdValueConfiguration = DEFAULT_CLUSTER_ID_VALUE_CONFIGURATION;

        private Class<? extends Serializer> keySerializerType = DEFAULT_KEY_SERIALIZER_TYPE;

        private Class<? extends Serializer> valueSerializerType = DEFAULT_VALUE_SERIALIZER_TYPE;

        private Acks acks = DEFAULT_ACKS;

        @JsonSerialize(contentAs = HostPortConfiguration.class)
        @JsonDeserialize(contentAs = HostPortConfiguration.class)
        private Set<HostPortConfiguration> bootstrapServerConfigurations = DEFAULT_BOOTSTRAP_SERVER_CONFIGURATIONS;

        @Nullable
        private DataSizeConfiguration bufferMemorySizeConfiguration = DEFAULT_BUFFER_MEMORY_SIZE_CONFIGURATION;

        @Nullable
        private CompressionType compressionType = DEFAULT_COMPRESSION_TYPE;

        @Nullable
        private Integer retries = DEFAULT_RETRIES;

        @Nullable
        private Integer batchSize = DEFAULT_BATCH_SIZE;

        @Nullable
        private ClientDnsLookup clientDnsLookup = DEFAULT_CLIENT_DNS_LOOKUP;

        @Nullable
        private TimeUnitConfiguration connectionsMaxIdleConfiguration = DEFAULT_CONNECTIONS_MAX_IDLE_CONFIGURATION;

        @Nullable
        private TimeoutConfiguration deliveryTimeoutConfiguration = DEFAULT_DELIVERY_TIMEOUT_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration lingerTimeConfiguration = DEFAULT_LINGER_TIME_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration maxBlockTimeConfiguration = DEFAULT_MAX_BLOCK_TIME_CONFIGURATION;

        @Nullable
        private DataSizeConfiguration maxRequestSizeConfiguration = DEFAULT_MAX_REQUEST_SIZE_CONFIGURATION;

        @Nullable
        private Class<? extends Partitioner> partitionerType = DEFAULT_PARTITIONER_TYPE;

        @Nullable
        private DataSizeConfiguration receiveBufferSizeConfiguration = DEFAULT_RECEIVE_BUFFER_SIZE_CONFIGURATION;

        @Nullable
        private TimeoutConfiguration requestTimeoutConfiguration = DEFAULT_REQUEST_TIMEOUT_CONFIGURATION;

        @Nullable
        private DataSizeConfiguration sendBufferSizeConfiguration = DEFAULT_SEND_BUFFER_SIZE_CONFIGURATION;

        @Nullable
        private Boolean enableIdempotence = DEFAULT_ENABLE_IDEMPOTENCE;

        @Nullable
        @JsonSerialize(contentAs = Class.class)
        @JsonDeserialize(contentAs = Class.class)
        private List<Class<? extends ProducerInterceptor>> interceptorTypes = DEFAULT_INTERCEPTOR_TYPES;

        @Nullable
        private Integer maxInFlightRequestsPerConnection = DEFAULT_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;

        @Nullable
        private TimeUnitConfiguration metadataMaxAgeConfiguration = DEFAULT_METADATA_MAX_AGE_CONFIGURATION;

        @Nullable
        private TimeUnitConfiguration metadataMaxIdleConfiguration = DEFAULT_METADATA_MAX_IDLE_CONFIGURATION;

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
        private TimeoutConfiguration transactionTimeoutConfiguration = DEFAULT_TRANSACTION_TIMEOUT_CONFIGURATION;

        @Nullable
        private String clientId = DEFAULT_CLIENT_ID;

        @Nullable
        private String transactionalId = DEFAULT_TRANSACTIONAL_ID;

        @Nullable
        private Duration closeDuration = DEFAULT_CLOSE_DURATION;

        @Nullable
        private SecurityProtocol securityProtocol = null;

        @Nullable
        private Long socketConnectionSetupTimeoutMs = null;

        @Nullable
        private Long socketConnectionSetupTimeoutMaxMs = null;

        @Nullable
        private Long partitionerAvailabilityTimeoutMs = null;

        @Nullable
        private Boolean partitionerIgnoreKeys = null;

        private boolean driverMetricsEnabled = true;



        private Builder() {
        }

        public Builder clusterIdValueConfiguration(final ClusterIdValueConfiguration clusterIdValueConfiguration) {
            this.clusterIdValueConfiguration = clusterIdValueConfiguration;
            return this;
        }

        public Builder keySerializerType(final Class<? extends Serializer> keySerializerType) {
            this.keySerializerType = keySerializerType;
            return this;
        }

        public Builder valueSerializerType(final Class<? extends Serializer> valueSerializerType) {
            this.valueSerializerType = valueSerializerType;
            return this;
        }

        public Builder acks(final Acks acks) {
            this.acks = acks;
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

        public Builder bufferMemorySizeConfiguration(final DataSizeConfiguration bufferMemorySizeConfiguration) {
            this.bufferMemorySizeConfiguration = bufferMemorySizeConfiguration;
            return this;
        }

        public Builder compressionType(final CompressionType compressionType) {
            this.compressionType = compressionType;
            return this;
        }

        public Builder retries(final Integer retries) {
            this.retries = retries;
            return this;
        }

        public Builder batchSize(final Integer batchSize) {
            this.batchSize = batchSize;
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

        public Builder deliveryTimeoutConfiguration(final TimeoutConfiguration deliveryTimeoutConfiguration) {
            this.deliveryTimeoutConfiguration = deliveryTimeoutConfiguration;
            return this;
        }

        public Builder lingerTimeConfiguration(final TimeUnitConfiguration lingerTimeConfiguration) {
            this.lingerTimeConfiguration = lingerTimeConfiguration;
            return this;
        }

        public Builder maxBlockTimeConfiguration(final TimeUnitConfiguration maxBlockTimeConfiguration) {
            this.maxBlockTimeConfiguration = maxBlockTimeConfiguration;
            return this;
        }

        public Builder maxRequestSizeConfiguration(final DataSizeConfiguration maxRequestSizeConfiguration) {
            this.maxRequestSizeConfiguration = maxRequestSizeConfiguration;
            return this;
        }

        public Builder partitionerType(final Class<? extends Partitioner> partitionerType) {
            this.partitionerType = partitionerType;
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

        public Builder enableIdempotence(final Boolean enableIdempotence) {
            this.enableIdempotence = enableIdempotence;
            return this;
        }

        public Builder interceptorTypes(
                @JsonSerialize(contentAs = Class.class)
                @JsonDeserialize(contentAs = Class.class)
                final List<Class<? extends ProducerInterceptor>> interceptorTypes)
        {
            this.interceptorTypes = interceptorTypes;
            return this;
        }

        public Builder maxInFlightRequestsPerConnection(final Integer maxInFlightRequestsPerConnection) {
            this.maxInFlightRequestsPerConnection = maxInFlightRequestsPerConnection;
            return this;
        }

        public Builder metadataMaxAgeConfiguration(final TimeUnitConfiguration metadataMaxAgeConfiguration) {
            this.metadataMaxAgeConfiguration = metadataMaxAgeConfiguration;
            return this;
        }

        public Builder metadataMaxIdleConfiguration(final TimeUnitConfiguration metadataMaxIdleConfiguration) {
            this.metadataMaxIdleConfiguration = metadataMaxIdleConfiguration;
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

        public Builder transactionTimeoutConfiguration(final TimeoutConfiguration transactionTimeoutConfiguration) {
            this.transactionTimeoutConfiguration = transactionTimeoutConfiguration;
            return this;
        }

        public Builder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder transactionalId(final String transactionalId) {
            this.transactionalId = transactionalId;
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

        public Builder partitionerAvailabilityTimeoutMs(final Long partitionerAvailabilityTimeoutMs) {
            this.partitionerAvailabilityTimeoutMs = partitionerAvailabilityTimeoutMs;
            return this;
        }

        public Builder partitionerIgnoreKeys(final Boolean partitionerIgnoreKeys) {
            this.partitionerIgnoreKeys = partitionerIgnoreKeys;
            return this;
        }

        public Builder driverMetricsEnabled(final boolean driverMetricsEnabled) {
            this.driverMetricsEnabled = driverMetricsEnabled;
            return this;
        }

        public DefaultKafkaProducerClientIntegrationServiceConfiguration build() {
            return new DefaultKafkaProducerClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
