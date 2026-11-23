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

package com.viveworks.appbase.client.service.druid.sql.client.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultDruidSqlClientSharedResourcesConfiguration.Builder.class)
public final class DefaultDruidSqlClientSharedResourcesConfiguration
        extends AbstractAppbaseManagedServiceConfiguration implements DruidSqlClientSharedResourcesConfiguration
{
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidQueryEventObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidResultSetObjectPoolConfiguration;
    private final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidSqlParameterObjectPoolConfiguration;

    private DefaultDruidSqlClientSharedResourcesConfiguration(final Builder builder) {
        super(builder);

        AppbaseUtil.notNull(
                builder.sharedDruidQueryEventObjectPoolConfiguration,
                "builder.sharedDruidQueryEventObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedDruidResultSetObjectPoolConfiguration,
                "builder.sharedDruidResultSetObjectPoolConfiguration");
        AppbaseUtil.notNull(
                builder.sharedDruidSqlParameterObjectPoolConfiguration,
                "builder.sharedDruidSqlParameterObjectPoolConfiguration");

        sharedDruidQueryEventObjectPoolConfiguration = builder.sharedDruidQueryEventObjectPoolConfiguration;
        sharedDruidResultSetObjectPoolConfiguration = builder.sharedDruidResultSetObjectPoolConfiguration;
        sharedDruidSqlParameterObjectPoolConfiguration = builder.sharedDruidSqlParameterObjectPoolConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedDruidQueryEventObjectPoolConfiguration() {
        return sharedDruidQueryEventObjectPoolConfiguration;
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedDruidResultSetObjectPoolConfiguration() {
        return sharedDruidResultSetObjectPoolConfiguration;
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getSharedDruidSqlParameterObjectPoolConfiguration() {
        return sharedDruidSqlParameterObjectPoolConfiguration;
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

        final DefaultDruidSqlClientSharedResourcesConfiguration that = (DefaultDruidSqlClientSharedResourcesConfiguration) o;
        return Objects.equals(
                sharedDruidQueryEventObjectPoolConfiguration,
                that.sharedDruidQueryEventObjectPoolConfiguration) &&
                Objects.equals(
                        sharedDruidResultSetObjectPoolConfiguration, that.sharedDruidResultSetObjectPoolConfiguration) &&
                Objects.equals(
                        sharedDruidSqlParameterObjectPoolConfiguration, that.sharedDruidSqlParameterObjectPoolConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                sharedDruidQueryEventObjectPoolConfiguration,
                sharedDruidResultSetObjectPoolConfiguration,
                sharedDruidSqlParameterObjectPoolConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultDruidSqlClientSharedResourcesConfiguration{" +
                "sharedDruidQueryEventObjectPoolConfiguration=" + sharedDruidQueryEventObjectPoolConfiguration +
                ", sharedDruidResultSetObjectPoolConfiguration=" + sharedDruidResultSetObjectPoolConfiguration +
                ", sharedDruidSqlParameterObjectPoolConfiguration=" + sharedDruidSqlParameterObjectPoolConfiguration +
                "} " + super.toString();
    }


    /**
     * Builder for {@link DefaultDruidSqlClientSharedResourcesConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder extends AbstractAppbaseManagedServiceConfiguration.Builder<Builder> {
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_DRUID_QUERY_EVENT_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_DRUID_RESULT_SET_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();
        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_SHARED_DRUID_SQL_PARAMETER_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(128)
                        .build();


        private ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidQueryEventObjectPoolConfiguration =
                DEFAULT_SHARED_DRUID_QUERY_EVENT_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidResultSetObjectPoolConfiguration =
                DEFAULT_SHARED_DRUID_RESULT_SET_OBJECT_POOL_CONFIGURATION;
        private ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidSqlParameterObjectPoolConfiguration =
                DEFAULT_SHARED_DRUID_SQL_PARAMETER_OBJECT_POOL_CONFIGURATION;


        private Builder() {
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder sharedDruidQueryEventObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidQueryEventObjectPoolConfiguration)
        {
            this.sharedDruidQueryEventObjectPoolConfiguration = sharedDruidQueryEventObjectPoolConfiguration;
            return this;
        }

        public Builder sharedDruidResultSetObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidResultSetObjectPoolConfiguration)
        {
            this.sharedDruidResultSetObjectPoolConfiguration = sharedDruidResultSetObjectPoolConfiguration;
            return this;
        }

        public Builder sharedDruidSqlParameterObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration sharedDruidSqlParameterObjectPoolConfiguration)
        {
            this.sharedDruidSqlParameterObjectPoolConfiguration = sharedDruidSqlParameterObjectPoolConfiguration;
            return this;
        }

        public DefaultDruidSqlClientSharedResourcesConfiguration build() {
            return new DefaultDruidSqlClientSharedResourcesConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
