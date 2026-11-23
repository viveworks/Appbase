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

package com.viveworks.appbase.core.util.objectpool.queued;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ManyToManyQueuedFixedObjectPoolTest {
    private static LogService logService;
    private static MetricService metricService;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
        metricService = CoreTestFixtures.getMetricService(logService);
    }

    @AfterAll
    static void tearDown() {
        if (metricService != null) {
            metricService.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testObjectPool() {
        final ManyToManyQueuedFixedObjectPool<CoreTestFixtures.TestPoolableObject> manyToManyQueuedFixedObjectPool =
                new ManyToManyQueuedFixedObjectPool<>(
                        metricService,
                        CoreTestFixtures.CONTAINER_SIZE,
                        new CoreTestFixtures.TestPoolableObjectFactory());

        final List<CoreTestFixtures.TestPoolableObject> testPoolableObjects = new ArrayList<>();
        for (int i = 0; i < CoreTestFixtures.CONTAINER_SIZE; ++i) {
            final CoreTestFixtures.TestPoolableObject testPoolableObject =
                    manyToManyQueuedFixedObjectPool.pollCleanObject();
            testPoolableObject.setName("name" + i);
            Assertions.assertNotNull(testPoolableObject);
            testPoolableObjects.add(testPoolableObject);
        }
        Assertions.assertEquals(CoreTestFixtures.CONTAINER_SIZE, testPoolableObjects.size());

        final CoreTestFixtures.TestPoolableObject testPoolableObject = manyToManyQueuedFixedObjectPool.pollCleanObject();
        Assertions.assertNotNull(testPoolableObject);

        testPoolableObjects.forEach(po -> {
            Assertions.assertNotNull(po);
            Assertions.assertTrue(po.getName().startsWith("name"));
            po.clean();
            manyToManyQueuedFixedObjectPool.offerCleanObject(po);
        });
        Assertions.assertEquals(CoreTestFixtures.CONTAINER_SIZE, testPoolableObjects.size());

        Assertions.assertNotNull(manyToManyQueuedFixedObjectPool.getAddedCountGauge());
        Assertions.assertNotNull(manyToManyQueuedFixedObjectPool.getRemovedCountGauge());
        Assertions.assertNotNull(manyToManyQueuedFixedObjectPool.getRemainingCapacityGauge());
        Assertions.assertNotNull(manyToManyQueuedFixedObjectPool.getSizeGauge());

        manyToManyQueuedFixedObjectPool.shutdown();
    }

}
