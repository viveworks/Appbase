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

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoostingSearchQuery implements SearchQuery {
    private Boosting boosting;

    @JsonProperty("negative_boost")
    private Float negativeBoost;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Boosting {
        private BoostingFactor positive;

        private BoostingFactor negative;

        public BoostingFactor getPositive() {
            return positive;
        }

        public void setPositive(final BoostingFactor positive) {
            this.positive = positive;
        }

        public BoostingFactor getNegative() {
            return negative;
        }

        public void setNegative(final BoostingFactor negative) {
            this.negative = negative;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Boosting boosting = (Boosting) o;
            return Objects.equals(positive, boosting.positive) &&
                    Objects.equals(negative, boosting.negative);
        }

        @Override
        public int hashCode() {
            return Objects.hash(positive, negative);
        }

        @Override
        public String toString() {
            return "Boosting{" +
                    "positive=" + positive +
                    ", negative=" + negative +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class BoostingFactor {
        // Map of 1 element:
        // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-boosting-query.html#boosting-query-ex-request
        private Map<String, String> term;

        public Map<String, String> getTerm() {
            return term;
        }

        public void setTerm(final Map<String, String> term) {
            this.term = term;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final BoostingFactor that = (BoostingFactor) o;
            return Objects.equals(term, that.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term);
        }

        @Override
        public String toString() {
            return "BoostingFactor{" +
                    "term=" + term +
                    '}';
        }
    }
}
