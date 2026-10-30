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
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = IdpToken.Builder.class)
public class IdpToken {
    private final String uid;

    private final String issuer;

    private final String name;

    private final String email;

    private final boolean emailVerified;

    private IdpToken(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.uid, "builder.uid");
        AppbaseUtil.notBlank(builder.issuer, "builder.issuer");
        AppbaseUtil.notBlank(builder.name, "builder.name");
        AppbaseUtil.notBlank(builder.email, "builder.email");

        uid = builder.uid;
        issuer = builder.issuer;
        name = builder.name;
        email = builder.email;
        emailVerified = builder.emailVerified;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUid() {
        return uid;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IdpToken idpToken = (IdpToken) o;
        return emailVerified == idpToken.emailVerified &&
                Objects.equals(uid, idpToken.uid) &&
                Objects.equals(issuer, idpToken.issuer) &&
                Objects.equals(name, idpToken.name) &&
                Objects.equals(email, idpToken.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, issuer, name, email, emailVerified);
    }

    @Override
    public String toString() {
        return "IdpToken{" +
                "uid='" + uid + '\'' +
                ", issuer='" + issuer + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }

    /**
     * Builder for {@link IdpToken}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String uid;
        private String issuer;
        private String name;
        private String email;
        private boolean emailVerified;

        private Builder() {
        }

        public Builder uid(final String uid) {
            this.uid = uid;
            return this;
        }

        public Builder issuer(final String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder emailVerified(final boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public IdpToken build() {
            return new IdpToken(this);
        }
    }
    // CheckStyle: Builder end
}
