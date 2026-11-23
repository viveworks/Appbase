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
package com.viveworks.appbase.configwizard.configurations.scan;

import com.viveworks.appbase.configwizard.configurations.prompt.FieldInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for bounded graph discovery and cache publication.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class SupportedConfigClassRegistryTest {

    @Test
    void selfReferentialNestedTypeIsVisitedOnce() {
        AtomicInteger introspections = new AtomicInteger();
        ConfigClassInfo self = info(Self.class);
        SupportedConfigClassRegistry registry = registry(List.of(self), ignored -> {
            introspections.incrementAndGet();
            return List.of(field(Self.class, null));
        });

        assertSame(self, registry.resolve(Self.class.getName()));
        assertEquals(1, introspections.get());
    }

    @Test
    void mutuallyReferentialNestedTypesAreEachVisitedOnce() {
        AtomicInteger introspections = new AtomicInteger();
        ConfigClassInfo first = info(First.class);
        SupportedConfigClassRegistry registry = registry(List.of(first), current -> {
            introspections.incrementAndGet();
            return current.configClass() == First.class
                    ? List.of(field(Second.class, null))
                    : List.of(field(First.class, null));
        });

        assertEquals(Second.class, registry.resolve(Second.class.getName()).configClass());
        assertEquals(2, introspections.get());
    }

    @Test
    void concurrentFirstResolvePublishesOneStableRegistry() throws Exception {
        AtomicInteger rootReads = new AtomicInteger();
        AtomicInteger introspections = new AtomicInteger();
        ConfigClassInfo root = info(First.class);
        SupportedConfigClassRegistry registry = new SupportedConfigClassRegistry(
                () -> {
                    rootReads.incrementAndGet();
                    return List.of(root);
                },
                SupportedConfigClassRegistryTest::info,
                ignored -> {
                    introspections.incrementAndGet();
                    return List.of();
                });
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<ConfigClassInfo>> calls = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                calls.add(() -> registry.resolve(First.class.getName()));
            }
            for (Future<ConfigClassInfo> result : executor.invokeAll(calls)) {
                assertSame(root, result.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, rootReads.get());
        assertEquals(1, introspections.get());
        assertSame(root, registry.resolve(First.class.getName()));
        assertEquals(1, rootReads.get(), "later resolves must use the published cache");
    }

    @Test
    void duplicateSimpleNameIsRejectedButEachExactFqcnResolves() {
        ConfigClassInfo first = info(One.Duplicate.class);
        ConfigClassInfo second = info(Two.Duplicate.class);
        SupportedConfigClassRegistry registry = registry(List.of(first, second), ignored -> List.of());

        assertNull(registry.resolve("Duplicate"));
        assertSame(first, registry.resolve(One.Duplicate.class.getName()));
        assertSame(second, registry.resolve(Two.Duplicate.class.getName()));
    }

    private static SupportedConfigClassRegistry registry(
            final List<ConfigClassInfo> roots,
            final java.util.function.Function<ConfigClassInfo, List<FieldInfo>> fieldsFor) {
        return new SupportedConfigClassRegistry(
                () -> roots, SupportedConfigClassRegistryTest::info, fieldsFor);
    }

    private static ConfigClassInfo info(final Class<?> type) {
        return new ConfigClassInfo(
                type.getName(), type.getSimpleName(), "test", "test", type, null);
    }

    private static FieldInfo field(final Class<?> nestedClass, final Class<?> elementClass) {
        return new FieldInfo("value", Object.class, "Object", null, false,
                nestedClass, null, elementClass);
    }

    private static final class Self {
    }

    private static final class First {
    }

    private static final class Second {
    }

    private static final class One {
        private static final class Duplicate {
        }
    }

    private static final class Two {
        private static final class Duplicate {
        }
    }
}
