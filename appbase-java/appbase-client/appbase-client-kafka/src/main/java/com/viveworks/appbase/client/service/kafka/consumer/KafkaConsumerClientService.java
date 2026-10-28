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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.kafka.consumer.reactive.ReactiveKafkaConsumerAccessor;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Represents {@link AppbaseClientService} for {@link Consumer}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public interface KafkaConsumerClientService<K, V> extends AppbaseClientService<Consumer<K, V>, ClusterIdValue> {
    @Override
    KafkaConsumerClientIntegrationService<K, V> getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;


    @Override
    KafkaConsumerClientIntegrationService<K, V> getClientIntegrationService(
            AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Reactive consumer accessor for the default client key, over a service-owned reactor-kafka
     * receiver.
     *
     * @return Reactive consumer accessor for the default key
     */
    ReactiveKafkaConsumerAccessor<K, V> getReactiveAccessor();

    /**
     * Reactive consumer accessor for {@code appbaseClientKey}, over a service-owned reactor-kafka
     * receiver.
     *
     * @param appbaseClientKey Client key selecting the consumer
     * @return Reactive consumer accessor for {@code appbaseClientKey}
     */
    ReactiveKafkaConsumerAccessor<K, V> getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Synchronous consumer accessor for the default client key, borrowing a pooled consumer per
     * operation.
     *
     * @return Synchronous consumer accessor for the default key
     */
    KafkaConsumerAccessor<K, V> getAccessor();

    /**
     * Synchronous consumer accessor for {@code appbaseClientKey}, borrowing a pooled consumer per
     * operation.
     *
     * @param appbaseClientKey Client key selecting the consumer
     * @return Synchronous consumer accessor for {@code appbaseClientKey}
     */
    KafkaConsumerAccessor<K, V> getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
