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
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CorsServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = 6511471405496837152L;

    private final CorsFilter corsFilter;

    public CorsServletFilter() {
        corsFilter = new DefaultCorsFilter();
    }

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore)
            throws IOException, ServletException
    {
        final String path;
        try {
            // Strip before decode: both getRequestURI() and getContextPath() are encoded; stripping
            // after decoding would let an encoded separator inside the prefix shift the boundary.
            path = AppbasePathCanonicalizer.canonicalize(
                    AppbasePathCanonicalizer.stripContextPath(request.getRequestURI(), request.getContextPath()));
        } catch (final AppbasePathException e) {
            // A 400 is sent and the chain is NOT continued.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            response.flushBuffer();
            return;
        }

        final Map<String, String> responseHeaders = corsFilter.corsHeaders(appbaseCore, path, null);
        if (responseHeaders != null) {
            responseHeaders.forEach(response::addHeader);
        }

        chain.doFilter(request, response);
    }
}
