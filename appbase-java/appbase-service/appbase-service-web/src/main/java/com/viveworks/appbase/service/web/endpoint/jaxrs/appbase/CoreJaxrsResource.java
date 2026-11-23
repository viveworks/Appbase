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
import com.viveworks.appbase.core.jmx.JmxAppbaseCore;
import com.viveworks.appbase.core.jmx.JmxAppbaseCoreMXBean;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_CORE_RESTAPI_PATH)
public class CoreJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @POST
    @Path("/reloadService/{appbaseServiceTypeName}/{appbaseServiceConfigurationTypeName}")
    @Operation(summary = "Reload an appbase service")
    @ApiResponse(responseCode = "201")
    @Timed
    public Response reloadService(
            @NotEmpty
            @PathParam("appbaseServiceTypeName")
            final String appbaseServiceTypeName,
            @NotEmpty
            @PathParam("appbaseServiceConfigurationTypeName")
            final String appbaseServiceConfigurationTypeName,
            @NotNull
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxAppbaseCoreMXBean(servletContext).reloadService(
                    appbaseServiceTypeName,
                    appbaseServiceConfigurationYaml,
                    appbaseServiceConfigurationTypeName);
        } catch (MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("reloadService error", e);
            throw new InternalServerErrorException(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/shutdownService/{appbaseServiceTypeName}")
    @Operation(summary = "Shutdown an appbase service")
    @ApiResponse(responseCode = "201")
    @Timed
    public Response shutdownService(
            @NotEmpty
            @PathParam("appbaseServiceTypeName")
            final String appbaseServiceTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxAppbaseCoreMXBean(servletContext).shutdownService(appbaseServiceTypeName);
        } catch (MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("shutdownService error", e);
            throw new InternalServerErrorException(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/shutdownAllServices")
    @Operation(summary = "Shutdown all appbase services - DOES NOT WORK!")
    @ApiResponse(responseCode = "201")
    @Timed
    public Response shutdownAllServices(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxAppbaseCoreMXBean(servletContext).shutdownAllServices();
        } catch (MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("shutdownAllServices error", e);
            throw new InternalServerErrorException(e);
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get appbase core status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus status = jmxAppbaseCoreMXBean(servletContext).getStatus();
        return Response.ok(status.name()).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload appbase core - DOES NOT WORK!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxAppbaseCoreMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("reload error", e);
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload appbase core - CAN RESULT IN UNDEFINED STATE!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxAppbaseCoreMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            appbaseCore(servletContext)
                    .getLogService()
                    .getLogger(getClass())
                    .error("reload error", e);
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown appbase core - CAN RESULT IN UNDEFINED STATE!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxAppbaseCoreMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    private JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxAppbaseCoreMXBean jmxAppbaseCoreMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxAppbaseCore.class);

        if (jmxAppbaseCoreMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxAppbaseCore.class.getName());
            throw new InternalServerErrorException(
                    JmxAppbaseCore.class.getName() + " is not initialized");
        }

        return jmxAppbaseCoreMXBean;
    }
}
