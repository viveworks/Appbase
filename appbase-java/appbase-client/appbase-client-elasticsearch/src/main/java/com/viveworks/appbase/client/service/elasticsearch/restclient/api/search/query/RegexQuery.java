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

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegexQuery implements SearchQuery {
    @JsonSerialize(keyAs = String.class, contentAs = FieldParams.class)
    @JsonDeserialize(keyAs = String.class, contentAs = FieldParams.class)
    private Map<String, FieldParams> params;

    public RegexQuery() {
    }

    public Map<String, FieldParams> getParams() {
        return params;
    }

    public void setParams(final Map<String, FieldParams> params) {
        this.params = params;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RegexQuery that = (RegexQuery) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return "RegexQuery{" +
                "params=" + params +
                '}';
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldParams {
        private String value;

        private String flags;

        @JsonProperty("case_insensitive")
        private Boolean caseInsensitive;

        @JsonProperty("max_determinized_states")
        private Integer maxDeterminizedStates;

        private String rewrite;

        public FieldParams() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getFlags() {
            return flags;
        }

        public void setFlags(final String flags) {
            this.flags = flags;
        }

        public Boolean getCaseInsensitive() {
            return caseInsensitive;
        }

        public void setCaseInsensitive(final Boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        public Integer getMaxDeterminizedStates() {
            return maxDeterminizedStates;
        }

        public void setMaxDeterminizedStates(final Integer maxDeterminizedStates) {
            this.maxDeterminizedStates = maxDeterminizedStates;
        }

        public String getRewrite() {
            return rewrite;
        }

        public void setRewrite(final String rewrite) {
            this.rewrite = rewrite;
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
            return Objects.equals(value, that.value) &&
                    Objects.equals(flags, that.flags) &&
                    Objects.equals(caseInsensitive, that.caseInsensitive) &&
                    Objects.equals(maxDeterminizedStates, that.maxDeterminizedStates) &&
                    Objects.equals(rewrite, that.rewrite);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, flags, caseInsensitive, maxDeterminizedStates, rewrite);
        }

        @Override
        public String toString() {
            return "FieldParams{" +
                    "value='" + value + '\'' +
                    ", flags='" + flags + '\'' +
                    ", caseInsensitive=" + caseInsensitive +
                    ", maxDeterminizedStates=" + maxDeterminizedStates +
                    ", rewrite='" + rewrite + '\'' +
                    '}';
        }
    }
}
