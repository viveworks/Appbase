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

package com.viveworks.appbase.client.service.jdbc.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.client.service.jdbc.DefaultJdbcClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultJdbcClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultJdbcClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultJdbcClientIntegrationServiceConfiguration implements JdbcClientIntegrationServiceConfiguration {
    @NullableConditionally(dependsOn = "HikariDataSourceConfiguration.serverHostPortConfiguration")
    private final TextValueConfiguration textValueKeyConfiguration;

    private final HikariDataSourceConfiguration hikariDataSourceConfiguration;


    private DefaultJdbcClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.hikariDataSourceConfiguration, "builder.hikariDataSourceConfiguration");

        textValueKeyConfiguration = builder.textValueKeyConfiguration;
        hikariDataSourceConfiguration = builder.hikariDataSourceConfiguration;
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

    public HikariDataSourceConfiguration getHikariDataSourceConfiguration() {
        return hikariDataSourceConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultJdbcClientIntegrationServiceConfiguration that = (DefaultJdbcClientIntegrationServiceConfiguration) o;
        return Objects.equals(textValueKeyConfiguration, that.textValueKeyConfiguration) &&
                Objects.equals(hikariDataSourceConfiguration, that.hikariDataSourceConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textValueKeyConfiguration, hikariDataSourceConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultJdbcClientIntegrationServiceConfiguration{" +
                "textValueKeyConfiguration=" + textValueKeyConfiguration +
                ", hikariDataSourceConfiguration=" + hikariDataSourceConfiguration +
                '}';
    }



    /**
     * Builder for {@link DefaultJdbcClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final HikariDataSourceConfiguration DEFAULT_HIKARI_DATA_SOURCE_CONFIGURATION =
                HikariDataSourceConfiguration.builder().build();


        private TextValueConfiguration textValueKeyConfiguration;
        private HikariDataSourceConfiguration hikariDataSourceConfiguration = DEFAULT_HIKARI_DATA_SOURCE_CONFIGURATION;


        private Builder() {
        }

        public Builder textValueKeyConfiguration(final TextValueConfiguration textValueKeyConfiguration) {
            this.textValueKeyConfiguration = textValueKeyConfiguration;
            return this;
        }

        public Builder hikariDataSourceConfiguration(final HikariDataSourceConfiguration hikariDataSourceConfiguration) {
            this.hikariDataSourceConfiguration = hikariDataSourceConfiguration;
            return this;
        }

        public DefaultJdbcClientIntegrationServiceConfiguration build() {
            return new DefaultJdbcClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
