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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiSearchRequestHeader {
    @JsonProperty("allow_no_indices")
    private Boolean allowNoIndices;

    @JsonProperty("expand_wildcards")
    private String expandWildcards;

    @JsonProperty("ignore_unavailable")
    private Boolean ignoreUnavailable;

    private String index;

    private String preference;

    @JsonProperty("request_cache")
    private Boolean requestCache;

    private String routing;

    @JsonProperty("search_type")
    private String searchType;

    public Boolean getAllowNoIndices() {
        return allowNoIndices;
    }

    public void setAllowNoIndices(final Boolean allowNoIndices) {
        this.allowNoIndices = allowNoIndices;
    }

    public String getExpandWildcards() {
        return expandWildcards;
    }

    public void setExpandWildcards(final String expandWildcards) {
        this.expandWildcards = expandWildcards;
    }

    public Boolean getIgnoreUnavailable() {
        return ignoreUnavailable;
    }

    public void setIgnoreUnavailable(final Boolean ignoreUnavailable) {
        this.ignoreUnavailable = ignoreUnavailable;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(final String index) {
        this.index = index;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(final String preference) {
        this.preference = preference;
    }

    public Boolean getRequestCache() {
        return requestCache;
    }

    public void setRequestCache(final Boolean requestCache) {
        this.requestCache = requestCache;
    }

    public String getRouting() {
        return routing;
    }

    public void setRouting(final String routing) {
        this.routing = routing;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(final String searchType) {
        this.searchType = searchType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MultiSearchRequestHeader that = (MultiSearchRequestHeader) o;
        return Objects.equals(allowNoIndices, that.allowNoIndices) &&
                Objects.equals(expandWildcards, that.expandWildcards) &&
                Objects.equals(ignoreUnavailable, that.ignoreUnavailable) &&
                Objects.equals(index, that.index) &&
                Objects.equals(preference, that.preference) &&
                Objects.equals(requestCache, that.requestCache) &&
                Objects.equals(routing, that.routing) &&
                Objects.equals(searchType, that.searchType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                allowNoIndices,
                expandWildcards,
                ignoreUnavailable,
                index,
                preference,
                requestCache,
                routing,
                searchType);
    }

    @Override
    public String toString() {
        return "MultiSearchRequestHeader{" +
                "allowNoIndices=" + allowNoIndices +
                ", expandWildcards='" + expandWildcards + '\'' +
                ", ignoreUnavailable=" + ignoreUnavailable +
                ", index='" + index + '\'' +
                ", preference='" + preference + '\'' +
                ", requestCache=" + requestCache +
                ", routing='" + routing + '\'' +
                ", searchType='" + searchType + '\'' +
                '}';
    }
}
