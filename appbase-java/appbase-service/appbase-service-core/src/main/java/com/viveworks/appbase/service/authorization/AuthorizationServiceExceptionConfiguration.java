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

package com.viveworks.appbase.service.authorization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Immutable response policy for authorization failures, built with forbidden responses enabled by default.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AuthorizationServiceExceptionConfiguration.Builder.class)
public class AuthorizationServiceExceptionConfiguration implements AppbaseConfiguration {
    private final boolean respondForbidden;

    private final String redirectUrl;

    private final int httpStatusCode;

    private AuthorizationServiceExceptionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        respondForbidden = builder.respondForbidden;
        redirectUrl = builder.redirectUrl;
        httpStatusCode = builder.httpStatusCode;
    }

    /**
     * Creates a builder initialized with the response defaults.
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reports whether an authorization failure is rendered as an HTTP forbidden response.
     * @return whether forbidden responses are enabled
     */
    public boolean isRespondForbidden() {
        return respondForbidden;
    }

    /**
     * Returns the configured failure redirect target.
     * @return redirect target, or null
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Returns the configured HTTP status code.
     * @return status code, or zero when none was supplied
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /** Compares AuthorizationServiceExceptionConfiguration values using all state that affects authorization behavior. */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AuthorizationServiceExceptionConfiguration that = (AuthorizationServiceExceptionConfiguration) o;
        return respondForbidden == that.respondForbidden &&
                Objects.equals(redirectUrl, that.redirectUrl) &&
                httpStatusCode == that.httpStatusCode;
    }

    /** Computes a hash consistent with AuthorizationServiceExceptionConfiguration equality. */
    @Override
    public int hashCode() {
        return Objects.hash(respondForbidden, redirectUrl, httpStatusCode);
    }

    /** Formats the AuthorizationServiceExceptionConfiguration state for diagnostics. */
    @Override
    public String toString() {
        return "AuthorizationServiceExceptionConfiguration{" +
                "respondForbidden=" + respondForbidden +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                '}';
    }

    /**
     * Builds {@link AuthorizationServiceExceptionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_RESPOND_FORBIDDEN = true;

        private boolean respondForbidden = DEFAULT_RESPOND_FORBIDDEN;

        private String redirectUrl;

        private int httpStatusCode;

        private Builder() {
        }

        /**
         * Sets whether authorization failures produce forbidden responses.
         * @param respondForbidden whether to respond with forbidden
         * @return this builder
         */
        public Builder respondForbidden(final boolean respondForbidden) {
            this.respondForbidden = respondForbidden;
            return this;
        }

        /**
         * Sets the optional failure redirect target.
         * @param redirectUrl redirect target, or null
         * @return this builder
         */
        public Builder redirectUrl(final String redirectUrl) {
            this.redirectUrl = redirectUrl;
            return this;
        }

        /**
         * Sets the response status code.
         * @param httpStatusCode response status code
         * @return this builder
         */
        public Builder httpStatusCode(final int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        /**
         * Creates an immutable configuration from the current fields.
         * @return a new AuthorizationServiceExceptionConfiguration
         */
        public AuthorizationServiceExceptionConfiguration build() {
            return new AuthorizationServiceExceptionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
