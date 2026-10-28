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

package com.viveworks.appbase.client.service.druid.sql.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.druid.sql.DefaultDruidSqlClientIntegrationService;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultDruidSqlClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultDruidSqlClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultDruidSqlClientIntegrationServiceConfiguration
        implements DruidSqlClientIntegrationServiceConfiguration
{
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    private final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration;


    private DefaultDruidSqlClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.clusterIdValueKeyConfiguration, "builder.clusterIdValueKeyConfiguration");
        AppbaseUtil.notNull(builder.defaultDruidSqlClientConfiguration, "builder.defaultDruidSqlClientConfiguration");

        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
        defaultDruidSqlClientConfiguration = builder.defaultDruidSqlClientConfiguration;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    public DefaultDruidSqlClientConfiguration getDefaultDruidSqlClientConfiguration() {
        return defaultDruidSqlClientConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultDruidSqlClientIntegrationServiceConfiguration that =
                (DefaultDruidSqlClientIntegrationServiceConfiguration) o;
        return Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration) &&
                Objects.equals(defaultDruidSqlClientConfiguration, that.defaultDruidSqlClientConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterIdValueKeyConfiguration, defaultDruidSqlClientConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultDruidSqlClientIntegrationServiceConfiguration{" +
                "clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                ", defaultDruidSqlClientConfiguration=" + defaultDruidSqlClientConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultDruidSqlClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final ClusterIdValueConfiguration DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION =
                ClusterIdValueConfiguration.PRIMARY;
        private static final DefaultDruidSqlClientConfiguration DEFAULT_DEFAULT_DRUID_SQL_CLIENT_CONFIGURATION =
                DefaultDruidSqlClientConfiguration.builder().build();


        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration =
                DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;
        private DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration =
                DEFAULT_DEFAULT_DRUID_SQL_CLIENT_CONFIGURATION;


        private Builder() {
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public Builder defaultDruidSqlClientConfiguration(
                final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration)
        {
            this.defaultDruidSqlClientConfiguration = defaultDruidSqlClientConfiguration;
            return this;
        }

        public DefaultDruidSqlClientIntegrationServiceConfiguration build() {
            return new DefaultDruidSqlClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
