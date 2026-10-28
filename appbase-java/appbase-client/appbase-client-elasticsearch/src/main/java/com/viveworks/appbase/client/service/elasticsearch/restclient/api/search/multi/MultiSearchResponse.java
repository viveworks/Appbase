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

import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.ErrorResponseBody;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MultiSearchResponse {
    @Nullable
    private final MultiSearchResponseBody multiSearchResponseBody;

    @Nullable
    private final ErrorResponseBody errorResponseBody;

    private final boolean error;

    public MultiSearchResponse() {
        this.multiSearchResponseBody = null;
        this.errorResponseBody = null;
        this.error = false;
    }

    public MultiSearchResponse(
            final MultiSearchResponseBody multiSearchResponseBody)
    {
        AppbaseUtil.notNull(multiSearchResponseBody, "multiSearchResponseBody");

        this.multiSearchResponseBody = multiSearchResponseBody;
        this.errorResponseBody = null;
        this.error = false;
    }

    public MultiSearchResponse(
            final ErrorResponseBody errorResponseBody)
    {
        AppbaseUtil.notNull(errorResponseBody, "errorResponseBody");

        this.errorResponseBody = errorResponseBody;
        this.error = true;
        this.multiSearchResponseBody = null;
    }

    public MultiSearchResponse(final boolean error) {
        this.error = error;
        this.multiSearchResponseBody = null;
        this.errorResponseBody = null;
    }

    @Nullable
    public MultiSearchResponseBody getMultiSearchResponseBody() {
        return multiSearchResponseBody;
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

        final MultiSearchResponse that = (MultiSearchResponse) o;
        return error == that.error &&
                Objects.equals(multiSearchResponseBody, that.multiSearchResponseBody) &&
                Objects.equals(errorResponseBody, that.errorResponseBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiSearchResponseBody, errorResponseBody, error);
    }

    @Override
    public String toString() {
        return "MultiSearchResponse{" +
                "multiSearchResponseBody=" + multiSearchResponseBody +
                ", errorResponseBody=" + errorResponseBody +
                ", error=" + error +
                '}';
    }
}
