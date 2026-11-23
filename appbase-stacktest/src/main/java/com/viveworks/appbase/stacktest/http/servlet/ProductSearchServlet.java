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

import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.pojo.product.SearchProductsRequest;
import com.viveworks.appbase.stacktest.data.pojo.product.SearchProductsResponse;
import com.viveworks.appbase.stacktest.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ProductSearchServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = -8990414822618569453L;

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
        final String sku = nonBlankParameter(StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        final SearchProductsRequest searchProductsRequest = new SearchProductsRequest(sku, traceId);
        return (T) productService.searchProducts(searchProductsRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<SearchProductsResponse> getResponseType() {
        return SearchProductsResponse.class;
    }

    @Override
    protected boolean isPostSupported() {
        return false;
    }

    @Override
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }
}
