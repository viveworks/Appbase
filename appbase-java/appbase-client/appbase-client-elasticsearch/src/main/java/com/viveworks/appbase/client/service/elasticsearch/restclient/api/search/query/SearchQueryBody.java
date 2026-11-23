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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
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
public class SearchQueryBody {
    @JsonProperty("docvalue_fields")
    @JsonSerialize(contentAs = Field.class)
    @JsonDeserialize(contentAs = Field.class)
    private List<Field> docValueFields;

    @JsonProperty("fields")
    @JsonSerialize(contentAs = Field.class)
    private List<Field> fields;

    @JsonProperty("stored_fields")
    private String storedFieldCsv;

    private Boolean explain;

    private Integer from;

    @JsonProperty("indices_boost")
    private List<Map<String, Float>> indicesBoost;

    private Knn knn;

    @JsonProperty("min_score")
    private Float minScore;

    private Pit pit;

    private SearchQuery query;

    @JsonProperty("runtime_mappings")
    @JsonSerialize(keyAs = String.class, contentAs = RuntimeMapping.class)
    @JsonDeserialize(keyAs = String.class, contentAs = RuntimeMapping.class)
    private Map<String, RuntimeMapping> runtimeMappings;

    @JsonProperty("seq_no_primary_term")
    private Boolean seqNoPrimaryTerm;

    private Integer size;

    @JsonIgnore
    private Boolean sourceDisable;

    @JsonIgnore
    private Boolean sourceEntireDocument;

    @JsonIgnore
    private List<String> sourceWildcardPatterns;

    @JsonIgnore
    private SourceObject sourceObject;

    @JsonProperty("_source")
    private Object sourceDerived;

    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private List<String> stats;

    @JsonProperty("terminate_after")
    private Integer terminateAfter;

    private String timeout;

    @JsonProperty("track_scores")
    private Boolean trackScores;

    private Boolean version;

    @JsonProperty("search_after")
    private List<Object> searchAfter;

    // Object can be a:
    // Map<String, String>
    // Map<String, OrderedSortValue>
    // FieldSortValue
    // Map<String, NestedSortValue>
    // Map<String, MissingSortValue>
    // Map<"_geo_distance", GeoDistanceSortValue>
    // ends with '_source' or '_doc'
    private List<Object> sort;

    private Collapse collapse;

    private Highlight highlight;

    private Rescore rescore;

    private Slice slice;

    @JsonProperty("aggs")
    @JsonRawValue
    private String aggregationsJson;

    public Object getSourceDerived() {
        if (sourceDisable != null) {
            return sourceDisable;
        }

        if (sourceEntireDocument != null) {
            return sourceEntireDocument;
        }

        if (sourceWildcardPatterns != null) {
            return sourceWildcardPatterns;
        }

        return sourceObject;
    }

    public void setSourceDerived(final Object sourceDerived) {
        this.sourceDerived = sourceDerived;
    }

    public List<Field> getDocValueFields() {
        return docValueFields;
    }

