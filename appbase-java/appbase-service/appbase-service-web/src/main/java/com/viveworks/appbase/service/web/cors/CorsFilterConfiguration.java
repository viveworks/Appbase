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

package com.viveworks.appbase.service.web.cors;

import com.google.common.annotations.VisibleForTesting;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;

import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CorsFilterConfiguration implements AppbaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilterConfiguration.class);

    /** The shipped permissive default (Access-Control-Allow-Origin: * on all paths), canonicalized. */
    @VisibleForTesting
    static final Map<String, MapOfStringStringValue> CANONICAL_DEFAULT =
            AppbasePathCanonicalizer.canonicalizeKeys(Builder.DEFAULT_PATH_BY_CORS_HEADERS_MAP);

    private final Map<String, MapOfStringStringValue> pathByCorsHeadersMap;

    private CorsFilterConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.pathByCorsHeadersMap, "builder.pathByCorsHeadersMap");

        // Canonicalized here, in the one constructor both Jackson's @JsonDeserialize(builder = ...)
        // and programmatic builder() construction reach, so a key authored as /%61dmin matches a
        // request canonicalized to /admin. A key that cannot canonicalize fails the build rather than
        // sitting inert in the map -- see AppbasePathCanonicalizer.canonicalizeKeys.
        pathByCorsHeadersMap = AppbasePathCanonicalizer.canonicalizeKeys(builder.pathByCorsHeadersMap);

        // AUD-002: the shipped default allows ANY origin (Access-Control-Allow-Origin: *) on every
        // path -- a permissive-by-default posture. We KEEP the default (flipping it would break
        // deployments that rely on it), but refuse to let it be SILENT: warn once, at config load, so
        // an operator who never configured CORS is told rather than left guessing.
        if (usesPermissiveWildcardDefault()) {
            LOG.warn("CORS is on the PERMISSIVE DEFAULT: 'Access-Control-Allow-Origin: *' on all paths, "
                    + "with no explicit CORS configuration. For any non-public deployment set an origin "
                    + "allowlist via CorsFilterConfiguration.pathByCorsHeadersMap (AUD-002).");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, MapOfStringStringValue> getPathByCorsHeadersMap() {
        return pathByCorsHeadersMap;
    }

    /**
     * @return {@code true} when the effective config is the shipped permissive wildcard default — i.e.
     *         no explicit CORS configuration was supplied. Drives the AUD-002 startup hardening warning.
     */
    public boolean usesPermissiveWildcardDefault() {
        return pathByCorsHeadersMap.equals(CANONICAL_DEFAULT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CorsFilterConfiguration that = (CorsFilterConfiguration) o;
        return Objects.equals(pathByCorsHeadersMap, that.pathByCorsHeadersMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathByCorsHeadersMap);
    }

    @Override
    public String toString() {
        return "CorsFilterConfiguration{" +
                "pathByCorsHeadersMap=" + pathByCorsHeadersMap +
                '}';
    }

    public static final class Builder {
        @VisibleForTesting
        public static final String DEFAULT_ALLOW_ORIGIN = "*";

        @VisibleForTesting
        public static final String DEFAULT_ALLOW_METHODS = "HEAD, GET, POST, DELETE, PUT, OPTIONS";

        @VisibleForTesting
        public static final String DEFAULT_ALLOW_HEADERS = "*";

        @VisibleForTesting
        public static final Map<String, MapOfStringStringValue> DEFAULT_PATH_BY_CORS_HEADERS_MAP =
                Map.of(AppbaseServiceConstants.WILDCARD,
                        new MapOfStringStringValue(Map.of(
                                "Access-Control-Allow-Origin", DEFAULT_ALLOW_ORIGIN,
                                "Access-Control-Allow-Methods", DEFAULT_ALLOW_METHODS,
                                "Access-Control-Allow-Headers", DEFAULT_ALLOW_HEADERS
                )));

        private Map<String, MapOfStringStringValue> pathByCorsHeadersMap = DEFAULT_PATH_BY_CORS_HEADERS_MAP;

        private Builder() {
        }

        public Builder pathByCorsHeadersMap(final Map<String, MapOfStringStringValue> pathByCorsHeadersMap) {
            this.pathByCorsHeadersMap = pathByCorsHeadersMap;
            return this;
        }

        public CorsFilterConfiguration build() {
            return new CorsFilterConfiguration(this);
        }
    }
}
