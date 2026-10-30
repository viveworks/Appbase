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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.service.jasync.DefaultJasyncClientService;
import com.viveworks.appbase.client.service.jasync.JasyncClientIntegrationService;
import com.viveworks.appbase.client.service.jasync.JasyncClientServiceTestHelper;
import com.viveworks.appbase.client.service.jasync.JasyncClientServiceTestUtil;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseTestInfraProfile;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "VitessClientServiceTests.enabled", matches = "true")
class DefaultVitessJasyncClientServiceTest {
    private static final String VITESS_JASYNC_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-vitess-jasync.yaml";

    private static DefaultJasyncClientService defaultJasyncClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultJasyncClientService.registerObjectMapperSubtypes();
        defaultJasyncClientService = JasyncClientServiceTestHelper.defaultJasyncClientService(
                AppbaseTestInfraProfile.resolve(VITESS_JASYNC_CONFIG_CLASSPATH_FILE));
    }

    @AfterAll
    static void tearDown() {
        if (defaultJasyncClientService != null) {
            defaultJasyncClientService.shutdown();
        }
    }

    @Test
    void testVitessDefaultJasyncClientService() throws AppbaseClientIntegrationService.NotFoundException {
        Assertions.assertNotNull(defaultJasyncClientService);

        final JasyncClientIntegrationService jasyncClientIntegrationService =
                defaultJasyncClientService.getClientIntegrationService();
        JasyncClientServiceTestUtil.jasyncClientServiceTest(defaultJasyncClientService, jasyncClientIntegrationService);
    }
}
