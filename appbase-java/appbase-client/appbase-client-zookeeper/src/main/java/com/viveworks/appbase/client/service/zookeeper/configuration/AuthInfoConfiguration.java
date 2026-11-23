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

package com.viveworks.appbase.client.service.zookeeper.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Immutable configuration for ZooKeeper authentication info, mapping to
 * {@link org.apache.curator.framework.AuthInfo}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = AuthInfoConfiguration.Builder.class)
public final class AuthInfoConfiguration {

    @Nullable
    private final String scheme;

    @Nullable
    private final String auth;

    private AuthInfoConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        scheme = builder.scheme;
        auth = builder.auth;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getScheme() {
        return scheme;
    }

    @Nullable
    public String getAuth() {
        return auth;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AuthInfoConfiguration that = (AuthInfoConfiguration) o;
        return Objects.equals(scheme, that.scheme) &&
                Objects.equals(auth, that.auth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, auth);
    }

    @Override
    public String toString() {
        return "AuthInfoConfiguration{" +
                "scheme='" + scheme + '\'' +
                ", auth='[REDACTED]'" +
                '}';
    }

    /**
     * Builder for {@link AuthInfoConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {

        @Nullable
        private String scheme;

        @Nullable
        private String auth;

        private Builder() {
        }

        public Builder scheme(@Nullable final String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder auth(@Nullable final String auth) {
            this.auth = auth;
            return this;
        }

        public AuthInfoConfiguration build() {
            return new AuthInfoConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
