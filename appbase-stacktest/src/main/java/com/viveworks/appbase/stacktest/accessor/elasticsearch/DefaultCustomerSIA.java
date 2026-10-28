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
package com.viveworks.appbase.stacktest.accessor.elasticsearch;

import com.viveworks.appbase.client.service.elasticsearch.restclient.accessor.AbstractSearchIndexAccessor;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.SearchRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQuery;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.model.Customer;
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
public class DefaultCustomerSIA extends AbstractSearchIndexAccessor<Customer, Customer> implements CustomerSIA {
    public DefaultCustomerSIA(final AppbaseClientServices clientServices) {
        super(clientServices.getCoreServices().appbaseCore(),
                clientServices.get(ElasticsearchRestClient.class),
                StackTestConstants.ELASTICSEARCH_CUSTOMER_INDEX_NAME);
    }

    @Override
    protected Class<Customer> documentType() {
        return Customer.class;
    }

    @Override
    protected String indexName() {
        return (indexNamePrefix + "-customer").toLowerCase();
    }

    @Override
    protected String documentId(final Customer document) {
        AppbaseUtil.notNull(document, "document");

        return String.valueOf(document.getCustomerId());
    }

    @Override
    protected void validateDocument(final Customer document) throws ValidationException {
        AppbaseUtil.notNull(document, "document");

        final Long customerId = document.getCustomerId();
        if (customerId == null || AppbaseUtil.isBlank(String.valueOf(customerId))) {
            throw new ValidationException("customerId must not be null / blank: " + customerId);
        }
    }

    @Override
    protected Map<String, CreateIndexRequestBody.FieldProperties> fieldProperties() {
        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();

        properties.put(Customer.CUSTOMER_ID_INDEX_FIELD, integerProperties());
        properties.put(Customer.EMAIL_INDEX_FIELD, textFieldProperties());
        properties.put(Customer.PHOTO_ID_INDEX_FIELD, textFieldProperties());

        return properties;
    }

    @Override
    protected SearchRequest searchRequest(final Customer request) throws IOException {
        final List<SearchQuery> mustQueries = new ArrayList<>();

        if (request.getCustomerId() != null) {
            addTermQuery(termQuery(Customer.CUSTOMER_ID_INDEX_FIELD, request.getCustomerId()), mustQueries);
        }

        if (!AppbaseUtil.isBlank(request.getEmail())) {
            addTermQuery(termQuery(Customer.EMAIL_INDEX_FIELD, request.getEmail()), mustQueries);
        }

        if (!AppbaseUtil.isBlank(request.getPhotoId())) {
            addTermQuery(termQuery(Customer.PHOTO_ID_INDEX_FIELD, request.getPhotoId()), mustQueries);
        }

        return searchRequest(mustQueries);
    }
}
