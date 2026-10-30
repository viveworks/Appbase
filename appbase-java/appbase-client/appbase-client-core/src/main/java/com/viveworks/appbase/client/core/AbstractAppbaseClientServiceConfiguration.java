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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.AppbaseClientPoolConfiguration;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManagerConfiguration;
import com.viveworks.appbase.core.service.configuration.AbstractAppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract representation of {@link AppbaseClientServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @param <T> {@link AbstractAppbaseClientServiceConfiguration.Builder} type
 * @since 1.0.0
 */
public abstract class AbstractAppbaseClientServiceConfiguration
        <T extends AbstractAppbaseClientServiceConfiguration.Builder<T>>
        extends AbstractAppbaseManagedServiceConfiguration implements AppbaseClientServiceConfiguration
{
    @JsonProperty("globalEnableRegisterNotFoundAppbaseClientIntegrationServices")
    private final boolean globalEnableRegisterNotFoundAppbaseClientIntegrationServices;

    @Nullable
    @JsonProperty("disabledRegisterNotFoundAppbaseClientIntegrationServices")
    private final Set<AppbaseClientKeyConfiguration<?>> disabledRegisterNotFoundAppbaseClientIntegrationServices;

    @JsonProperty("checkKeyedAppbaseClientIntegrationServicesSanity")
    private final boolean checkKeyedAppbaseClientIntegrationServicesSanity;

    @Nullable
    @JsonProperty("appbaseClientPoolManagerInjectedTypeConfiguration")
    private final InjectedTypeConfiguration<AppbaseClientPoolManager> appbaseClientPoolManagerInjectedTypeConfiguration;

    @Nullable
    @JsonProperty("customAppbaseClientPoolManagerConfiguration")
    private final AppbaseClientPoolManagerConfiguration customAppbaseClientPoolManagerConfiguration;

    @Nullable
    @JsonProperty("appbaseClientPoolInjectedTypeConfiguration")
    private final InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
            appbaseClientPoolInjectedTypeConfiguration;

    @Nullable
    @JsonProperty("customAppbaseClientPoolConfiguration")
    private final AppbaseClientPoolConfiguration customAppbaseClientPoolConfiguration;


    /**
     * Copies the settings accumulated by a builder. The builder validates its own required values;
     * this constructor does not re-check them.
     *
     * @param builder builder holding the client-service settings to copy
     */
    protected AbstractAppbaseClientServiceConfiguration(final Builder<T> builder) {
        super(builder);

        globalEnableRegisterNotFoundAppbaseClientIntegrationServices =
                builder.globalEnableRegisterNotFoundAppbaseClientIntegrationServices;
        disabledRegisterNotFoundAppbaseClientIntegrationServices =
                builder.disabledRegisterNotFoundAppbaseClientIntegrationServices;
        checkKeyedAppbaseClientIntegrationServicesSanity = builder.checkKeyedAppbaseClientIntegrationServicesSanity;
        appbaseClientPoolManagerInjectedTypeConfiguration = builder.appbaseClientPoolManagerInjectedTypeConfiguration;
        customAppbaseClientPoolManagerConfiguration = builder.customAppbaseClientPoolManagerConfiguration;
        appbaseClientPoolInjectedTypeConfiguration = builder.appbaseClientPoolInjectedTypeConfiguration;
        customAppbaseClientPoolConfiguration = builder.customAppbaseClientPoolConfiguration;

    }

    /** {@inheritDoc} */
    @Override
    public boolean isGlobalEnableRegisterNotFoundAppbaseClientIntegrationServices() {
        return globalEnableRegisterNotFoundAppbaseClientIntegrationServices;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public Set<AppbaseClientKeyConfiguration<?>> getDisabledRegisterNotFoundAppbaseClientIntegrationServices() {
        return disabledRegisterNotFoundAppbaseClientIntegrationServices;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCheckKeyedAppbaseClientIntegrationServicesSanity() {
        return checkKeyedAppbaseClientIntegrationServicesSanity;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public InjectedTypeConfiguration<AppbaseClientPoolManager> getAppbaseClientPoolManagerInjectedTypeConfiguration() {
        return appbaseClientPoolManagerInjectedTypeConfiguration;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public AppbaseClientPoolManagerConfiguration getCustomAppbaseClientPoolManagerConfiguration() {
        return customAppbaseClientPoolManagerConfiguration;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
    getAppbaseClientPoolInjectedTypeConfiguration()
    {
        return appbaseClientPoolInjectedTypeConfiguration;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public AppbaseClientPoolConfiguration getCustomAppbaseClientPoolConfiguration() {
        return customAppbaseClientPoolConfiguration;
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

        final AbstractAppbaseClientServiceConfiguration that = (AbstractAppbaseClientServiceConfiguration) o;
        return globalEnableRegisterNotFoundAppbaseClientIntegrationServices ==
                        that.globalEnableRegisterNotFoundAppbaseClientIntegrationServices &&
                checkKeyedAppbaseClientIntegrationServicesSanity == that.checkKeyedAppbaseClientIntegrationServicesSanity &&
                Objects.equals(
                        disabledRegisterNotFoundAppbaseClientIntegrationServices,
                        that.disabledRegisterNotFoundAppbaseClientIntegrationServices) &&
                Objects.equals(
                        appbaseClientPoolManagerInjectedTypeConfiguration,
                        that.appbaseClientPoolManagerInjectedTypeConfiguration) &&
                Objects.equals(
                        customAppbaseClientPoolManagerConfiguration,
                        that.customAppbaseClientPoolManagerConfiguration) &&
                Objects.equals(
                        appbaseClientPoolInjectedTypeConfiguration,
                        that.appbaseClientPoolInjectedTypeConfiguration) &&
                Objects.equals(customAppbaseClientPoolConfiguration, that.customAppbaseClientPoolConfiguration);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(
                globalEnableRegisterNotFoundAppbaseClientIntegrationServices,
                disabledRegisterNotFoundAppbaseClientIntegrationServices,
                checkKeyedAppbaseClientIntegrationServicesSanity,
                appbaseClientPoolManagerInjectedTypeConfiguration,
                customAppbaseClientPoolManagerConfiguration,
                appbaseClientPoolInjectedTypeConfiguration,
                customAppbaseClientPoolConfiguration);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AbstractAppbaseClientServiceConfiguration{" +
                "globalEnableRegisterNotFoundAppbaseClientIntegrationServices=" +
                globalEnableRegisterNotFoundAppbaseClientIntegrationServices +
                ", disabledRegisterNotFoundAppbaseClientIntegrationServices=" +
                disabledRegisterNotFoundAppbaseClientIntegrationServices +
                ", checkDefaultAppbaseClientIntegrationServiceSanity=" +
                ", appbaseClientPoolManagerInjectedTypeConfiguration=" + appbaseClientPoolManagerInjectedTypeConfiguration +
                ", customAppbaseClientPoolManagerConfiguration=" + customAppbaseClientPoolManagerConfiguration +
                ", appbaseClientPoolInjectedTypeConfiguration=" + appbaseClientPoolInjectedTypeConfiguration +
                ", customAppbaseClientPoolConfiguration=" + customAppbaseClientPoolConfiguration +
                '}';
    }

    /**
     * Builder for client-service selection and registration policies.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public abstract static class Builder<T extends Builder<T>>
            extends AbstractAppbaseManagedServiceConfiguration.Builder<T>
    {
        private static final boolean DEFAULT_GLOBAL_ENABLE_REGISTER_NOT_FOUND_APPBASE_CLIENT_INTEGRATION_SERVICES = true;
        private static final boolean DEFAULT_CHECK_KEYED_APPBASE_CLIENT_INTEGRATION_SERVICES_SANITY = false;



        @JsonProperty("globalEnableRegisterNotFoundAppbaseClientIntegrationServices")
        private boolean globalEnableRegisterNotFoundAppbaseClientIntegrationServices =
                DEFAULT_GLOBAL_ENABLE_REGISTER_NOT_FOUND_APPBASE_CLIENT_INTEGRATION_SERVICES;

        @JsonProperty("checkKeyedAppbaseClientIntegrationServicesSanity")
        private boolean checkKeyedAppbaseClientIntegrationServicesSanity =
                DEFAULT_CHECK_KEYED_APPBASE_CLIENT_INTEGRATION_SERVICES_SANITY;


        @Nullable
        @JsonProperty("disabledRegisterNotFoundAppbaseClientIntegrationServices")
        private Set<AppbaseClientKeyConfiguration<?>> disabledRegisterNotFoundAppbaseClientIntegrationServices;

        @Nullable
        @JsonProperty("appbaseClientPoolManagerInjectedTypeConfiguration")
        private InjectedTypeConfiguration<AppbaseClientPoolManager> appbaseClientPoolManagerInjectedTypeConfiguration;

        @Nullable
        @JsonProperty("customAppbaseClientPoolManagerConfiguration")
        private AppbaseClientPoolManagerConfiguration customAppbaseClientPoolManagerConfiguration;

        @Nullable
        @JsonProperty("appbaseClientPoolInjectedTypeConfiguration")
        private InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
                appbaseClientPoolInjectedTypeConfiguration;

        @Nullable
        @JsonProperty("customAppbaseClientPoolConfiguration")
        private AppbaseClientPoolConfiguration customAppbaseClientPoolConfiguration;



        protected abstract T getThis();



        protected Builder() {
        }

        public boolean isGlobalEnableRegisterNotFoundAppbaseClientIntegrationServices() {
            return globalEnableRegisterNotFoundAppbaseClientIntegrationServices;
        }

        public T globalEnableRegisterNotFoundAppbaseClientIntegrationServices(
                final boolean globalEnableRegisterNotFoundAppbaseClientIntegrationServices)
        {
            this.globalEnableRegisterNotFoundAppbaseClientIntegrationServices =
                    globalEnableRegisterNotFoundAppbaseClientIntegrationServices;
            return getThis();
        }

        @Nullable
        public Set<AppbaseClientKeyConfiguration<?>> getDisabledRegisterNotFoundAppbaseClientIntegrationServices() {
            return disabledRegisterNotFoundAppbaseClientIntegrationServices;
        }

        public T disabledRegisterNotFoundAppbaseClientIntegrationServices(
                final Set<AppbaseClientKeyConfiguration<?>> disabledRegisterNotFoundAppbaseClientIntegrationServices)
        {
            this.disabledRegisterNotFoundAppbaseClientIntegrationServices =
                    disabledRegisterNotFoundAppbaseClientIntegrationServices;
            return getThis();
        }

        public boolean isCheckKeyedAppbaseClientIntegrationServicesSanity() {
            return checkKeyedAppbaseClientIntegrationServicesSanity;
        }

        public T checkKeyedAppbaseClientIntegrationServicesSanity(
                final boolean checkKeyedAppbaseClientIntegrationServicesSanity)
        {
            this.checkKeyedAppbaseClientIntegrationServicesSanity = checkKeyedAppbaseClientIntegrationServicesSanity;
            return getThis();
        }

        @Nullable
        public InjectedTypeConfiguration<AppbaseClientPoolManager> getAppbaseClientPoolManagerInjectedTypeConfiguration() {
            return appbaseClientPoolManagerInjectedTypeConfiguration;
        }

        public T appbaseClientPoolManagerInjectedTypeConfiguration(
                final InjectedTypeConfiguration<AppbaseClientPoolManager> appbaseClientPoolManagerInjectedTypeConfiguration)
        {
            this.appbaseClientPoolManagerInjectedTypeConfiguration = appbaseClientPoolManagerInjectedTypeConfiguration;
            return getThis();
        }

        @Nullable
        public AppbaseClientPoolManagerConfiguration getCustomAppbaseClientPoolManagerConfiguration() {
            return customAppbaseClientPoolManagerConfiguration;
        }

        public T customAppbaseClientPoolManagerConfiguration(
                final AppbaseClientPoolManagerConfiguration customAppbaseClientPoolManagerConfiguration)
        {
            this.customAppbaseClientPoolManagerConfiguration = customAppbaseClientPoolManagerConfiguration;
            return getThis();
        }

        @Nullable
        public InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
        getAppbaseClientPoolInjectedTypeConfiguration()
        {
            return appbaseClientPoolInjectedTypeConfiguration;
        }

        public T appbaseClientPoolInjectedTypeConfiguration(
                final InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
                        appbaseClientPoolInjectedTypeConfiguration)
        {
            this.appbaseClientPoolInjectedTypeConfiguration = appbaseClientPoolInjectedTypeConfiguration;
            return getThis();
        }

        @Nullable
        public AppbaseClientPoolConfiguration getCustomAppbaseClientPoolConfiguration() {
            return customAppbaseClientPoolConfiguration;
        }

        public T customAppbaseClientPoolConfiguration(
                final AppbaseClientPoolConfiguration customAppbaseClientPoolConfiguration)
        {
            this.customAppbaseClientPoolConfiguration = customAppbaseClientPoolConfiguration;
            return getThis();
        }
    }
    // CheckStyle: Builder end
}
