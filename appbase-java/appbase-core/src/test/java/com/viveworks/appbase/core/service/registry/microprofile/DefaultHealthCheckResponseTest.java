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

package com.viveworks.appbase.core.service.registry.microprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class DefaultHealthCheckResponseTest {
    @Test
    void testSerde() throws JsonProcessingException {
        final DefaultHealthCheckResponse defaultHealthCheckResponse = new DefaultHealthCheckResponse(
                "name",
                HealthCheckResponse.State.UP,
                Map.of(ConfigurationService.class.getName(), ServiceStatus.STARTED));

        final ObjectMapper yamlMapper = YamlUtil.getYamlMapper();
        final String yaml = yamlMapper
                .writerFor(DefaultHealthCheckResponse.class)
                .writeValueAsString(defaultHealthCheckResponse);
        final DefaultHealthCheckResponse defaultHealthCheckResponse1 = yamlMapper
                .readerFor(DefaultHealthCheckResponse.class)
                .readValue(yaml);

        Assertions.assertEquals(defaultHealthCheckResponse.getName(), defaultHealthCheckResponse1.getName());
        Assertions.assertEquals(defaultHealthCheckResponse.getState(), defaultHealthCheckResponse1.getState());
        Assertions.assertEquals(defaultHealthCheckResponse.getData(), defaultHealthCheckResponse1.getData());
        Assertions.assertEquals(defaultHealthCheckResponse.getContext(), defaultHealthCheckResponse1.getContext());
        Assertions.assertEquals(defaultHealthCheckResponse, defaultHealthCheckResponse1);
        Assertions.assertEquals(defaultHealthCheckResponse.hashCode(), defaultHealthCheckResponse1.hashCode());
        Assertions.assertEquals(defaultHealthCheckResponse.toString(), defaultHealthCheckResponse1.toString());
    }
}
