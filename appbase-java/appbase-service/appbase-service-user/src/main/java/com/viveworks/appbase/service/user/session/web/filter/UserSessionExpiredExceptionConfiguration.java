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

package com.viveworks.appbase.service.user.session.web.filter;

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
@JsonDeserialize(builder = UserSessionExpiredExceptionConfiguration.Builder.class)
public class UserSessionExpiredExceptionConfiguration implements AppbaseConfiguration {
    private final boolean renewSession;

    private final String redirectUrl;

    private final int httpStatusCode;

    private UserSessionExpiredExceptionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        renewSession = builder.renewSession;
        redirectUrl = builder.redirectUrl;
        httpStatusCode = builder.httpStatusCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isRenewSession() {
        return renewSession;
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

        final UserSessionExpiredExceptionConfiguration that = (UserSessionExpiredExceptionConfiguration) o;
        return renewSession == that.renewSession &&
                Objects.equals(redirectUrl, that.redirectUrl) &&
                httpStatusCode == that.httpStatusCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(renewSession, redirectUrl, httpStatusCode);
    }

    @Override
    public String toString() {
        return "UserSessionExpiredExceptionConfiguration{" +
                "renewSession=" + renewSession +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", httpStatusCode=" + httpStatusCode +
                '}';
    }

    /**
     * Builder for {@link UserSessionExpiredExceptionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_RENEW_SESSION = true;

        private boolean renewSession = DEFAULT_RENEW_SESSION;

        private String redirectUrl;

        private int httpStatusCode;

        private Builder() {
        }

        public Builder renewSession(final boolean renewSession) {
            this.renewSession = renewSession;
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

        public UserSessionExpiredExceptionConfiguration build() {
            return new UserSessionExpiredExceptionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
