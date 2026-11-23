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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.service.jdbc.DefaultJdbcClientService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientIntegrationService;
import com.viveworks.appbase.client.service.jdbc.JdbcClientServiceTestHelper;
import com.viveworks.appbase.client.service.jdbc.JdbcClientServiceTestUtil;
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
class DefaultVitessMysqlJdbcClientServiceTest {
    private static final String VITESS_JDBC_CONFIG_CLASSPATH_FILE =
            AppbaseConstants.CLASSPATH_FILE_PREFIX + "/appbase-client-vitess-mysql-jdbc.yaml";

    private static DefaultJdbcClientService defaultJdbcClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultJdbcClientService.registerObjectMapperSubtypes();
        defaultJdbcClientService = JdbcClientServiceTestHelper
                .defaultJdbcClientService(AppbaseTestInfraProfile.resolve(VITESS_JDBC_CONFIG_CLASSPATH_FILE));
    }

    @AfterAll
    static void tearDown() {
        if (defaultJdbcClientService != null) {
            defaultJdbcClientService.shutdown();
        }
    }

    @Test
    void testVitessDefaultJdbcClientService() throws AppbaseClientIntegrationService.NotFoundException {
        Assertions.assertNotNull(defaultJdbcClientService);

        final JdbcClientIntegrationService jdbcClientIntegrationService =
                defaultJdbcClientService.getClientIntegrationService();
        JdbcClientServiceTestUtil.jdbcClientServiceTest(defaultJdbcClientService, jdbcClientIntegrationService);
    }
}
