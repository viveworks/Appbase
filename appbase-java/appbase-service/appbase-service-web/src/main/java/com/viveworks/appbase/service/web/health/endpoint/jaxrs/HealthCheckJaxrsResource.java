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

package com.viveworks.appbase.service.web.health.endpoint.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.health.HealthCheckService;
import com.viveworks.appbase.service.health.HealthChecks;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_HEALTH_CHECK_RESTAPI_PATH)
public class HealthCheckJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Run all registered health checks and return a cumulative response")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = HealthChecks.class)),
            description = "Cumulative health check response",
            responseCode = "200")
    public Response checkHealth(
            @DefaultValue("true")
            @QueryParam("force")
            final String force,
            @DefaultValue("false")
            @QueryParam("statusOnly")
            final String statusOnly,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final HealthChecks healthChecks;
        if (Boolean.parseBoolean(force)) {
            healthChecks = healthCheckService(servletContext).checkHealth(true);
        } else {
            healthChecks = healthCheckService(servletContext).checkHealth();
        }

        final boolean statusOnlyFlag = Boolean.parseBoolean(statusOnly);
        if (statusOnlyFlag) {
            if (healthChecks.isHealthy()) {
                return Response.ok().build();
            } else {
                return Response.serverError().entity(healthChecks).build();
            }
        }

        if (healthChecks.isHealthy()) {
            return Response.ok(healthChecks).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(healthChecks).build();
    }

    private HealthCheckService healthCheckService(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        if (appbaseCore == null) {
            throw new AppbaseServiceRuntimeException("Cannot find an instance of " + AppbaseCore.class.getName());
        }

        final AppbaseServiceEntry appbaseServiceEntry = appbaseCore
                .getServiceRegistry()
                .getAppbaseService(HealthCheckService.class);
        if (appbaseServiceEntry == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find a service entry for " + HealthCheckService.class.getName());
        }

        final HealthCheckService healthCheckService = (HealthCheckService) appbaseServiceEntry.getService();
        if (healthCheckService == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find an instance of " + HealthCheckService.class.getName());
        }

        return healthCheckService;
    }
}
