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

package com.viveworks.appbase.service.web.endpoint.jaxrs.appbase;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistry;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.MapOfStringServiceStatusValue;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.management.MBeanException;
import java.io.IOException;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_SERVICE_REGISTRY_RESTAPI_PATH)
public class ServiceRegistryJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @POST
    @Path("/pollAndUpdateStatuses")
    @Operation(summary = "Poll and update statuses of all services in the registry")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response pollAndUpdateStatuses(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxServiceRegistryMXBean(servletContext).pollAndUpdateStatuses();
        return Response.noContent().build();
    }

    @GET
    @Path("/overallStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the overall status")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ServiceRegistry.OverallStatus.class)),
            responseCode = "200 started, 503 if one or more services have not started")
    @Timed
    public Response overallStatus(
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
    public Response pollOverallStatus(
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

    @HEAD
    @Path("/appbaseServiceStatuses/{appbaseServiceTypeName}")
    @Operation(summary = "Get the status of a service")
    @ApiResponse(responseCode = "200 started, 503 if one or more services have not started")
    @Timed
    public Response getAppbaseServiceStatus(
            @NotBlank
            @PathParam("appbaseServiceTypeName")
            final String appbaseServiceTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final ServiceStatus appbaseServiceStatus = jmxServiceRegistryMXBean(servletContext)
                    .getAppbaseServiceStatus(appbaseServiceTypeName);
            return appbaseServiceStatus == ServiceStatus.STARTED ?
                    Response.ok().build() :
                    Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }  catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/appbaseServiceStatuses")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all service statuses by their name map")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringServiceStatusValue.class)),
            responseCode = "200")
    @Timed
    public Response getAllServiceStatusByServiceTypeName(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Map<String, ServiceStatus> result = jmxServiceRegistryMXBean(servletContext)
                .getAllServiceStatusByServiceTypeName();
        return Response.ok(new MapOfStringServiceStatusValue(result)).build();
    }

    @DELETE
    @Path("/appbaseServices/{appbaseServiceTypeName}")
    @Operation(summary = "Unregister service by type name")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response unregisterAppbaseService(
            @NotBlank
            @PathParam("appbaseServiceTypeName")
            final String appbaseServiceTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxServiceRegistryMXBean(servletContext).unregisterAppbaseService(appbaseServiceTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/appbaseServices")
    @Operation(summary = "Clear services")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response clearAppbaseServices(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxServiceRegistryMXBean(servletContext).clearAppbaseServices();
        return Response.noContent().build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get service registry service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus serviceStatus = jmxServiceRegistryMXBean(servletContext).getStatus();
        return Response.ok(serviceStatus).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload service registry service using original configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxServiceRegistryMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload service registry service using new configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxServiceRegistryMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown service registry service")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxServiceRegistryMXBean(servletContext).shutdown();
        return Response.noContent().build();
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
