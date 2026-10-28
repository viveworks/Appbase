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

package com.viveworks.appbase.client.service.kafka.reactive;

import com.viveworks.appbase.client.service.kafka.consumer.DefaultKafkaConsumerClientService;
import com.viveworks.appbase.client.service.kafka.consumer.KafkaConsumerClientServiceTestHelper;
import com.viveworks.appbase.client.service.kafka.consumer.reactive.ReactiveKafkaConsumerAccessor;
import com.viveworks.appbase.client.service.kafka.producer.DefaultKafkaProducerClientService;
import com.viveworks.appbase.client.service.kafka.producer.KafkaProducerClientServiceTestHelper;
import com.viveworks.appbase.client.service.kafka.producer.reactive.ReactiveKafkaProducerAccessor;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

/**
 * Integration tests for the reactive Kafka accessors against a live Kafka broker.
 *
 * <p>Requires a running Kafka broker with the topic {@code appbase-reactive-kafka-it} (the topic the
 * test consumer subscribes to via {@code subscriptionTopics} in {@code appbase-client-kafka-consumer.yaml}).
 * Enable via system property: {@code -DKafkaClientServiceTests.enabled=true}
 *
 * <p>Use the Docker scripts in {@code infra/docker/kafka/} to start a local Kafka broker.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "KafkaClientServiceTests.enabled", matches = "true")
class ReactiveKafkaAccessorIT {

    private static final String TOPIC = "appbase-reactive-kafka-it";
    private static final String KEY = "appbase-reactive-kafka-it-key";

    private static DefaultKafkaProducerClientService<String, String> producerClientService;
    private static DefaultKafkaConsumerClientService<String, String> consumerClientService;

    @SuppressWarnings("unchecked")
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultKafkaProducerClientService.registerObjectMapperSubtypes();
        DefaultKafkaConsumerClientService.registerObjectMapperSubtypes();

        // The test configs (appbase-client-kafka-{producer,consumer}.yaml) use String key/value serdes.
        producerClientService =
                (DefaultKafkaProducerClientService<String, String>)
                        KafkaProducerClientServiceTestHelper.defaultKafkaProducerClientService();
        consumerClientService =
                (DefaultKafkaConsumerClientService<String, String>)
                        KafkaConsumerClientServiceTestHelper.defaultKafkaConsumerClientService();
    }

    @AfterAll
    static void tearDown() {
        if (producerClientService != null) {
            producerClientService.shutdown();
        }
        if (consumerClientService != null) {
            consumerClientService.shutdown();
        }
    }

    /**
     * Verifies a reactive {@code send} followed by a reactive {@code receive} round-trips the record
     * through {@link ReactiveKafkaProducerAccessor} and {@link ReactiveKafkaConsumerAccessor}: the
     * consumer stream is subscribed first (so it has a partition assignment), then the record is
     * produced via {@code .then(...)}, and only this invocation's fresh value is asserted.
     */
    @Test
    void reactiveSendThenReceiveRoundTrip() {
        final ReactiveKafkaProducerAccessor<String, String> producer = producerClientService.getReactiveAccessor();
        final ReactiveKafkaConsumerAccessor<String, String> consumer = consumerClientService.getReactiveAccessor();
        // Each invocation must observe its own send, not an older record replayed by EARLIEST.
        final String value = "appbase-reactive-kafka-it-value-" + UUID.randomUUID();

        StepVerifier.create(consumer.receive("receive").map(record -> record.value()).filter(value::equals).next())
                .then(() -> producer.send("send", new ProducerRecord<>(TOPIC, KEY, value)).block())
                .expectNext(value)
                .expectComplete()
                .verify(Duration.ofSeconds(30));
    }
}
