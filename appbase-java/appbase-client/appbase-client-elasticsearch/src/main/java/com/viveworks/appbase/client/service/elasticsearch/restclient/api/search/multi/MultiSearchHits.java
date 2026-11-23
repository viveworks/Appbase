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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiSearchHits {
    private SearchHits hits;

    private Integer status;

    public SearchHits getHits() {
        return hits;
    }

    public void setHits(final SearchHits hits) {
        this.hits = hits;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MultiSearchHits that = (MultiSearchHits) o;
        return Objects.equals(hits, that.hits) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hits, status);
    }

    @Override
    public String toString() {
        return "MultiSearchHits{" +
                "hits=" + hits +
                ", status=" + status +
                '}';
    }
}
