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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.jackson.YamlUtil;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers Appbase configuration classes on the runtime classpath using ClassGraph.
 *
 * <p>Scans for concrete (non-abstract, non-interface) classes that either implement
 * {@link AppbaseConfiguration} or subclass
 * {@link AbstractAppbaseManagedServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class ConfigScanner {

    private static final String APPBASE_PACKAGE_PREFIX = "com.viveworks.appbase.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nullable
    private static volatile List<ConfigClassInfo> cache;

    /**
     * Scans the full classpath for concrete classes implementing {@link AppbaseConfiguration}
     * or subclassing {@link AbstractAppbaseManagedServiceConfiguration}.
     * Abstract classes and interfaces are excluded.
     * Results are cached after the first scan — the classpath is static at runtime, so the
     * cache is shared across every {@link ConfigScanner} instance rather than per-instance.
     *
     * @return all matches, sorted by module then simpleName
     */
    public List<ConfigClassInfo> scan() {
        List<ConfigClassInfo> result = cache;
        if (result != null) {
            return result;
        }
        synchronized (ConfigScanner.class) {
            result = cache;
            if (result == null) {
                result = scanClasspath();
                cache = result;
            }
        }
        return result;
    }

    private List<ConfigClassInfo> scanClasspath() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .scan()) {

            // Merge both queries into a map keyed by class name to avoid duplicates
            Map<String, ClassInfo> candidates = new LinkedHashMap<>();
            scanResult.getClassesImplementing(AppbaseConfiguration.class.getName())
                    .forEach(ci -> candidates.put(ci.getName(), ci));
            scanResult.getSubclasses(AbstractAppbaseManagedServiceConfiguration.class.getName())
                    .forEach(ci -> candidates.put(ci.getName(), ci));

            return candidates.values().stream()
                    .filter(ci -> !ci.isAbstract() && !ci.isInterface())
                    .map(ci -> toConfigClassInfo(ci.loadClass()))
                    .filter(info -> info.defaultInstance() != null)
                    .filter(info -> !"stacktest".equals(info.layer()))
                    .filter(this::isJacksonConvertible)
                    .sorted(Comparator.comparing(ConfigClassInfo::module)
                            .thenComparing(ConfigClassInfo::simpleName))
                    .toList();
        }
    }

    /**
     * Builds a {@link ConfigClassInfo} for any class — not just scanned
     * {@link AppbaseConfiguration} implementors.
     *
     * <p>Used to resolve nested configuration types that are embedded inside a top-level
     * config (e.g. {@code TextValueConfiguration} inside
     * {@code DefaultJdbcClientIntegrationServiceConfiguration}) but do not themselves
     * implement {@code AppbaseConfiguration}, so they are absent from {@link #scan()}.
     *
     * @param clazz the class to describe; must be loadable from the runtime classpath
     * @return populated metadata record
     */
    public ConfigClassInfo infoFor(final Class<?> clazz) {
        return toConfigClassInfo(clazz);
    }

    /**
     * Returns a filtered subset of {@link #scan()} results matching the given query.
     *
     * <p>The {@code query} is matched case-insensitively as a substring against both
     * {@link ConfigClassInfo#simpleName()} and {@link ConfigClassInfo#fqcn()}.
     * A {@code null} query returns all results.
     *
     * @param query case-insensitive substring matched against simpleName and fqcn; null = no filter
     * @return filtered and sorted list of matching configuration class metadata
     */
    public List<ConfigClassInfo> search(@Nullable String query) {
        return scan().stream()
                .filter(r -> query == null
                        || r.simpleName().toLowerCase().contains(query.toLowerCase())
                        || r.fqcn().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    /**
     * Converts a loaded class into a {@link ConfigClassInfo} by deriving layer and module
     * from the package name and attempting to construct a default instance.
     *
     * @param clazz the configuration class to describe
     * @return populated metadata record
     */
    private ConfigClassInfo toConfigClassInfo(final Class<?> clazz) {
        String fqcn = clazz.getName();
        String simpleClassName = clazz.getSimpleName();
        String[] layerAndModule = deriveLayerAndModule(fqcn);
        Object defaultInstance = tryCreateDefaultInstance(clazz);

        return new ConfigClassInfo(
                fqcn,
                simpleClassName,
                layerAndModule[0],
                layerAndModule[1],
                clazz,
                defaultInstance);
    }

    /**
     * Derives the layer and module from the fully qualified class name.
     *
     * <p>For a class in {@code com.viveworks.appbase.client.service.redis.configuration.*}:
     * <ul>
     *   <li>layer  = {@code "client"}  (segment 0 after the common prefix)</li>
     *   <li>module = {@code "redis"}   (segment 2 after the common prefix)</li>
     * </ul>
     *
     * @param fqcn fully qualified class name
     * @return two-element array: {@code [layer, module]}; empty strings when the class does
     *         not follow the expected naming convention
     */
    private String[] deriveLayerAndModule(final String fqcn) {
        if (fqcn.startsWith(APPBASE_PACKAGE_PREFIX)) {
            String remainder = fqcn.substring(APPBASE_PACKAGE_PREFIX.length());
            String[] segments = remainder.split("\\.");
            String layer = segments.length > 0 ? segments[0] : "";
            String module = segments.length > 2 ? segments[2] : "";
            return new String[]{layer, module};
        }
        return new String[]{"", ""};
    }

    /**
     * Attempts to create a default instance of the given class.
     *
     * <p>Strategy (first success wins):
     * <ol>
     *   <li>Invoke a public static no-arg {@code builder()} method, then call {@code build()}
     *       on the returned builder.</li>
     *   <li>Use Jackson's {@link ObjectMapper#convertValue(Object, Class)} with an empty map
     *       to apply field defaults.</li>
     *   <li>Return {@code null} if both strategies fail.</li>
     * </ol>
     *
     * @param clazz the class to instantiate
     * @return a default instance, or {@code null} if construction is not possible
     */
    @Nullable
    private Object tryCreateDefaultInstance(final Class<?> clazz) {
        // Strategy 1: builder pattern
        try {
            Object builder = clazz.getDeclaredMethod("builder").invoke(null);
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (Throwable ignored) {
            // fall through to next strategy
        }
        // Strategy 2: Jackson convertValue with empty map
        try {
            return OBJECT_MAPPER.convertValue(new HashMap<>(), clazz);
        } catch (Throwable ignored) {
            // fall through
        }
        return null;
    }

    /**
     * Returns true if the class round-trips cleanly through Jackson:
     * deserialization from an empty map <em>and</em> YAML serialization of
     * the real default instance.
     *
     * <p>Two failure modes are guarded:
     * <ol>
     *   <li>Classes with a Lombok builder but no Jackson creator fail
     *       {@code convertValue(empty-map, clazz)} (no delegate- or property-based Creator).</li>
     *   <li>Classes whose fields contain wildcard generics or types lacking a serializer
     *       pass deserialization but fail {@code writeValueAsString} with
     *       {@link com.fasterxml.jackson.databind.exc.InvalidDefinitionException}.
     *       The serialization test uses the real {@link ConfigClassInfo#defaultInstance()}
     *       (builder-created, with real field values) rather than an all-null Jackson instance,
     *       so it catches more cases.</li>
     * </ol>
     *
     * @param info the config class metadata including the precomputed default instance
     * @return {@code true} if the class round-trips through Jackson without error
     */
    private boolean isJacksonConvertible(final ConfigClassInfo info) {
        try {
            OBJECT_MAPPER.convertValue(new HashMap<>(), info.configClass());
            YamlUtil.getYamlMapper().writeValueAsString(info.defaultInstance());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
