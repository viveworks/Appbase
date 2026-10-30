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

package com.viveworks.appbase.client.service.kafka.consumer;

import com.viveworks.appbase.client.core.AbstractAppbaseClientService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.configuration.DefaultKafkaConsumerClientServiceConfiguration;
import com.viveworks.appbase.client.service.kafka.consumer.reactive.ReactiveKafkaConsumerAccessor;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.apache.kafka.clients.consumer.Consumer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link KafkaConsumerClientService}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public class DefaultKafkaConsumerClientService<K, V>
        extends AbstractAppbaseClientService<Consumer<K, V>, ClusterIdValue, DefaultKafkaConsumerClientServiceConfiguration>
        implements KafkaConsumerClientService<K, V>
{
    /**
     * Per-key cache of service-OWNED reactor-kafka receivers, built eagerly on the reactive
     * acquisition path from {@link KafkaConsumerClientIntegrationService#buildConsumerProperties()}
     * plus its {@link KafkaConsumerClientIntegrationService#subscriptionTopics()}. A
     * {@link KafkaReceiver} is STATELESS until its {@code receive()} {@code Flux} is subscribed: it
     * opens no Kafka {@code Consumer} and no network threads at creation time — the underlying
     * consumer is created lazily on subscription and closed automatically when the receive
     * {@code Flux} terminates or is cancelled. {@code reactor.kafka.receiver.KafkaReceiver} therefore
     * exposes NO {@code close()} (verified against reactor-kafka 1.3.x), so {@link #shutdownImpl()}
     * only clears this cache (there is no open resource to release here) before shutting down the
     * pool that backs the synchronous accessors.
     */
    private final ConcurrentMap<AppbaseClientKey<ClusterIdValue>, KafkaReceiver<K, V>> reactiveReceivers =
            new ConcurrentHashMap<>();

    @Inject
    public DefaultKafkaConsumerClientService(final AppbaseClientCore appbaseClientCore) {
        super(appbaseClientCore,
                DefaultKafkaConsumerClientServiceConfiguration.class,
                false,
                null);

        startOnCreate();
    }

    public DefaultKafkaConsumerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Consumer<K, V>, ClusterIdValue> appbaseClientPool,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultKafkaConsumerClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaConsumerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultKafkaConsumerClientServiceConfiguration.class,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaConsumerClientService(
            final AppbaseClientCore appbaseClientCore,
            final DefaultKafkaConsumerClientServiceConfiguration defaultKafkaConsumerClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                DefaultKafkaConsumerClientServiceConfiguration.class,
                defaultKafkaConsumerClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaConsumerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPool<Consumer<K, V>, ClusterIdValue> appbaseClientPool,
            final DefaultKafkaConsumerClientServiceConfiguration defaultKafkaConsumerClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPool,
                DefaultKafkaConsumerClientServiceConfiguration.class,
                defaultKafkaConsumerClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }

    public DefaultKafkaConsumerClientService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey,
            final AppbaseClientPoolManager appbaseClientPoolManager,
            final DefaultKafkaConsumerClientServiceConfiguration defaultKafkaConsumerClientServiceConfiguration,
            final boolean registerAsConfigurationListener,
            @Nullable final List<Class<? extends AppbaseConfiguration>> additionalConfigurationTypesToRegister)
    {
        super(appbaseClientCore,
                defaultAppbaseClientKey,
                appbaseClientPoolManager,
                DefaultKafkaConsumerClientServiceConfiguration.class,
                defaultKafkaConsumerClientServiceConfiguration,
                registerAsConfigurationListener,
                additionalConfigurationTypesToRegister);

        startOnCreate();
    }


    /**
     * Register subtypes using {@link AppbaseCoreObjectMapperSubTypes}.
     */
    public static void registerObjectMapperSubtypes() {
        AppbaseCoreObjectMapperSubTypes.register(DefaultKafkaConsumerClientIntegrationServiceConfiguration.class);
        AppbaseCoreObjectMapperSubTypes.register(DefaultKafkaConsumerClientServiceConfiguration.class);
    }

    @Override
    protected AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey(
            final DefaultKafkaConsumerClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final DefaultKafkaConsumerClientIntegrationServiceConfiguration
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultKafkaConsumerClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration,
                "defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration");

        return KafkaConsumerClientUtil.appbaseClientKey(defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration);
    }

    @Override
    protected Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue>>
    appbaseClientIntegrationServices(final DefaultKafkaConsumerClientServiceConfiguration configuration)
    {
        AppbaseUtil.notNull(configuration, "configuration");

        final Map<AppbaseClientKey<ClusterIdValue>, AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue>>
                appbaseClientIntegrationServices = new ConcurrentHashMap<>();

        final DefaultKafkaConsumerClientIntegrationServiceConfiguration
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration =
                configuration.getDefaultDefaultKafkaConsumerClientIntegrationServiceConfiguration();
        AppbaseUtil.notNull(
                defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration,
                "defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration");


        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = defaultAppbaseClientKey(configuration);
        final DefaultKafkaConsumerClientIntegrationService<K, V> defaultDefaultKafkaConsumerClientIntegrationService =
                new DefaultKafkaConsumerClientIntegrationService<>(
                        appbaseClientCore,
                        getAppbaseClientPool(),
                        defaultAppbaseClientKey,
                        defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration);
        appbaseClientIntegrationServices.put(defaultAppbaseClientKey, defaultDefaultKafkaConsumerClientIntegrationService);


        final Set<DefaultKafkaConsumerClientIntegrationServiceConfiguration>
                keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations =
                configuration.getKeyedDefaultKafkaConsumerClientIntegrationServiceConfigurations();
        if (keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations != null) {
            keyedDefaultKafkaConsumerClientIntegrationServiceConfigurations.forEach(
                    keyedDefaultKafkaConsumerClientIntegrationServiceConfiguration -> {
                        final AppbaseClientKey<ClusterIdValue> appbaseClientKey =
                                KafkaConsumerClientUtil.appbaseClientKey(
                                        keyedDefaultKafkaConsumerClientIntegrationServiceConfiguration);
                        final DefaultKafkaConsumerClientIntegrationService<K, V>
                                defaultKafkaConsumerClientIntegrationService =
                                new DefaultKafkaConsumerClientIntegrationService<>(
                                        appbaseClientCore,
                                        getAppbaseClientPool(),
                                        defaultAppbaseClientKey,
                                        defaultDefaultKafkaConsumerClientIntegrationServiceConfiguration);
                        appbaseClientIntegrationServices.put(appbaseClientKey, defaultKafkaConsumerClientIntegrationService);
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
                .registerAppbaseService(KafkaConsumerClientService.class, this);
    }

    @Override
    public KafkaConsumerClientIntegrationService<K, V> getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException
    {
        return (KafkaConsumerClientIntegrationService<K, V>) super.getClientIntegrationService();
    }


    @Override
    public KafkaConsumerClientIntegrationService<K, V> getClientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException
    {
        return (KafkaConsumerClientIntegrationService<K, V>) super.getClientIntegrationService(appbaseClientKey);
    }

    @Override
    public KafkaConsumerAccessor<K, V> getAccessor() {
        return new KafkaConsumerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                this::getClientIntegrationService);
    }

    @Override
    public KafkaConsumerAccessor<K, V> getAccessor(final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        return new KafkaConsumerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                getConfiguration().getReactiveOperationConfiguration(),
                () -> getClientIntegrationService(appbaseClientKey));
    }

    @Override
    public ReactiveKafkaConsumerAccessor<K, V> getReactiveAccessor() {
        final AppbaseClientKey<ClusterIdValue> key = getDefaultAppbaseClientKey();
        final KafkaReceiver<K, V> receiver =
                reactiveReceivers.computeIfAbsent(key, k -> buildReceiver(clientIntegrationService()));
        return new ReactiveKafkaConsumerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                receiver,
                clientIntegrationService().isDriverMetricsEnabled());
    }

    @Override
    public ReactiveKafkaConsumerAccessor<K, V> getReactiveAccessor(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        final KafkaReceiver<K, V> receiver =
                reactiveReceivers.computeIfAbsent(
                        appbaseClientKey, k -> buildReceiver(clientIntegrationService(appbaseClientKey)));
        return new ReactiveKafkaConsumerAccessor<>(
                appbaseClientCore.getAppbaseCore(),
                appbaseClientCore.getAppbaseCore().getReactorService(),
                getConfiguration().getReactiveOperationConfiguration(),
                receiver,
                clientIntegrationService(appbaseClientKey).isDriverMetricsEnabled());
    }

    /**
     * Builds a reactor-kafka receiver from the SAME driver {@link java.util.Properties} the pooled
     * synchronous consumer is built from (one config source), via
     * {@link KafkaConsumerClientIntegrationService#buildConsumerProperties()}, subscribed to the
     * integration service's {@link KafkaConsumerClientIntegrationService#subscriptionTopics()}.
     * reactor-kafka's {@link KafkaReceiver} REQUIRES a topic subscription, so a null/empty topic list
     * is a configuration error and fails fast with an {@link AppbaseRuntimeException}.
     *
     * @param integrationService Integration service supplying the consumer properties and topics
     * @return A new reactor-kafka {@link KafkaReceiver}
     */
    private KafkaReceiver<K, V> buildReceiver(final KafkaConsumerClientIntegrationService<K, V> integrationService) {
        final List<String> topics = integrationService.subscriptionTopics();
        if (topics == null || topics.isEmpty()) {
            throw new AppbaseRuntimeException("No subscriptionTopics configured for the Kafka consumer; "
                    + "set subscriptionTopics on the integration configuration to use the reactive receiver");
        }
        return KafkaReceiver.create(
                ReceiverOptions.<K, V>create(integrationService.buildConsumerProperties()).subscription(topics));
    }

    /**
     * Resolves the default integration service eagerly for the reactive path. Unlike the synchronous
     * {@link #getAccessor()} — which captures a LAZY {@code getClientIntegrationService} method
     * reference borrowed per operation — the reactive receiver must be BUILT NOW to obtain the driver
     * {@link java.util.Properties} and subscription for its {@link ReceiverOptions}. Since
     * {@link #getReactiveAccessor()} is not declared to throw the checked
     * {@link AppbaseClientIntegrationService.NotFoundException}, that failure is wrapped in an
     * {@link AppbaseRuntimeException}.
     *
     * @return The default integration service
     */
    private KafkaConsumerClientIntegrationService<K, V> clientIntegrationService() {
        try {
            return getClientIntegrationService();
        } catch (final AppbaseClientIntegrationService.NotFoundException e) {
            throw new AppbaseRuntimeException(
                    "No Kafka consumer integration service found for the default client key", e);
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
    private KafkaConsumerClientIntegrationService<K, V> clientIntegrationService(
            final AppbaseClientKey<ClusterIdValue> appbaseClientKey) {
        try {
            return getClientIntegrationService(appbaseClientKey);
        } catch (final AppbaseClientIntegrationService.NotFoundException e) {
            throw new AppbaseRuntimeException(
                    "No Kafka consumer integration service found for client key: " + appbaseClientKey, e);
        }
    }

    /**
     * Clears the cache of service-owned {@link KafkaReceiver}s before shutting down the pool. Unlike
     * the producer's {@code KafkaSender} (which owns a live {@code Producer} + network threads and
     * must be closed), a reactor-kafka {@link KafkaReceiver} holds NO open Kafka {@code Consumer}
     * until its {@code receive()} {@code Flux} is subscribed — the consumer is created lazily on
     * subscription and closed when that {@code Flux} terminates or is cancelled — and the interface
     * exposes no {@code close()} (verified against reactor-kafka 1.3.x). There is therefore nothing to
     * release at the service level: dropping the references is sufficient. {@code super.shutdownImpl()}
     * then shuts down the pool that backs the synchronous accessors.
     */
    @Override
    protected void shutdownImpl() {
        reactiveReceivers.clear();
        super.shutdownImpl();
    }
}
