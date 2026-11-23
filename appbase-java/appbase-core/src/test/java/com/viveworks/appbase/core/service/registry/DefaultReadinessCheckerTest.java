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

package com.viveworks.appbase.core.service.registry;

import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.DefaultLogService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultReadinessCheckerTest {
    private static LogService logService;

    @BeforeAll
    static void setUp() throws Exception {
        logService = CoreTestFixtures.getLogService();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testCheckReady() {
        final List<AppbaseServiceEntry> appbaseServiceEntries = new ArrayList<>();

        final UnreadyAppbaseServiceEntries unreadyAppbaseServiceEntries =
                new UnreadyAppbaseServiceEntries(appbaseServiceEntries);
        Assertions.assertNotNull(unreadyAppbaseServiceEntries.getUnreadyAppbaseServiceEntries());
        Assertions.assertTrue(unreadyAppbaseServiceEntries.getUnreadyAppbaseServiceEntries().isEmpty());

        final AppbaseServiceEntry appbaseServiceEntry = new AppbaseServiceEntry(logService, logService.getStatus());
        appbaseServiceEntries.add(appbaseServiceEntry);

        final DefaultReadinessChecker defaultReadinessChecker1 = new DefaultReadinessChecker();
        final Optional<UnreadyAppbaseServiceEntries> unreadyAppbaseServiceEntriesOptional1 =
                defaultReadinessChecker1.checkReady(appbaseServiceEntries);
        Assertions.assertTrue(unreadyAppbaseServiceEntriesOptional1.isEmpty());


        appbaseServiceEntries.clear();
        final AppbaseServiceEntry appbaseServiceEntry1 = new AppbaseServiceEntry(logService, ServiceStatus.SHUTDOWN);
        appbaseServiceEntries.add(appbaseServiceEntry1);
        final DefaultReadinessChecker defaultReadinessChecker2 = new DefaultReadinessChecker();
        final Optional<UnreadyAppbaseServiceEntries> unreadyAppbaseServiceEntriesOptional2 =
                defaultReadinessChecker2.checkReady(appbaseServiceEntries);
        Assertions.assertTrue(unreadyAppbaseServiceEntriesOptional2.isPresent());

        final UnreadyAppbaseServiceEntries unreadyAppbaseServiceEntries2 = unreadyAppbaseServiceEntriesOptional2.get();
        final Collection<AppbaseServiceEntry> unreadyAppbaseServiceEntries1 =
                unreadyAppbaseServiceEntries2.getUnreadyAppbaseServiceEntries();
        Assertions.assertNotNull(unreadyAppbaseServiceEntries1);
        Assertions.assertEquals(1, unreadyAppbaseServiceEntries1.size());
        final AppbaseServiceEntry appbaseServiceEntry2 =
                unreadyAppbaseServiceEntries1.stream().findFirst().isPresent() ?
                        unreadyAppbaseServiceEntries1.stream().findFirst().get() :
                        null;
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(
                DefaultLogService.class.getName(), appbaseServiceEntry2.getService().getClass().getName());
    }
}
