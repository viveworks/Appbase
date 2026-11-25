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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
public class TraceIdJaxrsResponseFilter extends AbstractJaxrsResponseFilter implements ContainerResponseFilter {
    public TraceIdJaxrsResponseFilter() {
    }

    public TraceIdJaxrsResponseFilter(final ResourceInfo resourceInfo) {
        super(resourceInfo);
    }

    @Override
    protected void filterResponse(
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCoreFromContext();
        final String traceIdKey = AppbaseServiceUtil.traceIdKey(appbaseCore);

        String traceId = (String) requestContext.getProperty(traceIdKey);
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = MDC.get(traceIdKey);
        }

        if (AppbaseUtil.isNotBlank(traceId)) {
            responseContext.getHeaders().put(traceIdKey, List.of(traceId));
        }

        // Restore exactly the value TraceIdJaxrsRequestFilter snapshotted before this request
        // resolved anything, instead of unconditionally removing -- ownership-safe, parity with
        // the servlet-side fix (internal work item 2633/L-1291). A request that never went through the request
        // filter (no snapshot property set) has nothing to restore, so this is also safe for that
        // case: getProperty returns null, and MDC.remove is the correct fallback.
        final Object priorAmbientValue = requestContext.getProperty(traceIdKey + ".priorAmbientValue");
        if (priorAmbientValue != null) {
            MDC.put(traceIdKey, (String) priorAmbientValue);
        } else {
            MDC.remove(traceIdKey);
        }
    }
}
