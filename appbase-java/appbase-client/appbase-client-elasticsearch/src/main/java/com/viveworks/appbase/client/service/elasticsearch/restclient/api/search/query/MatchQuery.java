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

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchQuery implements SearchQuery {
    @JsonSerialize(keyAs = String.class, contentAs = FieldParams.class)
    @JsonDeserialize(keyAs = String.class, contentAs = FieldParams.class)
    private Map<String, FieldParams> match;

    public Map<String, FieldParams> getMatch() {
        return match;
    }

    public void setMatch(final Map<String, FieldParams> match) {
        this.match = match;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MatchQuery that = (MatchQuery) o;
        return Objects.equals(match, that.match);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match);
    }

    @Override
    public String toString() {
        return "MatchQuery{" +
                "match=" + match +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldParams {
        private Object query;

        private String analyzer;

        @JsonProperty("auto_generate_synonyms_phrase_query")
        private Boolean autoGenerateSynonymsPhraseQuery;

        private String fuzziness;

        @JsonProperty("max_expansions")
        private Integer maxExpansions;

        @JsonProperty("prefix_length")
        private Integer prefixLength;

        @JsonProperty("fuzzy_transpositions")
        private Boolean fuzzyTranspositions;

        @JsonProperty("fuzzy_rewrite")
        private String fuzzyRewrite;

        private Boolean lenient;

        private String operator;

        @JsonProperty("minimum_should_match")
        private String minimumShouldMatch;

        @JsonProperty("zero_terms_query")
        private String zeroTermsQuery;

        public Object getQuery() {
            return query;
        }

        public void setQuery(final Object query) {
            this.query = query;
        }

        public String getAnalyzer() {
            return analyzer;
        }

        public void setAnalyzer(final String analyzer) {
            this.analyzer = analyzer;
        }

        public Boolean getAutoGenerateSynonymsPhraseQuery() {
            return autoGenerateSynonymsPhraseQuery;
        }

        public void setAutoGenerateSynonymsPhraseQuery(
                final Boolean autoGenerateSynonymsPhraseQuery)
        {
            this.autoGenerateSynonymsPhraseQuery = autoGenerateSynonymsPhraseQuery;
        }

        public String getFuzziness() {
            return fuzziness;
        }

        public void setFuzziness(final String fuzziness) {
            this.fuzziness = fuzziness;
        }

        public Integer getMaxExpansions() {
            return maxExpansions;
        }

        public void setMaxExpansions(final Integer maxExpansions) {
            this.maxExpansions = maxExpansions;
        }

        public Integer getPrefixLength() {
            return prefixLength;
        }

        public void setPrefixLength(final Integer prefixLength) {
            this.prefixLength = prefixLength;
        }

        public Boolean getFuzzyTranspositions() {
            return fuzzyTranspositions;
        }

        public void setFuzzyTranspositions(
                final Boolean fuzzyTranspositions)
        {
            this.fuzzyTranspositions = fuzzyTranspositions;
        }

        public String getFuzzyRewrite() {
            return fuzzyRewrite;
        }

        public void setFuzzyRewrite(final String fuzzyRewrite) {
            this.fuzzyRewrite = fuzzyRewrite;
        }

        public Boolean getLenient() {
            return lenient;
        }

        public void setLenient(final Boolean lenient) {
            this.lenient = lenient;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(final String operator) {
            this.operator = operator;
        }

        public String getMinimumShouldMatch() {
            return minimumShouldMatch;
        }

        public void setMinimumShouldMatch(final String minimumShouldMatch)
        {
            this.minimumShouldMatch = minimumShouldMatch;
        }

        public String getZeroTermsQuery() {
            return zeroTermsQuery;
        }

        public void setZeroTermsQuery(final String zeroTermsQuery) {
            this.zeroTermsQuery = zeroTermsQuery;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final FieldParams that = (FieldParams) o;
            return Objects.equals(query, that.query) &&
                    Objects.equals(analyzer, that.analyzer) &&
                    Objects.equals(
                            autoGenerateSynonymsPhraseQuery,
                            that.autoGenerateSynonymsPhraseQuery) &&
                    Objects.equals(fuzziness, that.fuzziness) &&
                    Objects.equals(maxExpansions, that.maxExpansions) &&
                    Objects.equals(prefixLength, that.prefixLength) &&
                    Objects.equals(fuzzyTranspositions, that.fuzzyTranspositions) &&
                    Objects.equals(fuzzyRewrite, that.fuzzyRewrite) &&
                    Objects.equals(lenient, that.lenient) &&
                    Objects.equals(operator, that.operator) &&
                    Objects.equals(minimumShouldMatch, that.minimumShouldMatch) &&
                    Objects.equals(zeroTermsQuery, that.zeroTermsQuery);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    query,
                    analyzer,
                    autoGenerateSynonymsPhraseQuery,
                    fuzziness,
                    maxExpansions,
                    prefixLength,
                    fuzzyTranspositions,
                    fuzzyRewrite,
                    lenient,
                    operator,
                    minimumShouldMatch,
                    zeroTermsQuery);
        }

        @Override
        public String toString() {
            return "FieldParams{" +
                    "query='" + query + '\'' +
                    ", analyzer='" + analyzer + '\'' +
                    ", autoGenerateSynonymsPhraseQuery=" +
                    autoGenerateSynonymsPhraseQuery +
                    ", fuzziness='" + fuzziness + '\'' +
                    ", maxExpansions=" + maxExpansions +
                    ", prefixLength=" + prefixLength +
                    ", fuzzyTranspositions=" + fuzzyTranspositions +
                    ", fuzzyRewrite='" + fuzzyRewrite + '\'' +
                    ", lenient=" + lenient +
                    ", operator=" + operator +
                    ", minimumShouldMatch='" + minimumShouldMatch + '\'' +
                    ", zeroTermsQuery=" + zeroTermsQuery +
                    '}';
        }
    }
}
