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

package com.viveworks.appbase.client.service.firebase.auth;

import com.viveworks.appbase.client.service.firebase.app.configuration.DefaultFirebaseAppClientServiceConfiguration;
import com.viveworks.appbase.client.service.firebase.auth.configuration.DefaultFirebaseAuthClientServiceConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Verifies that the helper's default classpath configuration can bootstrap the emulator services.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class FirebaseAuthClientServiceTestHelperTest {
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
    }

    @Test
    void defaultClasspathConfigurationsExistAndParse() {
        AppbaseClientObjectMapperSubTypes.register();
        assertConfigurationParses(
                "/appbase-client-firebase-app.yaml",
                DefaultFirebaseAppClientServiceConfiguration.class);
        assertConfigurationParses(
                "/appbase-client-firebase-auth.yaml",
                DefaultFirebaseAuthClientServiceConfiguration.class);
    }

    private void assertConfigurationParses(final String resource, final Class<?> configurationType) {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            Assertions.assertNotNull(input, () -> resource + " must exist on the test classpath");
            Assertions.assertDoesNotThrow(() -> YamlUtil.getYamlMapper().readValue(input, configurationType));
        } catch (final IOException exception) {
            throw new AssertionError("Unable to close " + resource, exception);
        }
    }
}
