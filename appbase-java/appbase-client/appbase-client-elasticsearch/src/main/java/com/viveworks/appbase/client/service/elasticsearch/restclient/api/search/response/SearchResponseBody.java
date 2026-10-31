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
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponseBody {
    @JsonProperty("_scroll_id")
    private String scrollId;

    @JsonProperty("took")
    private Integer tookMillis;

    @JsonProperty("timed_out")
    private Boolean timedOut;

    @JsonProperty("_shards")
    private Shards shards;

    @JsonProperty("hits")
    private SearchHits hits;

    @JsonRawValue
    private String aggregations;

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(final String scrollId) {
        this.scrollId = scrollId;
    }

    public Integer getTookMillis() {
        return tookMillis;
    }

    public void setTookMillis(final Integer tookMillis) {
        this.tookMillis = tookMillis;
    }

    public Boolean getTimedOut() {
        return timedOut;
    }

    public void setTimedOut(final Boolean timedOut) {
        this.timedOut = timedOut;
    }

    public Shards getShards() {
        return shards;
    }

    public void setShards(final Shards shards) {
        this.shards = shards;
    }

    public SearchHits getHits() {
        return hits;
    }

    public void setHits(final SearchHits hits) {
        this.hits = hits;
    }

    public String getAggregations() {
        return aggregations;
    }

    public void setAggregations(final String aggregations) {
        this.aggregations = aggregations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SearchResponseBody that = (SearchResponseBody) o;
        return Objects.equals(scrollId, that.scrollId) &&
                Objects.equals(tookMillis, that.tookMillis) &&
                Objects.equals(timedOut, that.timedOut) &&
                Objects.equals(shards, that.shards) &&
                Objects.equals(hits, that.hits) &&
                Objects.equals(aggregations, that.aggregations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                scrollId, tookMillis, timedOut, shards, hits, aggregations);
    }

    @Override
    public String toString() {
        return "SearchResponseBody{" +
                "scrollId='" + scrollId + '\'' +
                ", tookMillis=" + tookMillis +
                ", timedOut=" + timedOut +
                ", shards=" + shards +
                ", hits=" + hits +
                ", aggregations=" + aggregations +
                '}';
    }
}
