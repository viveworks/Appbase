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

package com.viveworks.appbase.service.user.account;

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
@JsonDeserialize(builder = UserAccountServiceExceptionConfiguration.Builder.class)
public class UserAccountServiceExceptionConfiguration implements AppbaseConfiguration {
    private final boolean respondInternalServerError;

    private final String redirectUrl;

    private final int httpStatusCode;

    private UserAccountServiceExceptionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        respondInternalServerError = builder.respondInternalServerError;
        redirectUrl = builder.redirectUrl;
        httpStatusCode = builder.httpStatusCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isRespondInternalServerError() {
        return respondInternalServerError;
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

        final UserAccountServiceExceptionConfiguration that = (UserAccountServiceExceptionConfiguration) o;
        return respondInternalServerError == that.respondInternalServerError &&
                Objects.equals(redirectUrl, that.redirectUrl) &&
                httpStatusCode == that.httpStatusCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(respondInternalServerError, redirectUrl, httpStatusCode);
    }

    @Override
    public String toString() {
        return "UserAccountServiceExceptionConfiguration{" +
                "respondInternalServerError=" + respondInternalServerError +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                '}';
    }

    /**
     * Builder for {@link UserAccountServiceExceptionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_RESPOND_INTERNAL_SERVER_ERROR = true;

        private boolean respondInternalServerError = DEFAULT_RESPOND_INTERNAL_SERVER_ERROR;

        private String redirectUrl;

        private int httpStatusCode;

        private Builder() {
        }

        public Builder respondInternalServerError(final boolean respondInternalServerError) {
            this.respondInternalServerError = respondInternalServerError;
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

        public UserAccountServiceExceptionConfiguration build() {
            return new UserAccountServiceExceptionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
