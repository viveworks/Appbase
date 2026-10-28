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
package com.viveworks.appbase.stacktest.http.servlet;

import com.viveworks.appbase.core.service.tracer.Traceable;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsRequest;
import com.viveworks.appbase.stacktest.data.pojo.customer.CustomerStatisticsResponse;
import com.viveworks.appbase.stacktest.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CustomerStatisticsServlet extends AbstractStackTestServlet {
    private static final long serialVersionUID = 8775057239841484534L;

    private CustomerService customerService;

    @Override
    public void init() {
        super.init();

        customerService = stackTestServices().getCustomerService();
    }

    @Override
    protected boolean isPutSupported() {
        return false;
    }

    @Override
    protected boolean isDeleteSupported() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected CustomerStatisticsResponse getImpl(
            final String traceId,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse)
            throws Exception
    {
        final Long customerId = longParameter(
                StackTestConstants.ID_PARAM, httpServletRequest, httpServletResponse);
        final CustomerStatisticsRequest customerStatisticsRequest =
                new CustomerStatisticsRequest(customerId, traceId);

        return customerService.customerStatistics(customerStatisticsRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CustomerStatisticsRequest> postRequestType() {
        return CustomerStatisticsRequest.class;
    }

    @Override
    protected <T extends Traceable> Object postImpl(final T traceable) throws Exception {
        return customerService.customerStatistics((CustomerStatisticsRequest) traceable);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CustomerStatisticsResponse> postResponseType() {
        return CustomerStatisticsResponse.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<CustomerStatisticsResponse> getResponseType() {
        return CustomerStatisticsResponse.class;
    }
}
