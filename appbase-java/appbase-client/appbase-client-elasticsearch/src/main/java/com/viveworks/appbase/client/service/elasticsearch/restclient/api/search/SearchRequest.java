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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search;

import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query.SearchQueryParam;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SearchRequest {
    private String targetCsv;

    private SearchQueryBody searchQueryBody;

    @Nullable
    private Map<SearchQueryParam, Object> queryParameters;

    public SearchRequest(
            final String targetCsv,
            final SearchQueryBody searchQueryBody)
    {
        this(targetCsv, searchQueryBody, null);
    }

    public SearchRequest(
            final String targetCsv,
            final SearchQueryBody searchQueryBody,
            @Nullable
            final Map<SearchQueryParam, Object> queryParameters)
    {
        this.targetCsv = targetCsv;
        this.searchQueryBody = searchQueryBody;
        this.queryParameters = queryParameters;
    }

    public String getTargetCsv() {
        return targetCsv;
    }

    public void setTargetCsv(String targetCsv) {
        this.targetCsv = targetCsv;
    }

    public SearchQueryBody getSearchQueryBody() {
        return searchQueryBody;
    }

    public void setSearchQueryBody(SearchQueryBody searchQueryBody) {
        this.searchQueryBody = searchQueryBody;
    }

    @Nullable
    public Map<SearchQueryParam, Object> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(
            @Nullable
            final Map<SearchQueryParam, Object> queryParameters)
    {
        this.queryParameters = queryParameters;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SearchRequest that = (SearchRequest) o;
        return Objects.equals(targetCsv, that.targetCsv) &&
                Objects.equals(searchQueryBody, that.searchQueryBody) &&
                Objects.equals(queryParameters, that.queryParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetCsv, searchQueryBody, queryParameters);
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "targetCsv='" + targetCsv + '\'' +
                ", searchQueryBody=" + searchQueryBody +
                ", queryParameters=" + queryParameters +
                '}';
    }
}
