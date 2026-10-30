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

package com.viveworks.appbase.client.service.kafka.consumer.configuration;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationServiceConfiguration;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.kafka.consumer.KafkaConsumerClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface KafkaConsumerClientIntegrationServiceConfiguration extends AppbaseClientIntegrationServiceConfiguration {
    /**
     * The topics the reactive receiver subscribes to when the client service builds the
     * reactor-kafka {@code ReceiverOptions}. Nullable; not part of the synchronous consumer driver
     * properties.
     *
     * @return Subscription topics, or {@code null} if none configured
     */
    @Nullable
    List<String> getSubscriptionTopics();

    /**
     * Whether Kafka driver-native metrics are routed into Appbase's shared Micrometer composite
     * registry (owned by {@code MetricService}, auto-exported to JMX). Defaults to {@code true}.
     *
     * @return {@code true} if driver-native metrics binding is enabled
     */
    boolean isDriverMetricsEnabled();
}
