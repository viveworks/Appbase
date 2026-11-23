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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vitess.client.grpc.tls.TlsOptions;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link TlsOptions}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = TlsOptionsConfiguration.Builder.class)
public final class TlsOptionsConfiguration {
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String keyAlias;
    private final String keyPassword;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final String trustAlias;

    private TlsOptionsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        keyStorePath = builder.keyStorePath;
        keyStorePassword = builder.keyStorePassword;
        keyAlias = builder.keyAlias;
        keyPassword = builder.keyPassword;
        trustStorePath = builder.trustStorePath;
        trustStorePassword = builder.trustStorePassword;
        trustAlias = builder.trustAlias;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getTrustAlias() {
        return trustAlias;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TlsOptionsConfiguration that = (TlsOptionsConfiguration) o;
        return Objects.equals(keyStorePath, that.keyStorePath) &&
                Objects.equals(keyStorePassword, that.keyStorePassword) &&
                Objects.equals(keyAlias, that.keyAlias) &&
                Objects.equals(keyPassword, that.keyPassword) &&
                Objects.equals(trustStorePath, that.trustStorePath) &&
                Objects.equals(trustStorePassword, that.trustStorePassword) &&
                Objects.equals(trustAlias, that.trustAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                keyStorePath,
                keyStorePassword,
                keyAlias,
                keyPassword,
                trustStorePath,
                trustStorePassword,
                trustAlias);
    }

    @Override
    public String toString() {
        return "TlsOptionsConfiguration{" +
                "keyStorePath='" + keyStorePath + '\'' +
                ", keyStorePassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", keyAlias='" + keyAlias + '\'' +
                ", keyPassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", trustStorePath='" + trustStorePath + '\'' +
                ", trustStorePassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                ", trustAlias='" + trustAlias + '\'' +
                '}';
    }

    /**
     * Builder for {@link TlsOptionsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String keyStorePath;
        private String keyStorePassword;
        private String keyAlias;
        private String keyPassword;
        private String trustStorePath;
        private String trustStorePassword;
        private String trustAlias;

        private Builder() {
        }

        public Builder keyStorePath(final String keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }

        public Builder keyStorePassword(final String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
            return this;
        }

        public Builder keyAlias(final String keyAlias) {
            this.keyAlias = keyAlias;
            return this;
        }

        public Builder keyPassword(final String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        public Builder trustStorePath(final String trustStorePath) {
            this.trustStorePath = trustStorePath;
            return this;
        }

        public Builder trustStorePassword(final String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        public Builder trustAlias(final String trustAlias) {
            this.trustAlias = trustAlias;
            return this;
        }

        public TlsOptionsConfiguration build() {
            return new TlsOptionsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
