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

package com.viveworks.appbase.core.util.net.ssl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for SSL / TLS private key.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SslKeyManagerConfiguration.Builder.class)
public final class SslKeyManagerConfiguration {
    private final String privateKeyCertificateChainFilename;
    private final String privateKeyFile;
    private final String privateKeyFilePassword;

    private SslKeyManagerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.privateKeyCertificateChainFilename, "builder.privateKeyCertificateChainFilename");
        AppbaseUtil.notBlank(builder.privateKeyFile, "builder.privateKeyFile");

        privateKeyCertificateChainFilename = builder.privateKeyCertificateChainFilename;
        privateKeyFile = builder.privateKeyFile;
        privateKeyFilePassword = builder.privateKeyFilePassword;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getPrivateKeyCertificateChainFilename() {
        return privateKeyCertificateChainFilename;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public String getPrivateKeyFilePassword() {
        return privateKeyFilePassword;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SslKeyManagerConfiguration that = (SslKeyManagerConfiguration) o;
        return Objects.equals(privateKeyCertificateChainFilename, that.privateKeyCertificateChainFilename) &&
                Objects.equals(privateKeyFile, that.privateKeyFile) &&
                Objects.equals(privateKeyFilePassword, that.privateKeyFilePassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateKeyCertificateChainFilename, privateKeyFile, privateKeyFilePassword);
    }

    @Override
    public String toString() {
        return "SslKeyManagerConfiguration{" +
                "privateKeyCertificateChainFilename='" + privateKeyCertificateChainFilename + '\'' +
                ", privateKeyFile='" + privateKeyFile + '\'' +
                ", privateKeyFilePassword='" + AppbaseUtil.OBFUSCATE + '\'' +
                '}';
    }

    /**
     * Builder for {@link SslKeyManagerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_PRIVATE_KEY_CERTIFICATE_CHAIN_FILENAME =
                AppbaseNetUtil.SSL_DIRECTORY + "/appbase.crt";
        private static final String DEFAULT_PRIVATE_KEY_FILE =
                AppbaseNetUtil.SSL_DIRECTORY + "/appbase.pem";
        private static final String DEFAULT_PRIVATE_KEY_FILE_PASSWORD = "appbase";

        private String privateKeyCertificateChainFilename = DEFAULT_PRIVATE_KEY_CERTIFICATE_CHAIN_FILENAME;
        private String privateKeyFile = DEFAULT_PRIVATE_KEY_FILE;
        private String privateKeyFilePassword = DEFAULT_PRIVATE_KEY_FILE_PASSWORD;

        private Builder() {
        }

        public Builder privateKeyCertificateChainFilename(final String privateKeyCertificateChainFilename) {
            this.privateKeyCertificateChainFilename = privateKeyCertificateChainFilename;
            return this;
        }

        public Builder privateKeyFile(final String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
            return this;
        }

        public Builder privateKeyFilePassword(final String privateKeyFilePassword) {
            this.privateKeyFilePassword = privateKeyFilePassword;
            return this;
        }

        public SslKeyManagerConfiguration build() {
            return new SslKeyManagerConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
