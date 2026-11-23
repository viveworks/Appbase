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
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.CreateProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.DeleteProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductsRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.ReadProductsResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.SearchProductsRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.SearchProductsResponse;
import com.viveworks.appbase.stacktest.data.pojo.product.UpdateProductRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.UpdateProductResponse;
import com.viveworks.appbase.stacktest.service.ProductService;
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
@Path(StackTestConstants.STACK_TEST_PRODUCTS_RESOURCE)
public class ProductJaxrsResource {
    private final Logger logger;
    private final ProductService productService;

    @Inject
    public ProductJaxrsResource(final StackTestServices stackTestServices) {
        final AppbaseCore appbaseCore = stackTestServices.getAppbaseServices().getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.productService = stackTestServices.getProductService();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create product")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = CreateProductResponse.class)),
            description = "The created product",
            responseCode = "201")
    @Timed
    public Response createProduct(
            @NotNull
            @CreateProductRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateProductRequest.class)),
                    description = "Create product request",
                    required = true)
            final CreateProductRequest createProductRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final CreateProductRequest finalCreateProductRequest;

            if (AppbaseUtil.isNotBlank(traceId)) {
                finalCreateProductRequest = new CreateProductRequest(
                        createProductRequest.getSku(),
                        createProductRequest.getDescription(),
                        createProductRequest.getPrice(),
                        traceId);
            } else {
                finalCreateProductRequest = new CreateProductRequest(
                        createProductRequest.getSku(),
                        createProductRequest.getDescription(),
                        createProductRequest.getPrice(),
                        AppbaseServiceUtil.traceId(createProductRequest));
            }

            final CreateProductResponse createProductResponse =
                    productService.createProduct(finalCreateProductRequest);
            AppbaseUtil.checkedValidation(new CreateProductResponse.Validator(), createProductResponse);

            final Product product = createProductResponse.getProduct();
            if (product != null) {
                return Response
                        .created(URI.create(StackTestConstants.STACK_TEST_PRODUCTS_RESOURCE + "/" + product.getSku()))
                        .entity(createProductResponse)
                        .build();
            } else {
                return Response.serverError().build();
            }
        } catch (Exception e) {
            logger.error("Create product={} error", createProductRequest, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read products")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadProductsResponse.class)),
            description = "Read products response",
            responseCode = "200")
    @Timed
    public Response readProducts(
            @NotNull
            @QueryParam("limit")
            final Integer limit,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadProductsRequest readProductsRequest =
                    new ReadProductsRequest(limit, AppbaseServiceUtil.traceId(traceId));

            final ReadProductsResponse readProductsResponse = productService.readProducts(readProductsRequest);
            AppbaseUtil.checkedValidation(new ReadProductsResponse.Validator(), readProductsResponse);

            return Response
                    .ok()
                    .entity(readProductsResponse)
                    .build();
        } catch (Exception e) {
            logger.error("Read products error", e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/{sku}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read product by SKU")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = ReadProductResponse.class)),
            description = "Read product response",
            responseCode = "200")
    @Timed
    public Response readProduct(
            @NotNull
            @PathParam("sku")
            final String sku,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final ReadProductRequest readProductRequest =
                    new ReadProductRequest(sku, AppbaseServiceUtil.traceId(traceId));

            final ReadProductResponse readProductResponse = productService.readProduct(readProductRequest);
            AppbaseUtil.checkedValidation(new ReadProductResponse.Validator(), readProductResponse);

            return Response
                    .ok()
                    .entity(readProductResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Read by ID={} validation error", sku, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Read by ID={} stacktest error", sku, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Read by ID={} error", sku, e);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/search/{sku}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search product by ID in elasticsearch")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = SearchProductsResponse.class)),
            description = "Search products response",
            responseCode = "200")
    @Timed
    public Response searchProducts(
            @NotNull
            @PathParam("sku")
            final String sku,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final SearchProductsRequest searchProductsRequest =
                    new SearchProductsRequest(sku, AppbaseServiceUtil.traceId(traceId));
            final SearchProductsResponse searchProductsResponse = productService.searchProducts(searchProductsRequest);
            AppbaseUtil.checkedValidation(new SearchProductsResponse.Validator(), searchProductsResponse);

            return Response
                    .ok()
                    .entity(searchProductsResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Search by ID={} validation error", sku, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Search by ID={} stacktest error", sku, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Search by ID={} error", sku, e);
            throw new WebApplicationException(e);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update product")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = UpdateProductResponse.class)),
            description = "The updated product",
            responseCode = "200")
    @Timed
    public Response updateProduct(
            @NotNull
            @UpdateProductRequest.Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = UpdateProductRequest.class)),
                    description = "Update product request",
                    required = true)
            final UpdateProductRequest updateProductRequest,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final UpdateProductRequest finalUpdateProductRequest;
            if (AppbaseUtil.isNotBlank(traceId)) {
                finalUpdateProductRequest = new UpdateProductRequest(
                        updateProductRequest.getProduct(), traceId);
            } else {
                finalUpdateProductRequest = new UpdateProductRequest(
                        updateProductRequest.getProduct(), AppbaseServiceUtil.traceId(updateProductRequest));
            }

            final UpdateProductResponse updateProductResponse =
                    productService.updateProduct(finalUpdateProductRequest);
            AppbaseUtil.checkedValidation(new UpdateProductResponse.Validator(), updateProductResponse);

            return Response
                    .ok()
                    .entity(updateProductResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Update order={} validation error", updateProductRequest, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Update order={} stacktest error", updateProductRequest, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Update order={} error", updateProductRequest, e);
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Path("/{sku}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete product")
    @ApiResponse(
            content = @Content(schema = @Schema(implementation = DeleteProductResponse.class)),
            description = "Delete product result",
            responseCode = "200")
    @Timed
    public Response deleteProduct(
            @NotNull
            @PathParam("sku")
            final String sku,
            @Nullable
            @HeaderParam(TracerService.TRACE_ID_KEY)
            @Parameter(in = ParameterIn.HEADER, name = TracerService.TRACE_ID_KEY)
            final String traceId)
    {
        try {
            final DeleteProductRequest deleteProductRequest =
                    new DeleteProductRequest(sku, AppbaseServiceUtil.traceId(traceId));

            final DeleteProductResponse deleteProductResponse = productService.deleteProduct(deleteProductRequest);
            AppbaseUtil.checkedValidation(new DeleteProductResponse.Validator(), deleteProductResponse);

            return Response
                    .ok()
                    .entity(deleteProductResponse)
                    .build();
        } catch (ValidationServiceException vse) {
            logger.error("Delete product={} validation error", sku, vse);
            throw new WebApplicationException(vse, Response.Status.BAD_REQUEST);
        } catch (StackTestServiceException stse) {
            logger.error("Delete product={} stacktest error", sku, stse);
            throw new WebApplicationException(stse);
        } catch (Exception e) {
            logger.error("Delete product={} error", sku, e);
            throw new WebApplicationException(e);
        }
    }
}
