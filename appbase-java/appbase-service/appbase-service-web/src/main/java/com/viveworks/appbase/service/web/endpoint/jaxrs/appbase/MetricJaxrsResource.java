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
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricService;
import com.viveworks.appbase.core.service.metric.jmx.JmxMetricServiceMXBean;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.SetOfStringsValue;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_METRIC_RESTAPI_PATH)
public class MetricJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Path("/serializeMetricRegistryAsJson")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get micrometer registry names")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response serializeMetricRegistryAsJson(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String json = jmxMetricServiceMXBean(servletContext).serializeMetricRegistryAsJson();
            return Response
                    .ok()
                    .entity(json)
                    .build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/clearMicrometerRegistries")
    @Operation(summary = "Unregister micrometer registry")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearMicrometerRegistries(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxMetricServiceMXBean(servletContext).clearMicrometerRegistries();
        return Response.noContent().build();
    }

    @DELETE
    @Path("/unregisterMicrometerRegistry/{meterRegistryTypeName}")
    @Operation(summary = "Unregister micrometer registry")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterMicrometerRegistry(
            @NotBlank
            @PathParam("meterRegistryTypeName")
            final String meterRegistryTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxMetricServiceMXBean(servletContext).unregisterMicrometerRegistry(meterRegistryTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/micrometerRegistryNames")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get micrometer registry names")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response micrometerRegistryNames(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Set<String> result =
                jmxMetricServiceMXBean(servletContext).getMicrometerRegistryNames();
        return Response.ok(new SetOfStringsValue(result)).build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get metric service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus serviceStatus = jmxMetricServiceMXBean(servletContext).getStatus();
        return Response.ok(serviceStatus).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload metric service using original configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxMetricServiceMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload metric service using new configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxMetricServiceMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown metric service")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxMetricServiceMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    private JmxMetricServiceMXBean jmxMetricServiceMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxMetricServiceMXBean jmxMetricServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxMetricService.class);

        if (jmxMetricServiceMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxMetricService.class.getName());
            throw new InternalServerErrorException(
                    JmxMetricService.class.getName() + " is not initialized");
        }

        return jmxMetricServiceMXBean;
    }
}
