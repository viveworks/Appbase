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

package com.viveworks.appbase.bootstrap.servlet;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the future-JDK guard distinguishes Jersey package scanning from explicit registration.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 */
final class JerseyResourceConfigCompatibilityTest {

    private static final int FUTURE_RUNTIME_FEATURE = 26;

    @Test
    void test_AC3015_2_directPackageScanRejectsRuntimeAfterJava25() {
        final ResourceConfig config = new ResourceConfig().packages(getClass().getPackageName());
        final ResourceConfig copiedConfig = new ResourceConfig(config);

        final IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> JerseyResourceConfigCompatibility.validate(copiedConfig, FUTURE_RUNTIME_FEATURE));

        assertTrue(exception.getMessage().contains("register(Class)"));
    }

    @Test
    void test_AC3015_3_propertyPackageScanRejectsRuntimeAfterJava25() {
        final ResourceConfig config = new ResourceConfig()
            .property(ServerProperties.PROVIDER_PACKAGES, getClass().getPackageName());

        final IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> JerseyResourceConfigCompatibility.validate(config, FUTURE_RUNTIME_FEATURE));

        assertTrue(exception.getMessage().contains("Java 26"));
    }

    @Test
    void test_AC3015_4_explicitRegistrationRemainsSupportedAfterJava25() {
        final ResourceConfig config = new ResourceConfig().register(ExplicitResource.class);

        assertDoesNotThrow(
            () -> JerseyResourceConfigCompatibility.validate(config, FUTURE_RUNTIME_FEATURE));
    }

    private static final class ExplicitResource {
    }
}
