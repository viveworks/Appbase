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
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.service.vitess.DefaultVitessGrpcClientIntegrationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableConditionally;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultVitessGrpcClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultVitessGrpcClientIntegrationServiceConfiguration.Builder.class)
public final class DefaultVitessGrpcClientIntegrationServiceConfiguration
        implements VitessGrpcClientIntegrationServiceConfiguration
{
    @NullableConditionally(dependsOn = "CustomGrpcClientFactoryConfiguration.targetConfiguration")
    private final CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration;

    private final VtSessionConfiguration vtSessionConfiguration;

    @Nullable
    private final ClusterIdValueConfiguration clusterIdValueKeyConfiguration;

    private DefaultVitessGrpcClientIntegrationServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.customGrpcClientFactoryConfiguration, "builder.customGrpcClientFactoryConfiguration");
        AppbaseUtil.notNull(builder.vtSessionConfiguration, "builder.vtSessionConfiguration");

        customGrpcClientFactoryConfiguration = builder.customGrpcClientFactoryConfiguration;
        vtSessionConfiguration = builder.vtSessionConfiguration;
        clusterIdValueKeyConfiguration = builder.clusterIdValueKeyConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public CustomGrpcClientFactoryConfiguration getCustomGrpcClientFactoryConfiguration() {
        return customGrpcClientFactoryConfiguration;
    }

    public VtSessionConfiguration getVtSessionConfiguration() {
        return vtSessionConfiguration;
    }

    @Nullable
    public ClusterIdValueConfiguration getClusterIdValueKeyConfiguration() {
        return clusterIdValueKeyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultVitessGrpcClientIntegrationServiceConfiguration that =
                (DefaultVitessGrpcClientIntegrationServiceConfiguration) o;
        return Objects.equals(customGrpcClientFactoryConfiguration, that.customGrpcClientFactoryConfiguration) &&
                Objects.equals(vtSessionConfiguration, that.vtSessionConfiguration) &&
                Objects.equals(clusterIdValueKeyConfiguration, that.clusterIdValueKeyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customGrpcClientFactoryConfiguration, vtSessionConfiguration, clusterIdValueKeyConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultVitessGrpcClientIntegrationServiceConfiguration{" +
                "customGrpcClientFactoryConfiguration=" + customGrpcClientFactoryConfiguration +
                ", vtSessionConfiguration=" + vtSessionConfiguration +
                ", clusterIdValueKeyConfiguration=" + clusterIdValueKeyConfiguration +
                '}';
    }


    /**
     * Builder for {@link DefaultVitessGrpcClientIntegrationServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final CustomGrpcClientFactoryConfiguration DEFAULT_CUSTOM_GRPC_CLIENT_FACTORY_CONFIGURATION =
                CustomGrpcClientFactoryConfiguration.builder().build();
        private static final VtSessionConfiguration DEFAULT_VTSESSION_CONFIGURATION =
                VtSessionConfiguration.builder().build();
        private static final ClusterIdValueConfiguration DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION =
                ClusterIdValueConfiguration.builder().build();


        private CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration =
                DEFAULT_CUSTOM_GRPC_CLIENT_FACTORY_CONFIGURATION;
        private VtSessionConfiguration vtSessionConfiguration = DEFAULT_VTSESSION_CONFIGURATION;
        private ClusterIdValueConfiguration clusterIdValueKeyConfiguration = DEFAULT_CLUSTER_ID_VALUE_KEY_CONFIGURATION;


        private Builder() {
        }

        public Builder customGrpcClientFactoryConfiguration(
                final CustomGrpcClientFactoryConfiguration customGrpcClientFactoryConfiguration)
        {
            this.customGrpcClientFactoryConfiguration = customGrpcClientFactoryConfiguration;
            return this;
        }

        public Builder vtSessionConfiguration(final VtSessionConfiguration vtSessionConfiguration) {
            this.vtSessionConfiguration = vtSessionConfiguration;
            return this;
        }

        public Builder clusterIdValueKeyConfiguration(final ClusterIdValueConfiguration clusterIdValueKeyConfiguration) {
            this.clusterIdValueKeyConfiguration = clusterIdValueKeyConfiguration;
            return this;
        }

        public DefaultVitessGrpcClientIntegrationServiceConfiguration build() {
            return new DefaultVitessGrpcClientIntegrationServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
