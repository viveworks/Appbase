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
package com.viveworks.appbase.service.user.account.web.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = IdpUserTokenCheckFilterConfiguration.Builder.class)
public class IdpUserTokenCheckFilterConfiguration implements AppbaseConfiguration {
    private final boolean respondForbidden;

    private final String redirectUrl;

    private final int httpStatusCode;

    private IdpUserTokenCheckFilterConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        respondForbidden = builder.respondForbidden;
        redirectUrl = builder.redirectUrl;
        httpStatusCode = builder.httpStatusCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isRespondForbidden() {
        return respondForbidden;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IdpUserTokenCheckFilterConfiguration that = (IdpUserTokenCheckFilterConfiguration) o;
        return respondForbidden == that.respondForbidden &&
                Objects.equals(redirectUrl, that.redirectUrl) &&
                httpStatusCode == that.httpStatusCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(respondForbidden, redirectUrl, httpStatusCode);
    }

    @Override
    public String toString() {
        return "IdpUserTokenCheckFilterConfiguration{" +
                "respondForbidden=" + respondForbidden +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                '}';
    }

    /**
     * Builder for {@link IdpUserTokenCheckFilterConfiguration}.
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

        public Builder respondForbidden(final boolean respondForbidden) {
            this.respondForbidden = respondForbidden;
            return this;
        }

        public Builder redirectUrl(final String redirectUrl) {
            this.redirectUrl = redirectUrl;
            return this;
        }

        public Builder httpStatusCode(final int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public IdpUserTokenCheckFilterConfiguration build() {
            return new IdpUserTokenCheckFilterConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
