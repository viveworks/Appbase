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
package com.viveworks.appbase.stacktest.accessor.vitess;

import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.stacktest.data.model.Customer;
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
public class DefaultCustomerJDBA implements CustomerJDBA {
    private final CustomerJDBA jdbiInstance;

    private final Timer createCustomerTimer;
    private final Timer createCustomerByEmailTimer;
    private final Timer readByIdTimer;
    private final Timer readCustomerOrdersTimer;
    private final Timer readCustomersTimer;
    private final Timer updateEmailTimer;
    private final Timer updatePhotoIdTimer;
    private final Timer updateCustomerTimer;
    private final Timer deleteByIdTimer;

    public DefaultCustomerJDBA(
            final AppbaseClientServices clientServices,
            final Handle handle)
    {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(handle, "handle");

        this.jdbiInstance = handle.attach(CustomerJDBA.class);

        final MetricService metricService = clientServices.getCoreServices().appbaseCore().getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createCustomerTimer =
                metricRegistry.timer(metricService.name(getClass(), "createCustomerTimer"));
        this.createCustomerByEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "createCustomerByEmailTimer"));
        this.readByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readByIdTimer"));
        this.readCustomerOrdersTimer =
                metricRegistry.timer(metricService.name(getClass(), "readCustomerOrdersTimer"));
        this.readCustomersTimer =
                metricRegistry.timer(metricService.name(getClass(), "readCustomersTimer"));
        this.updateEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateEmailTimer"));
        this.updatePhotoIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateEmailTimer"));
        this.updateCustomerTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateCustomerTimer"));
        this.deleteByIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteByIdTimer"));
    }

    @Override
    public int createCustomer(
            final String email,
            final String photoId) throws JdbiException
    {
        AppbaseUtil.notBlank(email, "email");
        AppbaseUtil.notBlank(photoId, "photoId");

        final Timer.Context timer = createCustomerTimer.time();
        try {
            return jdbiInstance.createCustomer(email);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int createCustomer(final String email) throws JdbiException {
        AppbaseUtil.notBlank(email, "email");

        final Timer.Context timer = createCustomerByEmailTimer.time();
        try {
            return jdbiInstance.createCustomer(email);
        } finally {
            timer.stop();
        }
    }

    @Override
    public List<Customer> readCustomers(final int limit) throws JdbiException {
        AppbaseUtil.positiveOrZero(limit, "limit");

        final Timer.Context timer = readCustomersTimer.time();
        try {
            return jdbiInstance.readCustomers(limit);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Customer readById(final Long customerId) throws JdbiException {
        AppbaseUtil.notNull(customerId, "customerId");

        final Timer.Context timer = readByIdTimer.time();
        try {
            return jdbiInstance.readById(customerId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public List<Order> readCustomerOrders(final Long customerId) {
        AppbaseUtil.notNull(customerId, "customerId");

        final Timer.Context timer = readCustomerOrdersTimer.time();
        try {
            return jdbiInstance.readCustomerOrders(customerId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateEmail(
            final String email,
            final Long customerId) throws JdbiException
    {
        AppbaseUtil.notNull(email, "email");
        AppbaseUtil.notNull(customerId, "customerId");

        final Timer.Context timer = updateEmailTimer.time();
        try {
            return jdbiInstance.updateEmail(email, customerId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updatePhotoId(
            final String photoId,
            final Long customerId) throws JdbiException
    {
        AppbaseUtil.notNull(photoId, "photoId");
        AppbaseUtil.notNull(customerId, "customerId");

        final Timer.Context timer = updatePhotoIdTimer.time();
        try {
            return jdbiInstance.updatePhotoId(photoId, customerId);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateCustomer(final Customer customer) throws JdbiException {
        AppbaseUtil.notNull(customer, "customer");

        final Timer.Context timer = updateCustomerTimer.time();
        try {
            return jdbiInstance.updateCustomer(customer);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteById(final Long customerId) throws JdbiException {
        AppbaseUtil.notNull(customerId, "customerId");

        final Timer.Context timer = deleteByIdTimer.time();
        try {
            return jdbiInstance.deleteById(customerId);
        } finally {
            timer.stop();
        }
    }
}
