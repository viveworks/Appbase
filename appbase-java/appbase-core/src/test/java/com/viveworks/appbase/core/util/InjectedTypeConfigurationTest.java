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

package com.viveworks.appbase.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.metric.DefaultMetricService;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class InjectedTypeConfigurationTest {
    @Test
    void testSerde() throws Exception {
        final InjectedTypeConfiguration<DefaultMetricService> injectedTypeConfiguration = InjectedTypeConfiguration
                .<DefaultMetricService>builder()
                .objectType(DefaultMetricService.class)
                .qualifierAnnotationType(null)
                .qualifierName(null)
                .build();
        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(InjectedTypeConfiguration.class)
                .writeValueAsString(injectedTypeConfiguration);
        final InjectedTypeConfiguration<DefaultMetricService> injectedTypeConfiguration1 = yamlMapper
                .readerFor(InjectedTypeConfiguration.class)
                .readValue(yaml);

        Assertions.assertEquals(injectedTypeConfiguration.getObjectType(), injectedTypeConfiguration1.getObjectType());
        Assertions.assertEquals(
                injectedTypeConfiguration.getQualifierAnnotationType(),
                injectedTypeConfiguration.getQualifierAnnotationType());
        Assertions.assertEquals(
                injectedTypeConfiguration.getQualifierName(),
                injectedTypeConfiguration1.getQualifierName());
        Assertions.assertEquals(injectedTypeConfiguration, injectedTypeConfiguration1);
        Assertions.assertEquals(injectedTypeConfiguration.hashCode(), injectedTypeConfiguration1.hashCode());
        Assertions.assertEquals(injectedTypeConfiguration.toString(), injectedTypeConfiguration1.toString());
    }
}
