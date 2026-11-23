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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Filter {
    // Map with 1 element
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private Map<String, String> term;

    // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html#named-queries
    private Map<String, Map<String, List<String>>> terms;

    public Map<String, String> getTerm() {
        return term;
    }

    public void setTerm(final Map<String, String> term) {
        this.term = term;
    }

    public Map<String, Map<String, List<String>>> getTerms() {
        return terms;
    }

    public void setTerms(final Map<String, Map<String, List<String>>> terms) {
        this.terms = terms;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Filter filter = (Filter) o;
        return Objects.equals(term, filter.term) &&
                Objects.equals(terms, filter.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, terms);
    }

    @Override
    public String toString() {
        return "Filter{" +
                "term=" + term +
                "terms=" + terms +
                '}';
    }
}
