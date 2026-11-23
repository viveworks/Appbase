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

package com.viveworks.appbase.service.authorization;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable authorization inputs carrying optional requester, session, and operation-specific parameters.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class AuthorizationContext {
    @Nullable
    private String appbaseRequesterId;

    @Nullable
    private String appbaseSessionId;

    @Nullable
    private Map<String, Object> parameters;

    /** Creates an empty mutable authorization context. */
    public AuthorizationContext() {
    }

    /**
     * Creates a context with the supplied nullable request attributes.
     * @param appbaseRequesterId requester identifier, or null
     * @param appbaseSessionId session identifier, or null
     * @param parameters operation parameters, or null
     */
    public AuthorizationContext(
            @Nullable final String appbaseRequesterId,
            @Nullable final String appbaseSessionId,
            @Nullable final Map<String, Object> parameters)
    {
        this.appbaseRequesterId = appbaseRequesterId;
        this.appbaseSessionId = appbaseSessionId;
        this.parameters = parameters;
    }

    /**
     * Returns the identifier of the principal requesting authorization.
     * @return requester identifier, or null
     */
    @Nullable
    public String getAppbaseRequesterId() {
        return appbaseRequesterId;
    }

    /**
     * Replaces the requester identifier held by this mutable context.
     * @param appbaseRequesterId requester identifier, or null
     */
    public void setAppbaseRequesterId(@Nullable final String appbaseRequesterId) {
        this.appbaseRequesterId = appbaseRequesterId;
    }

    /**
     * Returns the application session identifier associated with the request.
     * @return session identifier, or null
     */
    @Nullable
    public String getAppbaseSessionId() {
        return appbaseSessionId;
    }

    /**
     * Replaces the session identifier held by this mutable context.
     * @param appbaseSessionId session identifier, or null
     */
    public void setAppbaseSessionId(@Nullable final String appbaseSessionId) {
        this.appbaseSessionId = appbaseSessionId;
    }

    /**
     * Returns the live parameter map; no defensive copy is made.
     * @return parameter map, or null
     */
    @Nullable
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Replaces the live parameter map without copying its entries.
     * @param parameters parameter map, or null
     */
    public void setParameters(@Nullable final Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /** Compares AuthorizationContext values using all state that affects authorization behavior. */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AuthorizationContext that = (AuthorizationContext) o;
        return Objects.equals(appbaseRequesterId, that.appbaseRequesterId) &&
                Objects.equals(appbaseSessionId, that.appbaseSessionId) &&
                Objects.equals(parameters, that.parameters);
    }

    /** Computes a hash consistent with AuthorizationContext equality. */
    @Override
    public int hashCode() {
        return Objects.hash(appbaseRequesterId, appbaseSessionId, parameters);
    }

    /** Formats the AuthorizationContext state for diagnostics. */
    @Override
    public String toString() {
        return "AuthorizationContext{" +
                "appbaseRequesterId='" + appbaseRequesterId + '\'' +
                ", appbaseSessionId='" + appbaseSessionId + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
