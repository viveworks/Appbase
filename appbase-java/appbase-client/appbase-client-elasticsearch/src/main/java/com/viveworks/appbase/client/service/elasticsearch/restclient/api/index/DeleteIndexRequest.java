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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.index;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DeleteIndexRequest {
    private final String indexName;

    @Nullable
    private final Map<String, String> queryParams;

    public DeleteIndexRequest(final String indexName) {
        this(indexName, null);
    }

    public DeleteIndexRequest(
            final String indexName,
            @Nullable
            final Map<String, String> queryParams)
    {
        AppbaseUtil.notBlank(indexName, "indexName");

        this.indexName = indexName;
        this.queryParams = queryParams;
    }

    public String getIndexName() {
        return indexName;
    }

    @Nullable
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DeleteIndexRequest that = (DeleteIndexRequest) o;
        return Objects.equals(indexName, that.indexName) &&
                Objects.equals(queryParams, that.queryParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName, queryParams);
    }

    @Override
    public String toString() {
        return "DeleteIndexRequest{" +
                "indexName='" + indexName + '\'' +
                ", queryParams=" + queryParams +
                '}';
    }
}
