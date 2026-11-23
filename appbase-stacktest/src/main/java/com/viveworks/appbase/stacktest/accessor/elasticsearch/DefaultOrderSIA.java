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
package com.viveworks.appbase.stacktest.accessor.elasticsearch;

import com.viveworks.appbase.client.service.elasticsearch.restclient.accessor.AbstractSearchIndexAccessor;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQuery;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.model.Order;
import jakarta.validation.ValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultOrderSIA extends AbstractSearchIndexAccessor<Order, Order> implements OrderSIA {
    public DefaultOrderSIA(final AppbaseClientServices clientServices) {
        super(clientServices.getCoreServices().appbaseCore(),
                clientServices.get(ElasticsearchRestClient.class),
                StackTestConstants.ELASTICSEARCH_ORDER_INDEX_NAME);
    }

    @Override
    protected Class<Order> documentType() {
        return Order.class;
    }

    @Override
    protected String indexName() {
        return (indexNamePrefix + "-order").toLowerCase();
    }

    @Override
    protected String documentId(Order document) {
        AppbaseUtil.notNull(document, "document");
        AppbaseUtil.notNull(document.getOrderId(), "document.getOrderId()");

        return String.valueOf(document.getOrderId());
    }

    @Override
    protected void validateDocument(final Order document) throws ValidationException {
        AppbaseUtil.notNull(document, "document");

        final Long orderId = document.getOrderId();
        if (orderId == null) {
            throw new ValidationException("orderId must not be null");
        }
    }

    @Override
    protected Map<String, CreateIndexRequestBody.FieldProperties> fieldProperties() {
        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();

        properties.put(Order.ORDER_ID_INDEX_FIELD, integerProperties());
        properties.put(Order.SKU_INDEX_FIELD, textFieldProperties());
        properties.put(Order.CUSTOMER_ID_INDEX_FIELD, integerProperties());
        properties.put(Order.PRICE_INDEX_FIELD, doubleProperties());

        return properties;
    }

    @Override
    protected SearchRequest searchRequest(Order request) throws IOException {
        final List<SearchQuery> mustQueries = new ArrayList<>();

        if (request.getOrderId() != null) {
            addTermQuery(termQuery(Order.ORDER_ID_INDEX_FIELD, request.getOrderId()), mustQueries);
        }

        if (!AppbaseUtil.isBlank(request.getSku())) {
            addTermQuery(termQuery(Order.SKU_INDEX_FIELD, request.getSku()), mustQueries);
        }

        if (request.getCustomerId() != null) {
            addTermQuery(termQuery(Order.CUSTOMER_ID_INDEX_FIELD, request.getCustomerId()), mustQueries);
        }

        if (request.getPrice() != null) {
            addTermQuery(termQuery(Order.PRICE_INDEX_FIELD, request.getPrice()), mustQueries);
        }

        return searchRequest(mustQueries);
    }
}
