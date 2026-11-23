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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.SearchHits;

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Collapse {
    private String field;

    @JsonProperty("inner_hits")
    @JsonSerialize(keyAs = String.class, contentAs = SearchHits.class)
    @JsonDeserialize(keyAs = String.class, contentAs = SearchHits.class)
    private Map<String, SearchHits> innerHits;

    @JsonProperty("max_concurrent_group_searches")
    private Integer maxConcurrentGroupSearches;

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public Map<String, SearchHits> getInnerHits() {
        return innerHits;
    }

    public void setInnerHits(final Map<String, SearchHits> innerHits) {
        this.innerHits = innerHits;
    }

    public Integer getMaxConcurrentGroupSearches() {
        return maxConcurrentGroupSearches;
    }

    public void setMaxConcurrentGroupSearches(
            final Integer maxConcurrentGroupSearches)
    {
        this.maxConcurrentGroupSearches = maxConcurrentGroupSearches;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Collapse collapse = (Collapse) o;
        return Objects.equals(field, collapse.field) &&
                Objects.equals(innerHits, collapse.innerHits) &&
                Objects.equals(maxConcurrentGroupSearches, collapse.maxConcurrentGroupSearches);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, innerHits, maxConcurrentGroupSearches);
    }

    @Override
    public String toString() {
        return "Collapse{" +
                "field='" + field + '\'' +
                ", innerHits=" + innerHits +
                ", maxConcurrentGroupSearches=" + maxConcurrentGroupSearches +
                '}';
    }
}
