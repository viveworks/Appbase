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

package com.viveworks.appbase.service.id;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.testhelper.AppbaseServiceCoreTestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultIdServiceTest {
    private static AppbaseCore appbaseCore;

    private static IdService idService;

    @BeforeAll
    static void init() {
        appbaseCore = ClientCoreTestHelper.appbaseCore();
        idService = new DefaultIdService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultIdServiceConfiguration.builder().build());
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();
        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(IdService.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceCoreTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                IdService.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(idService, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 = serviceRegistry.getAppbaseService(DefaultIdService.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(idService, appbaseServiceEntry2.getService());
    }

    @Test
    void testNew14CharsId() {
        final String new14CharsId1 = idService.new14CharsId();
        Assertions.assertNotNull(new14CharsId1);
        Assertions.assertEquals(14, new14CharsId1.length());

        final String new14CharsId2 = idService.new14CharsId();
        Assertions.assertNotEquals(new14CharsId1, new14CharsId2);
    }

    @Test
    void testNewId() {
        final String newId1 = idService.newId();
        Assertions.assertNotNull(newId1);
        final UUID uuid1 = UUID.fromString(newId1);
        Assertions.assertNotNull(uuid1);

        final String newId2 = idService.newId();
        Assertions.assertNotNull(newId2);
        final UUID uuid2 = UUID.fromString(newId2);
        Assertions.assertNotNull(uuid2);

        Assertions.assertNotEquals(uuid1, uuid2);
    }
}
