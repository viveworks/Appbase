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

import com.viveworks.appbase.configwizard.configurations.prompt.FieldInfo;
import com.viveworks.appbase.configwizard.configurations.prompt.FieldPrompter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds the bounded set of configuration types that the wizard may address by name.
 *
 * <p>The roots are the scanner allowlist. A nested type becomes eligible only when Jackson field
 * metadata reachable from one of those roots advertises it as a nested object or collection
 * element. Recursion uses a visited map, so self-referential configuration graphs are safe.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class SupportedConfigClassRegistry {

    private final Supplier<List<ConfigClassInfo>> roots;
    private final Function<Class<?>, ConfigClassInfo> metadataFor;
    private final Function<ConfigClassInfo, List<FieldInfo>> fieldsFor;
    private volatile Map<Class<?>, ConfigClassInfo> supported;

    /**
     * Creates a registry whose reachable graph is built lazily and cached for the lifetime of the
     * owning resource or service.
     *
     * @param scanner scanner that supplies trusted roots and metadata
     * @param prompter field introspector used to discover advertised nested types
     */
    public SupportedConfigClassRegistry(final ConfigScanner scanner, final FieldPrompter prompter) {
        this(scanner::scan, scanner::infoFor, prompter::introspect);
    }

    SupportedConfigClassRegistry(final Supplier<List<ConfigClassInfo>> roots,
                                 final Function<Class<?>, ConfigClassInfo> metadataFor,
                                 final Function<ConfigClassInfo, List<FieldInfo>> fieldsFor) {
        this.roots = roots;
        this.metadataFor = metadataFor;
        this.fieldsFor = fieldsFor;
    }

    /**
     * Resolves an exact FQCN or simple name within the scanner roots and their reachable nested
     * configuration types. Arbitrary loadable classes are never considered.
     *
     * @param name exact FQCN or simple name to resolve
     * @return matching supported configuration metadata, or {@code null} on a miss
     */
    public ConfigClassInfo resolve(final String name) {
        ConfigClassInfo exact = supported().values().stream()
                .filter(info -> info.fqcn().equals(name))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            return exact;
        }
        List<ConfigClassInfo> simpleMatches = supported().values().stream()
                .filter(info -> info.simpleName().equals(name))
                .limit(2)
                .toList();
        return simpleMatches.size() == 1 ? simpleMatches.get(0) : null;
    }

    private Map<Class<?>, ConfigClassInfo> supported() {
        Map<Class<?>, ConfigClassInfo> result = supported;
        if (result == null) {
            synchronized (this) {
                result = supported;
                if (result == null) {
                    Map<Class<?>, ConfigClassInfo> discovered = new LinkedHashMap<>();
                    for (ConfigClassInfo root : roots.get()) {
                        addReachable(root, metadataFor, fieldsFor, discovered);
                    }
                    result = Collections.unmodifiableMap(discovered);
                    supported = result;
                }
            }
        }
        return result;
    }

    private static void addReachable(final ConfigClassInfo info,
                                     final Function<Class<?>, ConfigClassInfo> metadataFor,
                                     final Function<ConfigClassInfo, List<FieldInfo>> fieldsFor,
                                     final Map<Class<?>, ConfigClassInfo> supported) {
        if (supported.putIfAbsent(info.configClass(), info) != null) {
            return;
        }
        for (FieldInfo field : fieldsFor.apply(info)) {
            addNested(field.nestedClass(), metadataFor, fieldsFor, supported);
            addNested(field.elementClass(), metadataFor, fieldsFor, supported);
        }
    }

    private static void addNested(final Class<?> nestedClass,
                                  final Function<Class<?>, ConfigClassInfo> metadataFor,
                                  final Function<ConfigClassInfo, List<FieldInfo>> fieldsFor,
                                  final Map<Class<?>, ConfigClassInfo> supported) {
        if (nestedClass != null && !supported.containsKey(nestedClass)) {
            addReachable(metadataFor.apply(nestedClass), metadataFor, fieldsFor, supported);
        }
    }
}
