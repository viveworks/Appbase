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
package com.viveworks.appbase.stacktest.accessor.aerospike;

import com.viveworks.appbase.client.service.aerospike.accessor.AbstractAerospikeAccessor;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.model.Order;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultOrderASA extends AbstractAerospikeAccessor implements OrderASA {
    private static final String BIN_NAME = "json";

    public DefaultOrderASA(final AppbaseClientServices clientServices) {
        super(clientServices.get(AerospikeClientService.class),
                clientServices.getCoreServices().appbaseCore().getMetricService(),
                StackTestConstants.AEROSPIKE_NAMESPACE,
                StackTestConstants.AEROSPIKE_ORDER_SET);
    }

    @Override
    public void insertOrder(final Order order) throws AerospikeAccessorException {
        AppbaseUtil.notNull(order, "order");
        AppbaseUtil.notNull(order.getOrderId(), "order.getOrderId()");

        createOrReplace(String.valueOf(order.getOrderId()), BIN_NAME, order, Order.class);
    }

    @Nullable
    @Override
    public Order readOrder(final Long orderId) throws AerospikeAccessorException {
        AppbaseUtil.notNull(orderId, "orderId");

        return read(String.valueOf(orderId), BIN_NAME, Order.class);
    }

    @Override
    public boolean deleteOrder(final Long orderId) throws AerospikeAccessorException {
        AppbaseUtil.notNull(orderId, "orderId");

        return delete(String.valueOf(orderId));
    }
}
