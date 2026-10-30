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

package com.viveworks.appbase.service.web.trace.filter.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
@Priority(AppbaseServiceConstants.PRIORITY_HIGH)
public class TraceIdJaxrsRequestFilter extends AbstractJaxrsRequestFilter implements ContainerRequestFilter {
    public TraceIdJaxrsRequestFilter() {
    }

    public TraceIdJaxrsRequestFilter(final ResourceInfo resourceInfo) {
        super(resourceInfo);
    }

    @Override
    protected void filterRequest(final ContainerRequestContext requestContext) throws IOException {
        final AppbaseCore appbaseCore = appbaseCoreFromContext();
        final String traceIdKey = AppbaseServiceUtil.traceIdKey(appbaseCore);

        String traceId = MDC.get(traceIdKey);
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = cookieValue(requestContext, traceIdKey);
        }

        if (AppbaseUtil.isBlank(traceId)) {
            traceId = headerValue(requestContext, traceIdKey);
        }

        if (AppbaseUtil.isBlank(traceId)) {
            traceId = queryParameter(requestContext, traceIdKey);
        }

        if (AppbaseUtil.isBlank(traceId)) {
            final AppbaseServiceEntry appbaseServiceEntry =
                    appbaseCore.getServiceRegistry().getAppbaseService(IdService.class);
            if (appbaseServiceEntry != null) {
                final IdService idService = (IdService) appbaseServiceEntry.getService();
                traceId = idService.newId();
            }
        }

        if (AppbaseUtil.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(traceIdKey, traceId);
        requestContext.setProperty(traceIdKey, traceId);
    }
}
