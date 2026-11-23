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
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.supportmessage.DefaultSupportMessageService;
import com.viveworks.appbase.service.supportmessage.DefaultSupportMessageServiceConfiguration;
import com.viveworks.appbase.service.supportmessage.SupportMessageService;
import com.viveworks.appbase.service.supportmessage.store.SupportMessageStore;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link SupportMessageService}.
 *
 * <p>Cross-service dependencies (both retrieved from the partial {@link AppbaseServices} container):
 * <ul>
 *   <li>{@link SupportMessageStore} — register a {@link JdbiSupportMessageStoreFactory}</li>
 *   <li>{@link IdService} — register an {@link DefaultIdServiceFactory}</li>
 * </ul>
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultSupportMessageServiceFactory(SupportMessageService)}</li>
 *   <li>Config-based: {@link #DefaultSupportMessageServiceFactory(DefaultSupportMessageServiceConfiguration)}</li>
 *   <li>Default: {@link #DefaultSupportMessageServiceFactory()} — reads configuration from
 *       {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 */
public final class DefaultSupportMessageServiceFactory implements AppbaseServiceFactory<SupportMessageService> {

    @Nullable private final SupportMessageService predefinedService;
    @Nullable private final DefaultSupportMessageServiceConfiguration config;

    /** Wraps a pre-built {@link SupportMessageService} instance. */
    public DefaultSupportMessageServiceFactory(final SupportMessageService service) {
        this.predefinedService = Objects.requireNonNull(service, "service");
        this.config            = null;
    }

    /** Creates a {@link DefaultSupportMessageService} from {@code config}. */
    public DefaultSupportMessageServiceFactory(final DefaultSupportMessageServiceConfiguration config) {
        this.config            = Objects.requireNonNull(config, "config");
        this.predefinedService = null;
    }

    /**
     * Creates a {@link DefaultSupportMessageService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     */
    public DefaultSupportMessageServiceFactory() {
        this.predefinedService = null;
        this.config            = null;
    }

    @Override
    public Class<SupportMessageService> serviceType() {
        return SupportMessageService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.SUPPORT_MESSAGE.order();
    }

    @Override
    public SupportMessageService create(final AppbaseCoreServices coreServices,
                                         final AppbaseClientServices clientServices,
                                         final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultSupportMessageServiceConfiguration.class, config);
        }
        final DefaultSupportMessageServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultSupportMessageServiceConfiguration.class);
        return new DefaultSupportMessageService(
            partial.get(SupportMessageStore.class),
            partial.get(IdService.class),
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg);
    }
}
