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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class RescoreQuery {
    @JsonProperty("rescore_query")
    private Map<String, SearchQuery> rescoreQuery;

    @JsonProperty("query_weight")
    private Float queryWeight;

    @JsonProperty("rescore_query_weight")
    private Float rescoreQueryWeight;

    @JsonProperty("score_mode")
    private String scoreMode;

    public Map<String, SearchQuery> getRescoreQuery() {
        return rescoreQuery;
    }

    public void setRescoreQuery(final Map<String, SearchQuery> rescoreQuery) {
        this.rescoreQuery = rescoreQuery;
    }

    public Float getQueryWeight() {
        return queryWeight;
    }

    public void setQueryWeight(final Float queryWeight) {
        this.queryWeight = queryWeight;
    }

    public Float getRescoreQueryWeight() {
        return rescoreQueryWeight;
    }

    public void setRescoreQueryWeight(final Float rescoreQueryWeight) {
        this.rescoreQueryWeight = rescoreQueryWeight;
    }

    public String getScoreMode() {
        return scoreMode;
    }

    public void setScoreMode(final String scoreMode) {
        this.scoreMode = scoreMode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RescoreQuery that = (RescoreQuery) o;
        return Objects.equals(rescoreQuery, that.rescoreQuery) &&
                Objects.equals(queryWeight, that.queryWeight) &&
                Objects.equals(rescoreQueryWeight, that.rescoreQueryWeight) &&
                Objects.equals(scoreMode, that.scoreMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rescoreQuery, queryWeight, rescoreQueryWeight, scoreMode);
    }

    @Override
    public String toString() {
        return "RescoreQuery{" +
                "rescoreQuery=" + rescoreQuery +
                ", queryWeight=" + queryWeight +
                ", rescoreQueryWeight=" + rescoreQueryWeight +
                ", scoreMode='" + scoreMode + '\'' +
                '}';
    }
}
