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

package com.viveworks.appbase.core.util.net.ssl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for SSL / TLS trust manager.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SslTrustManagerConfiguration.Builder.class)
public final class SslTrustManagerConfiguration {
    private final String trustedCertificatesFilename;

    private SslTrustManagerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notBlank(builder.trustedCertificatesFilename, "builder.trustedCertificatesFilename");

        trustedCertificatesFilename = builder.trustedCertificatesFilename;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getTrustedCertificatesFilename() {
        return trustedCertificatesFilename;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SslTrustManagerConfiguration that = (SslTrustManagerConfiguration) o;
        return Objects.equals(trustedCertificatesFilename, that.trustedCertificatesFilename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trustedCertificatesFilename);
    }

    @Override
    public String toString() {
        return "SslTrustManagerConfiguration{" +
                "trustedCertificatesFilename='" + trustedCertificatesFilename + '\'' +
                '}';
    }

    /**
     * Builder for {@link SslTrustManagerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final String DEFAULT_TRUSTED_CERTIFICATES_FILENAME =
                AppbaseNetUtil.SSL_DIRECTORY + "/appbase_trusted_cacerts.jks";

        private String trustedCertificatesFilename = DEFAULT_TRUSTED_CERTIFICATES_FILENAME;

        private Builder() {
        }

        public Builder trustedCertificatesFilename(final String trustedCertificatesFilename) {
            this.trustedCertificatesFilename = trustedCertificatesFilename;
            return this;
        }

        public SslTrustManagerConfiguration build() {
            return new SslTrustManagerConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
