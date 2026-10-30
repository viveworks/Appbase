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

package com.viveworks.appbase.core.service.registry;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.DefaultLogServiceConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseServiceEntryTest {
    private static LogService logService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = new DefaultLogService(
                CoreTestFixtures.getMediatorEventDelegatorRegistry(),
                DefaultLogServiceConfiguration.builder().build());
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testAppbaseServiceEntry() {
        final AppbaseServiceEntry appbaseServiceEntry1 = new AppbaseServiceEntry(logService, ServiceStatus.STARTED);
        Assertions.assertNotNull(appbaseServiceEntry1.getService());
        Assertions.assertNotNull(appbaseServiceEntry1.getServiceStatus());

        final AppbaseServiceEntry appbaseServiceEntry2 = new AppbaseServiceEntry(logService, ServiceStatus.STARTED);
        Assertions.assertEquals(appbaseServiceEntry1, appbaseServiceEntry2);
        Assertions.assertEquals(appbaseServiceEntry1.hashCode(), appbaseServiceEntry2.hashCode());
        Assertions.assertEquals(appbaseServiceEntry1.toString(), appbaseServiceEntry2.toString());
    }
}
