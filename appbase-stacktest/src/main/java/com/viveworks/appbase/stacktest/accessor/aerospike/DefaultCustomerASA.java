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
package com.viveworks.appbase.stacktest.accessor.aerospike;

import com.viveworks.appbase.client.service.aerospike.accessor.AbstractAerospikeAccessor;
import com.viveworks.appbase.client.service.aerospike.accessor.AerospikeAccessorException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.client.service.aerospike.AerospikeClientService;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.model.Customer;

import javax.annotation.Nullable;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultCustomerASA extends AbstractAerospikeAccessor implements CustomerASA {
    private static final String BIN_NAME = "json";

    public DefaultCustomerASA(final AppbaseClientServices clientServices) {
        super(clientServices.get(AerospikeClientService.class),
                clientServices.getCoreServices().appbaseCore().getMetricService(),
                StackTestConstants.AEROSPIKE_NAMESPACE,
                StackTestConstants.AEROSPIKE_CUSTOMER_SET);
    }

    @Override
    public void insertCustomer(final Customer customer) throws AerospikeAccessorException {
        AppbaseUtil.notNull(customer, "customer");
        AppbaseUtil.notNull(customer.getCustomerId(), "customer.getCustomerId()");

        createOrReplace(String.valueOf(customer.getCustomerId()), BIN_NAME, customer, Customer.class);
    }

    @Nullable
    @Override
    public Customer readCustomer(final Long customerId) throws AerospikeAccessorException {
        AppbaseUtil.notNull(customerId, "customerId");

        return read(String.valueOf(customerId), BIN_NAME, Customer.class);
    }

    @Override
    public boolean deleteCustomer(final Long customerId) throws AerospikeAccessorException {
        AppbaseUtil.notNull(customerId, "customerId");

        return delete(String.valueOf(customerId));
    }
}
