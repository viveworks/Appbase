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
package com.viveworks.appbase.service.web.cors;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.web.filter.jaxrs.AbstractJaxrsRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Provider
@PreMatching
@Priority(AppbaseServiceConstants.PRIORITY_HIGH)
public class CorsRequestJaxrsFilter extends AbstractJaxrsRequestFilter implements ContainerRequestFilter {
    private final CorsFilter corsFilter;

    public CorsRequestJaxrsFilter() {
        corsFilter = new DefaultCorsFilter();
    }

    @Override
    protected void filterRequest(final ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
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
                // A 400 is sent and the chain is NOT continued: no CORS decision is made against a
                // path that could not be reduced to its canonical form.
                requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
                return;
            }

            final Map<String, String> responseHeaders = corsFilter.corsHeaders(
                    appbaseCore,
                    path,
                    null);
            final Response.ResponseBuilder responseBuilder = Response.ok();
            if (responseHeaders != null) {
                responseHeaders.forEach(responseBuilder::header);
            }

            requestContext.abortWith(responseBuilder.status(Response.Status.NO_CONTENT).build());
        }
    }
}
