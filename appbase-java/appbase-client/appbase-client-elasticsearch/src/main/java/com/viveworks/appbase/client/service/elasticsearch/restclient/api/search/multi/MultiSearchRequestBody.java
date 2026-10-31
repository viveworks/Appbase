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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQuery;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiSearchRequestBody {
    @JsonRawValue
    @JsonProperty("aggregations")
    private String aggregationsJson;

    @JsonProperty("query")
    private SearchQuery query;

    private Integer from;

    private Integer size;

    public String getAggregationsJson() {
        return aggregationsJson;
    }

    public void setAggregationsJson(final String aggregationsJson) {
        this.aggregationsJson = aggregationsJson;
    }

    public SearchQuery getQuery() {
        return query;
    }

    public void setQuery(final SearchQuery query) {
        this.query = query;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(final Integer from) {
        this.from = from;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MultiSearchRequestBody that = (MultiSearchRequestBody) o;
        return Objects.equals(aggregationsJson, that.aggregationsJson) &&
                Objects.equals(query, that.query) &&
                Objects.equals(from, that.from) &&
                Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggregationsJson, query, from, size);
    }

    @Override
    public String toString() {
        return "MultiSearchRequestBody{" +
                "aggregationsJson='" + aggregationsJson + '\'' +
                ", query=" + query +
                ", from=" + from +
                ", size=" + size +
                '}';
    }
}
