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
import com.viveworks.appbase.core.service.jmx.JmxJmxService;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MxBeanEntryList;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_JMX_RESTAPI_PATH)
public class JmxJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Path("/mxBeanEntries")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get MXBeanEntry")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = List.class)),
            responseCode = "200")
    @Timed
    public Response mxBeanEntries(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final List<String> mxBeanEntriesJson = jmxJmxServiceMXBean(servletContext).getMXBeanEntriesJson();
            if (mxBeanEntriesJson != null) {
                final List<MXBeanEntry> mxBeanEntries = new ArrayList<>();
                for (final String json : mxBeanEntriesJson) {
                    final MXBeanEntry mxBeanEntry = JsonUtil
                            .getJsonMapper()
                            .readerFor(MXBeanEntry.class)
                            .readValue(json);
                    mxBeanEntries.add(mxBeanEntry);
                }
                return Response.ok(new MxBeanEntryList(mxBeanEntries)).build();
            }

            return Response.ok().build();
        } catch (IOException | MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/mxBeanEntries/{mxBeanTypeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get MXBeanEntry")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MXBeanEntry.class)),
            responseCode = "200")
    @Timed
    public Response mxBeanEntry(
            @NotBlank
            @PathParam("mxBeanTypeName")
            final String mxBeanTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String mxBeanEntryJson = jmxJmxServiceMXBean(servletContext).getMXBeanEntryJson(mxBeanTypeName);
            final MXBeanEntry mxBeanEntry = JsonUtil
                    .getJsonMapper()
                    .readerFor(MXBeanEntry.class)
                    .readValue(mxBeanEntryJson);
            return Response.ok(mxBeanEntry).build();
        } catch (IOException ioe) {
            throw new InternalServerErrorException(ioe);
        } catch (MBeanException me) {
            if (me.getTargetException() != null &&
                    me.getTargetException().getClass().isAssignableFrom(ClassNotFoundException.class))
            {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            throw new InternalServerErrorException(me);
        }
    }

    @HEAD
    @Path("/mxBeans/{mxBeanTypeName}")
    @ApiResponse(responseCode = "204 if registered, 404 otherwise")
    @Timed
    public Response isMXBeanRegistered(
            @NotBlank
            @PathParam("mxBeanTypeName")
            final String mxBeanTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final boolean registered = jmxJmxServiceMXBean(servletContext)
                    .isMXBeanRegistered(mxBeanTypeName);
            return registered ?
                    Response.noContent().build() :
                    Response.status(Response.Status.NOT_FOUND).build();
        } catch (MBeanException e) {
            if (e.getTargetException() != null &&
                    e.getTargetException().getClass().isAssignableFrom(ClassNotFoundException.class))
            {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/mxBeans")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearMXBeans(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxJmxServiceMXBean(servletContext).clearMXBeans();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/mxBeans/{mxBeanTypeName}")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterMXBean(
            @NotBlank
            @PathParam("mxBeanTypeName")
            final String mxBeanTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxJmxServiceMXBean(servletContext).unregisterMXBean(mxBeanTypeName);
            return Response.noContent().build();
        } catch (MBeanException me) {
            if (me.getTargetException() != null &&
                    me.getTargetException().getClass().isAssignableFrom(ClassNotFoundException.class))
            {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            throw new InternalServerErrorException(me);
        }
    }


    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get JMX service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus serviceStatus = jmxJmxServiceMXBean(servletContext).getStatus();
        return Response.ok(serviceStatus).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload JMX service using original configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxJmxServiceMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload JMX service using new configuration - DOES NOT WORK!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @RequestBody(content = @Content(schema = @Schema(description = "YAML representation of configuration")))
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxJmxServiceMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown JMX service")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxJmxServiceMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    private JmxJmxServiceMXBean jmxJmxServiceMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxJmxServiceMXBean jmxServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxJmxService.class);

        if (jmxServiceMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxJmxService.class.getName());
            throw new InternalServerErrorException(
                    JmxJmxService.class.getName() + " is not initialized");
        }

        return jmxServiceMXBean;
    }
}
