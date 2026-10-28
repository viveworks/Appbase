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
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorService;
import com.viveworks.appbase.core.service.mediator.jmx.JmxMediatorServiceMXBean;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.Value;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.management.MBeanException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_MEDIATOR_RESTAPI_PATH)
public class MediatorJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Path("/configurationFilePath")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the configuration file path")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = Value.class)),
            responseCode = "200")
    @Timed
    public Response getConfigurationFilePath(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final String configurationFilePath = jmxMediatorServiceMXBean(servletContext).getConfigurationFilePath();
        final Value value = new Value(configurationFilePath);
        return Response.ok(value).build();
    }

    @GET
    @Path("/isInitWithDefaultConfiguration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if the init was with default configuration")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = Value.class)),
            responseCode = "200")
    @Timed
    public Response isInitWithDefaultConfiguration(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final boolean initWithDefaultConfiguration = jmxMediatorServiceMXBean(servletContext)
                .isInitWithDefaultConfiguration();
        final Value value = new Value(initWithDefaultConfiguration);
        return Response.ok(value).build();
    }

    @GET
    @Path("/isInitWithConfigurationFile")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if the init was with configuration file")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = Value.class)),
            responseCode = "200")
    @Timed
    public Response isInitWithConfigurationFile(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final boolean initWithConfigurationFile = jmxMediatorServiceMXBean(servletContext)
                .isInitWithConfigurationFile();
        final Value value = new Value(initWithConfigurationFile);
        return Response.ok(value).build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get mediator service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus serviceStatus = jmxMediatorServiceMXBean(servletContext).getStatus();
        return Response.ok(serviceStatus).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload mediator service using original configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxMediatorServiceMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload mediator service using new configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxMediatorServiceMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown mediator service - DOES NOT WORK!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxMediatorServiceMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    private JmxMediatorServiceMXBean jmxMediatorServiceMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxMediatorServiceMXBean jmxMediatorServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxMediatorService.class);

        if (jmxMediatorServiceMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxMediatorService.class.getName());
            throw new InternalServerErrorException(
                    JmxMediatorService.class.getName() + " is not initialized");
        }

        return jmxMediatorServiceMXBean;
    }
}
