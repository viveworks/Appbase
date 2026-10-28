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
package com.viveworks.appbase.service.web.cors;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsResponseFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
@Priority(AppbaseServiceConstants.PRIORITY_HIGH)
public class CorsResponseJaxrsFilter extends AbstractJaxrsResponseFilter implements ContainerResponseFilter {
    private final CorsFilter corsFilter;

    public CorsResponseJaxrsFilter() {
        corsFilter = new DefaultCorsFilter();
    }

    @Override
    protected void filterResponse(
            final ContainerRequestContext requestContext,
            final ContainerResponseContext responseContext) throws IOException
    {
        final AppbaseCore appbaseCore = appbaseCoreFromContext();

        if (appbaseCore == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find an instance of " + AppbaseCore.class.getName());
        }

        final String path;
        try {
            // getPath(false) is the ENCODED, base-URI-relative path. getPath() decodes -- see
            // jakarta.ws.rs-api: "All sequences of escaped octets are decoded, equivalent to
            // getPath(true)". Canonicalization must see the encoded form.
            path = AppbasePathCanonicalizer.canonicalize(requestContext.getUriInfo().getPath(false));
        } catch (final AppbasePathException e) {
            // Response filters cannot abort the chain (the request has already been processed), so the
            // response status is overridden to 400 instead -- no CORS header decision is made against a
            // path that could not be reduced to its canonical form.
            sendBadRequest(appbaseCore, getClass().getName(), responseContext);
            return;
        }

        final Map<String, String> responseHeaders = corsFilter.corsHeaders(
                appbaseCore,
                path,
                null);
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        responseHeaders.forEach(headers::putSingle);
    }
}
