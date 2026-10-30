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
import com.viveworks.appbase.stacktest.data.model.Product;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

import java.util.List;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultProductJDBA implements ProductJDBA {
    private final ProductJDBA jdbiInstance;

    private final Timer createProductTimer;
    private final Timer readByIdTimer;
    private final Timer readProductsTimer;
    private final Timer updateDescriptionTimer;
    private final Timer updatePriceTimer;
    private final Timer updateProductTimer;
    private final Timer deleteByIdTimer;

    public DefaultProductJDBA(final AppbaseClientServices clientServices, final Handle handle) {
        AppbaseUtil.notNull(clientServices, "clientServices");
        AppbaseUtil.notNull(handle, "handle");

        this.jdbiInstance = handle.attach(ProductJDBA.class);
        final MetricService metricService = clientServices.getCoreServices().appbaseCore().getMetricService();
        final MetricRegistry metricRegistry = metricService.getMetricRegistry();

        this.createProductTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "createProductTimer"));
        this.readByIdTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "readByIdTimer"));
        this.readProductsTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "readProductsTimer"));
        this.updateDescriptionTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "updateDescriptionTimer"));
        this.updatePriceTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "updatePriceTimer"));
        this.updateProductTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "updateProductTimer"));
        this.deleteByIdTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "deleteByIdTimer"));
    }

    @Override
    public int createProduct(final Product product) throws JdbiException {
        AppbaseUtil.notNull(product, "product");

        final Timer.Context timer = createProductTimer.time();
        try {
            return jdbiInstance.createProduct(product);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Product readById(final String sku) throws JdbiException {
        AppbaseUtil.notNull(sku, "sku");

        final Timer.Context timer = readByIdTimer.time();
        try {
            return jdbiInstance.readById(sku);
        } finally {
            timer.stop();
        }
    }

    @Override
    public List<Product> readProducts(final int limit) throws JdbiException {
        AppbaseUtil.positiveOrZero(limit, "limit");

        final Timer.Context timer = readProductsTimer.time();
        try {
            return jdbiInstance.readProducts(limit);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateDescription(
            final String sku,
            final String description) throws JdbiException
    {
        AppbaseUtil.notNull(sku, "sku");
        AppbaseUtil.notNull(description, "description");

        final Timer.Context timer = updateDescriptionTimer.time();
        try {
            return jdbiInstance.updateDescription(sku, description);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updatePrice(
            final String sku,
            final Double price) throws JdbiException
    {
        AppbaseUtil.notNull(sku, "sku");
        AppbaseUtil.notNull(price, "price");

        final Timer.Context timer = updatePriceTimer.time();
        try {
            return jdbiInstance.updatePrice(sku, price);
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateProduct(final Product product) throws JdbiException {
        AppbaseUtil.notNull(product, "product");

        final Timer.Context timer = updateProductTimer.time();
        try {
            return jdbiInstance.updateProduct(product);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteById(final String sku) throws JdbiException {
        AppbaseUtil.notNull(sku, "sku");

        final Timer.Context timer = deleteByIdTimer.time();
        try {
            return jdbiInstance.deleteById(sku);
        } finally {
            timer.stop();
        }
    }
}
