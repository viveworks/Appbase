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
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class KafkaProducerClientServiceTestUtil {
    private KafkaProducerClientServiceTestUtil() {
    }

    public static void kafkaProducerClientServiceTest(
            final KafkaProducerClientService<?, ?> kafkaProducerClientService,
            final KafkaProducerClientIntegrationService<?, ?> kafkaProducerClientIntegrationService)
    {
        kafkaProducerClientServiceTestImpl(kafkaProducerClientService, kafkaProducerClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        kafkaProducerClientIntegrationService.shutdown();
        kafkaProducerClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        kafkaProducerClientServiceTestImpl(kafkaProducerClientService, kafkaProducerClientIntegrationService);
        kafkaProducerClientService.shutdown();

        Assertions.assertNull(kafkaProducerClientIntegrationService.getClient());
        Assertions.assertFalse(kafkaProducerClientIntegrationService.isHealthy());
    }

    public static void kafkaProducerClientServiceTestImpl(
            final KafkaProducerClientService<?, ?> kafkaProducerClientService,
            final KafkaProducerClientIntegrationService<?, ?> kafkaProducerClientIntegrationService)
    {
        Assertions.assertNotNull(kafkaProducerClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey =
                kafkaProducerClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);

        Assertions.assertNotNull(kafkaProducerClientIntegrationService);
        final Producer<?, ?> producer = kafkaProducerClientIntegrationService.getClient();
        Assertions.assertNotNull(producer);

        Assertions.assertTrue(kafkaProducerClientIntegrationService.isHealthy());
        Assertions.assertTrue(kafkaProducerClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(Producer.class, kafkaProducerClientIntegrationService.getClientType());
    }
}
