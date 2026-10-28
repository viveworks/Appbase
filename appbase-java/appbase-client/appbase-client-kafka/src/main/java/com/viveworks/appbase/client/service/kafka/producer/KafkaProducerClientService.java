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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.service.kafka.producer.reactive.ReactiveKafkaProducerAccessor;
import org.apache.kafka.clients.producer.Producer;

/**
 * Represents {@link AppbaseClientService} for {@link Producer}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public interface KafkaProducerClientService<K, V> extends AppbaseClientService<Producer<K, V>, ClusterIdValue> {
    @Override
    KafkaProducerClientIntegrationService<K, V> getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;


    @Override
    KafkaProducerClientIntegrationService<K, V> getClientIntegrationService(
            AppbaseClientKey<ClusterIdValue> appbaseClientKey) throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Reactive producer accessor for the default client key, over a service-owned reactor-kafka
     * sender.
     *
     * @return Reactive producer accessor for the default key
     */
    ReactiveKafkaProducerAccessor<K, V> getReactiveAccessor();

    /**
     * Reactive producer accessor for {@code appbaseClientKey}, over a service-owned reactor-kafka
     * sender.
     *
     * @param appbaseClientKey Client key selecting the producer
     * @return Reactive producer accessor for {@code appbaseClientKey}
     */
    ReactiveKafkaProducerAccessor<K, V> getReactiveAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);

    /**
     * Synchronous producer accessor for the default client key, borrowing a pooled producer per
     * operation.
     *
     * @return Synchronous producer accessor for the default key
     */
    KafkaProducerAccessor<K, V> getAccessor();

    /**
     * Synchronous producer accessor for {@code appbaseClientKey}, borrowing a pooled producer per
     * operation.
     *
     * @param appbaseClientKey Client key selecting the producer
     * @return Synchronous producer accessor for {@code appbaseClientKey}
     */
    KafkaProducerAccessor<K, V> getAccessor(AppbaseClientKey<ClusterIdValue> appbaseClientKey);
}
