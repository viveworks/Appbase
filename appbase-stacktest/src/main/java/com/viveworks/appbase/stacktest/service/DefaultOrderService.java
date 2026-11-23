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
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.accessor.aerospike.OrderASA;
import com.viveworks.appbase.stacktest.accessor.elasticsearch.OrderSIA;
import com.viveworks.appbase.stacktest.accessor.vitess.OrderJDBA;
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
public class DefaultOrderService implements OrderService {
    private final Logger logger;
    private final ValidationService validationService;
    private final boolean manualValidation;
    private final OrderASA orderASA;
    private final OrderSIA orderSIA;
    private final OrderJDBA orderJDBA;

    private final Timer searchOrdersTimer;
    private final Timer createOrderTimer;
    private final Timer readOrdersTimer;
    private final Timer readByIdTimer;
    private final Timer updateOrderTimer;
    private final Timer deleteByIdTimer;

    public DefaultOrderService(
            final AppbaseClientServices clientServices,
            final boolean manualValidation,
            final OrderASA orderASA,
            final OrderSIA orderSIA,
            final OrderJDBA orderJDBA)
    {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(orderASA, "orderASA");
        AppbaseUtil.notNull(orderSIA, "orderSIA");
        AppbaseUtil.notNull(orderJDBA, "orderJDBA");

        final AppbaseCore appbaseCore = clientServices.getCoreServices().appbaseCore();
        this.logger = appbaseCore.getLogService().getLogger(getClass());
        this.validationService = appbaseCore.getValidationService();
        this.manualValidation = manualValidation;
        this.orderASA = orderASA;
        this.orderSIA = orderSIA;
        this.orderJDBA = orderJDBA;

        final MetricService metricService = appbaseCore.getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.searchOrdersTimer = metricRegistry.timer(metricService.name(getClass(), "searchOrdersTimer"));
        this.createOrderTimer = metricRegistry.timer(metricService.name(getClass(), "createOrderTimer"));
        this.readOrdersTimer = metricRegistry.timer(metricService.name(getClass(), "readOrdersTimer"));
        this.readByIdTimer = metricRegistry.timer(metricService.name(getClass(), "readByIdTimer"));
        this.updateOrderTimer = metricRegistry.timer(metricService.name(getClass(), "updateOrderTimer"));
        this.deleteByIdTimer = metricRegistry.timer(metricService.name(getClass(), "deleteByIdTimer"));
    }

