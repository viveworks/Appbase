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
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestUtil;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameterFactory;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientSharedResources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "DruidClientServiceTests.enabled", matches = "true")
class DefaultDruidSqlClientServiceTest {
    private static DruidSqlClientSharedResources defaultDruidSqlClientSharedResources;
    private static DefaultDruidSqlClientService defaultDruidSqlClientService;

    @BeforeAll
    static void setUp() {
        final AppbaseClientCore appbaseClientCore = DruidSqlClientTestHelper.appbaseClientCore();

        defaultDruidSqlClientSharedResources = new DefaultDruidSqlClientSharedResources(appbaseClientCore);
        defaultDruidSqlClientService = new DefaultDruidSqlClientService(
                appbaseClientCore,
                defaultDruidSqlClientSharedResources,
                new DefaultDruidSqlParameterFactory(defaultDruidSqlClientSharedResources));
    }

    @AfterAll
    static void tearDown() {
        if (defaultDruidSqlClientService != null) {
            defaultDruidSqlClientService.shutdown();
        }

        if (defaultDruidSqlClientSharedResources != null) {
            defaultDruidSqlClientSharedResources.shutdown();
        }
    }

    @Test
    void testDefaultDruidSqlClientService() {
        Assertions.assertNotNull(defaultDruidSqlClientService);

        final DruidSqlClientIntegrationService druidSqlClientIntegrationService =
                defaultDruidSqlClientService.getClientIntegrationService();
        DruidSqlClientTestUtil.druidSqlClientServiceTest(defaultDruidSqlClientService, druidSqlClientIntegrationService);
    }
}
