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

package com.viveworks.appbase.client.core;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultAppbaseClientCoreSharedResources}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAppbaseClientCoreSharedResourcesConfiguration.Builder.class)
public final class DefaultAppbaseClientCoreSharedResourcesConfiguration
        extends AbstractAppbaseManagedServiceConfiguration implements AppbaseClientCoreSharedResourcesConfiguration
{
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedMutableAppbaseClientKeyObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedHostPortValueObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedTextValueObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedClusterIdValueObjectPoolConfiguration;

    private DefaultAppbaseClientCoreSharedResourcesConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.sharedMutableAppbaseClientKeyObjectPoolConfiguration,
                "builder.sharedMutableAppbaseClientKeyObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedHostPortValueObjectPoolConfiguration,
                "builder.sharedHostPortValueObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedTextValueObjectPoolConfiguration,
                "builder.sharedTextValueObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedClusterIdValueObjectPoolConfiguration,
                "builder.sharedClusterIdValueObjectPoolConfiguration");

        sharedMutableAppbaseClientKeyObjectPoolConfiguration =
                builder.sharedMutableAppbaseClientKeyObjectPoolConfiguration;
        sharedHostPortValueObjectPoolConfiguration =
                builder.sharedHostPortValueObjectPoolConfiguration;
        sharedTextValueObjectPoolConfiguration =
                builder.sharedTextValueObjectPoolConfiguration;
        sharedClusterIdValueObjectPoolConfiguration =
                builder.sharedClusterIdValueObjectPoolConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    /** @return sizing and queue policy for the reusable mutable client-key pool */
    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedMutableAppbaseClientKeyObjectPoolConfiguration() {
        return sharedMutableAppbaseClientKeyObjectPoolConfiguration;
    }

    /** @return sizing and queue policy for pooled host-and-port values */
    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedHostPortValueObjectPoolConfiguration() {
        return sharedHostPortValueObjectPoolConfiguration;
    }

    /** @return sizing and queue policy for pooled text key values */
    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedTextValueObjectPoolConfiguration() {
        return sharedTextValueObjectPoolConfiguration;
    }

    /** @return sizing and queue policy for pooled mutable cluster identifiers */
    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedClusterIdValueObjectPoolConfiguration() {
        return sharedClusterIdValueObjectPoolConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        final DefaultAppbaseClientCoreSharedResourcesConfiguration that =
                (DefaultAppbaseClientCoreSharedResourcesConfiguration) o;
        return Objects.equals(
                sharedMutableAppbaseClientKeyObjectPoolConfiguration,
                that.sharedMutableAppbaseClientKeyObjectPoolConfiguration) &&
                Objects.equals(
                        sharedHostPortValueObjectPoolConfiguration, that.sharedHostPortValueObjectPoolConfiguration) &&
                Objects.equals(sharedTextValueObjectPoolConfiguration, that.sharedTextValueObjectPoolConfiguration) &&
                Objects.equals(
                        sharedClusterIdValueObjectPoolConfiguration, that.sharedClusterIdValueObjectPoolConfiguration);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                sharedMutableAppbaseClientKeyObjectPoolConfiguration,
                sharedHostPortValueObjectPoolConfiguration,
                sharedTextValueObjectPoolConfiguration,
                sharedClusterIdValueObjectPoolConfiguration);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DefaultAppbaseClientCoreSharedResourcesConfiguration{" +
                "sharedMutableAppbaseClientKeyObjectPoolConfiguration=" +
                sharedMutableAppbaseClientKeyObjectPoolConfiguration +
                ", sharedHostPortValueObjectPoolConfiguration=" + sharedHostPortValueObjectPoolConfiguration +
                ", sharedTextValueObjectPoolConfiguration=" + sharedTextValueObjectPoolConfiguration +
                ", sharedClusterIdValueObjectPoolConfiguration=" + sharedClusterIdValueObjectPoolConfiguration +
                "} " + super.toString();
    }

    /**
     * Builder for sizing the shared client-core pools.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseManagedServiceConfiguration.Builder<Builder> {
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_MUTABLE_APPBASE_CLIENT_KEY_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_HOST_PORT_VALUE_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(512)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_TEXT_VALUE_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(512)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_CLUSTER_ID_VALUE_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(512)
                        .build();

        private ManyToManyQueuedFixedObjectPoolConfiguration sharedMutableAppbaseClientKeyObjectPoolConfiguration =
                DEFAULT_SHARED_MUTABLE_APPBASE_CLIENT_KEY_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedHostPortValueObjectPoolConfiguration =
                DEFAULT_SHARED_HOST_PORT_VALUE_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedTextValueObjectPoolConfiguration =
                DEFAULT_SHARED_TEXT_VALUE_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedClusterIdValueObjectPoolConfiguration =
                DEFAULT_SHARED_CLUSTER_ID_VALUE_OBJECT_POOL_CONFIGURATION;

        private Builder() {
        }

        /** {@inheritDoc} */
        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder sharedMutableAppbaseClientKeyObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedMutableAppbaseClientKeyObjectPoolConfiguration)
        {
            this.sharedMutableAppbaseClientKeyObjectPoolConfiguration = sharedMutableAppbaseClientKeyObjectPoolConfiguration;
            return this;
        }

        public Builder sharedHostPortValueObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedHostPortValueObjectPoolConfiguration)
        {
            this.sharedHostPortValueObjectPoolConfiguration = sharedHostPortValueObjectPoolConfiguration;
            return this;
        }

        public Builder sharedTextValueObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedTextValueObjectPoolConfiguration)
        {
            this.sharedTextValueObjectPoolConfiguration = sharedTextValueObjectPoolConfiguration;
            return this;
        }

        public Builder sharedClusterIdValueObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedClusterIdValueObjectPoolConfiguration)
        {
            this.sharedClusterIdValueObjectPoolConfiguration = sharedClusterIdValueObjectPoolConfiguration;
            return this;
        }

        public DefaultAppbaseClientCoreSharedResourcesConfiguration build() {
            return new DefaultAppbaseClientCoreSharedResourcesConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
