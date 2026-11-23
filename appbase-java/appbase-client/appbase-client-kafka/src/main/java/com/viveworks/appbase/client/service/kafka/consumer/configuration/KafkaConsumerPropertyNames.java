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

package com.viveworks.appbase.client.service.kafka.consumer.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * Represents {@link org.apache.kafka.clients.consumer.Consumer} property names.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum KafkaConsumerPropertyNames {
    KEY_DESERIALIZER("key.deserializer"),
    VALUE_DESERIALIZER("value.deserializer"),
    BOOTSTRAP_SERVERS("bootstrap.servers"),
    FETCH_MIN_BYTES("fetch.min.bytes"),
    GROUP_ID("group.id"),
    HEARTBEAT_INTERVAL_MS("heartbeat.interval.ms"),
    MAX_PARTITION_FETCH_BYTES("max.partition.fetch.bytes"),
    SESSION_TIMEOUT_MS("session.timeout.ms"),
    ALLOW_AUTO_CREATE_TOPICS("allow.auto.create.topics"),
    AUTO_OFFSET_RESET("auto.offset.reset"),
    CLIENT_DNS_LOOKUP("client.dns.lookup"),
    CONNECTIONS_MAX_IDLE_MS("connections.max.idle.ms"),
    DEFAULT_API_TIMEOUT_MS("default.api.timeout.ms"),
    ENABLE_AUTO_COMMIT("enable.auto.commit"),
    EXCLUDE_INTERNAL_TOPICS("exclude.internal.topics"),
    FETCH_MAX_BYTES("fetch.max.bytes"),
    GROUP_INSTANCE_ID("group.instance.id"),
    ISOLATION_LEVEL("isolation.level"),
    MAX_POLL_INTERVAL_MS("max.poll.interval.ms"),
    MAX_POLL_RECORDS("max.poll.records"),
    PARTITION_ASSIGNMENT_STRATEGY("partition.assignment.strategy"),
    RECEIVE_BUFFER_BYTES("receive.buffer.bytes"),
    REQUEST_TIMEOUT_MS("request.timeout.ms"),
    SEND_BUFFER_BYTES("send.buffer.bytes"),
    SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS("socket.connection.setup.timeout.max.ms"),
    SOCKET_CONNECTION_SETUP_TIMEOUT_MS("socket.connection.setup.timeout.ms"),
    AUTO_COMMIT_INTERVAL_MS("auto.commit.interval.ms"),
    CHECK_CRCS("check.crcs"),
    CLIENT_ID("client.id"),
    CLIENT_RACK("client.rack"),
    FETCH_MAX_WAIT_MS("fetch.max.wait.ms"),
    INTERCEPTOR_CLASSES("interceptor.classes"),
    METADATA_MAX_AGE_MS("metadata.max.age.ms"),
    METRIC_REPORTERS("metric.reporters"),
    METRICS_NUM_SAMPLES("metrics.num.samples"),
    METRICS_RECORDING_LEVEL("metrics.recording.level"),
    METRICS_SAMPLE_WINDOW_MS("metrics.sample.window.ms"),
    RECONNECT_BACKOFF_MAX_MS("reconnect.backoff.max.ms"),
    RECONNECT_BACKOFF_MS("reconnect.backoff.ms"),
    RETRY_BACKOFF_MS("retry.backoff.ms"),
    SECURITY_PROTOCOL("security.protocol"),
    GROUP_PROTOCOL("group.protocol"),
    RETRY_BACKOFF_MAX_MS("retry.backoff.max.ms");

    private final String text;

    KafkaConsumerPropertyNames(final String text) {
        AppbaseUtil.notNull(text, "text");

        this.text = text.trim();
    }

    public String getText() {
        return text;
    }
}
