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

package com.viveworks.appbase.stacktest;

import com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexRequestBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.CreateIndexResponse;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexRequest;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.index.ExistsIndexResponse;
import com.viveworks.appbase.stacktest.data.model.Product;
import com.viveworks.appbase.stacktest.service.StackTestServices;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
class StackTestInfraElasticsearchTest {
    @Test
    void productIndexUsesProductNameAndExactSkuMapping() throws Exception {
        final StackTestServices services = mock(StackTestServices.class, RETURNS_DEEP_STUBS);
        final ElasticsearchRestClient client = mock(ElasticsearchRestClient.class);
        when(client.existsIndex(any(ExistsIndexRequest.class)))
                .thenReturn(new ExistsIndexResponse(false));
        when(client.createIndex(any(CreateIndexRequest.class)))
                .thenReturn(new CreateIndexResponse(true));

        final Method createProductIndex = StackTestInfra.class.getDeclaredMethod(
                "createStackTestProductIndexIfNotExists", ElasticsearchRestClient.class);
        createProductIndex.setAccessible(true);
        createProductIndex.invoke(new StackTestInfra(services), client);

        final ArgumentCaptor<CreateIndexRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateIndexRequest.class);
        verify(client).createIndex(requestCaptor.capture());
        final CreateIndexRequest request = requestCaptor.getValue();
        assertEquals(StackTestConstants.ELASTICSEARCH_PRODUCT_INDEX_NAME + "-product", request.getIndexName());
        assertNotNull(request.getRequestBody());
        assertNotNull(request.getRequestBody().getMappings());
        final Map<String, CreateIndexRequestBody.FieldProperties> properties =
                request.getRequestBody().getMappings().getProperties();
        assertEquals("keyword", properties.get(Product.SKU_INDEX_FIELD).getType());
        assertEquals("text", properties.get(Product.DESCRIPTION_INDEX_FIELD).getType());
        assertEquals("double", properties.get(Product.PRICE_INDEX_FIELD).getType());
    }
}
