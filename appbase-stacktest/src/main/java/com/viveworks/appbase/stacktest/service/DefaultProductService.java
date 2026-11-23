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
package com.viveworks.appbase.stacktest.service;

import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.accessor.aerospike.ProductASA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.ProductSIA;
import com.viveworks.appbase.stacktest.accessor.vitess.ProductJDBA;
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
import jakarta.validation.executable.ValidateOnExecution;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultProductService implements ProductService {
    private final Logger logger;
    private final boolean manualValidation;
    private final ProductASA productASA;
    private final ProductSIA productSIA;
    private final ProductJDBA productJDBA;

    private final Timer searchProductsTimer;
    private final Timer createProductTimer;
    private final Timer readByIdTimer;
    private final Timer readProductsTimer;
    private final Timer updateProductTimer;
    private final Timer deleteByIdTimer;

    public DefaultProductService(
            final AppbaseClientServices clientServices,
            final boolean manualValidation,
            final ProductASA productASA,
            final ProductSIA productSIA,
            final ProductJDBA productJDBA)
    {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(productASA, "productASA");
        AppbaseUtil.notNull(productSIA, "productSIA");
        AppbaseUtil.notNull(productJDBA, "productJDBA");

        final AppbaseCore appbaseCore = clientServices.getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.manualValidation = manualValidation;
        this.productASA = productASA;
        this.productSIA = productSIA;
        this.productJDBA = productJDBA;

        final MetricService metricService = clientServices.getCoreServices().appbaseCore().getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.searchProductsTimer = metricRegistry.timer(metricService.name(getClass(), "searchProductsTimer"));
        this.createProductTimer = metricRegistry.timer(metricService.name(getClass(), "createProductTimer"));
        this.readByIdTimer = metricRegistry.timer(metricService.name(getClass(), "readByIdTimer"));
        this.readProductsTimer = metricRegistry.timer(metricService.name(getClass(), "readProductsTimer"));
        this.updateProductTimer = metricRegistry.timer(metricService.name(getClass(), "updateProductTimer"));
        this.deleteByIdTimer = metricRegistry.timer(metricService.name(getClass(), "deleteByIdTimer"));
    }

    @Override
    @ValidateOnExecution
    @SearchProductsResponse.Valid
    public SearchProductsResponse searchProducts(
            @SearchProductsRequest.Valid
            final SearchProductsRequest searchProductsRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(searchProductsRequest, "searchProductsRequest");

        final Timer.Context timer = searchProductsTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new SearchProductsRequest.Validator(), searchProductsRequest);
            }

            final Product product = new Product();
            product.setSku(searchProductsRequest.getSkuPattern());

            try {
                final List<Product> products = productSIA.search(product);

                final SearchProductsResponse searchProductsResponse =
                        new SearchProductsResponse(products, searchProductsRequest.getTraceId());
                if (manualValidation) {
                    AppbaseUtil.checkedValidation(new SearchProductsResponse.Validator(), searchProductsResponse);
                }
                return searchProductsResponse;
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @CreateProductResponse.Valid
    public CreateProductResponse createProduct(
            @CreateProductRequest.Valid
            final CreateProductRequest createProductRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(createProductRequest, "createProductRequest");

        final Timer.Context timer = createProductTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CreateProductRequest.Validator(), createProductRequest);
            }

            final Product product = new Product();
            product.setSku(createProductRequest.getSku());
            product.setDescription(createProductRequest.getDescription());
            product.setPrice(createProductRequest.getPrice());

            final long count = productJDBA.createProduct(product);
            if (count != 1) {
                throw new StackTestServiceException("Failed to create product (exists already?)" + product);
            }

            try {
                productSIA.addDocument(product);
            } catch (IOException e) {
                logger.error("Error while adding document: {}", product, e);
            }

            try {
                productASA.insertProduct(product);
            } catch (AerospikeAccessorException e) {
                logger.error("Error while inserting: {}", product, e);
            }

            final CreateProductResponse createProductResponse =
                    new CreateProductResponse(product, createProductRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CreateProductResponse.Validator(), createProductResponse);
            }
            return createProductResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @ReadProductsResponse.Valid
    public ReadProductsResponse readProducts(
            @ReadProductsRequest.Valid
            final ReadProductsRequest readProductsRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(readProductsRequest, "readProductsRequest");

        final Timer.Context timer = readProductsTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadProductsRequest.Validator(), readProductsRequest);
            }

            final List<Product> products = productJDBA.readProducts(readProductsRequest.getLimit());
            final ReadProductsResponse readProductsResponse =
                    new ReadProductsResponse(products, readProductsRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadProductsResponse.Validator(), readProductsResponse);
            }
            return readProductsResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @ReadProductResponse.Valid
    public ReadProductResponse readProduct(
            @ReadProductRequest.Valid
            final ReadProductRequest readProductRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(readProductRequest, "readProductRequest");

        final Timer.Context timer = readByIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadProductRequest.Validator(), readProductRequest);
            }

            final String sku = readProductRequest.getSku();

            try {
                final Product product = productASA.readProduct(sku);
                if (product != null) {
                    logger.debug("Cache hit: {}", product);

                    return new ReadProductResponse(product, readProductRequest.getTraceId());
                }
                logger.debug("Cache miss: {}", sku);
            } catch (AerospikeAccessorException e) {
                logger.error("Error reading product", e);
            }

            final Product product = productJDBA.readById(sku);

            if (product != null) {
                try {
                    productASA.insertProduct(product);
                } catch (AerospikeAccessorException e) {
                    logger.error("Error while inserting: {}", product, e);
                }
            }

            final ReadProductResponse readProductResponse =
                    new ReadProductResponse(product, readProductRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadProductResponse.Validator(), readProductResponse);
            }
            return readProductResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @UpdateProductResponse.Valid
    public UpdateProductResponse updateProduct(
            @UpdateProductRequest.Valid
            final UpdateProductRequest updateProductRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(updateProductRequest, "updateProductRequest");

        final Timer.Context timer = updateProductTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new UpdateProductRequest.Validator(), updateProductRequest);
            }

            final Product product = updateProductRequest.getProduct();
            final int updated = productJDBA.updateProduct(product);

            if (updated > 0) {
                try {
                    productSIA.updateDocument(product);
                } catch (IOException e) {
                    logger.error("Error while updating document: {}", product, e);
                }

                try {
                    productASA.deleteProduct(product.getSku());
                } catch (AerospikeAccessorException e) {
                    logger.error("Error while deleting: {}", product, e);
                }
            }

            final UpdateProductResponse updateProductResponse =
                    new UpdateProductResponse(product, (updated > 0), updateProductRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new UpdateProductResponse.Validator(), updateProductResponse);
            }
            return updateProductResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @DeleteProductResponse.Valid
    public DeleteProductResponse deleteProduct(
            @DeleteProductRequest.Valid
            final DeleteProductRequest deleteProductRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(deleteProductRequest, "deleteProductRequest");

        final Timer.Context timer = deleteByIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new DeleteProductRequest.Validator(), deleteProductRequest);
            }

            final String sku = deleteProductRequest.getSku();
            final boolean deleted = productJDBA.deleteById(sku);

            if (deleted) {
                try {
                    productSIA.deleteDocument(sku);
                } catch (IOException e) {
                    logger.error("Error while deleting document: {}", sku, e);
                }

                try {
                    productASA.deleteProduct(sku);
                } catch (AerospikeAccessorException e) {
                    logger.error("Error while deleting product: {}", sku, e);
                }
            }

            final DeleteProductResponse deleteProductResponse =
                    new DeleteProductResponse(deleted, deleteProductRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new DeleteProductResponse.Validator(), deleteProductResponse);
            }
            return deleteProductResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }
}
