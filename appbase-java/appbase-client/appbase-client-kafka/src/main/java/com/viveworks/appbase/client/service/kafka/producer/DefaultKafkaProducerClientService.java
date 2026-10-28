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

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.configuration.DefaultKafkaProducerClientServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.producer.reactive.ReactiveKafkaProducerAccessor;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.producer.Producer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link KafkaProducerClientService}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public class DefaultKafkaProducerClientService<K, V>
        extends AbstractAppbaseClientService<Producer<K, V>, ClusterIdValue, DefaultKafkaProducerClientServiceConfiguration>
        implements KafkaProducerClientService<K, V>
{
    /**
     * Per-key cache of service-OWNED reactor-kafka senders. A {@link KafkaSender} owns a Kafka
     * {@code Producer} plus its background network threads, so it must be reused per key (not rebuilt
     * per accessor) and CLOSED on shutdown — see {@link #shutdownImpl()}.
     */
    private final ConcurrentMap<AppbaseClientKey<ClusterIdValue>, KafkaSender<K, V>> reactiveSenders =
            new ConcurrentHashMap<>();

    /**
     * Per-key driver-native metrics binders for the reactor-kafka senders, each bound onto Appbase's
     * shared Micrometer composite registry. Tracked so every binder is {@code close()}d with its
     * sender on shutdown — see {@link #shutdownImpl()}. The composite registry is owned by
     * {@code MetricService} and is NEVER shut down here; only the binders are closed.
     */
    private final ConcurrentMap<AppbaseClientKey<ClusterIdValue>, KafkaClientMetrics> senderMetrics =
            new ConcurrentHashMap<>();

    @Inject
    public DefaultKafkaProducerClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultKafkaProducerClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultKafkaProducerClientService(
            final AppbaseClientCore appbaseClientCore,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultKafkaProducerClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaProducerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Producer<K, V>, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultKafkaProducerClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaProducerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultKafkaProducerClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaProducerClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultKafkaProducerClientServiceConfiguration defaultKafkaProducerClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultKafkaProducerClientServiceConfiguration.class,
                defaultKafkaProducerClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaProducerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Producer<K, V>, ClusterIdValue> appbaseClientPool,
            final DefaultKafkaProducerClientServiceConfiguration defaultKafkaProducerClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultKafkaProducerClientServiceConfiguration.class,
                defaultKafkaProducerClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaProducerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultKafkaProducerClientServiceConfiguration defaultKafkaProducerClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultKafkaProducerClientServiceConfiguration.class,
                defaultKafkaProducerClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultKafkaProducerClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultKafkaProducerClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultKafkaProducerClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultKafkaProducerClientIntegrationServiceConfiguration
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration,
                "defaultDefaultKafkaProducerClientIntegrationServiceConfiguration");

        return KafkaProducerClientUtil.appbaseClientKey(defaultDefaultKafkaProducerClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<Producer<K, V>, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultKafkaProducerClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<Producer<K, V>, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultKafkaProducerClientIntegrationServiceConfiguration
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultKafkaProducerClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultKafkaProducerClientIntegrationServiceConfiguration,
                "defaultDefaultKafkaProducerClientIntegrationServiceConfiguration");


        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultKafkaProducerClientIntegrationService<K, V> defaultDefaultKafkaProducerClientIntegrationService =
                new DefaultKafkaProducerClientIntegrationService<>(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultKafkaProducerClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultKafkaProducerClientIntegrationService);


        final Set<DefaultKafkaProducerClientIntegrationServiceConfiguration>
                keyedDefaultKafkaProducerClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultKafkaProducerClientIntegrationServiceConfigurations();
        if (keyedDefaultKafkaProducerClientIntegrationServiceConfigurations != null) {
            keyedDefaultKafkaProducerClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultKafkaProducerClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                KafkaProducerClientUtil.appbaseClientKey(
                                        keyedDefaultKafkaProducerClientIntegrationServiceConfiguration);
                        final DefaultKafkaProducerClientIntegrationService<K, V>
                                defaultKafkaProducerClientIntegrationService =
                                new DefaultKafkaProducerClientIntegrationService<>(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        appbaseClientKey,
                                        keyedDefaultKafkaProducerClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultKafkaProducerClientIntegrationService);
                    });
        }

        return appbaseClientIntegrationServices;
    }

    @Override
    protected void registerService() {
        super.registerService();

        appbaseClientCore
                .getAppbaseCore()
                .getServiceRegistry()
                .registerAppbaseService(KafkaProducerClientService.class, this);
    }

    @Override
    public KafkaProducerClientIntegrationService<K, V> getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (KafkaProducerClientIntegrationService<K, V>) super.getClientIntegrationService();
    }

    @Override
    public KafkaProducerClientIntegrationService<K, V> getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (KafkaProducerClientIntegrationService<K, V>) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public KafkaProducerAccessor<K, V> getAccessor() {
        return new KafkaProducerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public KafkaProducerAccessor<K, V> getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new KafkaProducerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public ReactiveKafkaProducerAccessor<K, V> getReactiveAccessor() {
        final AppbaseClientKey<ClusterIdValue> key = getDefaultAppbaseClientKey();
        final KafkaSender<K, V> sender =
                reactiveSenders.computeIfAbsent(key, k -> buildSender(k, clientIntegrationService()));
        return new ReactiveKafkaProducerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                sender);
    }

    @Override
    public ReactiveKafkaProducerAccessor<K, V> getReactiveAccessor(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        final KafkaSender<K, V> sender =
                reactiveSenders.computeIfAbsent(
                        appbaseClientKey, k -> buildSender(k, clientIntegrationService(appbaseClientKey)));
        return new ReactiveKafkaProducerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                sender);
    }

    /**
     * Builds a reactor-kafka sender from the SAME driver {@link java.util.Properties} the pooled
     * synchronous producer is built from (one config source), via
     * {@link KafkaProducerClientIntegrationService#buildProducerProperties()}.
     *
     * <p>When the integration service has driver metrics enabled, Kafka driver-native metrics are
     * bound onto Appbase's shared Micrometer composite registry (→ JMX) via
     * {@link KafkaSender#doOnProducer}: subscribing lazily creates the reactor-kafka producer, whose
     * producer proxy whitelists {@code metrics()} — which is all {@link KafkaClientMetrics} polls. The
     * resulting binder is tracked per key in {@link #senderMetrics} and {@code close()}d with the
     * sender on shutdown; the composite registry is owned by {@code MetricService} and is NEVER shut
     * down here — only the binder is closed.</p>
     *
     * @param key Client key this sender is being built for (used to track its metrics binder)
     * @param integrationService Integration service supplying the producer properties
     * @return A new reactor-kafka {@link KafkaSender}
     */
    private KafkaSender<K, V> buildSender(
            final AppbaseClientKey<ClusterIdValue> key,
            final KafkaProducerClientIntegrationService<K, V> integrationService) {
        final KafkaSender<K, V> sender =
                KafkaSender.create(SenderOptions.<K, V>create(integrationService.buildProducerProperties()));

        if (integrationService.isDriverMetricsEnabled()) {
            // block() so the binder is stored SYNCHRONOUSLY before this method returns: doOnProducer runs
            // its function on the sender's scheduler, so a plain subscribe() would store the binder
            // asynchronously and a shutdown racing acquisition could miss (never close) it. Producer
            // creation here is local (no broker connect), so the block is bounded.
            final KafkaClientMetrics kafkaClientMetrics = sender.doOnProducer(producer -> {
                final KafkaClientMetrics binder = new KafkaClientMetrics(producer);
                binder.bindTo(
                        appbaseClientCore.getAppbaseCore().getMetricService().getCompositeMeterRegistry());
                return binder;
            }).block();
            if (kafkaClientMetrics != null) {
                senderMetrics.put(key, kafkaClientMetrics);
            }
        }

        return sender;
    }

    /**
     * Resolves the default integration service eagerly for the reactive path. Unlike the synchronous
     * {@link #getAccessor()} — which captures a LAZY {@code getClientIntegrationService} method
     * reference borrowed per operation — the reactive sender must be BUILT NOW to obtain the driver
     * {@link java.util.Properties} for its {@link SenderOptions}. Since {@link #getReactiveAccessor()}
     * is not declared to throw the checked {@link AppbaseClientIntegrationService.NotFoundException},
     * that failure is wrapped in an {@link AppbaseRuntimeException}.
     *
     * @return The default integration service
     */
    private KafkaProducerClientIntegrationService<K, V> clientIntegrationService() {
        try {
            return getClientIntegrationService();
        } catch (final AppbaseClientIntegrationService.NotFoundException e) {
            throw new AppbaseRuntimeException(
                    "No Kafka producer integration service found for the default client key", e);
        }
    }

    /**
     * Resolves the keyed integration service eagerly for the reactive path — see
     * {@link #clientIntegrationService()} for why the reactive build is eager (unlike the lazy sync
     * borrow) and why the checked {@link AppbaseClientIntegrationService.NotFoundException} is wrapped.
     *
     * @param appbaseClientKey Client key selecting the integration service
     * @return The keyed integration service
     */
    private KafkaProducerClientIntegrationService<K, V> clientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        try {
            return getClientIntegrationService(appbaseClientKey);
        } catch (final AppbaseClientIntegrationService.NotFoundException e) {
            throw new AppbaseRuntimeException(
                    "No Kafka producer integration service found for client key: " + appbaseClientKey, e);
        }
    }

    /**
     * Closes every service-owned {@link KafkaSender} before shutting down the pool. Leak trap: a
     * {@link KafkaSender} owns a Kafka {@code Producer} plus background network threads; dropping the
     * cache without closing would leak both. Each close is guarded so one failure does not prevent
     * the rest (and the pool) from shutting down, then {@code super.shutdownImpl()} shuts down the
     * pool that backs the synchronous accessors.
     */
    @Override
    protected void shutdownImpl() {
        // Snapshot the driver-metrics binders before closing: a defensive copy avoids relying on the
        // map's weakly-consistent iterator. Close ONLY the binders — the composite registry is owned
        // by MetricService and must never be shut down here.
        new ArrayList<>(senderMetrics.values()).forEach(kafkaClientMetrics -> {
            try {
                kafkaClientMetrics.close();
            } catch (final RuntimeException e) {
                logger.warn("Error closing Kafka sender KafkaClientMetrics binder", e);
            }
        });
        senderMetrics.clear();

        // Snapshot the senders before closing: a defensive copy avoids relying on the map's
        // weakly-consistent iterator and closes every sender captured at shutdown time.
        new ArrayList<>(reactiveSenders.values()).forEach(sender -> {
            try {
                sender.close();
            } catch (final RuntimeException e) {
                logger.warn("Error closing KafkaSender", e);
            }
        });
        reactiveSenders.clear();
        super.shutdownImpl();
    }
}
