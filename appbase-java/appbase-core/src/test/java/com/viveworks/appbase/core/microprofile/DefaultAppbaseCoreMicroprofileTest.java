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

package com.viveworks.appbase.core.microprofile;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapConfiguration;
import com.viveworks.appbase.core.bootstrap.AppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrap;
import com.viveworks.appbase.core.bootstrap.DefaultAppbaseCoreBootstrapRegistry;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.test.CoreTestFixtures;
import com.viveworks.appbase.core.util.AppbaseConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultAppbaseCoreMicroprofileTest {
    private static LogService logService;
    private static AppbaseCore defaultAppbaseCore;
    private static AppbaseCoreMicroprofile appbaseCoreMicroprofile;

    @BeforeAll
    static void setUp() {
        logService = CoreTestFixtures.getLogService();
    }

    @AfterAll
    static void tearDown() {
        // This test sets APPBASE_CONFIGURATION_FILE_PATH -- a JVM-wide system property -- and the
        // no-arg DefaultAppbaseCoreMicroprofile reads it. Clear it here so a later test class in the
        // same Surefire fork does not inherit the leaked, Lucene-disabled config path. Without this,
        // AppbaseCoreBootstrappedServicesTest.testBootstrap sees getLuceneService() == null (it passes
        // in isolation, fails when this class runs first) -- a test-ordering leak, not a product bug.
        System.clearProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY);

        if (appbaseCoreMicroprofile != null) {
            appbaseCoreMicroprofile.shutdown();
        }

        if (defaultAppbaseCore != null) {
            defaultAppbaseCore.shutdown();
        }

        if (logService != null) {
            logService.shutdown();
        }
    }

    @Test
    void testDefaultAppbaseCoreMicroprofile() {
        final String classpathFile = "classpath:///appbaseBootstrapConfiguration.yaml";
        System.setProperty(AppbaseConstants.APPBASE_CONFIGURATION_FILE_PATH_PROPERTY_KEY, classpathFile);

        defaultAppbaseCore = CoreTestFixtures.getAppbaseCore(logService);
        Assertions.assertNotNull(defaultAppbaseCore);

        appbaseCoreMicroprofile = new DefaultAppbaseCoreMicroprofile();
        Assertions.assertNotNull(appbaseCoreMicroprofile);
        runTests(appbaseCoreMicroprofile);
        appbaseCoreMicroprofile.shutdown();

        final AppbaseCoreBootstrapRegistry appbaseCoreBootstrapRegistry =
                new DefaultAppbaseCoreBootstrapRegistry();
        final String classpathFile1 = "classpath:///appbase-lucene-disabled-configuration.yaml";
        final AppbaseCoreBootstrap<AppbaseCoreBootstrapConfiguration> appbaseCoreBootstrap =
                new DefaultAppbaseCoreBootstrap(appbaseCoreBootstrapRegistry, classpathFile1);
        appbaseCoreMicroprofile = new DefaultAppbaseCoreMicroprofile(appbaseCoreBootstrap);
        runTests(appbaseCoreMicroprofile);
        appbaseCoreMicroprofile.shutdown();

        final AppbaseCoreBootstrapConfiguration defaultAppbaseCoreBootstrapConfiguration =
                CoreTestFixtures.getLuceneDisabledBootstrapConfiguration();
        appbaseCoreMicroprofile = new DefaultAppbaseCoreMicroprofile(
                appbaseCoreBootstrap, defaultAppbaseCoreBootstrapConfiguration);
        runTests(appbaseCoreMicroprofile);
        appbaseCoreMicroprofile.reload();
        runTests(appbaseCoreMicroprofile);
        appbaseCoreMicroprofile.reload(defaultAppbaseCoreBootstrapConfiguration);
        runTests(appbaseCoreMicroprofile);

        appbaseCoreMicroprofile.shutdown();
    }

    private void runTests(final AppbaseCoreMicroprofile appbaseCoreMicroprofile) {
        Assertions.assertNotNull(appbaseCoreMicroprofile.getLogService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getMetricService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getValidationService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getMediatorService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getMicroprofileConfigurationService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getMicroprofileServiceRegistry());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getTracerService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getReactorService());
        Assertions.assertNotNull(appbaseCoreMicroprofile.getJmxService());

        final AppbaseCoreBootstrapConfiguration appbaseCoreBootstrapConfiguration =
                appbaseCoreMicroprofile.getAppbaseBootstrapConfiguration();
        Assertions.assertNotNull(appbaseCoreBootstrapConfiguration);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseCoreMicroprofile.getStatus());
    }
}
