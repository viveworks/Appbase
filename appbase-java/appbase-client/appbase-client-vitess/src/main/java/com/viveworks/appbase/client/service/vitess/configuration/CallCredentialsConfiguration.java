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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.CallCredentials;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link CallCredentials}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = CallCredentialsConfiguration.Builder.class)
public final class CallCredentialsConfiguration {
    private final InjectedTypeConfiguration<? extends CallCredentials> callCredentialsInjectedTypeConfiguration;
    private final StaticAuthCredentialsConfiguration staticAuthCredentialsConfiguration;

    private CallCredentialsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.callCredentialsInjectedTypeConfiguration == null) {
            AppbaseUtil.notNull(builder.staticAuthCredentialsConfiguration, "builder.staticAuthCredentialsConfiguration");
        }

        if (builder.staticAuthCredentialsConfiguration == null) {
            AppbaseUtil.notNull(
                    builder.callCredentialsInjectedTypeConfiguration,
                    "builder.callCredentialsInjectedTypeConfiguration");
        }

        callCredentialsInjectedTypeConfiguration = builder.callCredentialsInjectedTypeConfiguration;
        staticAuthCredentialsConfiguration = builder.staticAuthCredentialsConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public InjectedTypeConfiguration<? extends CallCredentials> getCallCredentialsInjectedTypeConfiguration() {
        return callCredentialsInjectedTypeConfiguration;
    }

    public StaticAuthCredentialsConfiguration getStaticAuthCredentialsConfiguration() {
        return staticAuthCredentialsConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CallCredentialsConfiguration that = (CallCredentialsConfiguration) o;
        return Objects.equals(callCredentialsInjectedTypeConfiguration, that.callCredentialsInjectedTypeConfiguration) &&
                Objects.equals(staticAuthCredentialsConfiguration, that.staticAuthCredentialsConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callCredentialsInjectedTypeConfiguration, staticAuthCredentialsConfiguration);
    }

    @Override
    public String toString() {
        return "CallCredentialsConfiguration{" +
                "callCredentialsInjectedTypeConfiguration=" + callCredentialsInjectedTypeConfiguration +
                ", staticAuthCredentialsConfiguration=" + staticAuthCredentialsConfiguration +
                '}';
    }

    /**
     * Builder for {@link CallCredentialsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final StaticAuthCredentialsConfiguration DEFAULT_STATIC_AUTH_CREDENTIALS_CONFIGURATION =
                StaticAuthCredentialsConfiguration.builder().build();

        private InjectedTypeConfiguration<? extends CallCredentials> callCredentialsInjectedTypeConfiguration;
        private StaticAuthCredentialsConfiguration staticAuthCredentialsConfiguration =
                DEFAULT_STATIC_AUTH_CREDENTIALS_CONFIGURATION;

        private Builder() {
        }

        public Builder callCredentialsInjectedTypeConfiguration(
                final InjectedTypeConfiguration<? extends CallCredentials> callCredentialsInjectedTypeConfiguration)
        {
            this.callCredentialsInjectedTypeConfiguration = callCredentialsInjectedTypeConfiguration;
            return this;
        }

        public Builder staticAuthCredentialsConfiguration(
                final StaticAuthCredentialsConfiguration staticAuthCredentialsConfiguration)
        {
            this.staticAuthCredentialsConfiguration = staticAuthCredentialsConfiguration;
            return this;
        }

        public CallCredentialsConfiguration build() {
            return new CallCredentialsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
