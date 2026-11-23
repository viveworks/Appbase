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

package com.viveworks.appbase.service.web.oauth.microsoft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The admin-binding scope is bounded to profile lookup and group-membership read for this release (spec
 * §5): no token revocation and no management-API surface (the full Microsoft Graph management surface is
 * explicitly deferred). This guard scans every type the module actually ships and fails unless every
 * reachable method is a read-shaped one — a record accessor, an {@code Object} override, or one of the
 * module's named read/factory entry points.
 *
 * <p>It is an <em>allowlist</em>, not a banned-verb denylist, and deliberately so: a denylist of English
 * synonyms for "change state" ({@code revoke}, {@code terminate}, {@code signOutAllSessions},
 * {@code makeAdmin}, a homoglyph of {@code revoke}, ...) can never be complete, so anything not provably
 * a read is rejected. The scan recurses into subpackages and includes non-{@code private} methods, so a
 * management surface cannot hide in a subpackage or behind package-private access.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class EntraScopeGuardTest {

    /** Non-accessor method names a read-only binding module is permitted to expose. */
    private static final Set<String> ALLOWED_METHOD_NAMES = Set.of(
            "equals", "hashCode", "toString", // Object overrides / record-generated
            "parse",                          // the binding entry points
            "configuration");                 // the defaults factory

    @Test
    void exposesOnlyReadShapedSurface() throws IOException, URISyntaxException {
        List<Class<?>> shipped = shippedClasses();
        assertTrue(shipped.size() >= 4,
                "expected to scan the module's shipped classes, found " + shipped);

        for (Class<?> type : shipped) {
            Set<String> accessorNames = recordComponentNames(type);
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic() || Modifier.isPrivate(method.getModifiers())) {
                    // private members are unreachable from another class; synthetic ones are
                    // compiler-generated (lambdas, bridges) and carry no author-chosen name.
                    continue;
                }
                String name = method.getName();
                boolean readShaped = ALLOWED_METHOD_NAMES.contains(name) || accessorNames.contains(name);
                assertTrue(readShaped, () -> type.getName() + "#" + name
                        + " is not a read-shaped accessor/factory; this release is bounded to profile "
                        + "lookup + group-membership read with no management/revocation surface (spec §5). "
                        + "A genuine new read binding must be added to ALLOWED_METHOD_NAMES explicitly.");
            }
        }
    }

    private static Set<String> recordComponentNames(Class<?> type) {
        if (!type.isRecord()) {
            return Set.of();
        }
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }

    /** Every class the module compiled under its own package, recursively including subpackages. */
    private static List<Class<?>> shippedClasses() throws IOException, URISyntaxException {
        Path classesRoot = Path.of(EntraOAuthDefaults.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path packageRoot = classesRoot.resolve("com/viveworks/appbase/service/web/oauth/microsoft");
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> entries = Files.walk(packageRoot)) {
            for (Path entry : (Iterable<Path>) entries.sorted()::iterator) {
                if (!entry.getFileName().toString().endsWith(".class")) {
                    continue;
                }
                String relative = classesRoot.relativize(entry).toString();
                String className = relative
                        .substring(0, relative.length() - ".class".length())
                        .replace(entry.getFileSystem().getSeparator(), ".");
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("could not load shipped class " + className, e);
                }
            }
        }
        return classes;
    }
}
