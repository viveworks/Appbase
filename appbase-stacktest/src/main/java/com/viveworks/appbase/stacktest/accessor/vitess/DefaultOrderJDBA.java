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
package com.viveworks.appbase.stacktest.accessor.vitess;

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.data.model.Order;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultOrderJDBA implements OrderJDBA {
    private final OrderJDBA jdbiInstance;

    private final Timer createOrderTimer;
    private final Timer readByIdTimer;
    private final Timer readOrdersTimer;
    private final Timer updateOrderTimer;
    private final Timer deleteByIdTimer;

    public DefaultOrderJDBA(final AppbaseClientServices clientServices, final Handle handle) {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(handle, "handle");

        this.jdbiInstance = handle.attach(OrderJDBA.class);

        final MetricService metricService = clientServices.getCoreServices().appbaseCore().getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createOrderTimer = metricRegistry.timer(metricService.name(getClass(), "createOrderTimer"));
        this.readByIdTimer = metricRegistry.timer(metricService.name(getClass(), "readByIdTimer"));
        this.readOrdersTimer = metricRegistry.timer(metricService.name(getClass(), "readOrdersTimer"));
        this.updateOrderTimer = metricRegistry.timer(metricService.name(getClass(), "updateOrderTimer"));
        this.deleteByIdTimer = metricRegistry.timer(metricService.name(getClass(), "updateOrderTimer"));
    }

    @Override
    public int createOrder(
            final Long customerId,
            final String sku,
            final Double price) throws JdbiException
    {
        AppbaseUtil.notNull(customerId, "customerId");
        AppbaseUtil.notNull(sku, "sku");
        AppbaseUtil.notNull(price, "price");

        final Timer.Context timer = createOrderTimer.time();
        try {
            return jdbiInstance.createOrder(customerId, sku, price);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Order readById(final Long orderId) throws JdbiException {
        AppbaseUtil.notNull(orderId, "orderId");

        final Timer.Context timer = readByIdTimer.time();
        try {
            return jdbiInstance.readById(orderId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public List<Order> readOrders(final int limit) throws JdbiException {
        AppbaseUtil.positiveOrZero(limit, "limit");

        final Timer.Context timer = readOrdersTimer.time();
        try {
            return jdbiInstance.readOrders(limit);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateOrder(final Order order) throws JdbiException {
        AppbaseUtil.notNull(order, "order");

        final Timer.Context timer = updateOrderTimer.time();
        try {
            return jdbiInstance.updateOrder(order);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteById(final Long orderId) throws JdbiException {
        AppbaseUtil.notNull(orderId, "orderId");

        final Timer.Context timer = deleteByIdTimer.time();
        try {
            return jdbiInstance.deleteById(orderId);
        } finally {
            timer.stop();
        }
    }
}
