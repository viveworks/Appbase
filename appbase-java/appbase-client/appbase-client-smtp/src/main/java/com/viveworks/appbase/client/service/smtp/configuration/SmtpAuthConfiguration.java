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

package com.viveworks.appbase.client.service.smtp.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents {@code SMTP} auth configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SmtpAuthConfiguration.Builder.class)
public final class SmtpAuthConfiguration {
    private final SmtpAuthMechanism smtpAuthMechanism;
    private final String username;
    private final String passwordOrAccessToken;

    private SmtpAuthConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.smtpAuthMechanism, "builder.smtpAuthMechanism");
        AppbaseUtil.notNull(builder.username, "builder.username");
        AppbaseUtil.notNull(builder.passwordOrAccessToken, "builder.passwordOrAccessToken");

        smtpAuthMechanism = builder.smtpAuthMechanism;
        username = builder.username;
        passwordOrAccessToken = builder.passwordOrAccessToken;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public SmtpAuthMechanism getSmtpAuthMechanism() {
        return smtpAuthMechanism;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordOrAccessToken() {
        return passwordOrAccessToken;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SmtpAuthConfiguration that = (SmtpAuthConfiguration) o;
        return smtpAuthMechanism == that.smtpAuthMechanism &&
                Objects.equals(username, that.username) &&
                Objects.equals(passwordOrAccessToken, that.passwordOrAccessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(smtpAuthMechanism, username, passwordOrAccessToken);
    }

    @Override
    public String toString() {
        return "SmtpAuthConfiguration{" +
                "smtpAuthMechanism=" + smtpAuthMechanism +
                ", username='" + username + '\'' +
                ", passwordOrAccessToken='" + AppbaseUtil.OBFUSCATE + '\'' +
                '}';
    }


    /**
     * Builder for {@link SmtpAuthConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final SmtpAuthMechanism DEFAULT_SMTP_AUTH_MECHANISM = SmtpAuthMechanism.PLAIN;
        private static final String DEFAULT_USERNAME = "appbase-test-user";
        private static final String DEFAULT_PASSWORD_OR_ACCESS_TOKEN = "password";

        private SmtpAuthMechanism smtpAuthMechanism = DEFAULT_SMTP_AUTH_MECHANISM;
        private String username = DEFAULT_USERNAME;
        private String passwordOrAccessToken = DEFAULT_PASSWORD_OR_ACCESS_TOKEN;

        private Builder() {
        }

        public Builder smtpAuthMechanism(final SmtpAuthMechanism smtpAuthMechanism) {
            this.smtpAuthMechanism = smtpAuthMechanism;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder passwordOrAccessToken(final String passwordOrAccessToken) {
            this.passwordOrAccessToken = passwordOrAccessToken;
            return this;
        }

        public SmtpAuthConfiguration build() {
            return new SmtpAuthConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
