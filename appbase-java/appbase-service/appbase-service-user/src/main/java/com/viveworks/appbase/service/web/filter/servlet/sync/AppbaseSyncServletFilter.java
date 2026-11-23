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

package com.viveworks.appbase.service.web.filter.servlet.sync;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import com.viveworks.appbase.service.web.filter.servlet.AppbaseHttpServletRequestWrapper;
import com.viveworks.appbase.service.web.filter.servlet.AppbaseHttpServletResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AppbaseSyncServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = 5467590962667832554L;

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore)
            throws IOException, ServletException
    {
        final AppbaseHttpServletRequestWrapper appbaseHttpServletRequestWrapper =
                new AppbaseHttpServletRequestWrapper(request, appbaseCore);
        final AppbaseHttpServletResponseWrapper appbaseHttpServletResponseWrapper =
                new AppbaseHttpServletResponseWrapper(response, appbaseCore);

        chain.doFilter(appbaseHttpServletRequestWrapper, appbaseHttpServletResponseWrapper);
    }
}
