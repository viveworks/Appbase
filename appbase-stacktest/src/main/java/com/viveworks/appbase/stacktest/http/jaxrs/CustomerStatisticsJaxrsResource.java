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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import jakarta.inject.Inject;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Path(StackTestConstants.STACK_TEST_CUSTOMER_STATISTICS_RESOURCE)
public class CustomerStatisticsJaxrsResource {
    private final Logger logger;
    private final CustomerService customerService;

    @Inject
    public CustomerStatisticsJaxrsResource(final StackTestServices stackTestServices) {
        final AppbaseCore appbaseCore = stackTestServices.getAppbaseServices().getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.customerService = stackTestServices.getCustomerService();
    }

    @GET
    @Path("/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Customer statistics")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = CustomerStatisticsResponse.class)),
            description = "Customer statistics response",
            responseCode = "200")
    @Timed
    public Response statistics(
            @NotNull
            @PathParam("customerId")
            final Long customerId,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final CustomerStatisticsRequest customerStatisticsRequest =
                    new CustomerStatisticsRequest(customerId, AppbaseServiceUtil.traceId(traceId));

            final CustomerStatisticsResponse customerStatisticsResponse =
                    customerService.customerStatistics(customerStatisticsRequest);
            AppbaseUtil.checkedValidation(new CustomerStatisticsResponse.Validator(), customerStatisticsResponse);

            return Response
                    .ok()
                    .entity(customerStatisticsResponse)
                    .build();
        } catch (Exception e) {
            logger.error("Customer statistics={} error", customerId, e);
            throw new WebApplicationException(e);
        }
    }
}
