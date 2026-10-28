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

package com.viveworks.appbase.client.service.druid.sql.client;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestHelper;
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestUtil;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientConfiguration;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DruidConnectionConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "DruidClientServiceTests.enabled", matches = "true")
class DefaultDruidSqlClientTest {
    private static DruidSqlClientSharedResources druidSqlClientSharedResources;
    private static DefaultDruidSqlClient druidSqlClient;

    @BeforeAll
    static void setUp() {
        druidSqlClientSharedResources = DruidSqlClientTestHelper.defaultDruidSqlClientSharedResources();

        final AppbaseClientCore appbaseClientCore = druidSqlClientSharedResources.getAppbaseClientCore();
        final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration = DefaultDruidSqlClientConfiguration
                .builder()
                .druidConnectionConfiguration(
                        DruidConnectionConfiguration
                                .builder()
                                .url("jdbc:avatica:remote:url=" +
                                        "http://localhost:6082/druid/v2/sql/avatica-protobuf/;serialization=protobuf")
                                .build())
                .build();
        druidSqlClient = new DefaultDruidSqlClient(
                appbaseClientCore, druidSqlClientSharedResources, defaultDruidSqlClientConfiguration);
    }

    @AfterAll
    static void tearDown() {
        if (druidSqlClient != null) {
            druidSqlClient.shutdown();
        }

        if (druidSqlClientSharedResources != null) {
            druidSqlClientSharedResources.shutdown();
        }
    }

    @Test
    void testExecuteQuery() throws Exception {
        DruidSqlClientTestUtil.executeQueryTest(druidSqlClient);
    }

    @Test
    void testExecuteParameterizedQuery() {
        DruidSqlClientTestUtil.executeParameterizedQueryTest(druidSqlClient);
    }
}
