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

package com.viveworks.appbase.client.cloud.aws.common.configuration.tls;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link SystemPropertyTlsKeyManagersProvider}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = SystemPropertyTlsKeyManagersProviderConfiguration.Builder.class)
public final class SystemPropertyTlsKeyManagersProviderConfiguration implements AwsSdkTlsKeyManagersProviderConfiguration {
    private final boolean enabled;

    private SystemPropertyTlsKeyManagersProviderConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enabled = builder.enabled;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether enabled.
     *
     * @return whether enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SystemPropertyTlsKeyManagersProviderConfiguration that = (SystemPropertyTlsKeyManagersProviderConfiguration) o;
        return enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled);
    }

    @Override
    public String toString() {
        return "SystemPropertyTlsKeyManagersProviderConfiguration{" +
                "enabled=" + enabled +
                '}';
    }


    /**
     * Builder for {@link SystemPropertyTlsKeyManagersProviderConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;

        private boolean enabled = DEFAULT_ENABLED;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public SystemPropertyTlsKeyManagersProviderConfiguration build() {
            return new SystemPropertyTlsKeyManagersProviderConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
