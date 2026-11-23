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

package com.viveworks.appbase.client.service.kafka.consumer;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.kafka.configuration.AutoOffsetReset;
import com.viveworks.appbase.client.service.kafka.configuration.ClientDnsLookup;
import com.viveworks.appbase.client.service.kafka.configuration.GroupProtocol;
import com.viveworks.appbase.client.service.kafka.configuration.IsolationLevel;
import com.viveworks.appbase.client.service.kafka.configuration.MetricRecordingLevel;
import com.viveworks.appbase.client.service.kafka.configuration.SecurityProtocol;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.KafkaConsumerPropertyNames;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.Deserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link KafkaConsumerClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public class DefaultKafkaConsumerClientIntegrationService<K, V>
        extends AbstractAppbaseClientIntegrationService
        <Consumer<K, V>, ClusterIdValue, DefaultKafkaConsumerClientIntegrationServiceConfiguration>
        implements KafkaConsumerClientIntegrationService<K, V>
{
    private final AtomicReference<Consumer<K, V>> consumerRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultKafkaConsumerClientIntegrationServiceConfiguration
            defaultKafkaConsumerClientIntegrationServiceConfiguration;

    private final AtomicBoolean shutdown;

    /**
     * Driver-native metrics binder for the pooled synchronous {@code Consumer}, bound onto Appbase's
     * shared Micrometer composite registry. Non-final and nullable: {@code null} when driver metrics
     * are disabled or before {@code initImpl()} runs. Only the BINDER is closed on shutdown; the
     * composite registry is owned by {@code MetricService} and is NEVER shut down here.
     */
    @NullableWhenUnused(info = "null when driver metrics disabled or before initImpl")
    private KafkaClientMetrics pooledConsumerMetrics;


    @SuppressWarnings("unchecked")
    public DefaultKafkaConsumerClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Consumer<K, V>, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final Consumer<K, V> consumer)
    {
        super(appbaseClientCore,
                appbaseClientPool,
                appbaseClientKey,
                (Class<Consumer<K, V>>) AppbaseUtil.notNullType(Consumer.class));

        AppbaseUtil.notNull(consumer, "consumer");

        this.consumerRef = new AtomicReference<>(consumer);
        this.defaultKafkaConsumerClientIntegrationServiceConfiguration = null;
        this.shutdown = new AtomicBoolean(false);
    }

    @SuppressWarnings("unchecked")
    public DefaultKafkaConsumerClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<Consumer<K, V>, ClusterIdValue> appbaseClientPool,
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey,
            final DefaultKafkaConsumerClientIntegrationServiceConfiguration
                    defaultKafkaConsumerClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore,
                appbaseClientPool,
                appbaseClientKey,
                (Class<Consumer<K, V>>) AppbaseUtil.notNullType(Consumer.class));

        AppbaseUtil.notNull(
                defaultKafkaConsumerClientIntegrationServiceConfiguration,
                "defaultKafkaConsumerClientIntegrationServiceConfiguration");

        this.defaultKafkaConsumerClientIntegrationServiceConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration;
        this.consumerRef = new AtomicReference<>();
        this.shutdown = new AtomicBoolean(false);

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    /**
     * Builds the driver {@link Properties} from configuration. Extracted from {@link #initImpl()}
     * so it is shared by the synchronous client build here and the reactive {@code ReceiverOptions}
     * built by the consumer client service — a single source of truth for consumer configuration.
     * Promoted to {@code public} and {@code @Override} of
     * {@link KafkaConsumerClientIntegrationService#buildConsumerProperties()} so the client service
     * can build the reactor-kafka receiver from these same properties without a same-package cast;
     * behaviour is identical to the previous package-private block.
     *
     * @return Populated consumer {@link Properties}
     */
    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    public Properties buildConsumerProperties() {
        final Properties properties = new Properties();


        final Class<? extends Deserializer> keyDeserializerType =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getKeyDeserializerType();
        AppbaseUtil.notNull(keyDeserializerType, "keyDeserializerType");
        properties.put(
                KafkaConsumerPropertyNames.KEY_DESERIALIZER.getText(),
                keyDeserializerType.getName());


        final Class<? extends Deserializer> valueDeserializerType =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getValueDeserializerType();
        AppbaseUtil.notNull(valueDeserializerType, "valueDeserializerType");
        properties.put(
                KafkaConsumerPropertyNames.VALUE_DESERIALIZER.getText(),
                valueDeserializerType.getName());


        final Set<HostPortConfiguration> bootstrapServerConfigurations =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getBootstrapServerConfigurations();
        final String bootstrapServers = AppbaseClientUtil.hostPortCsv(bootstrapServerConfigurations);
        properties.put(
                KafkaConsumerPropertyNames.BOOTSTRAP_SERVERS.getText(),
                bootstrapServers);


        final String groupId = defaultKafkaConsumerClientIntegrationServiceConfiguration.getGroupId();
        if (groupId != null) {
            properties.put(
                    KafkaConsumerPropertyNames.GROUP_ID.getText(),
                    groupId);
        }


        final TimeUnitConfiguration autoCommitIntervalConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getAutoCommitIntervalConfiguration();
        if (autoCommitIntervalConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.AUTO_COMMIT_INTERVAL_MS.getText(),
                    AppbaseClientUtil.toStringMillis(autoCommitIntervalConfiguration));
        }


        final Boolean allowAutoCreateTopics =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getAllowAutoCreateTopics();
        if (allowAutoCreateTopics != null) {
            properties.put(
                    KafkaConsumerPropertyNames.ALLOW_AUTO_CREATE_TOPICS.getText(),
                    allowAutoCreateTopics.toString());
        }


        final AutoOffsetReset autoOffsetReset =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getAutoOffsetReset();
        if (autoOffsetReset != null) {
            properties.put(
                    KafkaConsumerPropertyNames.AUTO_OFFSET_RESET.getText(),
                    autoOffsetReset.getText());
        }


        final Boolean checkCrcs = defaultKafkaConsumerClientIntegrationServiceConfiguration.getCheckCrcs();
        if (checkCrcs != null) {
            properties.put(
                    KafkaConsumerPropertyNames.CHECK_CRCS.getText(),
                    checkCrcs.toString());
        }


        final ClientDnsLookup clientDnsLookup =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getClientDnsLookup();
        if (clientDnsLookup != null) {
            properties.put(
                    KafkaConsumerPropertyNames.CLIENT_DNS_LOOKUP.getText(),
                    clientDnsLookup.getText());
        }


        final String clientId =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getClientId();
        if (clientId != null) {
            properties.put(
                    KafkaConsumerPropertyNames.CLIENT_ID.getText(),
                    clientId);
        }


        final String clientRack =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getClientRack();
        if (clientRack != null) {
            properties.put(
                    KafkaConsumerPropertyNames.CLIENT_RACK.getText(),
                    clientRack);
        }


        final TimeUnitConfiguration connectionsMaxIdleConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getConnectionsMaxIdleConfiguration();
        if (connectionsMaxIdleConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.CONNECTIONS_MAX_IDLE_MS.getText(),
                    AppbaseClientUtil.toStringMillis(connectionsMaxIdleConfiguration));
        }


        final TimeoutConfiguration defaultApiTimeoutConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getDefaultApiTimeoutConfiguration();
        if (defaultApiTimeoutConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.DEFAULT_API_TIMEOUT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(defaultApiTimeoutConfiguration));
        }


        final Boolean enableAutoCommit =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getEnableAutoCommit();
        if (enableAutoCommit != null) {
            properties.put(
                    KafkaConsumerPropertyNames.ENABLE_AUTO_COMMIT.getText(),
                    enableAutoCommit.toString());
        }


        final Boolean excludeInternalTopics =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getExcludeInternalTopics();
        if (excludeInternalTopics != null) {
            properties.put(
                    KafkaConsumerPropertyNames.EXCLUDE_INTERNAL_TOPICS.getText(),
                    excludeInternalTopics.toString());
        }


        final DataSizeConfiguration fetchMaxDataSizeConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getFetchMaxDataSizeConfiguration();
        if (fetchMaxDataSizeConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.FETCH_MAX_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(fetchMaxDataSizeConfiguration));
        }


        final TimeUnitConfiguration fetchMaxWaitConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getFetchMaxWaitConfiguration();
        if (fetchMaxWaitConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.FETCH_MAX_WAIT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(fetchMaxWaitConfiguration));
        }


        final DataSizeConfiguration fetchMinDataSizeConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getFetchMinDataSizeConfiguration();
        if (fetchMinDataSizeConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.FETCH_MIN_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(fetchMinDataSizeConfiguration));
        }


        final String groupInstanceId =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getGroupInstanceId();
        if (groupInstanceId != null) {
            properties.put(
                    KafkaConsumerPropertyNames.GROUP_INSTANCE_ID.getText(),
                    groupInstanceId);
        }


        final TimeUnitConfiguration heartbeatIntervalConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getHeartbeatIntervalConfiguration();
        if (heartbeatIntervalConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.HEARTBEAT_INTERVAL_MS.getText(),
                    AppbaseClientUtil.toStringMillis(heartbeatIntervalConfiguration));
        }


        final List<Class<? extends ConsumerInterceptor>> interceptorTypes =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getInterceptorTypes();
        if (interceptorTypes != null) {
            properties.put(
                    KafkaConsumerPropertyNames.INTERCEPTOR_CLASSES.getText(),
                    AppbaseClientUtil.classNamesCsv(interceptorTypes));
        }


        final IsolationLevel isolationLevel =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getIsolationLevel();
        if (isolationLevel != null) {
            properties.put(
                    KafkaConsumerPropertyNames.ISOLATION_LEVEL.getText(),
                    isolationLevel.getText());
        }


        final DataSizeConfiguration maxPartitionFetchSizeConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getMaxPartitionFetchSizeConfiguration();
        if (maxPartitionFetchSizeConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.MAX_PARTITION_FETCH_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(maxPartitionFetchSizeConfiguration));
        }


        final TimeUnitConfiguration maxPollIntervalConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getMaxPollIntervalConfiguration();
        if (maxPollIntervalConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.MAX_POLL_INTERVAL_MS.getText(),
                    AppbaseClientUtil.toStringMillis(maxPollIntervalConfiguration));
        }


        final Integer maxPollRecords = defaultKafkaConsumerClientIntegrationServiceConfiguration.getMaxPollRecords();
        if (maxPollRecords != null) {
            properties.put(
                    KafkaConsumerPropertyNames.MAX_POLL_RECORDS.getText(),
                    maxPollRecords.toString());
        }


        final TimeUnitConfiguration metadataMaxAgeConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getMetadataMaxAgeConfiguration();
        if (metadataMaxAgeConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.METADATA_MAX_AGE_MS.getText(),
                    AppbaseClientUtil.toStringMillis(metadataMaxAgeConfiguration));
        }


        final Integer metricsNumSamples = defaultKafkaConsumerClientIntegrationServiceConfiguration.getMetricsNumSamples();
        if (metricsNumSamples != null) {
            properties.put(
                    KafkaConsumerPropertyNames.METRICS_NUM_SAMPLES.getText(),
                    metricsNumSamples.toString());
        }


        final MetricRecordingLevel metricsRecordingLevel =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getMetricsRecordingLevel();
        if (metricsRecordingLevel != null) {
            properties.put(
                    KafkaConsumerPropertyNames.METRICS_RECORDING_LEVEL.getText(),
                    metricsRecordingLevel.getText());
        }


        final List<Class<? extends MetricsReporter>> metricsReporterTypes =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getMetricsReporterTypes();
        if (metricsReporterTypes != null) {
            properties.put(
                    KafkaConsumerPropertyNames.METRIC_REPORTERS.getText(),
                    AppbaseClientUtil.classNamesCsv(metricsReporterTypes));
        }


        final TimeUnitConfiguration metricsSampleWindowConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getMetricsSampleWindowConfiguration();
        if (metricsSampleWindowConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.METRICS_SAMPLE_WINDOW_MS.getText(),
                    AppbaseClientUtil.toStringMillis(metricsSampleWindowConfiguration));
        }


        final List<Class<? extends ConsumerPartitionAssignor>> partitionAssignmentStrategyTypes =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getPartitionAssignmentStrategyTypes();
        if (partitionAssignmentStrategyTypes != null) {
            properties.put(
                    KafkaConsumerPropertyNames.PARTITION_ASSIGNMENT_STRATEGY.getText(),
                    AppbaseClientUtil.classNamesCsv(partitionAssignmentStrategyTypes));
        }


        final DataSizeConfiguration receiveBufferSizeConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getReceiveBufferSizeConfiguration();
        if (receiveBufferSizeConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.RECEIVE_BUFFER_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(receiveBufferSizeConfiguration));
        }


        final TimeUnitConfiguration reconnectBackoffConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getReconnectBackoffConfiguration();
        if (reconnectBackoffConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.RECONNECT_BACKOFF_MS.getText(),
                    AppbaseClientUtil.toStringMillis(reconnectBackoffConfiguration));
        }


        final TimeUnitConfiguration reconnectBackoffMaxConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getReconnectBackoffMaxConfiguration();
        if (reconnectBackoffMaxConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.RECONNECT_BACKOFF_MAX_MS.getText(),
                    AppbaseClientUtil.toStringMillis(reconnectBackoffMaxConfiguration));
        }


        final TimeoutConfiguration requestTimeoutConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getRequestTimeoutConfiguration();
        if (requestTimeoutConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.REQUEST_TIMEOUT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(requestTimeoutConfiguration));
        }


        final TimeUnitConfiguration retryBackoffConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getRetryBackoffConfiguration();
        if (retryBackoffConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.RETRY_BACKOFF_MS.getText(),
                    AppbaseClientUtil.toStringMillis(retryBackoffConfiguration));
        }


        final DataSizeConfiguration sendBufferSizeConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getSendBufferSizeConfiguration();
        if (sendBufferSizeConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.SEND_BUFFER_BYTES.getText(),
                    AppbaseClientUtil.toStringBytes(sendBufferSizeConfiguration));
        }


        final TimeoutConfiguration sessionTimeoutConfiguration =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getSessionTimeoutConfiguration();
        if (sessionTimeoutConfiguration != null) {
            properties.put(
                    KafkaConsumerPropertyNames.SESSION_TIMEOUT_MS.getText(),
                    AppbaseClientUtil.toStringMillis(sessionTimeoutConfiguration));
        }


        final SecurityProtocol securityProtocol =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getSecurityProtocol();
        if (securityProtocol != null) {
            properties.put(
                    KafkaConsumerPropertyNames.SECURITY_PROTOCOL.getText(),
                    securityProtocol.getText());
        }


        final Long socketConnectionSetupTimeoutMs =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getSocketConnectionSetupTimeoutMs();
        if (socketConnectionSetupTimeoutMs != null) {
            properties.put(
                    KafkaConsumerPropertyNames.SOCKET_CONNECTION_SETUP_TIMEOUT_MS.getText(),
                    socketConnectionSetupTimeoutMs.toString());
        }


        final Long socketConnectionSetupTimeoutMaxMs =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getSocketConnectionSetupTimeoutMaxMs();
        if (socketConnectionSetupTimeoutMaxMs != null) {
            properties.put(
                    KafkaConsumerPropertyNames.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS.getText(),
                    socketConnectionSetupTimeoutMaxMs.toString());
        }


        final GroupProtocol groupProtocol =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getGroupProtocol();
        if (groupProtocol != null) {
            properties.put(
                    KafkaConsumerPropertyNames.GROUP_PROTOCOL.getText(),
                    groupProtocol.getText());
        }


        final Long retryBackoffMaxMs =
                defaultKafkaConsumerClientIntegrationServiceConfiguration.getRetryBackoffMaxMs();
        if (retryBackoffMaxMs != null) {
            properties.put(
                    KafkaConsumerPropertyNames.RETRY_BACKOFF_MAX_MS.getText(),
                    retryBackoffMaxMs.toString());
        }


        return properties;
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultKafkaConsumerClientIntegrationServiceConfiguration,
                "defaultKafkaConsumerClientIntegrationServiceConfiguration");

        final Properties properties = buildConsumerProperties();

        final Consumer<K, V> consumer = new KafkaConsumer<>(properties);

        // Route driver-native metrics onto Appbase's shared Micrometer composite (→ JMX) when enabled.
        this.pooledConsumerMetrics = bindDriverMetrics(consumer, isDriverMetricsEnabled());

        this.consumerRef.set(consumer);
        this.shutdown.set(false);
    }

    /**
     * Binds Kafka driver-native metrics for the given {@link Consumer} onto Appbase's shared
     * Micrometer composite registry (owned by {@code MetricService}, auto-exported to JMX) when
     * driver metrics are enabled. Package-private testable seam: {@link KafkaClientMetrics#bindTo}
     * only registers gauges that poll the consumer's {@code metrics()} — it does NOT connect — so
     * this can be unit-tested with a mock {@link Consumer} without running the full {@code initImpl()}
     * config/pool graph.
     *
     * <p>Lifecycle: the returned binder is stored and {@code close()}d on shutdown; the composite
     * registry is NEVER shut down here.</p>
     *
     * @param consumer the driver consumer whose {@code metrics()} are polled
     * @param driverMetricsEnabled whether driver-native metrics binding is enabled
     * @return the bound {@link KafkaClientMetrics} binder, or {@code null} when disabled
     */
    KafkaClientMetrics bindDriverMetrics(final Consumer<K, V> consumer, final boolean driverMetricsEnabled) {
        if (!driverMetricsEnabled) {
            return null;
        }

        final KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(consumer);
        kafkaClientMetrics.bindTo(
                appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry());
        return kafkaClientMetrics;
    }

    @Override
    public boolean isDriverMetricsEnabled() {
        return defaultKafkaConsumerClientIntegrationServiceConfiguration != null
                && defaultKafkaConsumerClientIntegrationServiceConfiguration.isDriverMetricsEnabled();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultKafkaConsumerClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        // Close the driver-metrics binder BEFORE closing the consumer. Close ONLY the binder — the
        // composite registry is owned by MetricService and must never be shut down here.
        if (pooledConsumerMetrics != null) {
            pooledConsumerMetrics.close();
            pooledConsumerMetrics = null;
        }

        final Consumer<K, V> consumer = getClient();

        if (consumer != null) {
            boolean closed = false;
            if (defaultKafkaConsumerClientIntegrationServiceConfiguration != null) {
                final Duration closeDuration = defaultKafkaConsumerClientIntegrationServiceConfiguration.getCloseDuration();

                if (closeDuration != null) {
                    consumer.close(closeDuration);
                    closed = true;
                }
            }

            if (!closed) {
                consumer.close();
            }
        }

        consumerRef.set(null);
        shutdown.set(true);
    }

    @Override
    public Consumer<K, V> getClient() {
        return consumerRef.get();
    }

    @Override
    public boolean isHealthy() {
        final Consumer<K, V> consumer = getClient();
        if (consumer == null) {
            return false;
        }

        return !shutdown.get();
    }

    /**
     * The configured reactive subscription topics, sourced from the SAME configuration the driver
     * {@link Properties} are built from (single source of truth). Returns {@code null} when this
     * integration service was created directly from a {@link Consumer} (no configuration) — the
     * reactive receiver path always uses the configuration-based integration service.
     *
     * @return Subscription topics, or {@code null}/empty if none configured
     */
    @Override
    public List<String> subscriptionTopics() {
        if (defaultKafkaConsumerClientIntegrationServiceConfiguration == null) {
            return null;
        }
        return defaultKafkaConsumerClientIntegrationServiceConfiguration.getSubscriptionTopics();
    }
}
