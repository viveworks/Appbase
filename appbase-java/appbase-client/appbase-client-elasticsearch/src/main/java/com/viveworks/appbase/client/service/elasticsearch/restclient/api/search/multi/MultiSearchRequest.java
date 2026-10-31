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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.multi;

import com.viveworks.appbase.core.util.AppbaseUtil;
import jakarta.validation.constraints.NotEmpty;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class MultiSearchRequest {
    @Nullable
    private String targetCsv;

    // Path parameter ..........
    @Nullable
    private List<String> targets;


    // Query parameter ..........

    @Nullable
    private Boolean allowNoIndices;

    @Nullable
    private Boolean ccsMinimizeRoundTrips;

    @Nullable
    private String expandWildcards;

    @Nullable
    private Boolean ignoreThrottled;

    @Nullable
    private Boolean ignoreUnavailable;

    @Nullable
    private Integer maxConcurrentSearches;

    @Nullable
    private Integer maxConcurrentShardRequests;

    @Nullable
    private Integer preFilterShardSize;

    @Nullable
    private Boolean restTotalHitsAsInt;

    @Nullable
    private String routing;

    @Nullable
    private String searchType;

    @Nullable
    private Boolean typedKeys;

    @NotEmpty
    private final List<MultiSearchRequestEnvelope> multiSearchRequests;

    public MultiSearchRequest(
            final List<MultiSearchRequestEnvelope> multiSearchRequests)
    {
        AppbaseUtil.notEmpty(multiSearchRequests, "multiSearchRequests");

        this.multiSearchRequests = multiSearchRequests;
    }

    @Nullable
    public String getTargetCsv() {
        return targetCsv;
    }

    public void setTargetCsv(
            @Nullable final String targetCsv)
    {
        this.targetCsv = targetCsv;
    }

    @Nullable
    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(
            @Nullable final List<String> targets)
    {
        this.targets = targets;
    }

    @Nullable
    public Boolean getAllowNoIndices() {
        return allowNoIndices;
    }

    public void setAllowNoIndices(
            @Nullable final Boolean allowNoIndices)
    {
        this.allowNoIndices = allowNoIndices;
    }

    @Nullable
    public Boolean getCcsMinimizeRoundTrips() {
        return ccsMinimizeRoundTrips;
    }

    public void setCcsMinimizeRoundTrips(
            @Nullable final Boolean ccsMinimizeRoundTrips)
    {
        this.ccsMinimizeRoundTrips = ccsMinimizeRoundTrips;
    }

    @Nullable
    public String getExpandWildcards() {
        return expandWildcards;
    }

    public void setExpandWildcards(
            @Nullable final String expandWildcards)
    {
        this.expandWildcards = expandWildcards;
    }

    @Nullable
    public Boolean getIgnoreThrottled() {
        return ignoreThrottled;
    }

    public void setIgnoreThrottled(
            @Nullable final Boolean ignoreThrottled)
    {
        this.ignoreThrottled = ignoreThrottled;
    }

    @Nullable
    public Boolean getIgnoreUnavailable() {
        return ignoreUnavailable;
    }

    public void setIgnoreUnavailable(
            @Nullable final Boolean ignoreUnavailable)
    {
        this.ignoreUnavailable = ignoreUnavailable;
    }

    @Nullable
    public Integer getMaxConcurrentSearches() {
        return maxConcurrentSearches;
    }

    public void setMaxConcurrentSearches(
            @Nullable final Integer maxConcurrentSearches)
    {
        this.maxConcurrentSearches = maxConcurrentSearches;
    }

    @Nullable
    public Integer getMaxConcurrentShardRequests() {
        return maxConcurrentShardRequests;
    }

    public void setMaxConcurrentShardRequests(
            @Nullable final Integer maxConcurrentShardRequests)
    {
        this.maxConcurrentShardRequests = maxConcurrentShardRequests;
    }

    @Nullable
    public Integer getPreFilterShardSize() {
        return preFilterShardSize;
    }

    public void setPreFilterShardSize(
            @Nullable final Integer preFilterShardSize)
    {
        this.preFilterShardSize = preFilterShardSize;
    }

    @Nullable
    public Boolean getRestTotalHitsAsInt() {
        return restTotalHitsAsInt;
    }

    public void setRestTotalHitsAsInt(
            @Nullable final Boolean restTotalHitsAsInt)
    {
        this.restTotalHitsAsInt = restTotalHitsAsInt;
    }

    @Nullable
    public String getRouting() {
        return routing;
    }

    public void setRouting(@Nullable final String routing) {
        this.routing = routing;
    }

    @Nullable
    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(@Nullable final String searchType) {
        this.searchType = searchType;
    }

    @Nullable
    public Boolean getTypedKeys() {
        return typedKeys;
    }

    public void setTypedKeys(@Nullable final Boolean typedKeys) {
        this.typedKeys = typedKeys;
    }

    public List<MultiSearchRequestEnvelope> getMultiSearchRequests() {
        return multiSearchRequests;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MultiSearchRequest that = (MultiSearchRequest) o;
        return Objects.equals(targetCsv, that.targetCsv) &&
                Objects.equals(targets, that.targets) &&
                Objects.equals(allowNoIndices, that.allowNoIndices) &&
                Objects.equals(
                        ccsMinimizeRoundTrips,
                        that.ccsMinimizeRoundTrips) &&
                Objects.equals(expandWildcards, that.expandWildcards) &&
                Objects.equals(ignoreThrottled, that.ignoreThrottled) &&
                Objects.equals(ignoreUnavailable, that.ignoreUnavailable) &&
                Objects.equals(
                        maxConcurrentSearches,
                        that.maxConcurrentSearches) &&
                Objects.equals(
                        maxConcurrentShardRequests,
                        that.maxConcurrentShardRequests) &&
                Objects.equals(preFilterShardSize, that.preFilterShardSize) &&
                Objects.equals(restTotalHitsAsInt, that.restTotalHitsAsInt) &&
                Objects.equals(routing, that.routing) &&
                Objects.equals(searchType, that.searchType) &&
                Objects.equals(typedKeys, that.typedKeys) &&
                Objects.equals(multiSearchRequests, that.multiSearchRequests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                targetCsv,
                targets,
                allowNoIndices,
                ccsMinimizeRoundTrips,
                expandWildcards,
                ignoreThrottled,
                ignoreUnavailable,
                maxConcurrentSearches,
                maxConcurrentShardRequests,
                preFilterShardSize,
                restTotalHitsAsInt,
                routing,
                searchType,
                typedKeys,
                multiSearchRequests);
    }

    @Override
    public String toString() {
        return "MultiSearchRequest{" +
                ", targets=" + targets +
                ", allowNoIndices=" + allowNoIndices +
                ", ccsMinimizeRoundTrips=" + ccsMinimizeRoundTrips +
                ", expandWildcards='" + expandWildcards + '\'' +
                ", ignoreThrottled=" + ignoreThrottled +
                ", ignoreUnavailable=" + ignoreUnavailable +
                ", maxConcurrentSearches=" + maxConcurrentSearches +
                ", maxConcurrentShardRequests=" + maxConcurrentShardRequests +
                ", preFilterShardSize=" + preFilterShardSize +
                ", restTotalHitsAsInt=" + restTotalHitsAsInt +
                ", routing='" + routing + '\'' +
                ", searchType='" + searchType + '\'' +
                ", typedKeys=" + typedKeys +
                ", multiSearchRequests=" + multiSearchRequests +
                '}';
    }
}
