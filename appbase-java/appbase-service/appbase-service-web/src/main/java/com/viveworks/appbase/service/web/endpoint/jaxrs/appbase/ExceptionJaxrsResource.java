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
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionService;
import com.viveworks.appbase.core.service.exception.jmx.JmxExceptionServiceMXBean;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.appbase.data.ListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringBooleanValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringListOfStringsValue;
import com.viveworks.appbase.service.appbase.data.MapOfStringStringValue;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_EXCEPTION_RESTAPI_PATH)
public class ExceptionJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Path("/exceptionMessageDetails")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read exception message details")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readAllExceptionMessageDetails(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, List<String>> allExceptionMessageDetails =
                    jmxExceptionServiceMXBean(servletContext).readAllExceptionMessageDetails();
            final MapOfStringListOfStringsValue result =
                    MapOfStringListOfStringsValue.from(allExceptionMessageDetails);
            return Response.ok(result).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/exceptionMessageDetails/id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read exception message details by ID")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringStringValue.class)),
            responseCode = "200")
    @Timed
    public Response readExceptionMessageDetailsById(
            @NotNull
            @PathParam("id")
            final Integer id,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, String> result = jmxExceptionServiceMXBean(servletContext)
                    .readExceptionMessageDetailsById(id);
            final MapOfStringStringValue mapOfStringStringValue = new MapOfStringStringValue(result);
            return Response.ok(mapOfStringStringValue).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/exceptionMessageDetails/timestamps/{inclusiveMinTimestamp}/{exclusiveMaxTimestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read exception message details by timestamp")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readAllExceptionMessageDetailsByTimestamps(
            @NotNull
            @PathParam("inclusiveMinTimestamp")
            final Long inclusiveMinTimestamp,
            @NotNull
            @PathParam("exclusiveMaxTimestamp")
            final Long exclusiveMaxTimestamp,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, List<String>> result = jmxExceptionServiceMXBean(servletContext)
                    .readAllExceptionMessageDetailsByTimestamps(inclusiveMinTimestamp, exclusiveMaxTimestamp);
            final MapOfStringListOfStringsValue mapOfStringListOfStringsValue =
                    MapOfStringListOfStringsValue.from(result);
            return Response.ok(mapOfStringListOfStringsValue).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/uniqueExceptionTypeNames")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read unique exception type names")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readUniqueExceptionTypeNames(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, List<String>> result =
                    jmxExceptionServiceMXBean(servletContext).readUniqueExceptionTypeNames();
            return Response.ok(MapOfStringListOfStringsValue.from(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/exceptionMessageDetails/throwable/{typeName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read exception message details by type name")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readExceptionMessageDetailsByThrowableTypeName(
            @NotEmpty
            @PathParam("typeName")
            final String typeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, List<String>> result = jmxExceptionServiceMXBean(servletContext)
                    .readExceptionMessageDetailsByThrowableTypeName(typeName);
            return Response.ok(MapOfStringListOfStringsValue.from(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/exceptionMessageDetails/id/{id}")
    @Operation(summary = "Remove exception message details by ID")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringBooleanValue.class)),
            responseCode = "204")
    @Timed
    public Response removeExceptionMessageDetailsById(
            @PositiveOrZero
            @PathParam("id")
            final int id,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext).removeExceptionMessageDetailsById(id);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/exceptionMessageDetails/timestamp/{exclusiveBefore}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove exception message details by ID")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringBooleanValue.class)),
            responseCode = "204")
    @Timed
    public Response removeExceptionMessageDetailsExclusiveBefore(
            @NotNull
            @PathParam("exclusiveBefore")
            final Long timestamp,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, Boolean> result =
                    jmxExceptionServiceMXBean(servletContext).removeExceptionMessageDetailsExclusiveBefore(timestamp);
            return Response.ok(new MapOfStringBooleanValue(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/exceptionListenersByExceptionTypeMap")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get exception listener by exception type map")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getExceptionListenersByExceptionTypeMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Map<String, List<String>> result = jmxExceptionServiceMXBean(servletContext)
                .getExceptionListenersByExceptionTypeMap();
        return Response.ok(MapOfStringListOfStringsValue.from(result)).build();
    }

    @DELETE
    @Path("/exceptionListener/{exceptionListenerTypeName}/exceptionType/{exceptionTypeName}")
    @Operation(summary = "Unregister exception listener by exception type")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterExceptionListenerByExceptionType(
            @PathParam("exceptionTypeName")
            final String exceptionTypeName,
            @PathParam("exceptionListenerTypeName")
            final String exceptionListenerTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext)
                    .unregisterExceptionListenerByExceptionType(exceptionTypeName, exceptionListenerTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/exceptionListener/exceptionType")
    @Operation(summary = "Clear exception listener by exception type map")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearExceptionListenersByExceptionTypeMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxExceptionServiceMXBean(servletContext).clearExceptionListenersByExceptionTypeMap();
        return Response.noContent().build();
    }

    @GET
    @Path("/globalExceptionListeners")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get global exception listeners")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getGlobalExceptionListeners(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final List<String> result = jmxExceptionServiceMXBean(servletContext).getGlobalExceptionListeners();
        return Response.ok(new ListOfStringsValue(result)).build();
    }

    @DELETE
    @Path("/globalExceptionListeners/{exceptionListenerTypeName}")
    @Operation(summary = "Unregister global exception listener by exception type name")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterGlobalExceptionListener(
            @NotEmpty
            @PathParam("exceptionListenerTypeName")
            final String exceptionListenerTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext).unregisterGlobalExceptionListener(exceptionListenerTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/globalExceptionListeners")
    @Operation(summary = "Clear global exception listeners")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearGlobalExceptionListeners(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxExceptionServiceMXBean(servletContext).clearGlobalExceptionListeners();
        return Response.noContent().build();
    }

    @GET
    @Path("/exceptionStoreFactoryByConfigurationTypeNameMap")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get exceptionStoreFactory by configurationTypeName map")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringStringValue.class)),
            responseCode = "200")
    @Timed
    public Response getExceptionStoreFactoryByConfigurationTypeNameMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Map<String, String> result =
                jmxExceptionServiceMXBean(servletContext).getExceptionStoreFactoryByConfigurationTypeNameMap();
        return Response.ok(new MapOfStringStringValue(result)).build();
    }

    @DELETE
    @Path("/exceptionStoreFactory/{factoryTypeName}/configurationType/{configurationTypeName}")
    @Operation(summary = "Unregister exception store factory by configuration type")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterExceptionStoreFactoryByConfigurationType(
            @PathParam("configurationTypeName")
            final String exceptionStoreConfigurationTypeName,
            @PathParam("factoryTypeName")
            final String exceptionStoreFactoryTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext).unregisterExceptionStoreFactoryByConfigurationType(
                    exceptionStoreConfigurationTypeName,
                    exceptionStoreFactoryTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/exceptionStoreFactory/configurationType")
    @Operation(summary = "Clear exception store factory by configuration type map")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearExceptionStoreFactoryByConfigurationTypeMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxExceptionServiceMXBean(servletContext).clearExceptionStoreFactoryByConfigurationTypeMap();
        return Response.noContent().build();
    }

    @GET
    @Path("/exceptionStores")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get exceptionStoreFactory by configurationTypeName map")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getExceptionStores(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final List<String> exceptionStores = jmxExceptionServiceMXBean(servletContext).getExceptionStores();
        return Response.ok(new ListOfStringsValue(exceptionStores)).build();
    }

    @DELETE
    @Path("/exceptionStores/{typeName}")
    @Operation(summary = "Unregister exception store by type name")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterExceptionStore(
            @NotEmpty
            @PathParam("typeName")
            final String exceptionStoreTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext).unregisterExceptionStore(exceptionStoreTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/exceptionStores")
    @Operation(summary = "Clear exception stores")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearExceptionStores(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxExceptionServiceMXBean(servletContext).clearExceptionStores();
        return Response.noContent().build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get exception service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus serviceStatus = jmxExceptionServiceMXBean(servletContext).getStatus();
        return Response.ok(serviceStatus).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload exception service using original configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload exception service using new configuration")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxExceptionServiceMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown exception service")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxExceptionServiceMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    protected JmxExceptionServiceMXBean jmxExceptionServiceMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxExceptionServiceMXBean jmxExceptionServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxExceptionService.class);

        if (jmxExceptionServiceMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxExceptionService.class.getName());
            throw new InternalServerErrorException(
                    JmxExceptionService.class.getName() + " is not initialized");
        }

        return jmxExceptionServiceMXBean;
    }
}
