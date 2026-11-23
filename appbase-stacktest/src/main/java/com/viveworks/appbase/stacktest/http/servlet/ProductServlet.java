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
package com.viveworks.appbase.stacktest.http.servlet;

import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ProductServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = -6666384739765961226L;

    private ProductService productService;

    @Override
    public void init() {
        super.init();

        productService = stackTestServices().getProductService();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String sku = AppbaseServiceUtil.parameter(StackTestConstants.ID_PARAM, httpServletRequest);
        if (!AppbaseUtil.isBlank(sku)) {
            final ReadProductRequest readProductRequest = new ReadProductRequest(sku, traceId);
            final ReadProductResponse readProductResponse = productService.readProduct(readProductRequest);
            return (T) json(ReadProductResponse.class, readProductResponse);
        } else {
            final String searchSku = AppbaseServiceUtil.parameter(StackTestConstants.SEARCH_PARAM, httpServletRequest);
            if (!AppbaseUtil.isBlank(searchSku)) {
                final SearchProductsRequest searchProductsRequest = new SearchProductsRequest(searchSku, traceId);
                final SearchProductsResponse searchProductsResponse =
                        productService.searchProducts(searchProductsRequest);
                return (T) json(SearchProductsResponse.class, searchProductsResponse);
            }
        }

        final int limit = intParameter(StackTestConstants.LIMIT_PARAM, httpServletRequest, httpServletResponse);
        final ReadProductsRequest readProductsRequest = new ReadProductsRequest(limit, traceId);
        final ReadProductsResponse readProductsResponse = productService.readProducts(readProductsRequest);
        return (T) json(ReadProductsResponse.class, readProductsResponse);
    }

    @Override
    protected boolean isGetImplResultJson() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateProductRequest> postRequestType() {
        return CreateProductRequest.class;
    }

    @Override
    protected <T extends Traceable> Object postImpl(final T traceable) throws Exception {
        return productService.createProduct((CreateProductRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateProductResponse> postResponseType() {
        return CreateProductResponse.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateProductRequest> putRequestType() {
        return UpdateProductRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(final T traceable) throws Exception {
        return productService.updateProduct((UpdateProductRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateProductResponse> putResponseType() {
        return UpdateProductResponse.class;
    }

    @Override
    protected <T extends Traceable> Object deleteImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String sku = nonBlankParameter(StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        final DeleteProductRequest deleteProductRequest = new DeleteProductRequest(sku, traceId);
        return productService.deleteProduct(deleteProductRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<DeleteProductResponse> deleteResponseType() {
        return DeleteProductResponse.class;
    }
}
