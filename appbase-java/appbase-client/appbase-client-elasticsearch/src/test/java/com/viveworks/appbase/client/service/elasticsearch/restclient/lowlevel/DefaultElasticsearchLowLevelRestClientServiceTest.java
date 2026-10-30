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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
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
@EnabledIfSystemProperty(named = "ElasticsearchClientServiceTests.enabled", matches = "true")
class DefaultElasticsearchLowLevelRestClientServiceTest {
    private static DefaultElasticsearchLowLevelRestClientService defaultElasticsearchRestClientService;

    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultElasticsearchLowLevelRestClientService.registerObjectMapperSubtypes();
        defaultElasticsearchRestClientService =
                ElasticsearchLowLevelRestClientServiceTestHelper.defaultElasticsearchRestClientService();
    }

    @AfterAll
    static void tearDown() {
        if (defaultElasticsearchRestClientService != null) {
            defaultElasticsearchRestClientService.shutdown();
        }
    }

    @Test
    void testDefaultElasticsearchRestClientService() throws AppbaseClientIntegrationService.NotFoundException {
        Assertions.assertNotNull(defaultElasticsearchRestClientService);

        final ElasticsearchLowLevelRestClientIntegrationService elasticsearchLowLevelRestClientIntegrationService =
                defaultElasticsearchRestClientService.getClientIntegrationService();
        ElasticsearchLowLevelRestClientServiceTestUtil.elasticsearchRestClientServiceTest(
                defaultElasticsearchRestClientService, elasticsearchLowLevelRestClientIntegrationService);
    }
}
