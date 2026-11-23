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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ElasticsearchLowLevelRestClientServiceTestUtil {
    private ElasticsearchLowLevelRestClientServiceTestUtil() {
    }

    public static void elasticsearchRestClientServiceTest(
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService,
            final ElasticsearchLowLevelRestClientIntegrationService elasticsearchLowLevelRestClientIntegrationService)
    {
        elasticsearchRestClientServiceTestImpl(
                elasticsearchLowLevelRestClientService, elasticsearchLowLevelRestClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        elasticsearchLowLevelRestClientIntegrationService.shutdown();
        elasticsearchLowLevelRestClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        elasticsearchRestClientServiceTestImpl(
                elasticsearchLowLevelRestClientService, elasticsearchLowLevelRestClientIntegrationService);
        elasticsearchLowLevelRestClientService.shutdown();

        Assertions.assertNull(elasticsearchLowLevelRestClientIntegrationService.getClient());
        Assertions.assertFalse(elasticsearchLowLevelRestClientIntegrationService.isHealthy());
    }

    private static void elasticsearchRestClientServiceTestImpl(
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService,
            final ElasticsearchLowLevelRestClientIntegrationService elasticsearchLowLevelRestClientIntegrationService)
    {
        Assertions.assertNotNull(elasticsearchLowLevelRestClientService);
        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey =
                elasticsearchLowLevelRestClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(elasticsearchLowLevelRestClientIntegrationService);

        final RestClient restClient = elasticsearchLowLevelRestClientIntegrationService.getClient();
        Assertions.assertNotNull(restClient);

        Assertions.assertTrue(elasticsearchLowLevelRestClientIntegrationService.isHealthy());
        Assertions.assertTrue(elasticsearchLowLevelRestClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(RestClient.class, elasticsearchLowLevelRestClientIntegrationService.getClientType());
    }
}
