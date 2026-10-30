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

package com.viveworks.appbase.service.web.trace.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class TraceIdServletFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = 41003725323041811L;

    public TraceIdServletFilter() {
    }

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore)
            throws IOException, ServletException
    {
        final String traceIdKey = AppbaseServiceUtil.traceIdKey(appbaseCore);

        String traceId = MDC.get(traceIdKey);
        if (AppbaseUtil.isBlank(traceId)) {
            traceId = AppbaseServiceUtil.cookie(traceIdKey, request);
        }

        if (AppbaseUtil.isBlank(traceId)) {
            traceId = AppbaseServiceUtil.header(traceIdKey, request);
        }

        if (AppbaseUtil.isBlank(traceId)) {
            traceId = AppbaseServiceUtil.parameter(traceIdKey, request);
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
        request.setAttribute(traceIdKey, traceId);
        response.addHeader(traceIdKey, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(traceIdKey);
        }
    }
}
