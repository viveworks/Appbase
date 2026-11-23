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

package com.viveworks.appbase.client.service.clamav.client;

import com.viveworks.appbase.client.core.test.ClientCoreTestUtil;
import com.viveworks.appbase.client.service.clamav.ClamAVClientTestHelper;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVPingResult;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVScanResult;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "ClamAVClientServiceTests.enabled", matches = "true")
class DefaultClamAVClientSharedResourcesTest {
    private static DefaultClamAVClientSharedResources defaultClamAVClientSharedResources;

    @BeforeAll
    static void setUp() {
        defaultClamAVClientSharedResources = ClamAVClientTestHelper.defaultClamAVClientSharedResources();
    }

    @AfterAll
    static void tearDown() {
        if (defaultClamAVClientSharedResources != null) {
            defaultClamAVClientSharedResources.shutdown();
        }
    }

    @Test
    void testObjectPools() {
        Assertions.assertNotNull(defaultClamAVClientSharedResources.getAppbaseClientCore());

        final ManyToManyQueuedFixedObjectPool<ClamAVPingResult> clamAVPingResultObjectPool =
                defaultClamAVClientSharedResources.getClamAVPingResultObjectPool();
        ClientCoreTestUtil.testObjectPool(clamAVPingResultObjectPool);

        final ManyToManyQueuedFixedObjectPool<ClamAVScanResult> clamAVScanResultObjectPool =
                defaultClamAVClientSharedResources.getClamAVScanResultObjectPool();
        ClientCoreTestUtil.testObjectPool(clamAVScanResultObjectPool);

        final ManyToManyQueuedFixedObjectPool<ClamAVRequestEvent> clamAVRequestEventObjectPool =
                defaultClamAVClientSharedResources.getClamAVRequestEventObjectPool();
        ClientCoreTestUtil.testObjectPool(clamAVRequestEventObjectPool);
    }
}
