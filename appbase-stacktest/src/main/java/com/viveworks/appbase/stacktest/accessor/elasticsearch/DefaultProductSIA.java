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
import com.viveworks.appbase.client.service.elasticsearch.restclient.util.FieldDataTypes;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.bootstrap.client.AppbaseClientServices;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.stacktest.StackTestConstants;
import com.viveworks.appbase.stacktest.data.model.Product;
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
public class DefaultProductSIA extends AbstractSearchIndexAccessor<Product, Product> implements ProductSIA {
    public DefaultProductSIA(final AppbaseClientServices clientServices) {
        super(clientServices.getCoreServices().appbaseCore(),
                clientServices.get(ElasticsearchRestClient.class),
                StackTestConstants.ELASTICSEARCH_PRODUCT_INDEX_NAME);
    }

    @Override
    protected Class<Product> documentType() {
        return Product.class;
    }

    @Override
    protected String indexName() {
        return (indexNamePrefix + "-product").toLowerCase();
    }

    @Override
    protected String documentId(final Product document) {
        AppbaseUtil.notNull(document, "document");
        AppbaseUtil.notBlank(document.getSku(), "document.getSku()");

        return document.getSku();
    }

    @Override
    protected void validateDocument(final Product document) throws ValidationException {
        AppbaseUtil.notNull(document, "document");

        final String sku = document.getSku();
        if (AppbaseUtil.isBlank(sku)) {
            throw new ValidationException("sku must not be blank");
        }
    }

    @Override
    protected Map<String, CreateIndexRequestBody.FieldProperties> fieldProperties() {
        final Map<String, CreateIndexRequestBody.FieldProperties> properties = new HashMap<>();
        final CreateIndexRequestBody.FieldProperties skuProperties = new CreateIndexRequestBody.FieldProperties();
        skuProperties.setIndex(true);
        skuProperties.setType(FieldDataTypes.KEYWORD.getText());

        properties.put(Product.SKU_INDEX_FIELD, skuProperties);
        properties.put(Product.DESCRIPTION_INDEX_FIELD, textFieldProperties());
        properties.put(Product.PRICE_INDEX_FIELD, doubleProperties());

        return properties;
    }

    @Override
    protected SearchRequest searchRequest(Product request) throws IOException {
        final List<SearchQuery> mustQueries = new ArrayList<>();

        if (!AppbaseUtil.isBlank(request.getSku())) {
            addTermQuery(termQuery(Product.SKU_INDEX_FIELD, request.getSku()), mustQueries);
        }

        if (!AppbaseUtil.isBlank(request.getDescription())) {
            addTermQuery(termQuery(Product.DESCRIPTION_INDEX_FIELD, request.getDescription()), mustQueries);
        }

        if (request.getPrice() != null) {
            addTermQuery(termQuery(Product.PRICE_INDEX_FIELD, request.getPrice()), mustQueries);
        }

        return searchRequest(mustQueries);
    }
}
