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

import com.viveworks.appbase.core.service.log.LogService;
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
class UnreadyAppbaseServiceEntriesTest {
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
    void testGetUnreadyAppbaseServiceEntries() {
        final List<AppbaseServiceEntry> appbaseServiceEntries = new ArrayList<>();

        final UnreadyAppbaseServiceEntries unreadyAppbaseServiceEntries =
                new UnreadyAppbaseServiceEntries(appbaseServiceEntries);
        Assertions.assertNotNull(unreadyAppbaseServiceEntries.getUnreadyAppbaseServiceEntries());
        Assertions.assertTrue(unreadyAppbaseServiceEntries.getUnreadyAppbaseServiceEntries().isEmpty());

        final AppbaseServiceEntry appbaseServiceEntry = new AppbaseServiceEntry(logService, logService.getStatus());
        appbaseServiceEntries.add(appbaseServiceEntry);
        final UnreadyAppbaseServiceEntries unreadyAppbaseServiceEntries1 =
                new UnreadyAppbaseServiceEntries(appbaseServiceEntries);
        Assertions.assertNotNull(unreadyAppbaseServiceEntries.getUnreadyAppbaseServiceEntries());
        Assertions.assertEquals(1, unreadyAppbaseServiceEntries1.getUnreadyAppbaseServiceEntries().size());
    }
}
