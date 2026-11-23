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

package com.viveworks.appbase.client.service.clamav.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.clamav.DefaultClamAVClientIntegrationService;
import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultClamAVClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultClamAVClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultClamAVClientIntegrationServiceConfiguration
        implements ClamAVClientIntegrationServiceConfiguration
{
    @NullableConditionally(dependsOn = "DefaultClamAVClientConfiguration.hostPortConfiguration")
    private final TextValueConfiguration textValueKeyConfiguration;

    private final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration;


    private DefaultClamAVClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.defaultClamAVClientConfiguration, "builder.defaultClamAVClientConfiguration");

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        defaultClamAVClientConfiguration = builder.defaultClamAVClientConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TextValueConfiguration getTextValueKeyConfiguration() {
        return textValueKeyConfiguration;
    }

    public DefaultClamAVClientConfiguration getDefaultClamAVClientConfiguration() {
        return defaultClamAVClientConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultClamAVClientIntegrationServiceConfiguration that =
                (DefaultClamAVClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(defaultClamAVClientConfiguration, that.defaultClamAVClientConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textValueKeyConfiguration, defaultClamAVClientConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultClamAVClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", defaultClamAVClientConfiguration=" + defaultClamAVClientConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultClamAVClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final TextValueConfiguration DEFAULT_TEXT_VALUE_KEY_CONFIGURATION = TextValueConfiguration.PRIMARY;
        private static final DefaultClamAVClientConfiguration DEFAULT_DEFAULT_CLAMAV_CLIENT_CONFIGURATION =
                DefaultClamAVClientConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration =
                DEFAULT_TEXT_VALUE_KEY_CONFIGURATION;
        private DefaultClamAVClientConfiguration defaultClamAVClientConfiguration =
                DEFAULT_DEFAULT_CLAMAV_CLIENT_CONFIGURATION;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder defaultClamAVClientConfiguration(
                final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration)
        {
            this.defaultClamAVClientConfiguration = defaultClamAVClientConfiguration;
            return this;
        }

        public DefaultClamAVClientIntegrationServiceConfiguration build() {
            return new DefaultClamAVClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
