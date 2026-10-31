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

package com.viveworks.appbase.client.service.clamav.client.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClientSharedResources;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link DefaultClamAVClientSharedResources}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultClamAVClientSharedResourcesConfiguration.Builder.class)
public final class DefaultClamAVClientSharedResourcesConfiguration
        extends AbstractAppbaseManagedServiceConfiguration implements ClamAVClientSharedResourcesConfiguration
{
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVRequestEventObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVPingResultObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVScanResultObjectPoolConfiguration;

    private DefaultClamAVClientSharedResourcesConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.sharedClamAVRequestEventObjectPoolConfiguration,
                "builder.sharedClamAVRequestEventObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedClamAVPingResultObjectPoolConfiguration,
                "builder.sharedClamAVPingResultObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedClamAVScanResultObjectPoolConfiguration,
                "builder.sharedClamAVScanResultObjectPoolConfiguration");

        sharedClamAVRequestEventObjectPoolConfiguration = builder.sharedClamAVRequestEventObjectPoolConfiguration;
        sharedClamAVPingResultObjectPoolConfiguration = builder.sharedClamAVPingResultObjectPoolConfiguration;
        sharedClamAVScanResultObjectPoolConfiguration = builder.sharedClamAVScanResultObjectPoolConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedClamAVRequestEventObjectPoolConfiguration() {
        return sharedClamAVRequestEventObjectPoolConfiguration;
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedClamAVPingResultObjectPoolConfiguration() {
        return sharedClamAVPingResultObjectPoolConfiguration;
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedClamAVScanResultObjectPoolConfiguration() {
        return sharedClamAVScanResultObjectPoolConfiguration;
    }

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

        final DefaultClamAVClientSharedResourcesConfiguration that = (DefaultClamAVClientSharedResourcesConfiguration) o;
        return Objects.equals(
                sharedClamAVRequestEventObjectPoolConfiguration,
                that.sharedClamAVRequestEventObjectPoolConfiguration) &&
                Objects.equals(
                        sharedClamAVPingResultObjectPoolConfiguration,
                        that.sharedClamAVPingResultObjectPoolConfiguration) &&
                Objects.equals(
                        sharedClamAVScanResultObjectPoolConfiguration,
                        that.sharedClamAVScanResultObjectPoolConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                sharedClamAVRequestEventObjectPoolConfiguration,
                sharedClamAVPingResultObjectPoolConfiguration,
                sharedClamAVScanResultObjectPoolConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultClamAVClientSharedResourcesConfiguration{" +
                "sharedClamAVRequestEventObjectPoolConfiguration=" +
                sharedClamAVRequestEventObjectPoolConfiguration +
                ", sharedClamAVPingResultObjectPoolConfiguration=" +
                sharedClamAVPingResultObjectPoolConfiguration +
                ", sharedClamAVScanResultObjectPoolConfiguration=" +
                sharedClamAVScanResultObjectPoolConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultClamAVClientSharedResourcesConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseManagedServiceConfiguration.Builder<Builder> {
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_CLAMAV_REQUEST_EVENT_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_CLAMAV_PING_RESULT_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_CLAMAV_SCAN_RESULT_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();


        private ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVRequestEventObjectPoolConfiguration =
                DEFAULT_SHARED_CLAMAV_REQUEST_EVENT_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVPingResultObjectPoolConfiguration =
                DEFAULT_SHARED_CLAMAV_PING_RESULT_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVScanResultObjectPoolConfiguration =
                DEFAULT_SHARED_CLAMAV_SCAN_RESULT_OBJECT_POOL_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder sharedClamAVRequestEventObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVRequestEventObjectPoolConfiguration)
        {
            this.sharedClamAVRequestEventObjectPoolConfiguration = sharedClamAVRequestEventObjectPoolConfiguration;
            return this;
        }

        public Builder sharedClamAVPingResultObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVPingResultObjectPoolConfiguration)
        {
            this.sharedClamAVPingResultObjectPoolConfiguration = sharedClamAVPingResultObjectPoolConfiguration;
            return this;
        }

        public Builder sharedClamAVScanResultObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedClamAVScanResultObjectPoolConfiguration)
        {
            this.sharedClamAVScanResultObjectPoolConfiguration = sharedClamAVScanResultObjectPoolConfiguration;
            return this;
        }

        public DefaultClamAVClientSharedResourcesConfiguration build() {
            return new DefaultClamAVClientSharedResourcesConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
