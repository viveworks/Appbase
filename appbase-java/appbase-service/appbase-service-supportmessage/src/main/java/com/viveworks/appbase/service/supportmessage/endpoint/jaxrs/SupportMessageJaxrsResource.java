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
package com.viveworks.appbase.service.supportmessage.endpoint.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.AppbaseServiceException;
import com.viveworks.appbase.service.AppbaseServiceRuntimeException;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.supportmessage.SupportMessageService;
import com.viveworks.appbase.service.supportmessage.data.DeleteSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ExistsSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessageByIdResponse;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesRequest;
import com.viveworks.appbase.service.supportmessage.data.ReadSupportMessagesResponse;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.SendSupportMessageResponse;
import com.viveworks.appbase.service.supportmessage.data.SupportMessageIdRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateMessageRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateStatusRequest;
import com.viveworks.appbase.service.supportmessage.data.UpdateSupportMessageResponse;
import com.viveworks.appbase.service.web.endpoint.jaxrs.AbstractAppbaseJaxrsEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Path(AppbaseServiceConstants.DEFAULT_SUPPORT_MESSAGE_RESTAPI_PATH)
public class SupportMessageJaxrsResource extends AbstractAppbaseJaxrsEntity {
    public static final String SEND_SUPPORT_MESSAGES_PATH = "/send";
    public static final String READ_SUPPORT_MESSAGES_PATH = "/all";
    public static final String UPDATE_MESSAGES_PATH = "/message";
    public static final String UPDATE_STATUS_PATH = "/status";

    @POST
    @Path(SEND_SUPPORT_MESSAGES_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Send support message")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SendSupportMessageResponse.class)),
            description = "Send support message response",
            responseCode = "200")
    @Timed
    public Response sendSupportMessage(
            @NotNull
            @SendSupportMessageRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = SendSupportMessageRequest.class)),
                    description = "Send support message request",
                    required = true)
            final SendSupportMessageRequest sendSupportMessageRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final String finalTraceId = AppbaseServiceUtil.traceId(traceId, sendSupportMessageRequest);
            sendSupportMessageRequest.setTraceId(finalTraceId);

            final SendSupportMessageResponse sendSupportMessageResponse =
                    supportMessageService.sendSupportMessage(sendSupportMessageRequest);
            return okResponse(sendSupportMessageResponse);
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Path(READ_SUPPORT_MESSAGES_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read support messages")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadSupportMessagesResponse.class)),
            description = "Read support messages response",
            responseCode = "200")
    @Timed
    public Response readSupportMessages(
            @NotNull
            @PositiveOrZero
            @QueryParam("offset")
            final Integer offset,
            @NotNull
            @PositiveOrZero
            @Max(AppbaseServiceConstants.MAX_SUPPORT_MESSAGE_READ_LIMIT)
            @QueryParam("limit")
            final Integer limit,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final ReadSupportMessagesRequest readSupportMessagesRequest =
                new ReadSupportMessagesRequest(offset, limit, traceId);

        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final ReadSupportMessagesResponse readSupportMessagesResponse =
                    supportMessageService.readSupportMessages(readSupportMessagesRequest);
            return okResponse(readSupportMessagesResponse);
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read a support message")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadSupportMessageByIdResponse.class)),
            description = "Read support message response",
            responseCode = "200")
    @Timed
    public Response readSupportMessageById(
            @NotBlank
            @QueryParam("messageId")
            final String messageId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest(messageId, traceId);

        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final ReadSupportMessageByIdResponse readSupportMessageByIdResponse =
                    supportMessageService.readSupportMessageById(supportMessageIdRequest);
            return okResponse(readSupportMessageByIdResponse);
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check if a support message exists")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if the account exists, otherwise 404")
    @Timed
    public Response existsSupportMessageById(
            @NotNull
            @QueryParam("messageId")
            final String messageId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest(messageId, traceId);

        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final ExistsSupportMessageByIdResponse existsSupportMessageByIdResponse =
                    supportMessageService.existsSupportMessageById(supportMessageIdRequest);
            if (existsSupportMessageByIdResponse != null && existsSupportMessageByIdResponse.isExists()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(UPDATE_MESSAGES_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update support message")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateSupportMessageResponse.class)),
            description = "Update support message response",
            responseCode = "200")
    @Timed
    public Response updateMessage(
            @NotNull
            @UpdateMessageRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateMessageRequest.class)),
                    description = "Update message request",
                    required = true)
            final UpdateMessageRequest updateMessageRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final UpdateSupportMessageResponse updateSupportMessageResponse =
                    supportMessageService.updateMessage(updateMessageRequest);
            return okResponse(updateSupportMessageResponse);
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    @PUT
    @Path(UPDATE_STATUS_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update support message status")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateSupportMessageResponse.class)),
            description = "Update support message response",
            responseCode = "200")
    @Timed
    public Response updateStatus(
            @NotNull
            @UpdateStatusRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateStatusRequest.class)),
                    description = "Update status request",
                    required = true)
            final UpdateStatusRequest updateStatusRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final UpdateSupportMessageResponse updateSupportMessageResponse =
                    supportMessageService.updateStatus(updateStatusRequest);
            return okResponse(updateSupportMessageResponse);
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }



    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a support message")
    @ApiResponse(
            responseCode = "204",
            description = "Returns 204 if deleted, otherwise 404")
    @Timed
    public Response deleteSupportMessageById(
            @NotNull
            @QueryParam("messageId")
            final String messageId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId,
            @Context
            @Parameter(hidden = true)
            final ServletContext servletContext)
    {
        final SupportMessageIdRequest supportMessageIdRequest = new SupportMessageIdRequest(messageId, traceId);

        final SupportMessageService supportMessageService = supportMessageService(servletContext);
        try {
            final DeleteSupportMessageByIdResponse deleteSupportMessageByIdResponse =
                    supportMessageService.deleteSupportMessageById(supportMessageIdRequest);
            if (deleteSupportMessageByIdResponse != null && deleteSupportMessageByIdResponse.isDeleted()) {
                return noContentResponse();
            }
            return notFoundResponse();
        } catch (AppbaseServiceException ase) {
            handleAppbaseServiceException(servletContext, ase);
        } catch (Exception e) {
            logger(servletContext).error("Error while handling request", e);
        }

        return badRequest();
    }

    private SupportMessageService supportMessageService(@Nullable final ServletContext servletContext) {
        final AppbaseCore appbaseCore = appbaseCore(servletContext);
        if (appbaseCore == null) {
            throw new AppbaseServiceRuntimeException("Cannot find an instance of " + AppbaseCore.class.getName());
        }

        final AppbaseServiceEntry appbaseServiceEntry = appbaseCore
                .getServiceRegistry()
                .getAppbaseService(SupportMessageService.class);
        if (appbaseServiceEntry == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find a servie entry for " + SupportMessageService.class.getName());
        }

        final SupportMessageService supportMessageService = (SupportMessageService) appbaseServiceEntry.getService();
        if (supportMessageService == null) {
            throw new AppbaseServiceRuntimeException(
                    "Cannot find an instance of " + SupportMessageService.class.getName());
        }

        return supportMessageService;
    }
}
