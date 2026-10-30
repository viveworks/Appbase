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

package com.viveworks.appbase.service.web.endpoint.jaxrs.appbase;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.management.MBeanException;
import java.io.IOException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_STATUS_RESTAPI_PATH)
public class StatusJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @HEAD
    @Path("/overallStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the overall status")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ServiceRegistry.OverallStatus.class)),
            responseCode = "200 started, 503 if one or more services have not started")
    @Timed
    public Response getOverallStatus(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String overallStatusJson = jmxServiceRegistryMXBean(servletContext).overallStatusJson();
            return sendOverallStatus(overallStatusJson);
        } catch (IOException | MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("Error getting overall status", e);
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/pollOverallStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Poll / refresh and get the overall status")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ServiceRegistry.OverallStatus.class)),
            responseCode = "200 started, 503 if one or more services have not started")
    @Timed
    public Response pollAndGetOverallStatus(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String pollOverallStatusJson = jmxServiceRegistryMXBean(servletContext).pollOverallStatusJson();
            return sendOverallStatus(pollOverallStatusJson);
        } catch (IOException | MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("Error polling overall status", e);
            throw new InternalServerErrorException(e);
        }
    }

    private JmxServiceRegistryMXBean jmxServiceRegistryMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxServiceRegistryMXBean jmxServiceRegistryMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxServiceRegistry.class);

        if (jmxServiceRegistryMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxServiceRegistry.class.getName());
            throw new InternalServerErrorException(
                    JmxServiceRegistry.class.getName() + " is not initialized");
        }

        return jmxServiceRegistryMXBean;
    }

    private Response sendOverallStatus(final String overallStatusJson) throws IOException, MBeanException {
        if (overallStatusJson == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        final ServiceRegistry.OverallStatus overallStatus = JsonUtil
                .getJsonMapper()
                .readerFor(ServiceRegistry.OverallStatus.class)
                .readValue(overallStatusJson);
        return overallStatus.isStarted() ?
                Response.ok(overallStatus).build() :
                Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(overallStatus).build();
    }
}
