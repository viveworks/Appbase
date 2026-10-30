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
import com.viveworks.appbase.stacktest.data.pojo.search.SearchRequest;
import com.viveworks.appbase.stacktest.data.pojo.search.SearchResponse;
import com.viveworks.appbase.stacktest.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
@Path(StackTestConstants.STACK_TEST_SEARCH_RESOURCE)
public class SearchJaxrsResource {
    private final Logger logger;
    private final SearchService searchService;

    @Inject
    public SearchJaxrsResource(final StackTestServices stackTestServices) {
        final AppbaseCore appbaseCore = stackTestServices.getAppbaseServices().getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.searchService = stackTestServices.getSearchService();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search resources")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SearchResponse.class)),
            description = "Search response",
            responseCode = "200")
    @Timed
    public Response search(
            @NotNull
            @SearchRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = SearchRequest.class)),
                    description = "Search request",
                    required = true)
            final SearchRequest searchRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final SearchRequest finalSearchRequest;

            if (AppbaseUtil.isNotBlank(traceId)) {
                finalSearchRequest = new SearchRequest(
                        searchRequest.getCustomer(),
                        searchRequest.getOrder(),
                        searchRequest.getProduct(),
                        traceId);
            } else {
                finalSearchRequest = new SearchRequest(
                        searchRequest.getCustomer(),
                        searchRequest.getOrder(),
                        searchRequest.getProduct(),
                        AppbaseServiceUtil.traceId(searchRequest));
            }

            final SearchResponse searchResponse = searchService.search(finalSearchRequest);
            AppbaseUtil.checkedValidation(new SearchResponse.Validator(), searchResponse);

            return Response
                    .ok()
                    .entity(searchResponse)
                    .build();
        } catch (Exception e) {
            logger.error("Search={} error", searchRequest, e);
            throw new WebApplicationException(e);
        }
    }
}
