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
package com.viveworks.appbase.stacktest.http.jaxrs;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.tracer.TracerService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import jakarta.inject.Inject;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerOrdersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerOrdersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.order.DeleteOrderResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
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
@Path(StackTestConstants.STACK_TEST_CUSTOMERS_RESOURCE)
public class CustomerJaxrsResource {
    private final Logger logger;
    private final CustomerService customerService;

    @Inject
    public CustomerJaxrsResource(final StackTestServices stackTestServices) {
        final AppbaseCore appbaseCore = stackTestServices.getAppbaseServices().getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.customerService = stackTestServices.getCustomerService();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create customer")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = CreateCustomerResponse.class)),
            description = "The created customer",
            responseCode = "201")
    @Timed
    public Response createCustomer(
            @NotNull
            @CreateCustomerRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateCustomerRequest.class)),
                    description = "Create customer request",
                    required = true)
            final CreateCustomerRequest createCustomerRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final CreateCustomerRequest finalCreateCustomerRequest;

            if (AppbaseUtil.isNotBlank(traceId)) {
                finalCreateCustomerRequest = new CreateCustomerRequest(
                        createCustomerRequest.getEmail(),
                        createCustomerRequest.getPhotoId(),
                        traceId);
            } else {
                finalCreateCustomerRequest = new CreateCustomerRequest(
                        createCustomerRequest.getEmail(),
                        createCustomerRequest.getPhotoId(),
                        AppbaseServiceUtil.traceId(createCustomerRequest));
            }

            final CreateCustomerResponse createCustomerResponse =
                    customerService.createCustomer(finalCreateCustomerRequest);
            AppbaseUtil.checkedValidation(new CreateCustomerResponse.Validator(), createCustomerResponse);

            final Customer customer = createCustomerResponse.getCustomer();
            if (customer != null) {
                return Response
                        .created(URI.create(
                                StackTestConstants.STACK_TEST_CUSTOMERS_RESOURCE + "/" + customer.getCustomerId()))
                        .entity(createCustomerResponse)
                        .build();
            } else {
                return Response.serverError().build();
            }
        } catch (ValidationServiceException vse) {
            logger.error("Create customer={} validation error", createCustomerRequest, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Create customer={} stacktest error", createCustomerRequest, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Create customer={} error", createCustomerRequest, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read customers")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadCustomersResponse.class)),
            description = "Read customers response",
            responseCode = "200")
    @Timed
    public Response readCustomers(
            @NotNull
            @QueryParam("limit")
            final Integer limit,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadCustomersRequest readCustomersRequest =
                    new ReadCustomersRequest(limit, AppbaseServiceUtil.traceId(traceId));
            final ReadCustomersResponse readCustomersResponse = customerService.readCustomers(readCustomersRequest);
            AppbaseUtil.checkedValidation(new ReadCustomersResponse.Validator(), readCustomersResponse);

            return Response
                    .ok()
                    .entity(readCustomersResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Read customers validation error", vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Read customers stacktest error", stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Read customers error", e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read customer by ID")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadCustomerResponse.class)),
            description = "Read customer response",
            responseCode = "200")
    @Timed
    public Response readCustomer(
            @NotNull
            @PathParam("customerId")
            final Long customerId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadCustomerRequest readCustomerRequest =
                    new ReadCustomerRequest(customerId, AppbaseServiceUtil.traceId(traceId));

            final ReadCustomerResponse readCustomerResponse = customerService.readCustomer(readCustomerRequest);
            AppbaseUtil.checkedValidation(new ReadCustomerResponse.Validator(), readCustomerResponse);

            return Response
                    .ok()
                    .entity(readCustomerResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Read customer={} validation error", customerId, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Read customer={} stacktest error", customerId, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Read by ID={} error", customerId, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/{customerId}/orders")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read customer orders")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadCustomerOrdersResponse.class)),
            description = "Read customer orders response",
            responseCode = "200")
    @Timed
    public Response readCustomerOrders(
            @NotNull
            @PathParam("customerId")
            final Long customerId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadCustomerOrdersRequest readCustomerOrdersRequest =
                    new ReadCustomerOrdersRequest(customerId, AppbaseServiceUtil.traceId(traceId));
            final ReadCustomerOrdersResponse readCustomerOrdersResponse =
                    customerService.readCustomerOrders(readCustomerOrdersRequest);
            AppbaseUtil.checkedValidation(new ReadCustomerOrdersResponse.Validator(), readCustomerOrdersResponse);

            return Response.ok(readCustomerOrdersResponse).build();
        } catch (ValidationServiceException vse) {
            logger.error("Read customer={} orders validation error", customerId, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Read customer={} orders stacktest error", customerId, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Read customer orders error [customerId={}]", customerId, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/search/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search customer by email in elasticsearch - DOES NOT WORK!")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SearchCustomersResponse.class)),
            description = "Search customer response",
            responseCode = "200")
    @Timed
    public Response searchCustomers(
            @NotNull
            @PathParam("email")
            final String email,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final SearchCustomersRequest searchCustomersRequest =
                    new SearchCustomersRequest(email, AppbaseServiceUtil.traceId(traceId));
            final SearchCustomersResponse searchCustomersResponse =
                    customerService.searchCustomers(searchCustomersRequest);
            AppbaseUtil.checkedValidation(new SearchCustomersResponse.Validator(), searchCustomersResponse);

            return Response
                    .ok()
                    .entity(searchCustomersResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Search customers={} validation error", email, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Search customers={} orders stacktest error", email, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Search by email={} error", email, e);
            throw new WebApplicationException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update customer")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateCustomerResponse.class)),
            description = "The updated customer",
            responseCode = "200")
    @Timed
    public Response updateCustomer(
            @NotNull
            @UpdateCustomerRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateCustomerRequest.class)),
                    description = "Update customer request",
                    required = true)
            final UpdateCustomerRequest updateCustomerRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final UpdateCustomerRequest finalUpdateCustomerRequest;
            if (AppbaseUtil.isNotBlank(traceId)) {
                finalUpdateCustomerRequest = new UpdateCustomerRequest(updateCustomerRequest.getCustomer(), traceId);
            } else {
                finalUpdateCustomerRequest = new UpdateCustomerRequest(
                        updateCustomerRequest.getCustomer(), AppbaseServiceUtil.traceId(updateCustomerRequest));
            }

            final UpdateCustomerResponse updateCustomerResponse =
                    customerService.updateCustomer(finalUpdateCustomerRequest);
            AppbaseUtil.checkedValidation(new UpdateCustomerResponse.Validator(), updateCustomerResponse);

            return Response
                    .ok()
                    .entity(updateCustomerResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Update customer={} validation error", updateCustomerRequest, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Update customer={} stacktest error", updateCustomerRequest, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Update customer={} error", updateCustomerRequest, e);
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete customer")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = DeleteOrderResponse.class)),
            description = "Delete customer result",
            responseCode = "200")
    @Timed
    public Response deleteOrder(
            @NotNull
            @PathParam("customerId")
            final Long customerId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final DeleteCustomerRequest deleteCustomerRequest =
                    new DeleteCustomerRequest(customerId, AppbaseServiceUtil.traceId(traceId));

            final DeleteCustomerResponse deleteCustomerResponse =
                    customerService.deleteCustomer(deleteCustomerRequest);
            AppbaseUtil.checkedValidation(new DeleteCustomerResponse.Validator(), deleteCustomerResponse);

            return Response
                    .ok()
                    .entity(deleteCustomerResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Delete customer={} validation error", customerId, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Delete customer={} stacktest error", customerId, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Delete customer={} error", customerId, e);
            throw new WebApplicationException(e);
        }
    }
}
