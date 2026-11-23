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
import com.viveworks.appbase.stacktest.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class OrderServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = -3398399553152817976L;

    private OrderService orderService;

    @Override
    public void init() {
        super.init();

        orderService = stackTestServices().getOrderService();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String orderIdStr = AppbaseServiceUtil.parameter(StackTestConstants.ID_PARAM, httpServletRequest);
        if (!AppbaseUtil.isBlank(orderIdStr)) {
            final ReadOrderRequest readOrderRequest = new ReadOrderRequest(Long.parseLong(orderIdStr), traceId);
            final ReadOrderResponse readOrderResponse = orderService.readOrder(readOrderRequest);
            return (T) json(ReadOrderResponse.class, readOrderResponse);
        } else {
            final String searchOrderIdStr = AppbaseServiceUtil.parameter(StackTestConstants.SEARCH_PARAM, httpServletRequest);
            if (!AppbaseUtil.isBlank(searchOrderIdStr)) {
                final SearchOrdersRequest searchOrdersRequest =
                        new SearchOrdersRequest(Long.parseLong(searchOrderIdStr), traceId);
                final SearchOrdersResponse searchOrdersResponse = orderService.searchOrders(searchOrdersRequest);
                return (T) json(SearchOrdersResponse.class, searchOrdersResponse);
            }
        }

        final int limit = intParameter(StackTestConstants.LIMIT_PARAM, httpServletRequest, httpServletResponse);
        final ReadOrdersRequest readOrdersRequest = new ReadOrdersRequest(limit, traceId);
        final ReadOrdersResponse readOrdersResponse = orderService.readOrders(readOrdersRequest);
        return (T) json(ReadOrdersResponse.class, readOrdersResponse);
    }

    @Override
    protected boolean isGetImplResultJson() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateOrderRequest> postRequestType() {
        return CreateOrderRequest.class;
    }

    @Override
    protected <T extends Traceable> Object postImpl(
            final T traceable) throws Exception
    {
        return orderService.createOrder((CreateOrderRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateOrderResponse> postResponseType() {
        return CreateOrderResponse.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateOrderRequest> putRequestType() {
        return UpdateOrderRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(final T traceable) throws Exception {
        return orderService.updateOrder((UpdateOrderRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateOrderResponse> putResponseType() {
        return UpdateOrderResponse.class;
    }

    @Override
    protected <T extends Traceable> Object deleteImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final Long orderId = longParameter(
                StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        final DeleteOrderRequest deleteOrderRequest = new DeleteOrderRequest(orderId, traceId);
        return orderService.deleteOrder(deleteOrderRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<DeleteOrderResponse> deleteResponseType() {
        return DeleteOrderResponse.class;
    }
}
