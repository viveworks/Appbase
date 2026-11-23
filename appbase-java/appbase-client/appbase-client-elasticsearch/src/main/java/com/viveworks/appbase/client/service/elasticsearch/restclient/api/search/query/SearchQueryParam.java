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

import com.viveworks.appbase.core.util.AppbaseUtil;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum SearchQueryParam {
    ALLOW_NO_INDICES("allow_no_indices"),
    ALLOW_PARTIAL_SEARCH_RESULTS("allow_partial_search_results"),
    ANALYZER("analyzer"),
    ANALYZE_WILDCARD("analyze_wildcard"),
    BATCHED_REDUCE_SIZE("batched_reduce_size"),
    CCS_MINIMIZE_ROUND_TRIPS("ccs_minimize_roundtrips"),
    DEFAULT_OPERATOR("default_operator"),
    DF("df"),
    DOCVALUE_FIELDS("docvalue_fields"),
    EXPAND_WILDCARDS("expand_wildcards"),
    EXPLAIN("explain"),
    FROM("from"),
    IGNORE_THROTTLED("ignore_throttled"),
    IGNORE_UNAVAILABLE("ignore_unavailable"),
    LENIENT("lenient"),
    MAX_CONCURRENT_SEARCHES("max_concurrent_searches"),
    MAX_CONCURRENT_SHARD_REQUESTS("max_concurrent_shard_requests"),
    PRE_FILTER_SHARD_SIZE("pre_filter_shard_size"),
    PREFERENCE("preference"),
    Q("q"),
    REQUEST_CACHE("request_cache"),
    REST_TOTAL_HITS_AS_INT("rest_total_hits_as_int"),
    ROUTING("routing"),
    SCROLL("scroll"),
    SEARCH_TYPE("search_type"),
    SEQ_NO_PRIMARY_TERM("seq_no_primary_term"),
    SIZE("size"),
    SORT("sort"),
    UNDERSCORE_source("_score"),
    UNDERSCORE_SOURCE_EXCLUDES("_source_excludes"),
    UNDERSCORE_SOURCE_INCLUDES("_source_includes"),
    STATS("stats"),
    STORED_FIELDS("stored_fields"),
    SUGGEST_FIELD("suggest_field"),
    SUGGEST_MODE("suggest_mode"),
    SUGGEST_SIZE("suggest_size"),
    SUGGEST_TEXT("suggest_text"),
    TERMINATE_AFTER("terminate_after"),
    TIMEOUT("timeout"),
    TRACK_SCORES("track_scores"),
    TRACK_TOTAL_HITS("track_total_hits"),
    TYPED_KEYS("typed_keys"),
    VERSION("version")
    ;

    private final String text;

    SearchQueryParam(final String text) {
        AppbaseUtil.notBlank(text, "text");

        this.text = text;
    }

    public String getText() {
        return text;
    }
}
