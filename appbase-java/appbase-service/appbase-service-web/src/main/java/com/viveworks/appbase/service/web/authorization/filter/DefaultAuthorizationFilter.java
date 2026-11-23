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
package com.viveworks.appbase.service.web.authorization.filter;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.authorization.AuthorizationContext;
import com.viveworks.appbase.service.authorization.AuthorizationService;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAuthorizationFilter implements AuthorizationFilter {
    @Override
    public boolean isAuthorized(
            final AppbaseCore appbaseCore,
            final String service,
            @Nullable final String operation,
            @Nullable final String appbaseSessionId,
            @Nullable final String appbaseRequesterId)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(service, "service");

        final AuthorizationService authorizationService = authorizationService(appbaseCore);
        final AuthorizationContext authorizationContext = authorizationContext(appbaseSessionId, appbaseRequesterId);
        if (operation != null) {
            return authorizationService.isAuthorized(service, operation, authorizationContext);
        }

        return authorizationService.isAuthorized(service, authorizationContext);
    }

    @Override
    public boolean isAuthorized(
            final AppbaseCore appbaseCore,
            final Class<?> serviceType,
            @Nullable final String operation,
            @Nullable final String appbaseSessionId,
            @Nullable final String appbaseRequesterId)
    {
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");
        AppbaseUtil.notNull(serviceType, "serviceType");

        final AuthorizationService authorizationService = authorizationService(appbaseCore);
        final AuthorizationContext authorizationContext = authorizationContext(appbaseSessionId, appbaseRequesterId);
        if (operation != null) {
            return authorizationService.isAuthorized(serviceType, operation, authorizationContext);
        }

        return authorizationService.isAuthorized(serviceType, authorizationContext);
    }

    private AuthorizationContext authorizationContext(
            @Nullable final String appbaseSessionId, @Nullable final String appbaseRequesterId)
    {
        final AuthorizationContext authorizationContext = new AuthorizationContext();
        if (appbaseSessionId != null) {
            authorizationContext.setAppbaseSessionId(appbaseSessionId);
        }
        if (appbaseRequesterId != null) {
            authorizationContext.setAppbaseRequesterId(appbaseRequesterId);
        }

        return authorizationContext;
    }

    private AuthorizationService authorizationService(final AppbaseCore appbaseCore) {
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();

        AuthorizationService authorizationService = null;
        final AppbaseServiceEntry authorizationServiceEntry =
                serviceRegistry.getAppbaseService(AuthorizationService.class);
        if (authorizationServiceEntry != null) {
            authorizationService = (AuthorizationService) authorizationServiceEntry.getService();
        }

        if (authorizationService == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find an instance of " + AuthorizationService.class.getName());
        }

        return authorizationService;
    }
}
