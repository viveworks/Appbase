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

package com.viveworks.appbase.client.service.kafka.producer;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.kafka.configuration.Acks;
import com.viveworks.appbase.client.service.kafka.configuration.ClientDnsLookup;
import com.viveworks.appbase.client.service.kafka.configuration.CompressionType;
import com.viveworks.appbase.client.service.kafka.configuration.MetricRecordingLevel;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.configuration.KafkaProducerPropertyNames;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.Serializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link KafkaProducerClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public class DefaultKafkaProducerClientIntegrationService<K, V>
        extends AbstractAppbaseClientIntegrationService
        <Producer<K, V>, ClusterIdValue, DefaultKafkaProducerClientIntegrationServiceConfiguration>
        implements KafkaProducerClientIntegrationService<K, V>
{
    private final AtomicReference<Producer<K, V>> producerRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultKafkaProducerClientIntegrationServiceConfiguration
            defaultKafkaProducerClientIntegrationServiceConfiguration;

    private final AtomicBoolean shutdown;

    /**
     * Driver-native metrics binder for the pooled synchronous {@code Producer}, bound onto Appbase's
     * shared Micrometer composite registry. Non-final and nullable: {@code null} when driver metrics
     * are disabled or before {@code initImpl()} runs. Only the BINDER is closed on shutdown; the
     * composite registry is owned by {@code MetricService} and is NEVER shut down here.
     */
    @NullableWhenUnused(info = "null when driver metrics disabled or before initImpl")
    private KafkaClientMetrics pooledProducerMetrics;


    @SuppressWarnings("unchecked")
    public DefaultKafkaProducerClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Producer<K, V>, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final Producer<K, V> producer)
    {
        super(appbaseClientCore,
                appbaseClientPool,
                appbaseClientKey,
                (Class<Producer<K, V>>) AppbaseUtil.notNullType(Producer.class));

        AppbaseUtil.notNull(producer, "producer");

        this.producerRef = new AtomicReference<>(producer);
        this.defaultKafkaProducerClientIntegrationServiceConfiguration = null;
        this.shutdown = new AtomicBoolean(false);
    }

    @SuppressWarnings("unchecked")
    public DefaultKafkaProducerClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Producer<K, V>, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultKafkaProducerClientIntegrationServiceConfiguration
                    defaultKafkaProducerClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore,
                appbaseClientPool,
                appbaseClientKey,
                (Class<Producer<K, V>>) AppbaseUtil.notNullType(Producer.class));

        AppbaseUtil.notNull(
                defaultKafkaProducerClientIntegrationServiceConfiguration,
                "defaultKafkaProducerClientIntegrationServiceConfiguration");

        this.defaultKafkaProducerClientIntegrationServiceConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration;
        this.producerRef = new AtomicReference<>();
        this.shutdown = new AtomicBoolean(false);

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }


    /**
     * Builds the driver {@link Properties} from configuration. Extracted from {@link #initImpl()}
     * so it is shared by the synchronous client build here and the reactive {@code SenderOptions}
     * built by the producer client service — a single source of truth for producer configuration.
     * Promoted to {@code public} and {@code @Override} of
     * {@link KafkaProducerClientIntegrationService#buildProducerProperties()} so the client service
     * can build the reactor-kafka sender from these same properties without a same-package cast;
     * behaviour is identical to the previous package-private block.
     *
     * @return Populated producer {@link Properties}
     */
    @Override
    @SuppressWarnings({"checkstyle:MethodLength", "rawtypes"})
    public Properties buildProducerProperties() {
        final Properties properties = new Properties();


        final Class<? extends Serializer> keySerializerType =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getKeySerializerType();
        AppbaseUtil.notNull(keySerializerType, "keySerializerType");
        properties.put(
                KafkaProducerPropertyNames.KEY_SERIALIZER.getText(),
                keySerializerType.getName());


        final Class<? extends Serializer> valueSerializerType =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getValueSerializerType();
        AppbaseUtil.notNull(valueSerializerType, "valueSerializerType");
        properties.put(
                KafkaProducerPropertyNames.VALUE_SERIALIZER.getText(),
                valueSerializerType.getName());


        final Acks acks = defaultKafkaProducerClientIntegrationServiceConfiguration.getAcks();
        AppbaseUtil.notNull(acks, "acks");
        properties.put(
                KafkaProducerPropertyNames.ACKS.getText(),
                acks.getText());


        final Set<HostPortConfiguration> bootstrapServerConfigurations =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getBootstrapServerConfigurations();
        final String bootstrapServers = AppbaseClientUtil.hostPortCsv(bootstrapServerConfigurations);
        properties.put(
                KafkaProducerPropertyNames.BOOTSTRAP_SERVERS.getText(),
                bootstrapServers);


        final Integer batchSize =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getBatchSize();
        if (batchSize != null) {
            properties.put(
                    KafkaProducerPropertyNames.BATCH_SIZE.getText(),
                    batchSize.toString());
        }


        final DataSizeConfiguration bufferMemorySizeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getBufferMemorySizeConfiguration();
        if (bufferMemorySizeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.BUFFER_MEMORY.getText(),
                    AppbaseClientUtil.toStringBytes(bufferMemorySizeConfiguration));
        }


        final ClientDnsLookup clientDnsLookup =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getClientDnsLookup();
        if (clientDnsLookup != null) {
            properties.put(
                    KafkaProducerPropertyNames.CLIENT_DNS_LOOKUP.getText(),
                    clientDnsLookup.getText());
        }


        final String clientId =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getClientId();
        if (clientId != null) {
            properties.put(
                    KafkaProducerPropertyNames.CLIENT_ID.getText(),
                    clientId);
        }


        final CompressionType compressionType =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getCompressionType();
        if (compressionType != null) {
            properties.put(
                    KafkaProducerPropertyNames.COMPRESSION_TYPE.getText(),
                    compressionType.getText());
        }


        final TimeUnitConfiguration connectionsMaxIdleConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getConnectionsMaxIdleConfiguration();
        if (connectionsMaxIdleConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.CONNECTIONS_MAX_IDLE_MS.getText(),
                    AppbaseClientUtil.toStringMillis(connectionsMaxIdleConfiguration));
        }


        final TimeoutConfiguration deliveryTimeoutConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getDeliveryTimeoutConfiguration();
        if (deliveryTimeoutConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.DELIVERY_TIMEOUT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(deliveryTimeoutConfiguration));
        }


        final Boolean enableIdempotence =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getEnableIdempotence();
        if (enableIdempotence != null) {
            properties.put(
                    KafkaProducerPropertyNames.ENABLE_IDEMPOTENCE.getText(),
                    enableIdempotence.toString());
        }


        final List<Class<? extends ProducerInterceptor>> interceptorTypes =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getInterceptorTypes();
        if (interceptorTypes != null) {
            properties.put(
                    KafkaProducerPropertyNames.INTERCEPTOR_CLASSES.getText(),
                    AppbaseClientUtil.classNamesCsv(interceptorTypes));
        }


        final TimeUnitConfiguration lingerTimeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getLingerTimeConfiguration();
        if (lingerTimeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.LINGER_MS.getText(),
                    AppbaseClientUtil.toStringMillis(lingerTimeConfiguration));
        }


        final TimeUnitConfiguration maxBlockTimeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMaxBlockTimeConfiguration();
        if (maxBlockTimeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.MAX_BLOCK_MS.getText(),
                    AppbaseClientUtil.toStringMillis(maxBlockTimeConfiguration));
        }


        final Integer maxInFlightRequestsPerConnection =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMaxInFlightRequestsPerConnection();
        if (maxInFlightRequestsPerConnection != null) {
            properties.put(
                    KafkaProducerPropertyNames.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION.getText(),
                    maxInFlightRequestsPerConnection.toString());
        }


        final DataSizeConfiguration maxRequestSizeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMaxRequestSizeConfiguration();
        if (maxRequestSizeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.MAX_REQUEST_SIZE.getText(),
                    AppbaseClientUtil.toStringBytes(maxRequestSizeConfiguration));
        }


        final TimeUnitConfiguration metadataMaxAgeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMetadataMaxAgeConfiguration();
        if (metadataMaxAgeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.METADATA_MAX_AGE_MS.getText(),
                    AppbaseClientUtil.toStringMillis(metadataMaxAgeConfiguration));
        }


        final TimeUnitConfiguration metadataMaxIdleConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMetadataMaxIdleConfiguration();
        if (metadataMaxIdleConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.METADATA_MAX_IDLE_MS.getText(),
                    AppbaseClientUtil.toStringMillis(metadataMaxIdleConfiguration));
        }


        final Integer metricsNumSamples =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMetricsNumSamples();
        if (metricsNumSamples != null) {
            properties.put(
                    KafkaProducerPropertyNames.METRICS_NUM_SAMPLES.getText(),
                    metricsNumSamples.toString());
        }


        final MetricRecordingLevel metricsRecordingLevel =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMetricsRecordingLevel();
        if (metricsRecordingLevel != null) {
            properties.put(
                    KafkaProducerPropertyNames.METRICS_RECORDING_LEVEL.getText(),
                    metricsRecordingLevel.getText());
        }


        final List<Class<? extends MetricsReporter>> metricsReporterTypes =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMetricsReporterTypes();
        if (metricsReporterTypes != null) {
            properties.put(
                    KafkaProducerPropertyNames.METRIC_REPORTERS.getText(),
                    AppbaseClientUtil.classNamesCsv(metricsReporterTypes));
        }


        final TimeUnitConfiguration metricsSampleWindowConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getMetricsSampleWindowConfiguration();
        if (metricsSampleWindowConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.METRICS_SAMPLE_WINDOW_MS.getText(),
                    AppbaseClientUtil.toStringMillis(metricsSampleWindowConfiguration));
        }


        final Class<? extends Partitioner> partitionerType =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getPartitionerType();
        if (partitionerType != null) {
            properties.put(
                    KafkaProducerPropertyNames.PARTITIONER_CLASS.getText(),
                    partitionerType.getName());
        }


        final DataSizeConfiguration receiveBufferSizeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getReceiveBufferSizeConfiguration();
        if (receiveBufferSizeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.RECEIVE_BUFFER_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(receiveBufferSizeConfiguration));
        }


        final TimeUnitConfiguration reconnectBackoffConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getReconnectBackoffConfiguration();
        if (reconnectBackoffConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.RECONNECT_BACKOFF_MS.getText(),
                    AppbaseClientUtil.toStringMillis(reconnectBackoffConfiguration));
        }

        final TimeUnitConfiguration reconnectBackoffMaxConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getReconnectBackoffMaxConfiguration();
        if (reconnectBackoffMaxConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.RECONNECT_BACKOFF_MAX_MS.getText(),
                    AppbaseClientUtil.toStringMillis(reconnectBackoffMaxConfiguration));
        }


        final TimeoutConfiguration requestTimeoutConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getRequestTimeoutConfiguration();
        if (requestTimeoutConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.REQUEST_TIMEOUT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(requestTimeoutConfiguration));
        }


        final TimeUnitConfiguration retryBackoffConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getRetryBackoffConfiguration();
        if (retryBackoffConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.RETRY_BACKOFF_MS.getText(),
                    AppbaseClientUtil.toStringMillis(retryBackoffConfiguration));
        }

        final Integer retries = defaultKafkaProducerClientIntegrationServiceConfiguration.getRetries();
        if (retries != null) {
            properties.put(
                    KafkaProducerPropertyNames.RETRIES.getText(),
                    retries.toString());
        }


        final DataSizeConfiguration sendBufferSizeConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getSendBufferSizeConfiguration();
        if (sendBufferSizeConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.SEND_BUFFER_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(sendBufferSizeConfiguration));
        }


        final String transactionalId =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getTransactionalId();
        if (transactionalId != null) {
            properties.put(
                    KafkaProducerPropertyNames.TRANSACTIONAL_ID.getText(),
                    transactionalId);
        }


        final TimeoutConfiguration transactionTimeoutConfiguration =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getTransactionTimeoutConfiguration();
        if (transactionTimeoutConfiguration != null) {
            properties.put(
                    KafkaProducerPropertyNames.TRANSACTION_TIMEOUT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(transactionTimeoutConfiguration));
        }


        final SecurityProtocol securityProtocol =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getSecurityProtocol();
        if (securityProtocol != null) {
            properties.put(
                    KafkaProducerPropertyNames.SECURITY_PROTOCOL.getText(),
                    securityProtocol.getText());
        }


        final Long socketConnectionSetupTimeoutMs =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getSocketConnectionSetupTimeoutMs();
        if (socketConnectionSetupTimeoutMs != null) {
            properties.put(
                    KafkaProducerPropertyNames.SOCKET_CONNECTION_SETUP_TIMEOUT_MS.getText(),
                    socketConnectionSetupTimeoutMs.toString());
        }


        final Long socketConnectionSetupTimeoutMaxMs =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getSocketConnectionSetupTimeoutMaxMs();
        if (socketConnectionSetupTimeoutMaxMs != null) {
            properties.put(
                    KafkaProducerPropertyNames.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS.getText(),
                    socketConnectionSetupTimeoutMaxMs.toString());
        }


        final Long partitionerAvailabilityTimeoutMs =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getPartitionerAvailabilityTimeoutMs();
        if (partitionerAvailabilityTimeoutMs != null) {
            properties.put(
                    KafkaProducerPropertyNames.PARTITIONER_AVAILABILITY_TIMEOUT_MS.getText(),
                    partitionerAvailabilityTimeoutMs.toString());
        }


        final Boolean partitionerIgnoreKeys =
                defaultKafkaProducerClientIntegrationServiceConfiguration.getPartitionerIgnoreKeys();
        if (partitionerIgnoreKeys != null) {
            properties.put(
                    KafkaProducerPropertyNames.PARTITIONER_IGNORE_KEYS.getText(),
                    partitionerIgnoreKeys.toString());
        }


        return properties;
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultKafkaProducerClientIntegrationServiceConfiguration,
                "defaultKafkaProducerClientIntegrationServiceConfiguration");

        final Properties properties = buildProducerProperties();

        final Producer<K, V> producer = new KafkaProducer<>(properties);

        // Route driver-native metrics onto Appbase's shared Micrometer composite (→ JMX) when enabled.
        this.pooledProducerMetrics = bindDriverMetrics(producer, isDriverMetricsEnabled());

        this.producerRef.set(producer);
        this.shutdown.set(false);
    }

    /**
     * Binds Kafka driver-native metrics for the given {@link Producer} onto Appbase's shared
     * Micrometer composite registry (owned by {@code MetricService}, auto-exported to JMX) when
     * driver metrics are enabled. Package-private testable seam: {@link KafkaClientMetrics#bindTo}
     * only registers gauges that poll the producer's {@code metrics()} — it does NOT connect — so
     * this can be unit-tested with a mock {@link Producer} without running the full {@code initImpl()}
     * config/pool graph.
     *
     * <p>Lifecycle: the returned binder is stored and {@code close()}d on shutdown; the composite
     * registry is NEVER shut down here.</p>
     *
     * @param producer the driver producer whose {@code metrics()} are polled
     * @param driverMetricsEnabled whether driver-native metrics binding is enabled
     * @return the bound {@link KafkaClientMetrics} binder, or {@code null} when disabled
     */
    KafkaClientMetrics bindDriverMetrics(final Producer<K, V> producer, final boolean driverMetricsEnabled) {
        if (!driverMetricsEnabled) {
            return null;
        }

        final KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(producer);
        kafkaClientMetrics.bindTo(
                appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry());
        return kafkaClientMetrics;
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return defaultKafkaProducerClientIntegrationServiceConfiguration != null
                && defaultKafkaProducerClientIntegrationServiceConfiguration.isDriverMetricsEnabled();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultKafkaProducerClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        // Close the driver-metrics binder BEFORE closing the producer. Close ONLY the binder — the
        // composite registry is owned by MetricService and must never be shut down here.
        if (pooledProducerMetrics != null) {
            pooledProducerMetrics.close();
            pooledProducerMetrics = null;
        }

        final Producer<K, V> producer = getClient();

        if (producer != null) {
            boolean closed = false;
            if (defaultKafkaProducerClientIntegrationServiceConfiguration != null) {
                final Duration closeDuration = defaultKafkaProducerClientIntegrationServiceConfiguration.getCloseDuration();

                if (closeDuration != null) {
                    producer.close(closeDuration);
                    closed = true;
                }
            }

            if (!closed) {
                producer.close();
            }
        }

        producerRef.set(null);
        shutdown.set(true);
    }

    @Override
    public Producer<K, V> getClient() {
        return producerRef.get();
    }

    @Override
    public boolean isHealthy() {
        final Producer<K, V> producer = getClient();
        if (producer == null) {
            return false;
        }

        return !shutdown.get();
    }
}

