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

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.CustomerSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.OrderSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.ProductSIA;
import com.viveworks.appbase.stacktest.data.model.Customer;
import com.viveworks.appbase.stacktest.data.model.Order;
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.data.pojo.search.SearchRequest;
import com.viveworks.appbase.stacktest.data.pojo.search.SearchResponse;
import jakarta.validation.executable.ValidateOnExecution;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultSearchService implements SearchService {
    private final ValidationService validationService;
    private final boolean manualValidation;
    private final CustomerSIA customerSIA;
    private final OrderSIA orderSIA;
    private final ProductSIA productSIA;

    private final Timer searchTimer;

    public DefaultSearchService(
            final AppbaseClientServices clientServices,
            final boolean manualValidation,
            final CustomerSIA customerSIA,
            final OrderSIA orderSIA,
            final ProductSIA productSIA)
    {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(customerSIA, "customerSIA");
        AppbaseUtil.notNull(orderSIA, "orderSIA");
        AppbaseUtil.notNull(productSIA, "productSIA");

        final AppbaseCore appbaseCore = clientServices.getCoreServices().appbaseCore();
        this.validationService = appbaseCore.getValidationService();
        this.manualValidation = manualValidation;
        this.customerSIA = customerSIA;
        this.orderSIA = orderSIA;
        this.productSIA = productSIA;

        final MetricService metricService = clientServices.getCoreServices().appbaseCore().getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.searchTimer = metricRegistry.timer(metricService.name(getClass(), "searchTimer"));
    }

    @Override
    @ValidateOnExecution
    @SearchResponse.Valid
    public SearchResponse search(
            @SearchRequest.Valid
            final SearchRequest searchRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(searchRequest, "searchRequest");

        final Timer.Context timer = searchTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.validate(new SearchRequest.Validator(), searchRequest);
            }

            // TODO make each call async and get() the CompletableFuture at the end
            final List<Customer> customers = customerSearch(searchRequest.getCustomer());
            final List<Order> orders = orderSearch(searchRequest.getOrder());
            final List<Product> products = productSearch(searchRequest.getProduct());

            final SearchResponse searchResponse =
                    new SearchResponse(customers, orders, products, searchRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.validate(new SearchResponse.Validator(), searchResponse);
            }
            return searchResponse;
        } finally {
            timer.stop();
        }
    }

    private List<Product> productSearch(
            @Nullable final Product product) throws StackTestServiceException
    {
        final List<Product> products = new ArrayList<>();

        if (product != null) {
            try {
                final List<Product> productSearch = productSIA.search(product);
                if (productSearch != null) {
                    products.addAll(productSearch);
                }
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        }

        return products;
    }

    private List<Order> orderSearch(
            @Nullable final Order order) throws StackTestServiceException
    {
        final List<Order> orders = new ArrayList<>();

        if (order != null) {
            try {
                final List<Order> orderSearch = orderSIA.search(order);
                if (orderSearch != null) {
                    orders.addAll(orderSearch);
                }
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        }

        return orders;
    }

    private List<Customer> customerSearch(
            @Nullable final Customer customer) throws StackTestServiceException
    {
        final List<Customer> customers = new ArrayList<>();

        if (customer != null) {
            try {
                final List<Customer> customerSearch = customerSIA.search(customer);
                if (customerSearch != null) {
                    customers.addAll(customerSearch);
                }
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        }

        return customers;
    }
}
