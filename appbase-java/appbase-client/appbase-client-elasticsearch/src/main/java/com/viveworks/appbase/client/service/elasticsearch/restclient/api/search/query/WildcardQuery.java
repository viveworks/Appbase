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
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WildcardQuery implements SearchQuery {
    @JsonSerialize(keyAs = String.class, contentAs = FieldParams.class)
    @JsonDeserialize(keyAs = String.class, contentAs = FieldParams.class)
    private Map<String, FieldParams> wildcard;

    public Map<String, FieldParams> getWildcard() {
        return wildcard;
    }

    public void setWildcard(final Map<String, FieldParams> wildcard) {
        this.wildcard = wildcard;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final WildcardQuery that = (WildcardQuery) o;
        return Objects.equals(wildcard, that.wildcard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wildcard);
    }

    @Override
    public String toString() {
        return "WildcardQuery{" +
                "wildcard=" + wildcard +
                '}';
    }

    public static boolean hasWildcard(final String text) {
        AppbaseUtil.notBlank(text, "text");

        return text.contains("*") || text.contains("?");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldParams {
        private Float boost;

        @JsonProperty("case_insensitive")
        private Boolean caseInsensitive;

        private String rewrite;

        private String value;

        public FieldParams() {
        }

        public Float getBoost() {
            return boost;
        }

        public void setBoost(final Float boost) {
            this.boost = boost;
        }

        public Boolean getCaseInsensitive() {
            return caseInsensitive;
        }

        public void setCaseInsensitive(final Boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        public String getRewrite() {
            return rewrite;
        }

        public void setRewrite(final String rewrite) {
            this.rewrite = rewrite;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
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
            return Objects.equals(boost, that.boost) &&
                    Objects.equals(caseInsensitive, that.caseInsensitive) &&
                    Objects.equals(rewrite, that.rewrite) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(boost, caseInsensitive, rewrite, value);
        }

        @Override
        public String toString() {
            return "FieldParams{" +
                    "boost=" + boost +
                    ", caseInsensitive=" + caseInsensitive +
                    ", rewrite='" + rewrite + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
