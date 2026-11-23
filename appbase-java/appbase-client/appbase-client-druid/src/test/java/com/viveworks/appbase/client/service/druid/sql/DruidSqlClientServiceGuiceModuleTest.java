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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientCoreGuiceModule;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestHelper;
import com.viveworks.appbase.client.service.druid.DruidSqlClientTestUtil;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameterFactory;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientSharedResources;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.di.guice.AppbaseCoreGuiceModule;
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
public class DruidSqlClientServiceGuiceModuleTest {
    private static AppbaseCore appbaseCore;
    private static AppbaseClientCore appbaseClientCore;
    private static DruidSqlClientSharedResources defaultDruidSqlClientSharedResources;
    private static DruidSqlClientService druidSqlClientService;

    @BeforeAll
    static void setUp() {
        appbaseClientCore = DruidSqlClientTestHelper.appbaseClientCore();
        appbaseCore = appbaseClientCore.getAppbaseCore();
        defaultDruidSqlClientSharedResources = new DefaultDruidSqlClientSharedResources(appbaseClientCore);
        druidSqlClientService = new DefaultDruidSqlClientService(
                appbaseClientCore,
                defaultDruidSqlClientSharedResources,
                new DefaultDruidSqlParameterFactory(defaultDruidSqlClientSharedResources));
    }

    @AfterAll
    static void tearDown() {
        if (druidSqlClientService != null) {
            druidSqlClientService.shutdown();
        }

        if (defaultDruidSqlClientSharedResources != null) {
            defaultDruidSqlClientSharedResources.shutdown();
        }

        if (appbaseClientCore != null) {
            appbaseClientCore.shutdown();
        }

        if (appbaseCore != null) {
            appbaseCore.shutdown();
        }
    }

    @Test
    void testDruidSqlClientServiceGuiceModule() throws AppbaseClientIntegrationService.NotFoundException {
        final AppbaseCoreGuiceModule appbaseCoreGuiceModule = new AppbaseCoreGuiceModule(appbaseCore);
        final AppbaseClientCoreGuiceModule appbaseClientCoreGuiceModule =
                new AppbaseClientCoreGuiceModule(appbaseClientCore);

        final DruidSqlClientServiceGuiceModule druidSqlClientServiceGuiceModule =
                new DruidSqlClientServiceGuiceModule();
        final Injector injector = Guice.createInjector(
                appbaseCoreGuiceModule, appbaseClientCoreGuiceModule, druidSqlClientServiceGuiceModule);
        Assertions.assertNotNull(injector);

        druidSqlClientService = injector.getInstance(DruidSqlClientService.class);
        Assertions.assertNotNull(druidSqlClientService);

        final DruidSqlClientIntegrationService druidSqlClientIntegrationService =
                druidSqlClientService.getClientIntegrationService();
        DruidSqlClientTestUtil.druidSqlClientServiceTest(druidSqlClientService, druidSqlClientIntegrationService);
    }
}
