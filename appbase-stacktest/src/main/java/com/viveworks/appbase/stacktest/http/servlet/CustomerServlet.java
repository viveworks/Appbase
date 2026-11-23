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
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CreateCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.DeleteCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomerResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.ReadCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersResponse;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.UpdateCustomerResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = 7743773022688580582L;

    private CustomerService customerService;

    @Override
    public void init() {
        super.init();

        customerService = stackTestServices().getCustomerService();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final String customerIdStr = AppbaseServiceUtil.parameter(StackTestConstants.ID_PARAM, httpServletRequest);
        if (!AppbaseUtil.isBlank(customerIdStr)) {
            final ReadCustomerRequest readCustomerRequest =
                    new ReadCustomerRequest(Long.parseLong(customerIdStr), traceId);
            final ReadCustomerResponse readCustomerResponse = customerService.readCustomer(readCustomerRequest);
            return (T) json(ReadCustomerResponse.class, readCustomerResponse);
        } else {
            final String searchCustomerIdStr = AppbaseServiceUtil.parameter(StackTestConstants.SEARCH_PARAM, httpServletRequest);
            if (!AppbaseUtil.isBlank(searchCustomerIdStr)) {
                final SearchCustomersRequest searchCustomersRequest =
                        new SearchCustomersRequest(searchCustomerIdStr, AppbaseServiceUtil.traceId(traceId));
                final SearchCustomersResponse searchCustomersResponse =
                        customerService.searchCustomers(searchCustomersRequest);
                return (T) json(SearchCustomersResponse.class, searchCustomersResponse);
            }
        }

        final int limit = intParameter(StackTestConstants.LIMIT_PARAM, httpServletRequest, httpServletResponse);
        final ReadCustomersRequest readCustomersRequest = new ReadCustomersRequest(limit, traceId);
        final ReadCustomersResponse readCustomersResponse = customerService.readCustomers(readCustomersRequest);
        return (T) json(ReadCustomersResponse.class, readCustomersResponse);
    }

    @Override
    protected boolean isGetImplResultJson() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateCustomerRequest> postRequestType() {
        return CreateCustomerRequest.class;
    }

    @Override
    protected <T extends Traceable> Object postImpl(final T traceable) throws Exception {
        return customerService.createCustomer((CreateCustomerRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CreateCustomerResponse> postResponseType() {
        return CreateCustomerResponse.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateCustomerRequest> putRequestType() {
        return UpdateCustomerRequest.class;
    }

    @Override
    protected <T extends Traceable> Object putImpl(
            final T traceable) throws Exception
    {
        return customerService.updateCustomer((UpdateCustomerRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<UpdateCustomerResponse> putResponseType() {
        return UpdateCustomerResponse.class;
    }

    @Override
    protected <T extends Traceable> Object deleteImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final Long customerId = longParameter(
                StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        final DeleteCustomerRequest deleteCustomerRequest = new DeleteCustomerRequest(customerId, traceId);
        return customerService.deleteCustomer(deleteCustomerRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<DeleteCustomerResponse> deleteResponseType() {
        return DeleteCustomerResponse.class;
    }
}
