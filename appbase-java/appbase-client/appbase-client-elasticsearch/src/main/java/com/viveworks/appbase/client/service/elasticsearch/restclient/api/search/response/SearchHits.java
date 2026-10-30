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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response;

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
public class SearchHits {
    private Total total;

    @JsonProperty("max_score")
    private Float maxScore;

    @JsonSerialize(contentAs = Hits.class)
    @JsonDeserialize(contentAs = Hits.class)
    private List<Hits> hits;

    public Total getTotal() {
        return total;
    }

    public void setTotal(final Total total) {
        this.total = total;
    }

    public Float getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(final Float maxScore) {
        this.maxScore = maxScore;
    }

    public List<Hits> getHits() {
        return hits;
    }

    public void setHits(final List<Hits> hits) {
        this.hits = hits;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SearchHits searchHits = (SearchHits) o;
        return Objects.equals(total, searchHits.total) &&
                Objects.equals(maxScore, searchHits.maxScore) &&
                Objects.equals(hits, searchHits.hits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, maxScore, hits);
    }

    @Override
    public String toString() {
        return "SearchHits{" +
                "total=" + total +
                ", maxScore=" + maxScore +
                ", hits=" + hits +
                '}';
    }
}
