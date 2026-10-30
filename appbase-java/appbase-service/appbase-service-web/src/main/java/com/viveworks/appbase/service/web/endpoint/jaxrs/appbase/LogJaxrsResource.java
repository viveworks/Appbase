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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.log.accessor.LogReadPageSize;
import com.viveworks.appbase.core.service.log.jmx.JmxLogService;
import com.viveworks.appbase.core.service.log.jmx.JmxLogServiceMXBean;
import com.viveworks.appbase.core.service.log.store.criteria.LogRecordCriteria;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.jackson.JsonUtil;
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
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
@Path(AppbaseServiceConstants.DEFAULT_LOG_RESTAPI_PATH)
public class LogJaxrsResource extends AbstractAppbaseJaxrsEntity {
    @GET
    @Path("/logRecords" +
            "/inclusiveStartTimestamp/{inclusiveStartTimestamp}" +
            "/exclusiveEndTimestamp/{exclusiveEndTimestamp}" +
            "/pageSize/{pageSize}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read log records between timestamps")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readLogRecordsBetween(
            @PathParam("inclusiveStartTimestamp")
            final long inclusiveStartTimestamp,
            @PathParam("exclusiveEndTimestamp")
            final long exclusiveEndTimestamp,
            @PathParam("pageSize")
            final int pageSize,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final int boundedPageSize = boundedPageSize(pageSize);
        try {
            final Map<String, List<String>> result = jmxLogServiceMXBean(servletContext)
                    .readLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp, boundedPageSize);
            return Response.ok(MapOfStringListOfStringsValue.from(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/logRecords/timestamp/{timestamp}/pageSize/{pageSize}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read log records at a specific timestamp")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readLogRecordsAt(
            @PathParam("timestamp")
            final long timestamp,
            @PathParam("pageSize")
            final int pageSize,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final int boundedPageSize = boundedPageSize(pageSize);
        try {
            final Map<String, List<String>> result = jmxLogServiceMXBean(servletContext)
                    .readLogRecordsAt(timestamp, boundedPageSize);
            return Response.ok(MapOfStringListOfStringsValue.from(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/logRecords" +
            "/inclusiveStartTimestamp/{inclusiveStartTimestamp}" +
            "/exclusiveEndTimestamp/{exclusiveEndTimestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove log records between timestamps")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringBooleanValue.class)),
            responseCode = "200")
    @Timed
    public Response removeLogRecordsBetween(
            @PathParam("inclusiveStartTimestamp")
            final long inclusiveStartTimestamp,
            @PathParam("exclusiveEndTimestamp")
            final long exclusiveEndTimestamp,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, Boolean> result = jmxLogServiceMXBean(servletContext)
                    .removeLogRecordsBetween(inclusiveStartTimestamp, exclusiveEndTimestamp);
            return Response.ok(new MapOfStringBooleanValue(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/logRecords/timestamp/{timestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove log records at a specific timestamp")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringBooleanValue.class)),
            responseCode = "200")
    @Timed
    public Response removeLogRecordsAt(
            @PathParam("timestamp")
            final long timestamp,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final Map<String, Boolean> result = jmxLogServiceMXBean(servletContext)
                    .removeLogRecordsAt(timestamp);
            return Response.ok(new MapOfStringBooleanValue(result)).build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/readLogRecordsByCriteria/pageSize/{pageSize}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read log records at a specific timestamp")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response readLogRecordsByCriteria(
            @NotNull
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = LogRecordCriteria.class)),
                    description = "LogRecordCriteria",
                    required = true)
            final LogRecordCriteria logRecordCriteria,
            @PathParam("pageSize")
            final int pageSize,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final int boundedPageSize = boundedPageSize(pageSize);
        try {
            final String json = logRecordCriteriaJson(logRecordCriteria);
            final Map<String, List<String>> result = jmxLogServiceMXBean(servletContext)
                    .readLogRecordsByCriteria(json, boundedPageSize);
            return Response.ok(MapOfStringListOfStringsValue.from(result)).build();
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/removeLogRecordsByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove log records at a specific timestamp")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response removeLogRecordsByCriteria(
            @NotNull
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = LogRecordCriteria.class)),
                    description = "LogRecordCriteria",
                    required = true)
            final LogRecordCriteria logRecordCriteria,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            final String json = logRecordCriteriaJson(logRecordCriteria);
            final Map<String, Boolean> result = jmxLogServiceMXBean(servletContext)
                    .removeLogRecordsByCriteria(json);
            return Response.ok(new MapOfStringBooleanValue(result)).build();
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/logStoreFactory/configurationType")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read LogStoreFactory by configuration type map")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = MapOfStringStringValue.class)),
            responseCode = "200")
    @Timed
    public Response getLogStoreFactoryByConfigurationTypeMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final Map<String, String> result = jmxLogServiceMXBean(servletContext)
                .getLogStoreFactoryByConfigurationTypeMap();
        return Response.ok(new MapOfStringStringValue(result)).build();
    }

    @DELETE
    @Path("/unregisterLogStoreFactory" +
            "/logStoreFactoryTypeName/{logStoreFactoryTypeName}" +
            "/logStoreConfigurationTypeName/{logStoreConfigurationTypeName}")
    @Operation(summary = "Unregister LogStoreFactory by configuration type map")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterLogStoreFactoryByConfigurationType(
            @NotBlank
            @PathParam("logStoreFactoryTypeName")
            final String logStoreFactoryTypeName,
            @NotBlank
            @PathParam("logStoreConfigurationTypeName")
            final String logStoreConfigurationTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxLogServiceMXBean(servletContext).unregisterLogStoreFactoryByConfigurationType(
                    logStoreConfigurationTypeName, logStoreFactoryTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/logStoreFactory/configurationType")
    @Operation(summary = "Clear LogStoreFactory by configuration type map")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearLogStoreFactoryByConfigurationTypeMap(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxLogServiceMXBean(servletContext).clearLogStoreFactoryByConfigurationTypeMap();
        return Response.noContent().build();
    }

    @GET
    @Path("/logStores")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all registered LogStore implementations")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ListOfStringsValue.class)),
            responseCode = "200")
    @Timed
    public Response getLogStores(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final List<String> result = jmxLogServiceMXBean(servletContext).getLogStores();
        return Response.ok(new ListOfStringsValue(result)).build();
    }

    @DELETE
    @Path("/logStores/{logStoreTypeName}")
    @Operation(summary = "Unregister LogStore implementation")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response unregisterLogStore(
            @PathParam("logStoreTypeName")
            final String logStoreTypeName,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxLogServiceMXBean(servletContext).unregisterLogStore(logStoreTypeName);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @DELETE
    @Path("/logStores")
    @Operation(summary = "Clear all registered LogStore implementations")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response clearLogStores(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxLogServiceMXBean(servletContext).clearLogStores();
        return Response.noContent().build();
    }

    @POST
    @Path("/synchronizeLogStores")
    @Operation(summary = "Synchronize / flush all log stores")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response synchronizeLogStores(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxLogServiceMXBean(servletContext).synchronizeLogStores();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Get log service status")
    @ApiResponse(responseCode = "200")
    @Timed
    public Response status(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ServiceStatus serviceStatus = jmxLogServiceMXBean(servletContext).getStatus();
        return Response.ok(serviceStatus).build();
    }

    @PUT
    @Path("/reload")
    @Operation(summary = "Reload log service using original configuration - CAN RESULT IN UNDEFINED STATE!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxLogServiceMXBean(servletContext).reload();
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/reload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reload log service using new configuration - CAN RESULT IN UNDEFINED STATE!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response reload(
            final String appbaseServiceConfigurationYaml,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        try {
            jmxLogServiceMXBean(servletContext).reload(appbaseServiceConfigurationYaml);
            return Response.noContent().build();
        } catch (MBeanException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("/shutdown")
    @Operation(summary = "Shutdown log service - CAN RESULT IN UNDEFINED STATE!")
    @ApiResponse(responseCode = "204")
    @Timed
    public Response shutdown(
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        jmxLogServiceMXBean(servletContext).shutdown();
        return Response.noContent().build();
    }

    protected JmxLogServiceMXBean jmxLogServiceMXBean(final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        final JmxLogServiceMXBean jmxLogServiceMXBean = appbaseCore
                .getJmxService()
                .getMXBean(JmxLogService.class);
        if (jmxLogServiceMXBean == null) {
            appbaseCore
                    .getLogService()
                    .getLogger(getClass())
                    .error("{} is not initialized", JmxLogService.class.getName());
            throw new InternalServerErrorException(
                    JmxLogService.class.getName() + " is not initialized");
        }

        return jmxLogServiceMXBean;
    }

    private String logRecordCriteriaJson(
            final LogRecordCriteria logRecordCriteria)
            throws JsonProcessingException
    {
        AppbaseUtil.notNull(logRecordCriteria, "logRecordCriteria");

        return JsonUtil
                .getJsonMapper()
                .writerFor(LogRecordCriteria.class)
                .writeValueAsString(logRecordCriteria);
    }

    private static int boundedPageSize(final int pageSize) {
        try {
            return LogReadPageSize.requireValid(pageSize);
        } catch (IllegalArgumentException invalidPageSize) {
            throw new BadRequestException("Invalid pageSize");
        }
    }
}
