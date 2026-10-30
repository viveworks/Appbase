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

package com.viveworks.appbase.bootstrap.service.factory;

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceFactory;
import com.viveworks.appbase.bootstrap.service.AppbaseServiceOrder;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.ContextAuthorizer;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationService;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationServiceConfiguration;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Built-in factory for {@link AuthorizationService}.
 *
 * <p>Three construction modes:
 * <ul>
 *   <li>Pre-built: {@link #DefaultAuthorizationServiceFactory(AuthorizationService)}</li>
 *   <li>Config-based: {@link #DefaultAuthorizationServiceFactory(DefaultAuthorizationServiceConfiguration, ContextAuthorizer)}</li>
 *   <li>Default: {@link #DefaultAuthorizationServiceFactory(ContextAuthorizer)} — reads configuration
 *       from {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}</li>
 * </ul>
 *
 * <p>{@link ContextAuthorizer} is always required for the config and default modes because
 * authorization logic is application-specific and cannot be derived from configuration alone.
 */
public final class DefaultAuthorizationServiceFactory implements AppbaseServiceFactory<AuthorizationService> {

    @Nullable private final AuthorizationService predefinedService;
    @Nullable private final DefaultAuthorizationServiceConfiguration config;
    @Nullable private final ContextAuthorizer contextAuthorizer;

    /** Wraps a pre-built {@link AuthorizationService} instance. */
    public DefaultAuthorizationServiceFactory(final AuthorizationService service) {
        this.predefinedService  = Objects.requireNonNull(service, "service");
        this.config             = null;
        this.contextAuthorizer  = null;
    }

    /** Creates a {@link DefaultAuthorizationService} from {@code config} and {@code contextAuthorizer}. */
    public DefaultAuthorizationServiceFactory(final DefaultAuthorizationServiceConfiguration config,
                                        final ContextAuthorizer contextAuthorizer) {
        this.config             = Objects.requireNonNull(config, "config");
        this.contextAuthorizer  = Objects.requireNonNull(contextAuthorizer, "contextAuthorizer");
        this.predefinedService  = null;
    }

    /**
     * Creates a {@link DefaultAuthorizationService} reading configuration from the
     * {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
     *
     * @param contextAuthorizer the application-specific authorizer (must not be null)
     */
    public DefaultAuthorizationServiceFactory(final ContextAuthorizer contextAuthorizer) {
        this.contextAuthorizer  = Objects.requireNonNull(contextAuthorizer, "contextAuthorizer");
        this.predefinedService  = null;
        this.config             = null;
    }

    @Override
    public Class<AuthorizationService> serviceType() {
        return AuthorizationService.class;
    }

    @Override
    public int initializationOrder() {
        return AppbaseServiceOrder.AUTHORIZATION.order();
    }

    @Override
    public AuthorizationService create(final AppbaseCoreServices coreServices,
                                        final AppbaseClientServices clientServices,
                                        final AppbaseServices partial) throws AppbaseException {
        if (predefinedService != null) {
            return predefinedService;
        }
        if (config != null) {
            coreServices.getConfigurationService().putTypedConfiguration(
                DefaultAuthorizationServiceConfiguration.class, config);
        }
        final DefaultAuthorizationServiceConfiguration cfg = coreServices.getConfigurationService()
            .getTypedConfiguration(DefaultAuthorizationServiceConfiguration.class);
        return new DefaultAuthorizationService(
            coreServices.getLogService(),
            coreServices.getMetricService(),
            coreServices.getMediatorService(),
            coreServices.getServiceRegistry(),
            cfg,
            contextAuthorizer);
    }
}
