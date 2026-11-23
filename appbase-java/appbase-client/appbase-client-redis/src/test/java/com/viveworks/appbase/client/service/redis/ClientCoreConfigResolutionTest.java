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

package com.viveworks.appbase.client.service.redis;

import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Behavioural guard for the internal work item 503 test-resource bootstrap contract in this module.
 *
 * <p>Every Redis client/service integration test bootstraps the appbase client-core configuration
 * through {@code com.viveworks.appbase.client.core.ClientCoreTestHelper}, which resolves the two YAML
 * documents below from the test classpath root. The live Redis setup is where a missing document
 * otherwise surfaces, so this test provides a backend-free signal for the classpath contract.
 *
 * <p>This proves that both documents are available and readable, not which dependency supplied their
 * bytes. Provenance is enforced structurally: neither shared YAML remains in this module's test
 * resources, while its POM declares the {@code appbase-client-core:tests} dependency that owns both.
 * The exact-head L-02 campaign separately removes that dependency, requires RED, restores it, and
 * requires the live Redis test to return GREEN; no unexecuted campaign result is claimed here.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class ClientCoreConfigResolutionTest {

    private static final String CORE_CONFIG = "/appbase-core.yaml";

    private static final String CLIENT_CORE_CONFIG = "/appbase-client-core.yaml";

    @Test
    void appbaseCoreConfigResolvesAndIsReadableOnTestClasspath() throws Exception {
        assertResolvesAndReadable(CORE_CONFIG);
    }

    @Test
    void appbaseClientCoreConfigResolvesAndIsReadableOnTestClasspath() throws Exception {
        assertResolvesAndReadable(CLIENT_CORE_CONFIG);
    }

    private void assertResolvesAndReadable(final String resource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            Assertions.assertNotNull(
                    stream,
                    resource + " must resolve on the test classpath — supply it through the "
                            + "appbase-client-core:tests dependency");
            Assertions.assertTrue(stream.read() != -1, resource + " resolved but is empty");
        }
    }
}