    public void setDocValueFields(final List<Field> docValueFields) {
        this.docValueFields = docValueFields;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(final List<Field> fields) {
        this.fields = fields;
    }

    public String getStoredFieldCsv() {
        return storedFieldCsv;
    }

    public void setStoredFieldCsv(final String storedFieldCsv) {
        this.storedFieldCsv = storedFieldCsv;
    }

    public Boolean getExplain() {
        return explain;
    }

    public void setExplain(final Boolean explain) {
        this.explain = explain;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(final Integer from) {
        this.from = from;
    }

    public List<Map<String, Float>> getIndicesBoost() {
        return indicesBoost;
    }

    public void setIndicesBoost(final List<Map<String, Float>> indicesBoost) {
        this.indicesBoost = indicesBoost;
    }

    public Knn getKnn() {
        return knn;
    }

    public void setKnn(final Knn knn) {
        this.knn = knn;
    }

    public Float getMinScore() {
        return minScore;
    }

    public void setMinScore(final Float minScore) {
        this.minScore = minScore;
    }

    public Pit getPit() {
        return pit;
    }

    public void setPit(final Pit pit) {
        this.pit = pit;
    }

    public SearchQuery getQuery() {
        return query;
    }

    public void setQuery(final SearchQuery query) {
        this.query = query;
    }

    public Map<String, RuntimeMapping> getRuntimeMappings() {
        return runtimeMappings;
    }

    public void setRuntimeMappings(
            final Map<String, RuntimeMapping> runtimeMappings)
    {
        this.runtimeMappings = runtimeMappings;
    }

    public Boolean getSeqNoPrimaryTerm() {
        return seqNoPrimaryTerm;
    }

    public void setSeqNoPrimaryTerm(final Boolean seqNoPrimaryTerm) {
        this.seqNoPrimaryTerm = seqNoPrimaryTerm;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    public Boolean getSourceDisable() {
        return sourceDisable;
    }

    public void setSourceDisable(final Boolean sourceDisable) {
        this.sourceDisable = sourceDisable;
    }

    public Boolean getSourceEntireDocument() {
        return sourceEntireDocument;
    }

    public void setSourceEntireDocument(final Boolean sourceEntireDocument) {
        this.sourceEntireDocument = sourceEntireDocument;
    }

    public List<String> getSourceWildcardPatterns() {
        return sourceWildcardPatterns;
    }

    public void setSourceWildcardPatterns(
            final List<String> sourceWildcardPatterns)
    {
        this.sourceWildcardPatterns = sourceWildcardPatterns;
    }

    public SourceObject getSourceObject() {
        return sourceObject;
    }

    public void setSourceObject(final SourceObject sourceObject) {
        this.sourceObject = sourceObject;
    }

    public List<String> getStats() {
        return stats;
    }

    public void setStats(final List<String> stats) {
        this.stats = stats;
    }

    public Integer getTerminateAfter() {
        return terminateAfter;
    }

    public void setTerminateAfter(final Integer terminateAfter) {
        this.terminateAfter = terminateAfter;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(final String timeout) {
        this.timeout = timeout;
    }

    public Boolean getTrackScores() {
        return trackScores;
    }

    public void setTrackScores(final Boolean trackScores) {
        this.trackScores = trackScores;
    }

    public Boolean getVersion() {
        return version;
    }

    public void setVersion(final Boolean version) {
        this.version = version;
    }

    public List<Object> getSort() {
        return sort;
    }

    public void setSort(final List<Object> sort) {
        this.sort = sort;
    }

    public List<Object> getSearchAfter() {
        return searchAfter;
    }

    public void setSearchAfter(final List<Object> searchAfter) {
        this.searchAfter = searchAfter;
    }

    public Collapse getCollapse() {
        return collapse;
    }

    public void setCollapse(final Collapse collapse) {
        this.collapse = collapse;
    }

    public Highlight getHighlight() {
        return highlight;
    }

    public void setHighlight(final Highlight highlight) {
        this.highlight = highlight;
    }

    public Rescore getRescore() {
        return rescore;
    }

    public void setRescore(final Rescore rescore) {
        this.rescore = rescore;
    }

    public Slice getSlice() {
        return slice;
    }

    public void setSlice(final Slice slice) {
        this.slice = slice;
    }

    public String getAggregationsJson() {
        return aggregationsJson;
    }

    public void setAggregationsJson(final String aggregationsJson) {
        this.aggregationsJson = aggregationsJson;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SearchQueryBody that = (SearchQueryBody) o;
        return Objects.equals(docValueFields, that.docValueFields) &&
                Objects.equals(fields, that.fields) &&
                Objects.equals(storedFieldCsv, that.storedFieldCsv) &&
                Objects.equals(explain, that.explain) &&
                Objects.equals(from, that.from) &&
                Objects.equals(indicesBoost, that.indicesBoost) &&
                Objects.equals(knn, that.knn) &&
                Objects.equals(minScore, that.minScore) &&
                Objects.equals(pit, that.pit) &&
                Objects.equals(query, that.query) &&
                Objects.equals(runtimeMappings, that.runtimeMappings) &&
                Objects.equals(seqNoPrimaryTerm, that.seqNoPrimaryTerm) &&
                Objects.equals(size, that.size) &&
                Objects.equals(sourceDisable, that.sourceDisable) &&
                Objects.equals(sourceEntireDocument, that.sourceEntireDocument) &&
                Objects.equals(sourceWildcardPatterns, that.sourceWildcardPatterns) &&
                Objects.equals(sourceObject, that.sourceObject) &&
                Objects.equals(sourceDerived, that.sourceDerived) &&
                Objects.equals(stats, that.stats) &&
                Objects.equals(terminateAfter, that.terminateAfter) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(trackScores, that.trackScores) &&
                Objects.equals(version, that.version) &&
                Objects.equals(sort, that.sort) &&
                Objects.equals(searchAfter, that.searchAfter) &&
                Objects.equals(collapse, that.collapse) &&
                Objects.equals(highlight, that.highlight) &&
                Objects.equals(rescore, that.rescore) &&
                Objects.equals(slice, that.slice) &&
                Objects.equals(aggregationsJson, that.aggregationsJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                docValueFields,
                fields,
                storedFieldCsv,
                explain,
                from,
                indicesBoost,
                knn,
                minScore,
                pit,
                query,
                runtimeMappings,
                seqNoPrimaryTerm,
                size,
                sourceDisable,
                sourceEntireDocument,
                sourceWildcardPatterns,
                sourceObject,
                sourceDerived,
                stats,
                terminateAfter,
                timeout,
                trackScores,
                version,
                sort,
                searchAfter,
                collapse,
                highlight,
                rescore,
                slice,
                aggregationsJson);
    }

    @Override
    public String toString() {
        return "SearchQueryBody{" +
                "docValueFields=" + docValueFields +
                ", fields=" + fields +
                ", storedFieldCsv='" + storedFieldCsv + '\'' +
                ", explain=" + explain +
                ", from=" + from +
                ", indicesBoost=" + indicesBoost +
                ", knn=" + knn +
                ", minScore=" + minScore +
                ", pit=" + pit +
                ", query=" + query +
                ", runtimeMappings=" + runtimeMappings +
                ", seqNoPrimaryTerm=" + seqNoPrimaryTerm +
                ", size=" + size +
                ", sourceDisable=" + sourceDisable +
                ", sourceEntireDocument=" + sourceEntireDocument +
                ", sourceWildcardPatterns=" + sourceWildcardPatterns +
                ", sourceObject=" + sourceObject +
                ", sourceDerived=" + sourceDerived +
                ", stats=" + stats +
                ", terminateAfter=" + terminateAfter +
                ", timeout='" + timeout + '\'' +
                ", trackScores=" + trackScores +
                ", version=" + version +
                ", sort=" + sort +
                ", searchAfter=" + searchAfter +
                ", collapse=" + collapse +
                ", highlight=" + highlight +
                ", rescore=" + rescore +
                ", slice=" + slice +
                ", aggregationsJson=" + aggregationsJson +
                '}';
    }
}
