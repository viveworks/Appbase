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

package com.viveworks.appbase.client.service.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.EventLoop;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AerospikeClientTestUtil {
    private AerospikeClientTestUtil() {
    }

    public static void aerospikeClientServiceTest(
            final AerospikeClientService aerospikeClientService,
            final AerospikeClientIntegrationService aerospikeClientIntegrationService)
    {
        aerospikeClientServiceTestImpl(aerospikeClientService, aerospikeClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>(false);
        aerospikeClientIntegrationService.shutdown();
        aerospikeClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        aerospikeClientServiceTestImpl(aerospikeClientService, aerospikeClientIntegrationService);
        aerospikeClientService.shutdown();

        Assertions.assertNull(aerospikeClientIntegrationService.getClient());
        Assertions.assertFalse(aerospikeClientIntegrationService.isHealthy());
    }

    private static void aerospikeClientServiceTestImpl(
            final AerospikeClientService aerospikeClientService,
            final AerospikeClientIntegrationService appbaseClientIntegrationService)
    {
        Assertions.assertNotNull(aerospikeClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = aerospikeClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(appbaseClientIntegrationService);

        final EventLoop eventLoop = appbaseClientIntegrationService.nextEventLoop();
        Assertions.assertNotNull(eventLoop);

        final IAerospikeClient aerospikeClient = appbaseClientIntegrationService.getClient();
        Assertions.assertNotNull(aerospikeClient);

        final AppbaseClientKey<ClusterIdValue> appbaseClientKey = appbaseClientIntegrationService.getAppbaseClientKey();
        Assertions.assertNotNull(appbaseClientKey);

        Assertions.assertTrue(appbaseClientIntegrationService.isHealthy());
        Assertions.assertTrue(appbaseClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(AerospikeClient.class, appbaseClientIntegrationService.getClientType());

        Assertions.assertTrue(aerospikeClient.isConnected());
    }
}
