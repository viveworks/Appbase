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

import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.bootstrap.service.AppbaseServices;
import com.viveworks.appbase.client.core.clientkey.DefaultMutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.zookeeper.accessor.DefaultDistributedCounterZKA;
import com.viveworks.appbase.client.service.zookeeper.accessor.DistributedCounterZKA;
import com.viveworks.appbase.client.service.zookeeper.ZooKeeperClientService;
import com.viveworks.appbase.client.service.jdbi.JdbiClientService;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.Shutdownable;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.accessor.aerospike.CustomerASA;
import com.viveworks.appbase.stacktest.accessor.aerospike.DefaultCustomerASA;
import com.viveworks.appbase.stacktest.accessor.aerospike.DefaultOrderASA;
import com.viveworks.appbase.stacktest.accessor.aerospike.DefaultProductASA;
import com.viveworks.appbase.stacktest.accessor.aerospike.OrderASA;
import com.viveworks.appbase.stacktest.accessor.aerospike.ProductASA;
import com.viveworks.appbase.stacktest.accessor.druid.DefaultDummyUserDRA;
import com.viveworks.appbase.stacktest.accessor.druid.DummyUserDRA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.CustomerSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.DefaultCustomerSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.DefaultOrderSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.DefaultProductSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.OrderSIA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.ProductSIA;
import com.viveworks.appbase.stacktest.accessor.s3.CustomerPhotoOSA;
import com.viveworks.appbase.stacktest.accessor.s3.DefaultCustomerPhotoOSA;
import com.viveworks.appbase.stacktest.accessor.vitess.CustomerJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.DefaultCustomerJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.DefaultOrderJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.DefaultProductJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.OrderJDBA;
import com.viveworks.appbase.stacktest.accessor.vitess.ProductJDBA;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class StackTestServices implements Shutdownable {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTestServices.class);

    private final AppbaseClientServices clientServices;
    private final AppbaseServices appbaseServices;
    private final Handle shardedHandle;
    private final Handle unshardedHandle;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final ProductService productService;
    private final SearchService searchService;
    private final AtomicBoolean shutdown;

    public StackTestServices(
            final AppbaseClientServices clientServices,
            final AppbaseServices appbaseServices)
    {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(appbaseServices, "appbaseServices");

        this.clientServices = clientServices;
        this.appbaseServices = appbaseServices;

        final JdbiClientService vitessJdbiClientService =
                clientServices.get(JdbiClientService.class);

        final Jdbi shardedJdbi = vitessJdbiClientService
                .getClientIntegrationService(
                        new DefaultMutableAppbaseClientKey<>(new TextValue(StackTestConstants.SHARDED)))
                .getClient();
        this.shardedHandle = shardedJdbi.open();

        final Jdbi unshardedJdbi = vitessJdbiClientService
                .getClientIntegrationService(
                        new DefaultMutableAppbaseClientKey<>(new TextValue(StackTestConstants.UNSHARDED)))
                .getClient();
        this.unshardedHandle = unshardedJdbi.open();

        final DistributedCounterZKA distributedCounterZKA = new DefaultDistributedCounterZKA(
                clientServices.getCoreServices().appbaseCore(),
                clientServices.get(ZooKeeperClientService.class),
                StackTestConstants.ZOOKEEPER_SHARED_COUNTER_PATH);
        final DummyUserDRA dummyUserDRA = new DefaultDummyUserDRA(clientServices);

        final CustomerASA customerASA = new DefaultCustomerASA(clientServices);
        final CustomerSIA customerSIA = new DefaultCustomerSIA(clientServices);
        final CustomerPhotoOSA customerPhotoOSA = new DefaultCustomerPhotoOSA(clientServices);
        final CustomerJDBA customerJDBA = new DefaultCustomerJDBA(clientServices, this.shardedHandle);
        this.customerService = new DefaultCustomerService(
                clientServices,
                true,
                customerASA,
                dummyUserDRA,
                customerSIA,
                customerPhotoOSA,
                customerJDBA,
                distributedCounterZKA);

        final OrderASA orderASA = new DefaultOrderASA(clientServices);
        final OrderSIA orderSIA = new DefaultOrderSIA(clientServices);
        final OrderJDBA orderJDBA = new DefaultOrderJDBA(clientServices, this.shardedHandle);
        this.orderService = new DefaultOrderService(clientServices, true, orderASA, orderSIA, orderJDBA);

        final ProductASA productASA = new DefaultProductASA(clientServices);
        final ProductSIA productSIA = new DefaultProductSIA(clientServices);
        final ProductJDBA productJDBA = new DefaultProductJDBA(clientServices, this.unshardedHandle);

        // The request and response objects are validated at the JAX-RS resources or servlets.
        // Disable manual validation and the automated validation on the service does not kick-in either
        this.productService = new DefaultProductService(clientServices, true, productASA, productSIA, productJDBA);

        this.searchService = new DefaultSearchService(clientServices, true, customerSIA, orderSIA, productSIA);
        this.shutdown = new AtomicBoolean(false);
    }

    public AppbaseClientServices getClientServices() {
        return clientServices;
    }

    public AppbaseServices getAppbaseServices() {
        return appbaseServices;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public OrderService getOrderService() {
        return orderService;
    }

    public ProductService getProductService() {
        return productService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    @Override
    public synchronized void shutdown() throws AppbaseRuntimeException {
        try {
            appbaseServices.shutdown();
        } finally {
            try {
                shardedHandle.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close sharded JDBI handle during shutdown", e);
            }
            try {
                unshardedHandle.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close unsharded JDBI handle during shutdown", e);
            }
            clientServices.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}
