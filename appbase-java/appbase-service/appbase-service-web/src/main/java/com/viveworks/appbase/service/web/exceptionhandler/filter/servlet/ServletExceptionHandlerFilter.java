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

package com.viveworks.appbase.service.web.exceptionhandler.filter.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.jmx.PermissionDeniedException;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.service.authorization.AuthorizationServiceException;
import com.viveworks.appbase.service.web.filter.servlet.AbstractAppbaseServletFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ServletExceptionHandlerFilter extends AbstractAppbaseServletFilter {
    private static final long serialVersionUID = 3032052996941129109L;

    @Override
    protected void doFilter(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain,
            final AppbaseCore appbaseCore)
            throws IOException, ServletException
    {
        try {
            chain.doFilter(request, response);
        } catch (Exception ex) {
            if (ex instanceof PermissionDeniedException) {
                sendForbidden(appbaseCore, request.getMethod(), response);
            } else if (ex instanceof AuthorizationServiceException) {
                sendForbidden(appbaseCore, request.getMethod(), response);
            } else if (ex instanceof ValidationServiceException) {
                sendBadRequest(appbaseCore, request.getMethod(), ValidationServiceException.class.getName(), response);
            } else if (ex instanceof ValidationException) {
                sendBadRequest(appbaseCore, request.getMethod(), ValidationServiceException.class.getName(), response);
            } else if (ex instanceof AppbaseRuntimeException) {
                sendBadRequest(appbaseCore, request.getMethod(), ValidationServiceException.class.getName(), response);
            } else {
                sendInternalServerError(appbaseCore, request.getMethod(), Exception.class.getName(), response);
            }
        }
    }
}
