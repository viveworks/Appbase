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

package com.viveworks.appbase.core;

import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.microprofile.AppbaseCoreMicroprofile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseCoreFactoryTest {
    private AppbaseCore appbaseCore1;
    private AppbaseCore appbaseCore2;
    private AppbaseCore appbaseCore3;
    private AppbaseCoreMicroprofile appbaseCoreMicroprofile1;
    private AppbaseCoreMicroprofile appbaseCoreMicroprofile2;
    private AppbaseCoreMicroprofile appbaseCoreMicroprofile3;
    private AppbaseCoreMicroprofile appbaseCoreMicroprofile4;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        if (appbaseCore1 != null) {
            appbaseCore1.shutdown();
        }

        if (appbaseCore2 != null) {
            appbaseCore2.shutdown();
        }

        if (appbaseCore3 != null) {
            appbaseCore3.shutdown();
        }

        if (appbaseCoreMicroprofile1 != null) {
            appbaseCoreMicroprofile1.shutdown();
        }

        if (appbaseCoreMicroprofile2 != null) {
            appbaseCoreMicroprofile2.shutdown();
        }

        if (appbaseCoreMicroprofile3 != null) {
            appbaseCoreMicroprofile3.shutdown();
        }

        if (appbaseCoreMicroprofile4 != null) {
            appbaseCoreMicroprofile4.shutdown();
        }
    }

    @Test
    void testFactory() {
        appbaseCore1 = AppbaseCoreFactory.newAppbaseCore();
        Assertions.assertNotNull(appbaseCore1);
        appbaseCore1.shutdown();

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                new DefaultAppbaseCoreBootstrapRegistry();
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, null);
        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                DefaultAppbaseCoreBootstrapConfiguration.builder().build();

        appbaseCore2 = AppbaseCoreFactory.newAppbaseCore(appbaseCoreBootstrap);
        Assertions.assertNotNull(appbaseCore2);
        appbaseCore2.shutdown();

        appbaseCore3 = AppbaseCoreFactory.newAppbaseCore(
                appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
        Assertions.assertNotNull(appbaseCore3);
        appbaseCore3.shutdown();

        appbaseCoreMicroprofile1 = AppbaseCoreFactory.newAppbaseCoreMicroprofile();
        Assertions.assertNotNull(appbaseCoreMicroprofile1);
        appbaseCoreMicroprofile1.shutdown();

        appbaseCoreMicroprofile2 = AppbaseCoreFactory.newAppbaseCoreMicroprofile(appbaseCoreBootstrap);
        Assertions.assertNotNull(appbaseCoreMicroprofile2);
        appbaseCoreMicroprofile2.shutdown();

        appbaseCoreMicroprofile3 = AppbaseCoreFactory
                .newAppbaseCoreMicroprofile(appbaseCoreBootstrap, appbaseCoreBootstrapConfiguration);
        Assertions.assertNotNull(appbaseCoreMicroprofile3);
        appbaseCoreMicroprofile3.shutdown();

        final AppbaseCore appbaseCore = AppbaseCoreFactory.newAppbaseCore();
        final AppbaseCoreMicroprofile appbaseCoreMicroprofile =
                AppbaseCoreFactory.newAppbaseCoreMicroprofile();
        appbaseCoreMicroprofile4 = AppbaseCoreFactory.newAppbaseCoreMicroprofile(
                appbaseCore,
                appbaseCoreMicroprofile.getMicroprofileConfigurationService(),
                appbaseCoreMicroprofile.getMicroprofileServiceRegistry());
        Assertions.assertNotNull(appbaseCoreMicroprofile4);
        appbaseCoreMicroprofile4.shutdown();
    }
}
