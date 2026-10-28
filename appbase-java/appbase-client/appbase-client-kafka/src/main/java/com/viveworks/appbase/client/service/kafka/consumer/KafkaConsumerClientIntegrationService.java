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
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import org.apache.kafka.clients.consumer.Consumer;

import java.util.List;
import java.util.Properties;

/**
 * Represents {@link AppbaseClientIntegrationService} for {@link Consumer}.
 *
 * @author Navaneeth Mysore G
 * @param <K> Kafka message {@code key} type
 * @param <V> Kafka message {@code value} type
 * @since 1.0.0
 */
public interface KafkaConsumerClientIntegrationService<K, V>
        extends AppbaseClientIntegrationService<Consumer<K, V>, ClusterIdValue>
{
    /**
     * Builds the driver {@link Properties} from this integration service's configuration. Exposed on
     * the interface so the consumer client service can construct the reactor-kafka
     * {@code ReceiverOptions} from the SAME properties the pooled synchronous consumer is built from —
     * a single source of truth for consumer configuration, and mockable in tests (cleaner than a
     * same-package cast to the default implementation).
     *
     * @return Populated consumer {@link Properties}
     */
    Properties buildConsumerProperties();

    /**
     * The topics the reactive receiver subscribes to. Exposed on the interface alongside
     * {@link #buildConsumerProperties()} so the consumer client service can build the reactor-kafka
     * {@code ReceiverOptions.subscription(...)} from one config source; reactor-kafka's
     * {@code KafkaReceiver} requires a topic subscription, unlike the synchronous consumer whose
     * subscription is an explicit driver API call.
     *
     * @return Subscription topics, or {@code null}/empty if none configured
     */
    List<String> subscriptionTopics();

    /**
     * Whether Kafka driver-native metrics are routed into Appbase's shared Micrometer composite
     * registry. Exposed on the interface (alongside {@link #buildConsumerProperties()}) so the
     * consumer client service can gate the reactor-kafka receiver's driver-metrics binding on the
     * SAME configuration flag the pooled synchronous consumer uses — one config source. Defaults to
     * {@code true}; returns {@code false} when this integration service was created directly from a
     * {@link Consumer} (no configuration).
     *
     * @return {@code true} if driver-native metrics binding is enabled
     */
    boolean isDriverMetricsEnabled();
}
