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
package com.viveworks.appbase.stacktest.http.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import jakarta.inject.Inject;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.pojo.order.CreateOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.CreateOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrderResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrdersRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.ReadOrdersResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.SearchOrdersRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.SearchOrdersResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.UpdateOrderRequest;
import com.viveworks.appbase.stacktest.data.pojo.order.UpdateOrderResponse;
import com.viveworks.appbase.stacktest.service.OrderService;
import com.viveworks.appbase.stacktest.service.StackTestServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(StackTestConstants.STACK_TEST_ORDERS_RESOURCE)
public class OrderJaxrsResource {
    private final Logger logger;
    private final OrderService orderService;

    @Inject
    public OrderJaxrsResource(final StackTestServices stackTestServices) {
        final AppbaseCore appbaseCore = stackTestServices.getAppbaseServices().getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.orderService = stackTestServices.getOrderService();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create order")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = CreateOrderResponse.class)),
            description = "The created order",
            responseCode = "201")
    @Timed
    public Response createOrder(
            @NotNull
            @CreateOrderRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateOrderRequest.class)),
                    description = "Create order request",
                    required = true)
            final CreateOrderRequest createOrderRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final CreateOrderRequest finalCreateOrderRequest;

            if (AppbaseUtil.isNotBlank(traceId)) {
                finalCreateOrderRequest = new CreateOrderRequest(
                        createOrderRequest.getCustomerId(),
                        createOrderRequest.getSku(),
                        createOrderRequest.getPrice(),
                        traceId);
            } else {
                finalCreateOrderRequest = new CreateOrderRequest(
                        createOrderRequest.getCustomerId(),
                        createOrderRequest.getSku(),
                        createOrderRequest.getPrice(),
                        AppbaseServiceUtil.traceId(createOrderRequest));
            }

            final CreateOrderResponse createOrderResponse = orderService.createOrder(finalCreateOrderRequest);
            AppbaseUtil.checkedValidation(new CreateOrderResponse.Validator(), createOrderResponse);

            final Order order = createOrderResponse.getOrder();
            if (order != null) {
                return Response
                        .created(URI.create(StackTestConstants.STACK_TEST_ORDERS_RESOURCE + "/" + order.getOrderId()))
                        .entity(createOrderResponse)
                        .build();
            } else {
                return Response.serverError().build();
            }
        } catch (ValidationServiceException vse) {
            logger.error("Create order={} validation error", createOrderRequest, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Create order={} stacktest error", createOrderRequest, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Create order={} error", createOrderRequest, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read orders")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadOrdersResponse.class)),
            description = "Read orders response",
            responseCode = "200")
    @Timed
    public Response readOrders(
            @NotNull
            @QueryParam("limit")
            final Integer limit,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadOrdersRequest readOrdersRequest =
                    new ReadOrdersRequest(limit, AppbaseServiceUtil.traceId(traceId));

            final ReadOrdersResponse readOrdersResponse = orderService.readOrders(readOrdersRequest);
            AppbaseUtil.checkedValidation(new ReadOrdersResponse.Validator(), readOrdersResponse);

            return Response
                    .ok()
                    .entity(readOrdersResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Read orders validation error", vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Read orders stacktest error", stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Read orders error", e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read order by ID")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadOrderResponse.class)),
            description = "Read order response",
            responseCode = "200")
    @Timed
    public Response readOrder(
            @NotNull
            @PathParam("orderId")
            final Long orderId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadOrderRequest readOrderRequest =
                    new ReadOrderRequest(orderId, AppbaseServiceUtil.traceId(traceId));

            final ReadOrderResponse readOrderResponse = orderService.readOrder(readOrderRequest);
            AppbaseUtil.checkedValidation(new ReadOrderResponse.Validator(), readOrderResponse);

            return Response
                    .ok()
                    .entity(readOrderResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Read order={} validation error", orderId, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Read order={} stacktest error", orderId, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Read by ID={} error", orderId, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/search/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search order by ID in elasticsearch")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SearchOrdersResponse.class)),
            description = "Search orders response",
            responseCode = "200")
    @Timed
    public Response searchOrders(
            @NotNull
            @PathParam("orderId")
            final Long orderId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final SearchOrdersRequest searchOrdersRequest =
                    new SearchOrdersRequest(orderId, AppbaseServiceUtil.traceId(traceId));
            final SearchOrdersResponse searchOrdersResponse = orderService.searchOrders(searchOrdersRequest);
            AppbaseUtil.checkedValidation(new SearchOrdersResponse.Validator(), searchOrdersResponse);

            return Response
                    .ok()
                    .entity(searchOrdersResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Search orders={} validation error", orderId, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Search orders={} stacktest error", orderId, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Search by ID={} error", orderId, e);
            throw new WebApplicationException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update order")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateOrderResponse.class)),
            description = "The updated order",
            responseCode = "200")
    @Timed
    public Response updateOrder(
            @NotNull
            @UpdateOrderRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateOrderRequest.class)),
                    description = "Update order request",
                    required = true)
            final UpdateOrderRequest updateOrderRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final UpdateOrderRequest finalUpdateOrderRequest;
            if (AppbaseUtil.isNotBlank(traceId)) {
                finalUpdateOrderRequest = new UpdateOrderRequest(updateOrderRequest.getOrder(), traceId);
            } else {
                finalUpdateOrderRequest = new UpdateOrderRequest(
                        updateOrderRequest.getOrder(), AppbaseServiceUtil.traceId(updateOrderRequest));
            }

            final UpdateOrderResponse updateOrderResponse = orderService.updateOrder(finalUpdateOrderRequest);
            AppbaseUtil.checkedValidation(new UpdateOrderResponse.Validator(), updateOrderResponse);

            return Response
                    .ok()
                    .entity(updateOrderResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Update orders={} validation error", updateOrderRequest, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Update orders={} stacktest error", updateOrderRequest, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Update order={} error", updateOrderRequest, e);
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Path("/{orderId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete order")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = DeleteOrderResponse.class)),
            description = "Delete order result",
            responseCode = "200")
    @Timed
    public Response deleteOrder(
            @NotNull
            @PathParam("orderId")
            final Long orderId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final DeleteOrderRequest deleteOrderRequest =
                    new DeleteOrderRequest(orderId, AppbaseServiceUtil.traceId(traceId));

            final DeleteOrderResponse deleteOrderResponse = orderService.deleteOrder(deleteOrderRequest);
            AppbaseUtil.checkedValidation(new DeleteOrderResponse.Validator(), deleteOrderResponse);

            return Response
                    .ok()
                    .entity(deleteOrderResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Delete order={} validation error", orderId, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Delete order={} stacktest error", orderId, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Delete order={} error", orderId, e);
            throw new WebApplicationException(e);
        }
    }
}
