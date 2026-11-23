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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.core.pool.AppbaseClientPoolConfiguration;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManager;
import com.viveworks.appbase.client.core.pool.manager.AppbaseClientPoolManagerConfiguration;
import com.viveworks.appbase.core.service.configuration.AppbaseManagedServiceConfiguration;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Represents configuration for {@link AppbaseClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface AppbaseClientServiceConfiguration extends AppbaseManagedServiceConfiguration {
    /** @return whether the service registers keyed integration services in the pool registry, both those
     *         supplied at startup and those returned by a lookup, unless the key is excluded; when
     *         {@code false}, no such registration happens */
    boolean isGlobalEnableRegisterNotFoundAppbaseClientIntegrationServices();

    /** @return keys excluded from that registration (counted as disabled instead), or {@code null} when none are excluded */
    @Nullable
    Set<AppbaseClientKeyConfiguration<?>> getDisabledRegisterNotFoundAppbaseClientIntegrationServices();

    /** @return whether startup validates every configured keyed integration service */
    boolean isCheckKeyedAppbaseClientIntegrationServicesSanity();

    /** @return injected pool-manager selection, or {@code null} to try custom settings and then retain any existing manager */
    @Nullable
    InjectedTypeConfiguration<AppbaseClientPoolManager> getAppbaseClientPoolManagerInjectedTypeConfiguration();

    /** @return custom pool-manager settings used when injection is absent, or {@code null} to retain any existing manager */
    @Nullable
    AppbaseClientPoolManagerConfiguration getCustomAppbaseClientPoolManagerConfiguration();

    /** @return injected client-pool selection, or {@code null} to try custom settings and then retain any existing pool */
    @Nullable
    InjectedTypeConfiguration<AppbaseClientPool<?, ? extends ComparableValue>>
    getAppbaseClientPoolInjectedTypeConfiguration();

    /** @return custom client-pool settings used when injection is absent, or {@code null} to require an existing pool */
    @Nullable
    AppbaseClientPoolConfiguration getCustomAppbaseClientPoolConfiguration();
}
