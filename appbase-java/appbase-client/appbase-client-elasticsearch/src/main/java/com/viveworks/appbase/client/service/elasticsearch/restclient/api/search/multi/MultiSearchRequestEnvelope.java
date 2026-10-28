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

import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Objects;

/**
 * This encapsulates 1 pair-entry for a search request (header &amp; body) in multiple search.
 * This cannot be serialized to JSON as-is as a new-line must be added after a header &amp; body.
 * The {@link com.viveworks.appbase.client.service.elasticsearch.restclient.api.ElasticsearchRestClient}
 * implementation must serialize this manually.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MultiSearchRequestEnvelope {
    private final MultiSearchRequestHeader multiSearchRequestHeader;
    private final MultiSearchRequestBody multiSearchRequestBody;

    public MultiSearchRequestEnvelope(
            final MultiSearchRequestHeader multiSearchRequestHeader,
            final MultiSearchRequestBody multiSearchRequestBody)
    {
        AppbaseUtil.notNull(multiSearchRequestHeader, "multiSearchRequestHeader");
        AppbaseUtil.notNull(multiSearchRequestBody, "multiSearchRequestBody");

        this.multiSearchRequestHeader = multiSearchRequestHeader;
        this.multiSearchRequestBody = multiSearchRequestBody;
    }

    public MultiSearchRequestHeader getMultiSearchRequestHeader() {
        return multiSearchRequestHeader;
    }

    public MultiSearchRequestBody getMultiSearchRequestBody() {
        return multiSearchRequestBody;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MultiSearchRequestEnvelope that = (MultiSearchRequestEnvelope) o;
        return Objects.equals(
                multiSearchRequestHeader, that.multiSearchRequestHeader) &&
                Objects.equals(
                        multiSearchRequestBody, that.multiSearchRequestBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiSearchRequestHeader, multiSearchRequestBody);
    }

    @Override
    public String toString() {
        return "MultiSearchRequestEnvelope{" +
                "multiSearchRequestHeader=" + multiSearchRequestHeader +
                ", multiSearchRequestBody=" + multiSearchRequestBody +
                '}';
    }
}
