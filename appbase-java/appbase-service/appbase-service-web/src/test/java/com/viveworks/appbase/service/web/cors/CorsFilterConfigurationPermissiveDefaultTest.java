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

package com.viveworks.appbase.service.web.cors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * AUD-002: the shipped CORS default is permissive (Access-Control-Allow-Origin: * on all paths). The
 * default is kept (flipping it would break deployments), but it must be DETECTABLE so it can be warned
 * about at startup rather than staying silent. This asserts the detector.
 *
 * @author Navaneeth Mysore G
 */
class CorsFilterConfigurationPermissiveDefaultTest {

    @Test
    void unconfiguredCorsIsFlaggedAsThePermissiveDefault() {
        assertTrue(CorsFilterConfiguration.builder().build().usesPermissiveWildcardDefault(),
                "the shipped default (no explicit CORS config) must be detected as permissive");
    }

    @Test
    void anExplicitOriginAllowlistIsNotThePermissiveDefault() {
        final CorsFilterConfiguration configured = CorsFilterConfiguration.builder()
                .pathByCorsHeadersMap(Map.of("/api", new MapOfStringStringValue(
                        Map.of("Access-Control-Allow-Origin", "https://app.example.com"))))
                .build();
        assertFalse(configured.usesPermissiveWildcardDefault(),
                "an explicit origin allowlist must NOT be flagged as the permissive default");
    }
}
