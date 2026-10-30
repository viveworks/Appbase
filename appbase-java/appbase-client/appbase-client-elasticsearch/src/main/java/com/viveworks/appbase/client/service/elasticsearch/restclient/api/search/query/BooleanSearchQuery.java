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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BooleanSearchQuery implements SearchQuery {
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html#named-queries
    private Bool bool;

    @JsonProperty("minimum_should_match")
    private String minimumShouldMatch;

    // Map.of("must", new Object())
    @JsonProperty("must")
    private Map<String, Object> mustMatchAll;

    private Float boost;

    public Bool getBool() {
        return bool;
    }

    public void setBool(final Bool bool) {
        this.bool = bool;
    }

    public String getMinimumShouldMatch() {
        return minimumShouldMatch;
    }

    public void setMinimumShouldMatch(final String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
    }

    public Map<String, Object> getMustMatchAll() {
        return mustMatchAll;
    }

    public void setMustMatchAll(final Map<String, Object> mustMatchAll) {
        this.mustMatchAll = mustMatchAll;
    }

    public Float getBoost() {
        return boost;
    }

    public void setBoost(final Float boost) {
        this.boost = boost;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BooleanSearchQuery that = (BooleanSearchQuery) o;
        return Objects.equals(bool, that.bool) &&
                Objects.equals(minimumShouldMatch, that.minimumShouldMatch) &&
                Objects.equals(mustMatchAll, that.mustMatchAll) &&
                Objects.equals(boost, that.boost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bool, minimumShouldMatch, mustMatchAll, boost);
    }

    @Override
    public String toString() {
        return "BooleanSearchQuery{" +
                "bool=" + bool +
                ", minimumShouldMatch='" + minimumShouldMatch + '\'' +
                ", mustMatchAll=" + mustMatchAll +
                ", boost=" + boost +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Bool {
        private List<SearchQuery> should;
        private List<SearchQuery> must;
        private List<SearchQuery> must_not;
        private Filter filter;

        public List<SearchQuery> getShould() {
            return should;
        }

        public void setShould(final List<SearchQuery> should) {
            this.should = should;
        }

        public List<SearchQuery> getMust() {
            return must;
        }

        public void setMust(final List<SearchQuery> must) {
            this.must = must;
        }

        public List<SearchQuery> getMust_not() {
            return must_not;
        }

        public void setMust_not(final List<SearchQuery> must_not) {
            this.must_not = must_not;
        }

        public Filter getFilter() {
            return filter;
        }

        public void setFilter(final Filter filter) {
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

            final Bool bool = (Bool) o;
            return Objects.equals(should, bool.should) &&
                    Objects.equals(must, bool.must) &&
                    Objects.equals(must_not, bool.must_not) &&
                    Objects.equals(filter, bool.filter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(should, must, must_not, filter);
        }

        @Override
        public String toString() {
            return "Bool{" +
                    "should=" + should +
                    ", must=" + must +
                    ", must_not=" + must_not +
                    ", filter=" + filter +
                    '}';
        }
    }

    public enum QueryNames {
        WILDCARD ("wildcard"),
        TERM ("term"),
        MATCH ("match"),
        RANGE ("range"),
        REGEX( "regexp");

        private final String text;

        QueryNames(final String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    };
}
