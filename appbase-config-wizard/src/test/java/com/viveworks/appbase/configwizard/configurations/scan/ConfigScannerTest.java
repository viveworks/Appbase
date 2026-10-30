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

package com.viveworks.appbase.configwizard.configurations.scan;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ConfigScanner}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ConfigScannerTest {

    private final ConfigScanner scanner = new ConfigScanner();

    @Test
    void scan_cacheIsSharedAcrossInstances() {
        // Tests construct a fresh ConfigScanner per test method. If the cache is
        // per-instance, every test re-triggers the full unbounded classpath scan —
        // the recurring CPU excursion this guards against. A second instance must
        // reuse the first instance's cached scan result rather than rescanning.
        var first = new ConfigScanner().scan();
        var second = new ConfigScanner().scan();

        assertSame(first, second, "a second ConfigScanner instance should reuse the cached "
                + "scan result instead of re-scanning the classpath");
    }

    @Test
    void scan_discoversAtLeastOneConfigClass() {
        // Aerospike/Redis/etc. are on classpath via appbase-bootstrap-core
        assertFalse(scanner.scan().isEmpty());
    }

    @Test
    void scan_excludesAbstractClassesAndInterfaces() {
        var results = scanner.scan();
        assertTrue(results.stream().noneMatch(r -> Modifier.isAbstract(r.configClass().getModifiers())));
        assertTrue(results.stream().noneMatch(r -> r.configClass().isInterface()));
    }

    @Test
    void search_filtersByLayer() {
        var results = scanner.search("client");
        assertFalse(results.isEmpty(), "Expected at least one match");
        assertTrue(results.stream().allMatch(r ->
                r.fqcn().toLowerCase().contains("client")
                        || r.simpleName().toLowerCase().contains("client")));
    }

    @Test
    void search_filtersByQuery() {
        var results = scanner.search("redis");
        assertFalse(results.isEmpty(), "Expected at least one match");
        assertTrue(results.stream().allMatch(r ->
                r.simpleName().toLowerCase().contains("redis")
                        || r.fqcn().toLowerCase().contains("redis")));
    }

    @Test
    void scan_eachInfoHasNonNullFields() {
        var first = scanner.scan().get(0);
        assertNotNull(first.fqcn());
        assertNotNull(first.simpleName());
        assertNotNull(first.layer());
        assertNotNull(first.module());
        assertNotNull(first.configClass());
        // defaultInstance may be null — no assertion
    }

    @Test
    void infoFor_loadsArbitraryClassNotInScanList() {
        // String is not an AppbaseConfiguration, so it is never in scan().
        // infoFor() must still wrap it in a ConfigClassInfo without throwing.
        ConfigClassInfo info = scanner.infoFor(String.class);

        assertNotNull(info);
        assertEquals("java.lang.String", info.fqcn());
        assertEquals("String", info.simpleName());
        assertNotNull(info.layer());
        assertNotNull(info.module());
        assertEquals(String.class, info.configClass());
    }

    @Test
    void infoFor_derivesLayerAndModuleForNestedAppbaseClass() {
        // Pick any scanned class and verify infoFor() matches the scan result.
        var scanned = scanner.scan().get(0);
        ConfigClassInfo info = scanner.infoFor(scanned.configClass());

        assertEquals(scanned.fqcn(), info.fqcn());
        assertEquals(scanned.layer(), info.layer());
        assertEquals(scanned.module(), info.module());
    }
}
