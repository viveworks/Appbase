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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DisjunctionMaxSearchQuery implements SearchQuery {
    @JsonProperty("dis_max")
    private DisjunctionMax disjunctionMax;

    @JsonProperty("tie_breaker")
    private Float tieBreaker;

    public DisjunctionMax getDisjunctionMax() {
        return disjunctionMax;
    }

    public void setDisjunctionMax(final DisjunctionMax disjunctionMax) {
        this.disjunctionMax = disjunctionMax;
    }

    public Float getTieBreaker() {
        return tieBreaker;
    }

    public void setTieBreaker(final Float tieBreaker) {
        this.tieBreaker = tieBreaker;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DisjunctionMaxSearchQuery that = (DisjunctionMaxSearchQuery) o;
        return Objects.equals(disjunctionMax, that.disjunctionMax) &&
                Objects.equals(tieBreaker, that.tieBreaker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disjunctionMax, tieBreaker);
    }

    @Override
    public String toString() {
        return "DisjunctionMaxSearchQuery{" +
                "disjunctionMax=" + disjunctionMax +
                ", tieBreaker=" + tieBreaker +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class DisjunctionMax {
        @JsonSerialize(contentAs = SearchQuery.class)
        @JsonDeserialize(contentAs = SearchQuery.class)
        private List<SearchQuery> queries;

        public List<SearchQuery> getQueries() {
            return queries;
        }

        public void setQueries(final List<SearchQuery> queries) {
            this.queries = queries;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final DisjunctionMax that = (DisjunctionMax) o;
            return Objects.equals(queries, that.queries);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queries);
        }

        @Override
        public String toString() {
            return "DisjunctionMax{" +
                    "queries=" + queries +
                    '}';
        }
    }
}
