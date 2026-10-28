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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for SSL / TLS client.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SslClientContextConfiguration.Builder.class)
public final class SslClientContextConfiguration {
    private final SslContextCommonConfiguration sslContextCommonConfiguration;
    private final SslKeyManagerConfiguration sslKeyManagerConfiguration;

    private SslClientContextConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.sslContextCommonConfiguration, "builder.sslContextCommonConfiguration");

        sslContextCommonConfiguration = builder.sslContextCommonConfiguration;
        sslKeyManagerConfiguration = builder.sslKeyManagerConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public SslContextCommonConfiguration getSslContextCommonConfiguration() {
        return sslContextCommonConfiguration;
    }

    public SslKeyManagerConfiguration getSslKeyManagerConfiguration() {
        return sslKeyManagerConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SslClientContextConfiguration that = (SslClientContextConfiguration) o;
        return Objects.equals(sslContextCommonConfiguration, that.sslContextCommonConfiguration) &&
                Objects.equals(sslKeyManagerConfiguration, that.sslKeyManagerConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sslContextCommonConfiguration, sslKeyManagerConfiguration);
    }

    @Override
    public String toString() {
        return "SslClientContextConfiguration{" +
                "sslContextCommonConfiguration=" + sslContextCommonConfiguration +
                "sslKeyManagerConfiguration=" + sslKeyManagerConfiguration +
                '}';
    }

    /**
     * Builder for {@link SslClientContextConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private SslContextCommonConfiguration DEFAULT_SSL_CONTEXT_COMMON_CONFIGURATION =
                SslContextCommonConfiguration.builder().build();
        private static final SslKeyManagerConfiguration DEFAULT_SSL_KEY_MANAGER_CONFIGURATION =
                SslKeyManagerConfiguration.builder().build();

        private SslContextCommonConfiguration sslContextCommonConfiguration = DEFAULT_SSL_CONTEXT_COMMON_CONFIGURATION;
        private SslKeyManagerConfiguration sslKeyManagerConfiguration = DEFAULT_SSL_KEY_MANAGER_CONFIGURATION;

        private Builder() {
        }

        public Builder sslContextCommonConfiguration(
                final SslContextCommonConfiguration sslContextCommonConfiguration)
        {
            this.sslContextCommonConfiguration = sslContextCommonConfiguration;
            return this;
        }

        public Builder sslKeyManagerConfiguration(final SslKeyManagerConfiguration sslKeyManagerConfiguration) {
            this.sslKeyManagerConfiguration = sslKeyManagerConfiguration;
            return this;
        }

        public SslClientContextConfiguration build() {
            return new SslClientContextConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
