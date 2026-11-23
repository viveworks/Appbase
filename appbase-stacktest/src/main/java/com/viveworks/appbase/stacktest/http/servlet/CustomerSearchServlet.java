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
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.SearchCustomersResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerSearchServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = 4994608951482024103L;

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
        final String email = nonBlankParameter(StackTestConstants.EMAIL_PARAM, httpServletRequest, httpServletResponse);
        final SearchCustomersRequest searchCustomersRequest = new SearchCustomersRequest(email, traceId);
        return (T) customerService.searchCustomers(searchCustomersRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<SearchCustomersResponse> getResponseType() {
        return SearchCustomersResponse.class;
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
