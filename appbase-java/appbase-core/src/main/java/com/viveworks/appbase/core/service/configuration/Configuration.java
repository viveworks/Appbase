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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = Configuration.Builder.class)
public final class Configuration {
    private final ConfigurationKey configurationKey;
    private final ConfigurationValue<?> configurationValue;
    private final RawConfiguration rawConfiguration;

    private Configuration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.configurationKey, "configurationKey");
        AppbaseUtil.notNull(builder.configurationValue, "configurationValue");
        AppbaseUtil.notNull(builder.rawConfiguration, "rawConfiguration");

        configurationKey = builder.configurationKey;
        configurationValue = builder.configurationValue;
        rawConfiguration = builder.rawConfiguration;
    }

    /**
     * Get the builder used to build this object.
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the property-or-type key used to identify this configuration.
     *
     * @return the property-or-type key used to identify this configuration
     */
    public ConfigurationKey getConfigurationKey() {
        return configurationKey;
    }

    /**
     * Returns the decoded value associated with the configuration key.
     *
     * @return the decoded value associated with the configuration key
     */
    public ConfigurationValue<?> getConfigurationValue() {
        return configurationValue;
    }

    /**
     * Returns the source bytes, MIME type, context, and timestamp for this configuration.
     *
     * @return the source bytes, MIME type, context, and timestamp for this configuration
     */
    public RawConfiguration getRawConfiguration() {
        return rawConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Configuration that = (Configuration) o;
        return Objects.equals(configurationKey, that.configurationKey) &&
                Objects.equals(configurationValue, that.configurationValue) &&
                Objects.equals(rawConfiguration, that.rawConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurationKey, configurationValue, rawConfiguration);
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "configurationKey=" + configurationKey +
                ", configurationValue=" + configurationValue +
                ", rawConfiguration=" + rawConfiguration +
                '}';
    }

    /**
     * {@link Configuration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private ConfigurationKey configurationKey;
        private ConfigurationValue<?> configurationValue;
        private RawConfiguration rawConfiguration;

        private Builder() {
        }

        public Builder configurationKey(final ConfigurationKey configurationKey) {
            this.configurationKey = configurationKey;
            return this;
        }

        public Builder configurationValue(final ConfigurationValue<?> configurationValue) {
            this.configurationValue = configurationValue;
            return this;
        }

        public Builder rawConfiguration(final RawConfiguration rawConfiguration) {
            this.rawConfiguration = rawConfiguration;
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }
    // CheckStyle: Builder end
}
