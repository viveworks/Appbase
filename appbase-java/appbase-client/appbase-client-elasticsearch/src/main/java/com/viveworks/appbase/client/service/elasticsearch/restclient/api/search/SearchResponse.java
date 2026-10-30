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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search;

import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.ErrorResponseBody;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchResponseBody;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SearchResponse {
    @Nullable
    private final SearchResponseBody searchResponseBody;

    @Nullable
    private final ErrorResponseBody errorResponseBody;

    private final boolean error;

    public SearchResponse() {
        this.searchResponseBody = null;
        this.errorResponseBody = null;
        this.error = false;
    }

    public SearchResponse(
            final SearchResponseBody searchResponseBody)
    {
        AppbaseUtil.notNull(searchResponseBody, "searchResponseBody");

        this.searchResponseBody = searchResponseBody;
        this.errorResponseBody = null;
        this.error = false;
    }

    public SearchResponse(final ErrorResponseBody errorResponseBody) {
        AppbaseUtil.notNull(errorResponseBody, "errorResponseBody");

        this.errorResponseBody = errorResponseBody;
        this.error = true;
        this.searchResponseBody = null;
    }

    public SearchResponse(final boolean error) {
        this.error = error;
        this.searchResponseBody = null;
        this.errorResponseBody = null;
    }

    @Nullable
    public SearchResponseBody getSearchResponseBody() {
        return searchResponseBody;
    }

    @Nullable
    public ErrorResponseBody getErrorResponseBody() {
        return errorResponseBody;
    }

    public boolean isError() {
        return error;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SearchResponse that = (SearchResponse) o;
        return error == that.error &&
                Objects.equals(searchResponseBody, that.searchResponseBody) &&
                Objects.equals(errorResponseBody, that.errorResponseBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchResponseBody, errorResponseBody, error);
    }

    @Override
    public String toString() {
        return "SearchResponse{" +
                "searchResponseBody=" + searchResponseBody +
                ", errorResponseBody=" + errorResponseBody +
                ", error=" + error +
                '}';
    }
}
