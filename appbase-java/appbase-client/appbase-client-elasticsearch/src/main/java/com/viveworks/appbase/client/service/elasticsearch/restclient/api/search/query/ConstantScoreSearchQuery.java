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

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConstantScoreSearchQuery implements SearchQuery {
    @JsonProperty("constant_score")
    private ConstantScore constantScore;

    public ConstantScore getConstantScore() {
        return constantScore;
    }

    public void setConstantScore(final ConstantScore constantScore) {
        this.constantScore = constantScore;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConstantScoreSearchQuery that = (ConstantScoreSearchQuery) o;
        return Objects.equals(constantScore, that.constantScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constantScore);
    }

    @Override
    public String toString() {
        return "ConstantScoreSearchQuery{" +
                "constantScore=" + constantScore +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ConstantScore {
        private Filter filter;

        private Float boost;

        public Filter getFilter() {
            return filter;
        }

        public void setFilter(final Filter filter) {
            this.filter = filter;
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

            final ConstantScore that = (ConstantScore) o;
            return Objects.equals(filter, that.filter) &&
                    Objects.equals(boost, that.boost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filter, boost);
        }

        @Override
        public String toString() {
            return "ConstantScore{" +
                    "filter=" + filter +
                    ", boost=" + boost +
                    '}';
        }
    }
}
