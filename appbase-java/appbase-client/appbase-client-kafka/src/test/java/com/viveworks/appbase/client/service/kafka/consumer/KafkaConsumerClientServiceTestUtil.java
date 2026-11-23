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

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class KafkaConsumerClientServiceTestUtil {
    private KafkaConsumerClientServiceTestUtil() {
    }

    public static void kafkaConsumerClientServiceTest(
            final KafkaConsumerClientService<?, ?> kafkaConsumerClientService,
            final KafkaConsumerClientIntegrationService<?, ?> kafkaConsumerClientIntegrationService)
    {
        kafkaConsumerClientServiceTestImpl(kafkaConsumerClientService, kafkaConsumerClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        kafkaConsumerClientIntegrationService.shutdown();
        kafkaConsumerClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
            @Override
            public void onSuccess() {
                repaired.setReference(true);
            }

            @Override
            public void onFailure() {
                repaired.setReference(false);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                throw new AppbaseRuntimeException(throwable);
            }
        });
        Assertions.assertTrue(repaired.getReference());

        kafkaConsumerClientServiceTestImpl(kafkaConsumerClientService, kafkaConsumerClientIntegrationService);
        kafkaConsumerClientService.shutdown();

        Assertions.assertNull(kafkaConsumerClientIntegrationService.getClient());
        Assertions.assertFalse(kafkaConsumerClientIntegrationService.isHealthy());
    }

    public static void kafkaConsumerClientServiceTestImpl(
            final KafkaConsumerClientService<?, ?> kafkaConsumerClientService,
            final KafkaConsumerClientIntegrationService<?, ?> kafkaConsumerClientIntegrationService)
    {
        Assertions.assertNotNull(kafkaConsumerClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey =
                kafkaConsumerClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(kafkaConsumerClientIntegrationService);
        final Consumer<?, ?> consumer = kafkaConsumerClientIntegrationService.getClient();
        Assertions.assertNotNull(consumer);

        Assertions.assertTrue(kafkaConsumerClientIntegrationService.isHealthy());
        Assertions.assertTrue(kafkaConsumerClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(Consumer.class, kafkaConsumerClientIntegrationService.getClientType());
    }
}
