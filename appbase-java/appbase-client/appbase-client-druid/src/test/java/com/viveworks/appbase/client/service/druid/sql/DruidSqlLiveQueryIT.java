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
package com.viveworks.appbase.client.service.druid.sql;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestHelper;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameterFactory;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSet;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClient;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientSharedResources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Deterministic live query through Appbase's Druid Avatica adapter. */
@EnabledIfSystemProperty(named = "DruidClientServiceTests.enabled", matches = "true")
class DruidSqlLiveQueryIT {
    private static DruidSqlClientSharedResources resources;
    private static DefaultDruidSqlClientService service;

    @BeforeAll
    static void setUp() {
        AppbaseClientCore core = DruidSqlClientTestHelper.appbaseClientCore();
        resources = new DefaultDruidSqlClientSharedResources(core);
        service = new DefaultDruidSqlClientService(core, resources,
                new DefaultDruidSqlParameterFactory(resources));
    }

    @AfterAll
    static void tearDown() {
        if (service != null) {
            service.shutdown();
        }
        if (resources != null) {
            resources.shutdown();
        }
    }

    @Test
    void constantQueryAndMissingDatasourceNegative() throws Exception {
        DruidSqlClient client = service.getClientIntegrationService().getClient();
        try (DruidResultSet rows = client.executeQuery("SELECT 1 AS appbase_value")) {
            Assertions.assertTrue(rows.next());
            Assertions.assertEquals(1, rows.getInteger("appbase_value"));
            Assertions.assertFalse(rows.next());
        }
        Assertions.assertThrows(Exception.class, () -> {
            try (DruidResultSet missing = client.executeQuery("SELECT * FROM appbase_missing_datasource")) {
                missing.next();
            }
        });
    }
}
