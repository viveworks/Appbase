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

package com.viveworks.appbase.client.core.pool.manager;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.core.service.mediator.SchedulableConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ScheduledExecutorServiceConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.queued.ManyToManyQueuedFixedObjectPoolConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link DefaultAppbaseClientPoolManager}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultAppbaseClientPoolManagerConfiguration.Builder.class)
public final class DefaultAppbaseClientPoolManagerConfiguration implements AppbaseClientPoolManagerConfiguration {
    private final boolean unhealthyAppbaseClientIntegrationServiceManagementDisabled;

    private final SchedulableConfiguration clientPoolCheckerTaskSchedulableConfiguration;

    private final SchedulableConfiguration clientIntegrationServiceRepairTaskSchedulableConfiguration;

    private final boolean useMediatorSharedScheduler;

    private final ScheduledExecutorServiceConfiguration customScheduledExecutorServiceConfiguration;

    @JsonSerialize(contentAs = AppbaseClientKeyConfiguration.class)
    @JsonDeserialize(contentAs = AppbaseClientKeyConfiguration.class)
    private final Set<AppbaseClientKeyConfiguration<?>> repairDisabledAppbaseClientKeyConfigurations;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private final Set<String> repairDisabledAppbaseClientIntegrationServiceTypeNames;

    private final boolean enableGlobalMaxRepairAttemptsCheck;

    private final int globalMaxRepairAttempts;

    @JsonSerialize(contentAs = MaxRepairAttempsConfiguration.class)
    @JsonDeserialize(contentAs = MaxRepairAttempsConfiguration.class)
    private final Set<MaxRepairAttempsConfiguration> maxRepairAttemptsByAppbaseClientKeyConfigurations;

    @JsonSerialize(keyAs = String.class, contentAs = Integer.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Integer.class)
    private final Map<String, Integer> maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap;

    private final boolean globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices;

    @JsonSerialize(contentAs = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.class)
    @JsonDeserialize(contentAs = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.class)
    private final Set<DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration>
            disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations;

    @JsonSerialize(keyAs = String.class, contentAs = Boolean.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Boolean.class)
    private final Map<String, Boolean> disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap;

    private final ManyToManyQueuedFixedObjectPoolConfiguration repairListenerImplObjectPoolConfiguration;

    private final ManyToManyQueuedFixedObjectPoolConfiguration unhealthyClientIntegrationServiceEntryObjectPoolConfiguration;

    private DefaultAppbaseClientPoolManagerConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (!builder.unhealthyAppbaseClientIntegrationServiceManagementDisabled) {
            AppbaseUtil.notNull(
                    builder.clientPoolCheckerTaskSchedulableConfiguration,
                    "builder.clientPoolCheckerTaskSchedulableConfiguration");
            AppbaseUtil.notNull(
                    builder.clientIntegrationServiceRepairTaskSchedulableConfiguration,
                    "builder.clientIntegrationServiceRepairTaskSchedulableConfiguration");

            if (!builder.useMediatorSharedScheduler) {
                AppbaseUtil.notNull(
                        builder.customScheduledExecutorServiceConfiguration,
                        "builder.customScheduledExecutorServiceConfiguration");
            }

            AppbaseUtil.positiveOrZero(builder.globalMaxRepairAttempts, "builder.globalMaxRepairAttempts");

            AppbaseUtil.notNull(
                    builder.repairListenerImplObjectPoolConfiguration,
                    "builder.repairListenerImplObjectPoolConfiguration");
            AppbaseUtil.notNull(
                    builder.unhealthyClientIntegrationServiceEntryObjectPoolConfiguration,
                    "builder.unhealthyClientIntegrationServiceEntryObjectPoolConfiguration");
        }

