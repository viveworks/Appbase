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

package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.service.supportmessage.store.JdbiSupportMessageStore;
import com.viveworks.appbase.service.supportmessage.store.JdbiSupportMessageStoreConfiguration;
import com.viveworks.appbase.service.supportmessage.store.SupportMessageStore;
import org.jdbi.v3.core.Handle;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link SupportMessageStore} using {@link JdbiSupportMessageStore}.
 *
 * <p>The factory accepts a {@link JdbiClientService} and opens a JDBI handle internally,
 * so callers do not need to manage handle lifecycle.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #JdbiSupportMessageStoreFactory(SupportMessageStore)}</li>
 *   <li>Config-based: {@link #JdbiSupportMessageStoreFactory(JdbiSupportMessageStoreConfiguration, JdbiClientService)}</li>
 *   <li>Default: {@link #JdbiSupportMessageStoreFactory(JdbiClientService)} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class JdbiSupportMessageStoreFactory implements AppbaseServiceFactory<SupportMessageStore> {

    @Nullable private final SupportMessageStore predefinedService;
    @Nullable private final JdbiSupportMessageStoreConfiguration config;
    @Nullable private final JdbiClientService jdbiClientService;

    /** Wraps a pre-built {@link SupportMessageStore} instance. */
    public JdbiSupportMessageStoreFactory(final SupportMessageStore service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
        this.jdbiClientService = null;
    }

    /**
     * Creates a {@link JdbiSupportMessageStore} from {@code config}, opening a JDBI handle
     * internally from the supplied {@link JdbiClientService}.
     *
     * @param config           store configuration
     * @param jdbiClientService the JDBI client service used to open a handle
     */
    public JdbiSupportMessageStoreFactory(final JdbiSupportMessageStoreConfiguration config,
                                          final JdbiClientService jdbiClientService) {
        this.config            = Objects.requireNonNull(config, "config");
        this.jdbiClientService = Objects.requireNonNull(jdbiClientService, "jdbiClientService");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link JdbiSupportMessageStore} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService},
     * opening a JDBI handle internally from the supplied {@link JdbiClientService}.
     *
     * @param jdbiClientService the JDBI client service used to open a handle
     */
    public JdbiSupportMessageStoreFactory(final JdbiClientService jdbiClientService) {
        this.jdbiClientService = Objects.requireNonNull(jdbiClientService, "jdbiClientService");
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<SupportMessageStore> serviceType() {
        return SupportMessageStore.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.SUPPORT_MESSAGE_STORE.order();
    }

    @Override
    public SupportMessageStore create(final AppbaseCoreServices coreServices,
                                       final AppbaseClientServices clientServices,
                                       final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                JdbiSupportMessageStoreConfiguration.class, config);
        }
        final JdbiSupportMessageStoreConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(JdbiSupportMessageStoreConfiguration.class);
        final Handle handle = jdbiClientService.getClient().open();
        return new JdbiSupportMessageStore(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            handle,
            cfg);
    }
}
