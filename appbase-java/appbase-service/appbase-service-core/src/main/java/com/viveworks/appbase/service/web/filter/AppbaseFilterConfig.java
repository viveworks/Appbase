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

package com.viveworks.appbase.service.web.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.appbase.data.MapOfStringSetOfStringsValue;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AppbaseFilterConfig.Builder.class)
public class AppbaseFilterConfig implements AppbaseConfiguration {
    @JsonSerialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    private final Map<String, SetOfStringsValue> allowedPathsByFilterTypeNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = MapOfStringSetOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = MapOfStringSetOfStringsValue.class)
    private final Map<String, MapOfStringSetOfStringsValue> allowedMethodsByPathsByFilterTypeNameMap;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> allowedPaths;

    @JsonSerialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    private final Map<String, SetOfStringsValue> allowedMethodsByPathMap;

    @JsonSerialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    private final Map<String, SetOfStringsValue> blockedPathsByFilterTypeNameMap;

    @JsonSerialize(keyAs = String.class, contentAs = MapOfStringSetOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = MapOfStringSetOfStringsValue.class)
    private final Map<String, MapOfStringSetOfStringsValue> blockedMethodsByPathsByFilterTypeNameMap;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> blockedPaths;

    @JsonSerialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    @JsonDeserialize(keyAs = String.class, contentAs = SetOfStringsValue.class)
    private final Map<String, SetOfStringsValue> blockedMethodsByPathMap;

    private AppbaseFilterConfig(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        // Canonicalized here, in the one constructor both Jackson's @JsonDeserialize(builder = ...)
        // and programmatic builder() construction reach. A key/element that cannot canonicalize fails
        // the build rather than sitting inert in the map.
        //
        // allowedPathsByFilterTypeNameMap / blockedPathsByFilterTypeNameMap are keyed on a FILTER
        // CLASS NAME at the outer level, not a path -- see AppbaseServiceUtil.isFilterEnabled, which
        // looks the outer key up by filterType.getName() and only tests path membership inside the
        // value's SetOfStringsValue. Canonicalizing the outer key would prepend a slash to a class
        // name and silently disable every filter-type override, the same failure mode amendment A5
        // fixed for DefaultAuthorizationServiceConfiguration's class-name-keyed maps. Only the path
        // elements inside each value are canonicalized; the outer key is untouched.
        allowedPathsByFilterTypeNameMap = canonicalizePathSetValues(builder.allowedPathsByFilterTypeName);
        blockedPathsByFilterTypeNameMap = canonicalizePathSetValues(builder.blockedPathsByFilterTypeNameMap);

        // allowedMethodsByPathsByFilterTypeNameMap / blockedMethodsByPathsByFilterTypeNameMap carry the
        // same outer-is-a-filter-class-name shape, just with a nested path-to-methods map as the value
        // instead of a flat set of paths. Only the INNER keys (paths) are canonicalized.
        allowedMethodsByPathsByFilterTypeNameMap =
                canonicalizeNestedPathMaps(builder.allowedMethodsByPathsByFilterTypeNameMap);
        blockedMethodsByPathsByFilterTypeNameMap =
                canonicalizeNestedPathMaps(builder.blockedMethodsByPathsByFilterTypeNameMap);

        // allowedPaths / blockedPaths are Sets of paths outright; allowedMethodsByPathMap /
        // blockedMethodsByPathMap are keyed directly on path. Both canonicalize straightforwardly.
        allowedPaths = AppbasePathCanonicalizer.canonicalizeSet(builder.allowedPaths);
        blockedPaths = AppbasePathCanonicalizer.canonicalizeSet(builder.blockedPaths);
        allowedMethodsByPathMap = AppbasePathCanonicalizer.canonicalizeKeys(builder.allowedMethodsByPathMap);
        blockedMethodsByPathMap = AppbasePathCanonicalizer.canonicalizeKeys(builder.blockedMethodsByPathMap);
    }

    /**
     * Rebuilds a {@code filterTypeName -> SetOfStringsValue(paths)} map with every path element inside
     * each value canonicalized, leaving the outer filter-class-name key untouched.
     */
    private static Map<String, SetOfStringsValue> canonicalizePathSetValues(
            final Map<String, SetOfStringsValue> source)
    {
        if (source == null) {
            return Map.of();
        }
        final Map<String, SetOfStringsValue> canonical = new LinkedHashMap<>(source.size());
        for (final Map.Entry<String, SetOfStringsValue> entry : source.entrySet()) {
            final SetOfStringsValue value = entry.getValue();
            final Set<String> paths = value == null ? null : value.getValue();
            canonical.put(entry.getKey(), new SetOfStringsValue(AppbasePathCanonicalizer.canonicalizeSet(paths)));
        }
        return Map.copyOf(canonical);
    }

    /**
     * Rebuilds a {@code filterTypeName -> MapOfStringSetOfStringsValue(path -> methods)} map with the
     * inner map's path keys canonicalized, leaving the outer filter-class-name key untouched.
     */
    private static Map<String, MapOfStringSetOfStringsValue> canonicalizeNestedPathMaps(
            final Map<String, MapOfStringSetOfStringsValue> source)
    {
        if (source == null) {
            return Map.of();
        }
        final Map<String, MapOfStringSetOfStringsValue> canonical = new LinkedHashMap<>(source.size());
        for (final Map.Entry<String, MapOfStringSetOfStringsValue> entry : source.entrySet()) {
            final MapOfStringSetOfStringsValue value = entry.getValue();
            final Map<String, SetOfStringsValue> methodsByPath = value == null ? null : value.getValue();
            canonical.put(entry.getKey(),
                    new MapOfStringSetOfStringsValue(AppbasePathCanonicalizer.canonicalizeKeys(methodsByPath)));
        }
        return Map.copyOf(canonical);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, SetOfStringsValue> getAllowedPathsByFilterTypeNameMap() {
        return allowedPathsByFilterTypeNameMap;
    }

    public Map<String, MapOfStringSetOfStringsValue> getAllowedMethodsByPathsByFilterTypeNameMap() {
        return allowedMethodsByPathsByFilterTypeNameMap;
    }

    public Set<String> getAllowedPaths() {
        return allowedPaths;
    }

    public Map<String, SetOfStringsValue> getAllowedMethodsByPathMap() {
        return allowedMethodsByPathMap;
    }

    public Map<String, SetOfStringsValue> getBlockedPathsByFilterTypeNameMap() {
        return blockedPathsByFilterTypeNameMap;
    }

    public Map<String, MapOfStringSetOfStringsValue> getBlockedMethodsByPathsByFilterTypeNameMap() {
        return blockedMethodsByPathsByFilterTypeNameMap;
    }

    public Set<String> getBlockedPaths() {
        return blockedPaths;
    }

    public Map<String, SetOfStringsValue> getBlockedMethodsByPathMap() {
        return blockedMethodsByPathMap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AppbaseFilterConfig that = (AppbaseFilterConfig) o;
        return Objects.equals(allowedPathsByFilterTypeNameMap, that.allowedPathsByFilterTypeNameMap) &&
                Objects.equals(
                        allowedMethodsByPathsByFilterTypeNameMap,
                        that.allowedMethodsByPathsByFilterTypeNameMap) &&
                Objects.equals(allowedPaths, that.allowedPaths) &&
                Objects.equals(allowedMethodsByPathMap, that.allowedMethodsByPathMap) &&
                Objects.equals(blockedPathsByFilterTypeNameMap, that.blockedPathsByFilterTypeNameMap) &&
                Objects.equals(
                        blockedMethodsByPathsByFilterTypeNameMap,
                        that.blockedMethodsByPathsByFilterTypeNameMap) &&
                Objects.equals(blockedPaths, that.blockedPaths) &&
                Objects.equals(blockedMethodsByPathMap, that.blockedMethodsByPathMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                allowedPathsByFilterTypeNameMap,
                allowedMethodsByPathsByFilterTypeNameMap,
                allowedPaths,
                allowedMethodsByPathMap,
                blockedPathsByFilterTypeNameMap,
                blockedMethodsByPathsByFilterTypeNameMap,
                blockedPaths,
                blockedMethodsByPathMap);
    }

    @Override
    public String toString() {
        return "AppbaseFilterConfig{" +
                "allowedPathsByFilterTypeName=" + allowedPathsByFilterTypeNameMap +
                ", allowedMethodsByPathsByFilterTypeNameMap=" + allowedMethodsByPathsByFilterTypeNameMap +
                ", allowedPaths=" + allowedPaths +
                ", allowedMethodsByPathMap=" + allowedMethodsByPathMap +
                ", blockedPathsByFilterTypeNameMap=" + blockedPathsByFilterTypeNameMap +
                ", blockedMethodsByPathsByFilterTypeNameMap=" + blockedMethodsByPathsByFilterTypeNameMap +
                ", blockedPaths=" + blockedPaths +
                ", blockedMethodsByPathMap=" + blockedMethodsByPathMap +
                '}';
    }

    /**
     * Builder for {@link AppbaseFilterConfig}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private Map<String, SetOfStringsValue> allowedPathsByFilterTypeName;
        private Map<String, MapOfStringSetOfStringsValue> allowedMethodsByPathsByFilterTypeNameMap;
        private Set<String> allowedPaths;
        private Map<String, SetOfStringsValue> allowedMethodsByPathMap;
        private Map<String, SetOfStringsValue> blockedPathsByFilterTypeNameMap;
        private Map<String, MapOfStringSetOfStringsValue> blockedMethodsByPathsByFilterTypeNameMap;
        private Set<String> blockedPaths;
        private Map<String, SetOfStringsValue> blockedMethodsByPathMap;

        private Builder() {
        }

        public Builder allowedPathsByFilterTypeName(final Map<String, SetOfStringsValue> allowedPathsByFilterTypeName) {
            this.allowedPathsByFilterTypeName = allowedPathsByFilterTypeName;
            return this;
        }

        public Builder allowedMethodsByPathsByFilterTypeNameMap(
                final Map<String, MapOfStringSetOfStringsValue> allowedMethodsByPathsByFilterTypeNameMap)
        {
            this.allowedMethodsByPathsByFilterTypeNameMap = allowedMethodsByPathsByFilterTypeNameMap;
            return this;
        }

        public Builder allowedPaths(final Set<String> allowedPaths) {
            this.allowedPaths = allowedPaths;
            return this;
        }

        public Builder allowedMethodsByPathMap(final Map<String, SetOfStringsValue> allowedMethodsByPathMap) {
            this.allowedMethodsByPathMap = allowedMethodsByPathMap;
            return this;
        }

        public Builder blockedPathsByFilterTypeNameMap(
                final Map<String, SetOfStringsValue> blockedPathsByFilterTypeNameMap)
        {
            this.blockedPathsByFilterTypeNameMap = blockedPathsByFilterTypeNameMap;
            return this;
        }

        public Builder blockedMethodsByPathsByFilterTypeNameMap(
                final Map<String, MapOfStringSetOfStringsValue> blockedMethodsByPathsByFilterTypeNameMap)
        {
            this.blockedMethodsByPathsByFilterTypeNameMap = blockedMethodsByPathsByFilterTypeNameMap;
            return this;
        }

        public Builder blockedPaths(final Set<String> blockedPaths) {
            this.blockedPaths = blockedPaths;
            return this;
        }

        public Builder blockedMethodsByPathMap(final Map<String, SetOfStringsValue> blockedMethodsByPathMap) {
            this.blockedMethodsByPathMap = blockedMethodsByPathMap;
            return this;
        }

        public AppbaseFilterConfig build() {
            return new AppbaseFilterConfig(this);
        }
    }
    // CheckStyle: Builder end
}
