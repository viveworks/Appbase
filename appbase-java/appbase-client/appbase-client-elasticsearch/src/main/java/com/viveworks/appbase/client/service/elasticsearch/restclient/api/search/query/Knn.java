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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Knn {
    private String field;

    @JsonProperty("query_vector")
    @JsonSerialize(contentAs = Float.class)
    @JsonDeserialize(contentAs = Float.class)
    private List<Float> queryVector;

    private Integer k;

    @JsonProperty("num_candidates")
    private Integer numCandidates;
    
    private SearchQuery filter;

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public List<Float> getQueryVector() {
        return queryVector;
    }

    public void setQueryVector(
            final List<Float> queryVector)
    {
        this.queryVector = queryVector;
    }

    public Integer getK() {
        return k;
    }

    public void setK(final Integer k) {
        this.k = k;
    }

    public Integer getNumCandidates() {
        return numCandidates;
    }

    public void setNumCandidates(final Integer numCandidates) {
        this.numCandidates = numCandidates;
    }

    public SearchQuery getFilter() {
        return filter;
    }

    public void setFilter(final SearchQuery filter) {
        this.filter = filter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Knn knn = (Knn) o;
        return Objects.equals(field, knn.field) &&
                Objects.equals(queryVector, knn.queryVector) &&
                Objects.equals(k, knn.k) &&
                Objects.equals(numCandidates, knn.numCandidates) &&
                Objects.equals(filter, knn.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, queryVector, k, numCandidates, filter);
    }

    @Override
    public String toString() {
        return "Knn{" +
                "field='" + field + '\'' +
                ", queryVector=" + queryVector +
                ", k=" + k +
                ", numCandidates=" + numCandidates +
                ", filter=" + filter +
                '}';
    }
}
