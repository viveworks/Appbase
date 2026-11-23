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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auth.oauth2.AccessToken;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Date;
import java.util.Objects;

/**
 * Configuration for {@link AccessToken}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = FirebaseAccessTokenConfiguration.Builder.class)
public final class FirebaseAccessTokenConfiguration {
    private final String tokenValue;
    private final Date expirationTimeMillis;

    private FirebaseAccessTokenConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.tokenValue, "builder.tokenValue");
        AppbaseUtil.notNull(builder.expirationTimeMillis, "builder.expirationTimeMillis");

        tokenValue = builder.tokenValue;
        expirationTimeMillis = builder.expirationTimeMillis;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public Date getExpirationTimeMillis() {
        return expirationTimeMillis;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FirebaseAccessTokenConfiguration that = (FirebaseAccessTokenConfiguration) o;
        return Objects.equals(tokenValue, that.tokenValue) &&
                Objects.equals(expirationTimeMillis, that.expirationTimeMillis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenValue, expirationTimeMillis);
    }

    @Override
    public String toString() {
        return "AccessTokenConfiguration{" +
                "tokenValue='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", expirationTimeMillis=" + expirationTimeMillis +
                '}';
    }


    /**
     * Builder for {@link FirebaseAccessTokenConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String tokenValue;
        private Date expirationTimeMillis;

        private Builder() {
        }

        public Builder tokenValue(final String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }

        public Builder expirationTimeMillis(final Date expirationTimeMillis) {
            this.expirationTimeMillis = expirationTimeMillis;
            return this;
        }

        public FirebaseAccessTokenConfiguration build() {
            return new FirebaseAccessTokenConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
