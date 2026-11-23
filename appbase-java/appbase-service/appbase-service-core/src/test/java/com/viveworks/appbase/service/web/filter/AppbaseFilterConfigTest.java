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

package com.viveworks.appbase.service.web.filter;

import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringSetOfStringsValue;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.path.AppbasePathException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppbaseFilterConfigTest {
    private static final String FILTER_TYPE_NAME = "com.viveworks.appbase.service.web.cors.CorsServletFilter";

    @Test
    void aPathElementInTheAllowedPathsSetIsCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedPaths(Set.of("/%61dmin"))
                .build();

        assertTrue(config.getAllowedPaths().contains("/admin"),
                "an element authored as /%61dmin must match a request for /admin");
    }

    @Test
    void aPathElementInTheBlockedPathsSetIsCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .blockedPaths(Set.of("/%61dmin"))
                .build();

        assertTrue(config.getBlockedPaths().contains("/admin"));
    }

    @Test
    void theWildcardSentinelIsNeverCanonicalizedInAllowedOrBlockedPaths() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedPaths(Set.of(AppbaseServiceConstants.WILDCARD))
                .blockedPaths(Set.of(AppbaseServiceConstants.WILDCARD))
                .build();

        assertTrue(config.getAllowedPaths().contains(AppbaseServiceConstants.WILDCARD));
        assertTrue(config.getBlockedPaths().contains(AppbaseServiceConstants.WILDCARD));
    }

    @Test
    void allowedMethodsByPathMapKeyIsCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedMethodsByPathMap(Map.of("/%61dmin", new SetOfStringsValue(Set.of("GET"))))
                .build();

        assertTrue(config.getAllowedMethodsByPathMap().containsKey("/admin"));
    }

    @Test
    void blockedMethodsByPathMapKeyIsCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .blockedMethodsByPathMap(Map.of("/%61dmin", new SetOfStringsValue(Set.of("GET"))))
                .build();

        assertTrue(config.getBlockedMethodsByPathMap().containsKey("/admin"));
    }

    @Test
    void aKeyThatCanNeverMatchFailsAtLoadRatherThanSilently() {
        assertThrows(AppbasePathException.class, () -> AppbaseFilterConfig.builder()
                .allowedMethodsByPathMap(Map.of("/a%2Fb", new SetOfStringsValue(Set.of("GET"))))
                .build());
    }

    /**
     * {@code allowedMethodsByPathsByFilterTypeNameMap} / {@code blockedMethodsByPathsByFilterTypeNameMap}
     * are keyed on a FILTER CLASS NAME at the outer level (see {@code AppbaseServiceUtil.isFilterEnabled}'s
     * {@code containsKey(filterTypeName)}) and on PATH at the inner level. Canonicalizing the outer key
     * would prepend a slash to a class name and silently disable every filter-type override -- the same
     * failure mode Task 3's amendment A5 fixed for {@code DefaultAuthorizationServiceConfiguration}.
     */
    @Test
    void onlyTheInnerKeysOfTheNestedAllowedMapAreCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedMethodsByPathsByFilterTypeNameMap(Map.of(
                        FILTER_TYPE_NAME,
                        new MapOfStringSetOfStringsValue(Map.of("/%61dmin", new SetOfStringsValue(Set.of("GET"))))))
                .build();

        final MapOfStringSetOfStringsValue byPath = config.getAllowedMethodsByPathsByFilterTypeNameMap()
                .get(FILTER_TYPE_NAME);

        assertNotNull(byPath, "the outer key is a filter class name and must not be canonicalized");
        assertTrue(byPath.getValue().containsKey("/admin"), "the inner key is a path and must be canonicalized");
    }

    @Test
    void onlyTheInnerKeysOfTheNestedBlockedMapAreCanonicalized() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .blockedMethodsByPathsByFilterTypeNameMap(Map.of(
                        FILTER_TYPE_NAME,
                        new MapOfStringSetOfStringsValue(Map.of("/%61dmin", new SetOfStringsValue(Set.of("GET"))))))
                .build();

        final MapOfStringSetOfStringsValue byPath = config.getBlockedMethodsByPathsByFilterTypeNameMap()
                .get(FILTER_TYPE_NAME);

        assertNotNull(byPath, "the outer key is a filter class name and must not be canonicalized");
        assertTrue(byPath.getValue().containsKey("/admin"), "the inner key is a path and must be canonicalized");
    }

    /**
     * {@code allowedPathsByFilterTypeNameMap} / {@code blockedPathsByFilterTypeNameMap} carry the same
     * outer-is-a-class-name shape as the nested maps above, just with a flat {@link SetOfStringsValue}
     * of paths instead of a nested map -- confirmed by {@code AppbaseServiceUtil.isFilterEnabled}, which
     * looks the outer key up by {@code filterType.getName()} and then tests path membership inside the
     * value's set. The outer key must not be canonicalized; the path elements inside the value must be.
     */
    @Test
    void theOuterKeyOfAllowedPathsByFilterTypeNameMapIsAFilterClassNameNotAPath() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedPathsByFilterTypeName(Map.of(FILTER_TYPE_NAME, new SetOfStringsValue(Set.of("/%61dmin"))))
                .build();

        final SetOfStringsValue paths = config.getAllowedPathsByFilterTypeNameMap().get(FILTER_TYPE_NAME);

        assertNotNull(paths, "the outer key is a filter class name and must not be canonicalized");
        assertTrue(paths.getValue().contains("/admin"), "the path element inside the value must be canonicalized");
    }

    @Test
    void theOuterKeyOfBlockedPathsByFilterTypeNameMapIsAFilterClassNameNotAPath() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .blockedPathsByFilterTypeNameMap(Map.of(FILTER_TYPE_NAME, new SetOfStringsValue(Set.of("/%61dmin"))))
                .build();

        final SetOfStringsValue paths = config.getBlockedPathsByFilterTypeNameMap().get(FILTER_TYPE_NAME);

        assertNotNull(paths, "the outer key is a filter class name and must not be canonicalized");
        assertTrue(paths.getValue().contains("/admin"), "the path element inside the value must be canonicalized");
    }

    /**
     * Regression pin for {@code AppbaseServiceUtilTest#testIsFilterEnabled}, which relies on
     * {@code /path1} -- already canonical -- surviving every one of these maps unchanged.
     */
    @Test
    void anAlreadyCanonicalPathIsUnaffected() {
        final AppbaseFilterConfig config = AppbaseFilterConfig.builder()
                .allowedPaths(Set.of("/path1"))
                .blockedPaths(Set.of("/path1"))
                .allowedMethodsByPathMap(Map.of("/path1", new SetOfStringsValue(Set.of("GET"))))
                .blockedMethodsByPathMap(Map.of("/path1", new SetOfStringsValue(Set.of("GET"))))
                .build();

        assertTrue(config.getAllowedPaths().contains("/path1"));
        assertTrue(config.getBlockedPaths().contains("/path1"));
        assertTrue(config.getAllowedMethodsByPathMap().containsKey("/path1"));
        assertTrue(config.getBlockedMethodsByPathMap().containsKey("/path1"));
        assertFalse(config.getAllowedPaths().contains("/%70ath1"));
    }
}
