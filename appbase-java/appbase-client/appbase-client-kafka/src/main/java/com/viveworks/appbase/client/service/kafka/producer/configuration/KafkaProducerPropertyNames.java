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

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Represents {@link org.apache.kafka.clients.producer.Producer} property names.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum KafkaProducerPropertyNames {
    KEY_SERIALIZER("key.serializer"),
    VALUE_SERIALIZER("value.serializer"),
    ACKS("acks"),
    BOOTSTRAP_SERVERS("bootstrap.servers"),
    BUFFER_MEMORY("buffer.memory"),
    COMPRESSION_TYPE("compression.type"),
    RETRIES("retries"),
    BATCH_SIZE("batch.size"),
    CLIENT_DNS_LOOKUP("client.dns.lookup"),
    CLIENT_ID("client.id"),
    CONNECTIONS_MAX_IDLE_MS("connections.max.idle.ms"),
    DELIVERY_TIMEOUT_MS("delivery.timeout.ms"),
    LINGER_MS("linger.ms"),
    MAX_BLOCK_MS("max.block.ms"),
    MAX_REQUEST_SIZE("max.request.size"),
    PARTITIONER_CLASS("partitioner.class"),
    RECEIVE_BUFFER_BYTES("receive.buffer.bytes"),
    REQUEST_TIMEOUT_MS("request.timeout.ms"),
    SEND_BUFFER_BYTES("send.buffer.bytes"),
    ENABLE_IDEMPOTENCE("enable.idempotence"),
    INTERCEPTOR_CLASSES("interceptor.classes"),
    MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION("max.in.flight.requests.per.connection"),
    METADATA_MAX_AGE_MS("metadata.max.age.ms"),
    METADATA_MAX_IDLE_MS("metadata.max.idle.ms"),
    METRIC_REPORTERS("metric.reporters"),
    METRICS_NUM_SAMPLES("metrics.num.samples"),
    METRICS_RECORDING_LEVEL("metrics.recording.level"),
    METRICS_SAMPLE_WINDOW_MS("metrics.sample.window.ms"),
    RECONNECT_BACKOFF_MAX_MS("reconnect.backoff.max.ms"),
    RECONNECT_BACKOFF_MS("reconnect.backoff.ms"),
    RETRY_BACKOFF_MS("retry.backoff.ms"),
    TRANSACTION_TIMEOUT_MS("transaction.timeout.ms"),
    TRANSACTIONAL_ID("transactional.id"),
    SECURITY_PROTOCOL("security.protocol"),
    SOCKET_CONNECTION_SETUP_TIMEOUT_MS("socket.connection.setup.timeout.ms"),
    SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS("socket.connection.setup.timeout.max.ms"),
    PARTITIONER_AVAILABILITY_TIMEOUT_MS("partitioner.availability.timeout.ms"),
    PARTITIONER_IGNORE_KEYS("partitioner.ignore.keys");

    private final String text;

    KafkaProducerPropertyNames(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text.trim();
    }

    public String getText() {
        return text;
    }

}
