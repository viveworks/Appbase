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

package com.viveworks.appbase.service.user.account.firebaseauth;

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
@JsonDeserialize(builder = DefaultFirebaseAuthServiceConfiguration.Builder.class)
public class DefaultFirebaseAuthServiceConfiguration implements AppbaseConfiguration {
    private final boolean userTokenVerificationEnabled;

    private final boolean checkUserTokenRevocation;

    private DefaultFirebaseAuthServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        userTokenVerificationEnabled = builder.userTokenVerificationEnabled;
        checkUserTokenRevocation = builder.checkUserTokenRevocation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isUserTokenVerificationEnabled() {
        return userTokenVerificationEnabled;
    }

    public boolean isCheckUserTokenRevocation() {
        return checkUserTokenRevocation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultFirebaseAuthServiceConfiguration that = (DefaultFirebaseAuthServiceConfiguration) o;
        return userTokenVerificationEnabled == that.userTokenVerificationEnabled &&
                checkUserTokenRevocation == that.checkUserTokenRevocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userTokenVerificationEnabled, checkUserTokenRevocation);
    }

    @Override
    public String toString() {
        return "DefaultFirebaseAuthServiceConfiguration{" +
                "userTokenVerificationEnabled=" + userTokenVerificationEnabled +
                ", checkUserTokenRevocation=" + checkUserTokenRevocation +
                '}';
    }

    /**
     * Builder for {@link DefaultFirebaseAuthServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_USER_TOKEN_VERIFICATION_ENABLED = true;
        private static final boolean DEFAULT_CHECK_USER_TOKEN_REVOCATION = true;

        private boolean userTokenVerificationEnabled = DEFAULT_USER_TOKEN_VERIFICATION_ENABLED;
        private boolean checkUserTokenRevocation = DEFAULT_CHECK_USER_TOKEN_REVOCATION;

        private Builder() {
        }

        public Builder userTokenVerificationEnabled(boolean userTokenVerificationEnabled) {
            this.userTokenVerificationEnabled = userTokenVerificationEnabled;
            return this;
        }

        public Builder checkUserTokenRevocation(boolean checkUserTokenRevocation) {
            this.checkUserTokenRevocation = checkUserTokenRevocation;
            return this;
        }

        public DefaultFirebaseAuthServiceConfiguration build() {
            return new DefaultFirebaseAuthServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
