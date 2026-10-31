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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.viveworks.appbase.core.util.AppbaseUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DefaultFirebaseAppClientIntegrationServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultFirebaseAppClientIntegrationServiceConfigurationTest {

    @Test
    void toString_doesNotDiscloseDatabaseUrl() {
        // CFG-LEAK-08 (CWE-532, CWE-200): databaseUrl may embed sensitive project/instance
        // identification (and, depending on deployment, access-controlling query parameters).
        // It must be redacted in toString(), matching the pattern DruidConnectionConfiguration
        // already applies to its own URL field. googleCredentialsConfiguration is already
        // redacted; textValueKeyConfiguration is a non-secret cache-key identifier and is
        // intentionally left as-is — neither is touched by this fix.
        final String databaseUrlSentinel = "sentinel-firebase-databaseurl-CFGLEAK08";
        final DefaultFirebaseAppClientIntegrationServiceConfiguration config =
                DefaultFirebaseAppClientIntegrationServiceConfiguration
                        .builder()
                        .databaseUrl(databaseUrlSentinel)
                        .build();

        final String result = config.toString();

        assertFalse(result.contains(databaseUrlSentinel), "toString() must not contain databaseUrl's content");
        assertTrue(result.contains(AppbaseUtil.OBFUSCATE), "toString() must obfuscate the redacted field");
    }

    @Test
    void getDatabaseUrl_stillReturnsTheRealValue() {
        final String databaseUrl = "sentinel-firebase-databaseurl-CFGLEAK08";
        final DefaultFirebaseAppClientIntegrationServiceConfiguration config =
                DefaultFirebaseAppClientIntegrationServiceConfiguration
                        .builder()
                        .databaseUrl(databaseUrl)
                        .build();

        assertEquals(databaseUrl, config.getDatabaseUrl());
    }
}