        unhealthyAppbaseClientIntegrationServiceManagementDisabled =
                builder.unhealthyAppbaseClientIntegrationServiceManagementDisabled;
        clientPoolCheckerTaskSchedulableConfiguration =
                builder.clientPoolCheckerTaskSchedulableConfiguration;
        clientIntegrationServiceRepairTaskSchedulableConfiguration =
                builder.clientIntegrationServiceRepairTaskSchedulableConfiguration;
        useMediatorSharedScheduler =
                builder.useMediatorSharedScheduler;
        customScheduledExecutorServiceConfiguration =
                builder.customScheduledExecutorServiceConfiguration;
        repairDisabledAppbaseClientKeyConfigurations =
                builder.repairDisabledAppbaseClientKeyConfigurations;
        repairDisabledAppbaseClientIntegrationServiceTypeNames =
                builder.repairDisabledAppbaseClientIntegrationServiceTypeNames;
        enableGlobalMaxRepairAttemptsCheck =
                builder.enableGlobalMaxRepairAttemptsCheck;
        globalMaxRepairAttempts =
                builder.globalMaxRepairAttempts;
        maxRepairAttemptsByAppbaseClientKeyConfigurations =
                builder.maxRepairAttemptsByAppbaseClientKeyConfigurations;
        maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap =
                builder.maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap;
        globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices =
                builder.globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices;
        disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations =
                builder.disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations;
        disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap =
                builder.disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap;
        repairListenerImplObjectPoolConfiguration =
                builder.repairListenerImplObjectPoolConfiguration;
        unhealthyClientIntegrationServiceEntryObjectPoolConfiguration =
                builder.unhealthyClientIntegrationServiceEntryObjectPoolConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isUnhealthyAppbaseClientIntegrationServiceManagementDisabled() {
        return unhealthyAppbaseClientIntegrationServiceManagementDisabled;
    }

    public SchedulableConfiguration getClientPoolCheckerTaskSchedulableConfiguration() {
        return clientPoolCheckerTaskSchedulableConfiguration;
    }

    public SchedulableConfiguration getClientIntegrationServiceRepairTaskSchedulableConfiguration() {
        return clientIntegrationServiceRepairTaskSchedulableConfiguration;
    }

    public boolean isUseMediatorSharedScheduler() {
        return useMediatorSharedScheduler;
    }

    public ScheduledExecutorServiceConfiguration getCustomScheduledExecutorServiceConfiguration() {
        return customScheduledExecutorServiceConfiguration;
    }

    public Set<AppbaseClientKeyConfiguration<?>> getRepairDisabledAppbaseClientKeyConfigurations() {
        return repairDisabledAppbaseClientKeyConfigurations;
    }

    public Set<String> getRepairDisabledAppbaseClientIntegrationServiceTypeNames() {
        return repairDisabledAppbaseClientIntegrationServiceTypeNames;
    }

    public boolean isEnableGlobalMaxRepairAttemptsCheck() {
        return enableGlobalMaxRepairAttemptsCheck;
    }

    public int getGlobalMaxRepairAttempts() {
        return globalMaxRepairAttempts;
    }

    public Set<MaxRepairAttempsConfiguration> getMaxRepairAttemptsByAppbaseClientKeyConfigurations() {
        return maxRepairAttemptsByAppbaseClientKeyConfigurations;
    }

    public Map<String, Integer> getMaxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap() {
        return maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap;
    }

    public boolean isGlobalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices() {
        return globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices;
    }

    public Set<DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration>
    getDisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations()
    {
        return disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations;
    }

