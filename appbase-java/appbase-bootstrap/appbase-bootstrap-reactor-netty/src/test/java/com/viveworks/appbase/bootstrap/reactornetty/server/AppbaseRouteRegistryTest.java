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

package com.viveworks.appbase.bootstrap.reactornetty.server;

import com.viveworks.appbase.service.exchange.AppbaseServiceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AppbaseRouteRegistryTest {

    private static final AppbaseServiceHandler NOOP = exchange -> Mono.empty();

    @Test
    void aRegisteredRouteIsFoundByMethodAndPath() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/health", NOOP);

        Assertions.assertSame(NOOP, registry.lookup("GET", "/health"));
    }

    @Test
    void theMethodIsPartOfTheKey() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/health", NOOP);

        Assertions.assertNull(registry.lookup("POST", "/health"));
    }

    @Test
    void lookupIsCaseInsensitiveOnTheMethod() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("get", "/health", NOOP);

        Assertions.assertSame(NOOP, registry.lookup("GET", "/health"));
    }

    @Test
    void anUnknownRouteReturnsNull() {
        Assertions.assertNull(new AppbaseRouteRegistry().lookup("GET", "/nope"));
    }

    @Test
    void registeringTheSameRouteTwiceIsRejected() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/health", NOOP);

        Assertions.assertThrows(IllegalStateException.class, () -> registry.route("GET", "/health", NOOP));
    }

    @Test
    void removingARegisteredRouteReturnsTrueAndTheRouteNoLongerLooksUp() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();
        registry.route("GET", "/health", NOOP);

        Assertions.assertTrue(registry.remove("GET", "/health"));
        Assertions.assertNull(registry.lookup("GET", "/health"));
    }

    @Test
    void removingAnUnregisteredRouteReturnsFalse() {
        final AppbaseRouteRegistry registry = new AppbaseRouteRegistry();

        Assertions.assertFalse(registry.remove("GET", "/nope"));
    }
}
