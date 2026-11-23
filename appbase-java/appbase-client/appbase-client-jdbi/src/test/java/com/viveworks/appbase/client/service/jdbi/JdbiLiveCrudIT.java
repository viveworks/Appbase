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

package com.viveworks.appbase.client.service.jdbi;

import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Live MariaDB CRUD contract through JDBI handles and statements. */
@EnabledIfSystemProperty(named = "JdbiClientServiceTests.enabled", matches = "true")
class JdbiLiveCrudIT {
    private static final String TABLE = "appbase_jdbi_live_it";
    private static DefaultJdbiClientService service;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultJdbiClientService.registerObjectMapperSubtypes();
        service = JdbiClientServiceTestHelper.defaultJdbiClientService();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (service != null) {
            service.getClientIntegrationService().getClient().useHandle(
                    handle -> handle.execute("DROP TABLE IF EXISTS " + TABLE));
            service.shutdown();
        }
    }

    @Test
    void createReadUpdateDeleteAndNegativeQuery() throws Exception {
        final Jdbi jdbi = service.getClientIntegrationService().getClient();
        jdbi.useHandle(handle -> {
            handle.execute("DROP TABLE IF EXISTS " + TABLE);
            handle.execute("CREATE TABLE " + TABLE + " (id INT PRIMARY KEY, value VARCHAR(64))");
            Assertions.assertEquals(1, handle.createUpdate("INSERT INTO " + TABLE + " (id,value) VALUES (:id,:value)")
                    .bind("id", 1).bind("value", "before").execute());
            Assertions.assertEquals("before", handle.createQuery("SELECT value FROM " + TABLE + " WHERE id=:id")
                    .bind("id", 1).mapTo(String.class).one());
            Assertions.assertEquals(1, handle.createUpdate("UPDATE " + TABLE + " SET value=:value WHERE id=:id")
                    .bind("value", "after").bind("id", 1).execute());
            Assertions.assertEquals(1, handle.execute("DELETE FROM " + TABLE + " WHERE id=1"));
            Assertions.assertThrows(Exception.class,
                    () -> handle.createQuery("SELECT * FROM appbase_jdbi_missing_table").mapToMap().list());
            handle.execute("DROP TABLE " + TABLE);
        });
    }
}
