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

package com.viveworks.appbase.client.service.spark;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.MutableReference;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.StreamingContext;
import org.junit.jupiter.api.Assertions;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class SparkClientServiceTestUtil {
    private SparkClientServiceTestUtil() {
    }

    public static void sparkClientServiceTest(
            final SparkClientService sparkClientService,
            final SparkClientIntegrationService sparkClientIntegrationService)
    {
        sparkClientServiceTestImpl(sparkClientService, sparkClientIntegrationService);

        final MutableReference<Boolean> repaired = new MutableReference<>();
        sparkClientIntegrationService.shutdown();
        sparkClientIntegrationService.repair(new AppbaseClientIntegrationService.RepairListener() {
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

        sparkClientServiceTestImpl(sparkClientService, sparkClientIntegrationService);
        sparkClientService.shutdown();

        Assertions.assertNull(sparkClientIntegrationService.getClient());
        Assertions.assertFalse(sparkClientIntegrationService.isHealthy());
    }

    private static void sparkClientServiceTestImpl(
            final SparkClientService sparkClientService,
            final SparkClientIntegrationService sparkClientIntegrationService)
    {
        Assertions.assertNotNull(sparkClientService);

        final AppbaseClientKey<ClusterIdValue> defaultAppbaseClientKey = sparkClientService.getDefaultAppbaseClientKey();
        Assertions.assertNotNull(defaultAppbaseClientKey);


        Assertions.assertNotNull(sparkClientIntegrationService);

        final SparkClient sparkClient = sparkClientIntegrationService.getClient();
        Assertions.assertNotNull(sparkClient);

        final SparkContext sparkContext = sparkClient.getSparkContext();
        Assertions.assertFalse(sparkContext.isStopped());

        final JavaSparkContext javaSparkContext = sparkClient.getJavaSparkContext();
        Assertions.assertEquals(sparkClient.getSparkSession().sparkContext(), sparkContext);
        Assertions.assertEquals(javaSparkContext.sc(), sparkContext);

        final StreamingContext streamingContext = sparkClient.getStreamingContext();
        Assertions.assertEquals(streamingContext.sparkContext(), sparkContext);

        Assertions.assertTrue(sparkClientIntegrationService.isHealthy());
        Assertions.assertTrue(sparkClientIntegrationService.isRepairEnabled());
        Assertions.assertEquals(SparkClient.class, sparkClientIntegrationService.getClientType());
    }
}
