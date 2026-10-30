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

package com.viveworks.appbase.client.service.druid.sql.client.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultDruidSqlClientConfiguration.Builder.class)
public final class DefaultDruidSqlClientConfiguration {
    private final DruidConnectionConfiguration druidConnectionConfiguration;
    private final DruidSqlQueryConfiguration druidSqlQueryConfiguration;

    private DefaultDruidSqlClientConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.druidConnectionConfiguration, "builder.druidConnectionConfiguration");
        AppbaseUtil.notNull(builder.druidSqlQueryConfiguration, "builder.druidSqlQueryConfiguration");

        druidConnectionConfiguration = builder.druidConnectionConfiguration;
        druidSqlQueryConfiguration = builder.druidSqlQueryConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public DruidConnectionConfiguration getDruidConnectionConfiguration() {
        return druidConnectionConfiguration;
    }

    public DruidSqlQueryConfiguration getDruidSqlQueryConfiguration() {
        return druidSqlQueryConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultDruidSqlClientConfiguration that = (DefaultDruidSqlClientConfiguration) o;
        return Objects.equals(druidConnectionConfiguration, that.druidConnectionConfiguration) &&
                Objects.equals(druidSqlQueryConfiguration, that.druidSqlQueryConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(druidConnectionConfiguration, druidSqlQueryConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultDruidSqlClientConfiguration{" +
                "druidConnectionConfiguration=" + druidConnectionConfiguration +
                ", druidSqlQueryConfiguration=" + druidSqlQueryConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultDruidSqlClientConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final DruidConnectionConfiguration DEFAULT_DRUID_CONNECTION_CONFIGURATION =
                DruidConnectionConfiguration.builder().build();
        private static final DruidSqlQueryConfiguration DEFAULT_DRUID_SQL_QUERY_CONFIGURATION =
                DruidSqlQueryConfiguration.builder().build();


        private DruidConnectionConfiguration druidConnectionConfiguration = DEFAULT_DRUID_CONNECTION_CONFIGURATION;
        private DruidSqlQueryConfiguration druidSqlQueryConfiguration = DEFAULT_DRUID_SQL_QUERY_CONFIGURATION;


        private Builder() {
        }

        public Builder druidConnectionConfiguration(
                final DruidConnectionConfiguration druidConnectionConfiguration)
        {
            this.druidConnectionConfiguration = druidConnectionConfiguration;
            return this;
        }

        public Builder druidSqlQueryConfiguration(
                final DruidSqlQueryConfiguration druidSqlQueryConfiguration)
        {
            this.druidSqlQueryConfiguration = druidSqlQueryConfiguration;
            return this;
        }

        public DefaultDruidSqlClientConfiguration build() {
            return new DefaultDruidSqlClientConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
