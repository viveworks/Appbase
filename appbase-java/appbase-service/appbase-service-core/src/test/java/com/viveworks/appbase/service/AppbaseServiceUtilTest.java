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

package com.viveworks.appbase.service;

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.appbase.data.MapOfStringSetOfStringsValue;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.web.filter.AppbaseFilterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class AppbaseServiceUtilTest {
    private static AppbaseCore appbaseCore;

    @BeforeAll
    static void beforeAll() {
        appbaseCore = ClientCoreTestHelper.appbaseCore();
    }

    @Test
    void testIsFilterEnabled() {
        final ConfigurationService configurationService = appbaseCore.getConfigurationService();
        final Class<?> filterType1 = AppbaseFilterConfig.class;
        final String path1 = "/path1";
        final String method1 = "GET";

        // Filter config does not exist in configuration service
        Assertions.assertNull(configurationService.getTypedConfiguration(AppbaseFilterConfig.class));
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));

        // Filter config is empty
        final AppbaseFilterConfig appbaseFilterConfig1 = AppbaseFilterConfig.builder().build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig1);
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // All paths are blocked
        final AppbaseFilterConfig appbaseFilterConfig2 = AppbaseFilterConfig
                .builder()
                .blockedPaths(Set.of(AppbaseServiceConstants.WILDCARD))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig2);
        Assertions.assertFalse(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);


        // All paths are allowed
        final AppbaseFilterConfig appbaseFilterConfig3 = AppbaseFilterConfig
                .builder()
                .allowedPaths(Set.of(AppbaseServiceConstants.WILDCARD))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig3);
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Filter + path blocked
        final AppbaseFilterConfig appbaseFilterConfig4 = AppbaseFilterConfig
                .builder()
                .blockedPathsByFilterTypeNameMap(Map.of(path1, new SetOfStringsValue(Set.of(filterType1.getName()))))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig4);
        Assertions.assertFalse(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Filter + path + method blocked
        final AppbaseFilterConfig appbaseFilterConfig5 = AppbaseFilterConfig
                .builder()
                .blockedMethodsByPathsByFilterTypeNameMap(
                        Map.of(filterType1.getName(),
                                new MapOfStringSetOfStringsValue(
                                        Map.of(path1, new SetOfStringsValue(Set.of(method1))))))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig5);
        Assertions.assertFalse(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Path + method blocked
        final AppbaseFilterConfig appbaseFilterConfig6 = AppbaseFilterConfig
                .builder()
                .blockedMethodsByPathMap(Map.of(path1, new SetOfStringsValue(Set.of(method1))))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig6);
        Assertions.assertFalse(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Path blocked
        final AppbaseFilterConfig appbaseFilterConfig7 = AppbaseFilterConfig
                .builder()
                .blockedPaths(Set.of(path1))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig7);
        Assertions.assertFalse(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Path allowed
        final AppbaseFilterConfig appbaseFilterConfig8 = AppbaseFilterConfig
                .builder()
                .allowedPaths(Set.of(path1))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig8);
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Path + method allowed
        final AppbaseFilterConfig appbaseFilterConfig9 = AppbaseFilterConfig
                .builder()
                .allowedMethodsByPathMap(Map.of(path1, new SetOfStringsValue(Set.of(method1))))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig9);
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Filter + path allowed
        final AppbaseFilterConfig appbaseFilterConfig10 = AppbaseFilterConfig
                .builder()
                .allowedPathsByFilterTypeName(Map.of(filterType1.getName(), new SetOfStringsValue(Set.of(path1))))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig10);
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);

        // Filter + path + method allowed
        final AppbaseFilterConfig appbaseFilterConfig11 = AppbaseFilterConfig
                .builder()
                .allowedMethodsByPathsByFilterTypeNameMap(
                        Map.of(filterType1.getName(),
                                new MapOfStringSetOfStringsValue(
                                        Map.of(path1, new SetOfStringsValue(Set.of(method1))))))
                .build();
        configurationService.putTypedConfiguration(AppbaseFilterConfig.class, appbaseFilterConfig11);
        Assertions.assertTrue(AppbaseServiceUtil.isFilterEnabled(configurationService, filterType1, path1, method1));
        removeAppbaseFilterConfig(configurationService);
    }

    private void removeAppbaseFilterConfig(final ConfigurationService configurationService) {
        configurationService.removeTypedConfiguration(AppbaseFilterConfig.class);
        Assertions.assertNull(configurationService.getTypedConfiguration(AppbaseFilterConfig.class));
    }

    @Test
    void testSqlTinyInt() {
        Assertions.assertEquals(1, AppbaseServiceUtil.sqlTinyInt(true));
        Assertions.assertEquals(0, AppbaseServiceUtil.sqlTinyInt(null));
        Assertions.assertEquals(0, AppbaseServiceUtil.sqlTinyInt(false));
    }

    @Test
    void testNow() {
        Assertions.assertNotNull(AppbaseServiceUtil.nowSeconds());
    }

    @Test
    void testTraceIdKey() {
        final String traceIdKey1 = AppbaseServiceUtil.traceIdKey(appbaseCore);
        Assertions.assertEquals(TracerService.TRACE_ID_KEY, traceIdKey1);

        final String traceIdKey = UUID.randomUUID().toString();
        appbaseCore.getConfigurationService().putString(TracerService.TRACE_ID_KEY_PROPERTY, traceIdKey);

        final String traceIdKey2 = AppbaseServiceUtil.traceIdKey(appbaseCore);
        Assertions.assertEquals(traceIdKey, traceIdKey2);
    }
}