    @Override
    @ValidateOnExecution
    @SearchOrdersResponse.Valid
    public SearchOrdersResponse searchOrders(
            @SearchOrdersRequest.Valid
            final SearchOrdersRequest searchOrdersRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(searchOrdersRequest, "searchOrdersRequest");

        final Timer.Context timer = searchOrdersTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new SearchOrdersRequest.Validator(), searchOrdersRequest);
            }

            try {

                final Order order = new Order();
                order.setOrderId(searchOrdersRequest.getOrderId());

                final List<Order> orders = orderSIA.search(order);
                final SearchOrdersResponse searchOrdersResponse =
                        new SearchOrdersResponse(orders, searchOrdersRequest.getTraceId());
                if (manualValidation) {
                    AppbaseUtil.checkedValidation(new SearchOrdersResponse.Validator(), searchOrdersResponse);
                }

                return searchOrdersResponse;
            } catch (IOException e) {
                throw new StackTestServiceException(e);
            }
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        }
        finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @CreateOrderResponse.Valid
    public CreateOrderResponse createOrder(
            @CreateOrderRequest.Valid
            final CreateOrderRequest createOrderRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(createOrderRequest, "createOrderRequest");

        final Timer.Context timer = createOrderTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CreateOrderRequest.Validator(), createOrderRequest);
            }

            final Long customerId = createOrderRequest.getCustomerId();
            final String sku = createOrderRequest.getSku();
            final Double price = createOrderRequest.getPrice();

            final long orderId = orderJDBA.createOrder(customerId, sku, price);
            final Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerId(customerId);
            order.setSku(sku);
            order.setPrice(price);

            try {
                orderSIA.addDocument(order);
            } catch (IOException e) {
                logger.error("Error adding document: {}", order, e);
            }

            try {
                orderASA.insertOrder(order);
            } catch (AerospikeAccessorException e) {
                logger.error("Error inserting order: {}", order, e);
            }

            final CreateOrderResponse createOrderResponse =
                    new CreateOrderResponse(order, createOrderRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new CreateOrderResponse.Validator(), createOrderResponse);
            }

            return createOrderResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @ReadOrdersResponse.Valid
    public ReadOrdersResponse readOrders(
            @ReadOrdersRequest.Valid
            final ReadOrdersRequest readOrdersRequest)
            throws StackTestServiceException
    {
        AppbaseUtil.notNull(readOrdersRequest, "readOrdersRequest");

        final Timer.Context timer = readOrdersTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadOrdersRequest.Validator(), readOrdersRequest);
            }

            final List<Order> orders = orderJDBA.readOrders(readOrdersRequest.getLimit());
            final ReadOrdersResponse readOrdersResponse = new ReadOrdersResponse(orders, readOrdersRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadOrdersResponse.Validator(), readOrdersResponse);
            }

            return readOrdersResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @ReadOrderResponse.Valid
    public ReadOrderResponse readOrder(
            @ReadOrderRequest.Valid
            final ReadOrderRequest readOrderRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(readOrderRequest, "readOrderRequest");

        final Timer.Context timer = readByIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadOrderRequest.Validator(), readOrderRequest);
            }

            final Long orderId = readOrderRequest.getOrderId();
            try {
                final Order order = orderASA.readOrder(orderId);
                if (order != null) {
                    logger.debug("Cache hit: {}", order);

                    return new ReadOrderResponse(order, readOrderRequest.getTraceId());
                }
                logger.debug("Cache miss: {}", order);
            } catch (AerospikeAccessorException e) {
                logger.error("Error reading product", e);
            }

            final Order order = orderJDBA.readById(orderId);

            if (order != null) {
                try {
                    orderASA.insertOrder(order);
                } catch (AerospikeAccessorException e) {
                    logger.error("Error inserting order: {}", order, e);
                }
            }

            final ReadOrderResponse readOrderResponse =
                    new ReadOrderResponse(order, readOrderRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new ReadOrderResponse.Validator(), readOrderResponse);
            }

            return readOrderResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @UpdateOrderResponse.Valid
    public UpdateOrderResponse updateOrder(
            @UpdateOrderRequest.Valid
            final UpdateOrderRequest updateOrderRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(updateOrderRequest, "updateOrderRequest");

        final Timer.Context timer = updateOrderTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new UpdateOrderRequest.Validator(), updateOrderRequest);
            }

            final Order order = updateOrderRequest.getOrder();
            final int updated = orderJDBA.updateOrder(order);

            if (updated > 0) {
                try {
                    orderSIA.updateDocument(order);
                } catch (IOException e) {
                    logger.error("Error updating document: {}", order, e);
                }

                try {
                    orderASA.deleteOrder(order.getOrderId());
                } catch (AerospikeAccessorException e) {
                    logger.error("Error updating: {}", order, e);
                }
            }

            final UpdateOrderResponse updateOrderResponse =
                    new UpdateOrderResponse(order, (updated > 0), updateOrderRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new UpdateOrderResponse.Validator(), updateOrderResponse);
            }
            return updateOrderResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    @ValidateOnExecution
    @DeleteOrderResponse.Valid
    public DeleteOrderResponse deleteOrder(
            @DeleteOrderRequest.Valid
            final DeleteOrderRequest deleteOrderRequest) throws StackTestServiceException
    {
        AppbaseUtil.notNull(deleteOrderRequest, "deleteOrderRequest");

        final Timer.Context timer = deleteByIdTimer.time();
        try {
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new DeleteOrderRequest.Validator(), deleteOrderRequest);
            }

            final Long orderId = deleteOrderRequest.getOrderId();
            final boolean deleted = orderJDBA.deleteById(orderId);

            if (deleted) {
                try {
                    orderSIA.deleteDocument(String.valueOf(orderId));
                } catch (IOException e) {
                    logger.error("Error deleting document: {}", orderId, e);
                }

                try {
                     orderASA.deleteOrder(orderId);
                } catch (AerospikeAccessorException e) {
                    logger.error("Error deleting: {}", orderId, e);
                }
            }

            final DeleteOrderResponse deleteOrderResponse =
                    new DeleteOrderResponse(deleted, deleteOrderRequest.getTraceId());
            if (manualValidation) {
                AppbaseUtil.checkedValidation(new DeleteOrderResponse.Validator(), deleteOrderResponse);
            }

            return deleteOrderResponse;
        } catch (ValidationServiceException e) {
            throw new StackTestServiceException(e);
        } finally {
            timer.stop();
        }
    }
}
