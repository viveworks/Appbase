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

package com.viveworks.appbase.service.web.health.endpoint.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.health.HealthChecks;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class HealthCheckServlet extends AbstractAppbaseSyncServlet {
    public static final String DEFAULT_PATH = "/health";

    private static final long serialVersionUID = -8857571160200787016L;

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final HealthCheckService healthCheckService = healthCheckService();
        final String forceParam = parameter(AppbaseServiceConstants.FORCE_PARAM, httpServletRequest);
        if (Boolean.parseBoolean(forceParam)) {
            return (T) healthCheckService.checkHealth(true);
        }
        return (T) healthCheckService.checkHealth();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<HealthChecks> getResponseType() {
        return HealthChecks.class;
    }

    @Override
    protected boolean isGetResponseAutoHandled() {
        return false;
    }

    @Override
    protected void handleGetResponse(
            final Object response,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final HealthChecks healthChecks = (HealthChecks) response;

        final String statusOnly = parameter(AppbaseServiceConstants.STATUS_ONLY_PARAM, httpServletRequest);
        final boolean statusOnlyFlag;
        if (AppbaseUtil.isBlank(statusOnly)) {
            statusOnlyFlag = false;
        } else {
            statusOnlyFlag = Boolean.parseBoolean(statusOnly);
        }

        if (statusOnlyFlag) {
            if (healthChecks.isHealthy()) {
                sendOK(httpServletResponse);
            } else {
                sendInternalServerError("GET", "healthChecks", httpServletResponse);
            }
        } else {
            if (healthChecks.isHealthy()) {
                sendOK(healthChecks, HealthChecks.class, httpServletResponse);
            } else {
                writeInternalServerError("GET", healthChecks, HealthChecks.class, httpServletResponse);
            }
        }
    }

    @Override
    protected boolean isGetSupported() {
        return true;
    }

    @Override
    protected boolean isHeadSupported() {
        return false;
    }

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }

    @Override
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isPostSupported() {
        return false;
    }

    private HealthCheckService healthCheckService() throws ServletException {
        final AppbaseCore appbaseCore = appbaseCore();
        if (appbaseCore == null) {
            throw new ServletException("Cannot find an instance of " + AppbaseCore.class.getName());
        }

        final AppbaseServiceEntry appbaseServiceEntry = appbaseCore
                .getServiceRegistry()
                .getAppbaseService(HealthCheckService.class);
        if (appbaseServiceEntry == null) {
            throw new ServletException("Cannot find a service entry for " + HealthCheckService.class.getName());
        }

        final HealthCheckService healthCheckService = (HealthCheckService) appbaseServiceEntry.getService();
        if (healthCheckService == null) {
            throw new ServletException("Cannot find an instance of " + HealthCheckService.class.getName());
        }

        return healthCheckService;
    }
}
