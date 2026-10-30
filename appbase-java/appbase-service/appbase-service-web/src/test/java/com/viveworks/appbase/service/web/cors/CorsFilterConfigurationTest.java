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

package com.viveworks.appbase.service.web.cors;

import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.service.path.AppbasePathException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class CorsFilterConfigurationTest {

    @Test
    void configuredKeysAreStoredCanonical() {
        final CorsFilterConfiguration configuration = CorsFilterConfiguration.builder()
                .pathByCorsHeadersMap(Map.of("/%61dmin", new MapOfStringStringValue(Map.of())))
                .build();

        assertTrue(configuration.getPathByCorsHeadersMap().containsKey("/admin"),
                "a key authored as /%61dmin must match a request for /admin");
    }

    @Test
    void theWildcardSentinelIsNeverCanonicalized() {
        final CorsFilterConfiguration configuration = CorsFilterConfiguration.builder()
                .pathByCorsHeadersMap(Map.of(AppbaseServiceConstants.WILDCARD, new MapOfStringStringValue(Map.of())))
                .build();

        assertTrue(configuration.getPathByCorsHeadersMap().containsKey(AppbaseServiceConstants.WILDCARD));
    }

    @Test
    void aKeyThatCanNeverMatchFailsAtLoadRatherThanSilently() {
        assertThrows(AppbasePathException.class, () -> CorsFilterConfiguration.builder()
                .pathByCorsHeadersMap(Map.of("/a%2Fb", new MapOfStringStringValue(Map.of())))
                .build());
    }
}