    public Map<String, Boolean> getDisableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap() {
        return disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap;
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getRepairListenerImplObjectPoolConfiguration() {
        return repairListenerImplObjectPoolConfiguration;
    }

    public ManyToManyQueuedFixedObjectPoolConfiguration getUnhealthyClientIntegrationServiceEntryObjectPoolConfiguration() {
        return unhealthyClientIntegrationServiceEntryObjectPoolConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DefaultAppbaseClientPoolManagerConfiguration that = (DefaultAppbaseClientPoolManagerConfiguration) o;
        return unhealthyAppbaseClientIntegrationServiceManagementDisabled ==
                that.unhealthyAppbaseClientIntegrationServiceManagementDisabled &&
                useMediatorSharedScheduler == that.useMediatorSharedScheduler &&
                enableGlobalMaxRepairAttemptsCheck == that.enableGlobalMaxRepairAttemptsCheck &&
                globalMaxRepairAttempts == that.globalMaxRepairAttempts &&
                globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices ==
                        that.globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices &&
                Objects.equals(
                        clientPoolCheckerTaskSchedulableConfiguration,
                        that.clientPoolCheckerTaskSchedulableConfiguration) &&
                Objects.equals(
                        clientIntegrationServiceRepairTaskSchedulableConfiguration,
                        that.clientIntegrationServiceRepairTaskSchedulableConfiguration) &&
                Objects.equals(
                        customScheduledExecutorServiceConfiguration, that.customScheduledExecutorServiceConfiguration) &&
                Objects.equals(
                        repairDisabledAppbaseClientKeyConfigurations, that.repairDisabledAppbaseClientKeyConfigurations) &&
                Objects.equals(
                        repairDisabledAppbaseClientIntegrationServiceTypeNames,
                        that.repairDisabledAppbaseClientIntegrationServiceTypeNames) &&
                Objects.equals(
                        maxRepairAttemptsByAppbaseClientKeyConfigurations,
                        that.maxRepairAttemptsByAppbaseClientKeyConfigurations) &&
                Objects.equals(
                        maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap,
                        that.maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap) &&
                Objects.equals(
                        disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations,
                        that.disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations) &&
                Objects.equals(
                        disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap,
                        that.disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap) &&
                Objects.equals(
                        repairListenerImplObjectPoolConfiguration,
                        that.repairListenerImplObjectPoolConfiguration) &&
                Objects.equals(
                        unhealthyClientIntegrationServiceEntryObjectPoolConfiguration,
                        that.unhealthyClientIntegrationServiceEntryObjectPoolConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unhealthyAppbaseClientIntegrationServiceManagementDisabled,
                clientPoolCheckerTaskSchedulableConfiguration,
                clientIntegrationServiceRepairTaskSchedulableConfiguration,
                useMediatorSharedScheduler,
                customScheduledExecutorServiceConfiguration,
                repairDisabledAppbaseClientKeyConfigurations,
                repairDisabledAppbaseClientIntegrationServiceTypeNames,
                enableGlobalMaxRepairAttemptsCheck,
                globalMaxRepairAttempts,
                maxRepairAttemptsByAppbaseClientKeyConfigurations,
                maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap,
                globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices,
                disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations,
                disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap,
                repairListenerImplObjectPoolConfiguration,
                unhealthyClientIntegrationServiceEntryObjectPoolConfiguration);
    }

    @Override
    public String toString() {
        return "DefaultAppbaseClientPoolManagerConfiguration{" +
                "unhealthyAppbaseClientIntegrationServiceManagementDisabled=" +
                unhealthyAppbaseClientIntegrationServiceManagementDisabled +
                ", clientPoolCheckerTaskSchedulableConfiguration=" + clientPoolCheckerTaskSchedulableConfiguration +
                ", clientIntegrationServiceRepairTaskSchedulableConfiguration=" +
                clientIntegrationServiceRepairTaskSchedulableConfiguration +
                ", useMediatorSharedScheduler=" + useMediatorSharedScheduler +
                ", customScheduledExecutorServiceConfiguration=" + customScheduledExecutorServiceConfiguration +
                ", repairDisabledAppbaseClientKeyConfigurations=" + repairDisabledAppbaseClientKeyConfigurations +
                ", repairDisabledAppbaseClientIntegrationServiceTypeNames=" +
                repairDisabledAppbaseClientIntegrationServiceTypeNames +
                ", enableGlobalMaxRepairAttemptsCheck=" + enableGlobalMaxRepairAttemptsCheck +
                ", globalMaxRepairAttempts=" + globalMaxRepairAttempts +
                ", maxRepairAttemptsByAppbaseClientKeyConfigurations=" +
                maxRepairAttemptsByAppbaseClientKeyConfigurations +
                ", maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap=" +
                maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap +
                ", globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices=" +
                globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices +
                ", disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations=" +
                disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations +
                ", disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap=" +
                disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap +
                ", repairListenerImplObjectPoolConfiguration=" + repairListenerImplObjectPoolConfiguration +
                ", unhealthyClientIntegrationServiceEntryObjectPoolConfiguration=" +
                unhealthyClientIntegrationServiceEntryObjectPoolConfiguration +
                '}';
    }

    /**
     * Builder for {@link DefaultAppbaseClientPoolManagerConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_UNHEALTHY_APPBASE_CLIENT_INTEGRATION_SERVICE_MANAGEMENT_DISABLED = false;

        private static final SchedulableConfiguration
                DEFAULT_CLIENT_POOL_CHECKER_TASK_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration
                        .builder()
                        .enabled(true)
                        .initialDelay(0L)
                        .delay(30L)
                        .timeUnit(TimeUnit.SECONDS)
                        .build();

        private static final SchedulableConfiguration
                DEFAULT_CLIENT_INTEGRATION_SERVICE_REPAIR_TASK_SCHEDULABLE_CONFIGURATION =
                SchedulableConfiguration
                        .builder()
                        .enabled(true)
                        .initialDelay(60L)
                        .delay(30L)
                        .timeUnit(TimeUnit.SECONDS)
                        .build();

        private static final boolean DEFAULT_USE_MEDIATOR_SHARED_SCHEDULER = true;

        private static final boolean DEFAULT_ENABLE_GLOBAL_MAX_REPAIR_ATTEMPTS_CHECK = true;

        private static final int DEFAULT_GLOBAL_MAX_REPAIR_ATTEMPTS = 3;

        private static final boolean
                DEFAULT_GLOBAL_DISABLE_UNREGISTER_UNHEALTHY_IRREPARABLE_APPBASE_CLIENT_INTEGRATION_SERVICES = false;

        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_REPAIR_LISTENER_IMPL_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(4096)
                        .build();

        private static final ManyToManyQueuedFixedObjectPoolConfiguration
                DEFAULT_UNHEALTHY_CLIENT_INTEGRATION_SERVICE_ENTRY_OBJECT_POOL_CONFIGURATION =
                ManyToManyQueuedFixedObjectPoolConfiguration
                        .builder()
                        .enabled(true)
                        .objectPoolCapacity(4096)
                        .build();




        private boolean unhealthyAppbaseClientIntegrationServiceManagementDisabled =
                DEFAULT_UNHEALTHY_APPBASE_CLIENT_INTEGRATION_SERVICE_MANAGEMENT_DISABLED;

        private SchedulableConfiguration clientPoolCheckerTaskSchedulableConfiguration =
                DEFAULT_CLIENT_POOL_CHECKER_TASK_SCHEDULABLE_CONFIGURATION;

        private SchedulableConfiguration clientIntegrationServiceRepairTaskSchedulableConfiguration =
                DEFAULT_CLIENT_INTEGRATION_SERVICE_REPAIR_TASK_SCHEDULABLE_CONFIGURATION;

        private boolean useMediatorSharedScheduler = DEFAULT_USE_MEDIATOR_SHARED_SCHEDULER;

        private ScheduledExecutorServiceConfiguration customScheduledExecutorServiceConfiguration;

        @JsonSerialize(contentAs = AppbaseClientKeyConfiguration.class)
        @JsonDeserialize(contentAs = AppbaseClientKeyConfiguration.class)
        private Set<AppbaseClientKeyConfiguration<?>> repairDisabledAppbaseClientKeyConfigurations;

        @JsonSerialize(contentAs = String.class)
        @JsonDeserialize(contentAs = String.class)
        private Set<String> repairDisabledAppbaseClientIntegrationServiceTypeNames;

        private boolean enableGlobalMaxRepairAttemptsCheck = DEFAULT_ENABLE_GLOBAL_MAX_REPAIR_ATTEMPTS_CHECK;

        private int globalMaxRepairAttempts = DEFAULT_GLOBAL_MAX_REPAIR_ATTEMPTS;

        @JsonSerialize(contentAs = MaxRepairAttempsConfiguration.class)
        @JsonDeserialize(contentAs = MaxRepairAttempsConfiguration.class)
        private Set<MaxRepairAttempsConfiguration> maxRepairAttemptsByAppbaseClientKeyConfigurations;

        @JsonSerialize(keyAs = String.class, contentAs = Integer.class)
        @JsonDeserialize(keyAs = String.class, contentAs = Integer.class)
        private Map<String, Integer> maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap;

        private boolean globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices =
                DEFAULT_GLOBAL_DISABLE_UNREGISTER_UNHEALTHY_IRREPARABLE_APPBASE_CLIENT_INTEGRATION_SERVICES;

        @JsonSerialize(contentAs = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.class)
        @JsonDeserialize(contentAs = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.class)
        private Set<DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration>
                disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations;

        @JsonSerialize(keyAs = String.class, contentAs = Boolean.class)
        @JsonDeserialize(keyAs = String.class, contentAs = Boolean.class)
        private Map<String, Boolean> disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap;

        private ManyToManyQueuedFixedObjectPoolConfiguration repairListenerImplObjectPoolConfiguration =
                DEFAULT_REPAIR_LISTENER_IMPL_OBJECT_POOL_CONFIGURATION;

        private ManyToManyQueuedFixedObjectPoolConfiguration unhealthyClientIntegrationServiceEntryObjectPoolConfiguration =
                DEFAULT_UNHEALTHY_CLIENT_INTEGRATION_SERVICE_ENTRY_OBJECT_POOL_CONFIGURATION;



        private Builder() {
        }

        public Builder unhealthyAppbaseClientIntegrationServiceManagementDisabled(
                final boolean unhealthyAppbaseClientIntegrationServiceManagementDisabled)
        {
            this.unhealthyAppbaseClientIntegrationServiceManagementDisabled =
                    unhealthyAppbaseClientIntegrationServiceManagementDisabled;
            return this;
        }

        public Builder clientPoolCheckerTaskSchedulableConfiguration(
                final SchedulableConfiguration clientPoolCheckerTaskSchedulableConfiguration)
        {
            this.clientPoolCheckerTaskSchedulableConfiguration = clientPoolCheckerTaskSchedulableConfiguration;
            return this;
        }

        public Builder clientIntegrationServiceRepairTaskSchedulableConfiguration(
                final SchedulableConfiguration clientIntegrationServiceRepairTaskSchedulableConfiguration)
        {
            this.clientIntegrationServiceRepairTaskSchedulableConfiguration =
                    clientIntegrationServiceRepairTaskSchedulableConfiguration;
            return this;
        }

        public Builder useMediatorSharedScheduler(final boolean useMediatorSharedScheduler) {
            this.useMediatorSharedScheduler = useMediatorSharedScheduler;
            return this;
        }

        public Builder customScheduledExecutorServiceConfiguration(
                final ScheduledExecutorServiceConfiguration customScheduledExecutorServiceConfiguration)
        {
            this.customScheduledExecutorServiceConfiguration = customScheduledExecutorServiceConfiguration;
            return this;
        }

        public Builder repairDisabledAppbaseClientKeyConfigurations(
                @JsonSerialize(contentAs = AppbaseClientKeyConfiguration.class)
                @JsonDeserialize(contentAs = AppbaseClientKeyConfiguration.class)
                final Set<AppbaseClientKeyConfiguration<?>> repairDisabledAppbaseClientKeyConfigurations)
        {
            this.repairDisabledAppbaseClientKeyConfigurations = repairDisabledAppbaseClientKeyConfigurations;
            return this;
        }

        public Builder repairDisabledAppbaseClientIntegrationServiceTypeNames(
                @JsonSerialize(contentAs = String.class)
                @JsonDeserialize(contentAs = String.class)
                final Set<String> repairDisabledAppbaseClientIntegrationServiceTypeNames)
        {
            this.repairDisabledAppbaseClientIntegrationServiceTypeNames =
                    repairDisabledAppbaseClientIntegrationServiceTypeNames;
            return this;
        }

        public Builder enableGlobalMaxRepairAttemptsCheck(final boolean enableGlobalMaxRepairAttemptsCheck) {
            this.enableGlobalMaxRepairAttemptsCheck = enableGlobalMaxRepairAttemptsCheck;
            return this;
        }

        public Builder globalMaxRepairAttempts(final int globalMaxRepairAttempts) {
            this.globalMaxRepairAttempts = globalMaxRepairAttempts;
            return this;
        }

        public Builder maxRepairAttemptsByAppbaseClientKeyConfigurationMap(
                @JsonSerialize(contentAs = MaxRepairAttempsConfiguration.class)
                @JsonDeserialize(contentAs = MaxRepairAttempsConfiguration.class)
                final Set<MaxRepairAttempsConfiguration> maxRepairAttemptsByAppbaseClientKeyConfigurations)
        {
            this.maxRepairAttemptsByAppbaseClientKeyConfigurations = maxRepairAttemptsByAppbaseClientKeyConfigurations;
            return this;
        }

        public Builder maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap(
                @JsonSerialize(keyAs = String.class, contentAs = Integer.class)
                @JsonDeserialize(keyAs = String.class, contentAs = Integer.class)
                final Map<String, Integer> maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap)
        {
            this.maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap =
                    maxRepairAttemptsByAppbaseClientIntegrationServiceTypeNameMap;
            return this;
        }

        public Builder globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices(
                final boolean globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices)
        {
            this.globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices =
                    globalDisableUnregisterUnhealthyIrreparableAppbaseClientIntegrationServices;
            return this;
        }

        public Builder disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurationMap(
                @JsonSerialize(contentAs = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.class)
                @JsonDeserialize(contentAs = DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration.class)
                final Set<DisableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfiguration>
                        disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations)
        {
            this.disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations =
                    disableUnregisterUnhealthyIrreparableByAppbaseClientKeyConfigurations;
            return this;
        }

        public Builder disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap(
                @JsonSerialize(keyAs = String.class, contentAs = Boolean.class)
                @JsonDeserialize(keyAs = String.class, contentAs = Boolean.class)
                final Map<String, Boolean> disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap)
        {
            this.disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap =
                    disableUnregisterUnhealthyIrreparableByAppbaseClientIntegrationServiceTypeNameMap;
            return this;
        }

        public Builder repairListenerImplObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration repairListenerImplObjectPoolConfiguration)
        {
            this.repairListenerImplObjectPoolConfiguration = repairListenerImplObjectPoolConfiguration;
            return this;
        }

        public Builder unhealthyClientIntegrationServiceEntryObjectPoolConfiguration(
                final ManyToManyQueuedFixedObjectPoolConfiguration
                        unhealthyClientIntegrationServiceEntryObjectPoolConfiguration)
        {
            this.unhealthyClientIntegrationServiceEntryObjectPoolConfiguration =
                    unhealthyClientIntegrationServiceEntryObjectPoolConfiguration;
            return this;
        }

        public DefaultAppbaseClientPoolManagerConfiguration build() {
            return new DefaultAppbaseClientPoolManagerConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
