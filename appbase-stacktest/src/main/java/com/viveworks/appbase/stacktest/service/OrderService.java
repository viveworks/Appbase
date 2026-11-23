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

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface OrderService {
    SearchOrdersResponse searchOrders(
            SearchOrdersRequest searchOrdersRequest) throws StackTestServiceException;

    CreateOrderResponse createOrder(
            CreateOrderRequest createOrderRequest) throws StackTestServiceException;

    ReadOrdersResponse readOrders(
            ReadOrdersRequest readOrderRequest) throws StackTestServiceException;

    ReadOrderResponse readOrder(
            ReadOrderRequest readOrderRequest) throws StackTestServiceException;

    UpdateOrderResponse updateOrder(
            UpdateOrderRequest updateOrderRequest) throws StackTestServiceException;

    DeleteOrderResponse deleteOrder(
            DeleteOrderRequest deleteOrderRequest) throws StackTestServiceException;
}
