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

package com.viveworks.appbase.service.web.oauth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class TokenCacheServiceLoaderTest {

    @Test
    void caffeineIsResolvableAsAServiceLoaderBackend() {
        // The built-in Caffeine cache is the default TokenCache the platform resolves without extra config.
        // Mutation: remove META-INF/services/...TokenCache and this goes red.
        boolean caffeinePresent = ServiceLoader.load(TokenCache.class).stream()
                .map(ServiceLoader.Provider::get)
                .anyMatch(CaffeineTokenCache.class::isInstance);
        assertTrue(caffeinePresent, "expected CaffeineTokenCache to be a ServiceLoader-registered TokenCache");
    }
}
