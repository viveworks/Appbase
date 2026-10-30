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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
// https://www.elastic.co/guide/en/elasticsearch/reference/current/highlighting.html
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Highlight {
    @JsonProperty("boundary_chars")
    private String boundaryChars;

    @JsonProperty("boundary_max_scan")
    private Integer boundaryMaxScan;

    @JsonProperty("boundary_scanner")
    private String boundaryScanner;

    @JsonProperty("boundary_scanner_locale")
    private String boundaryScannerLocale;

    @JsonProperty("encoder")
    private String encoder;

    @JsonSerialize(keyAs = String.class, contentAs = Highlight.class)
    @JsonDeserialize(keyAs = String.class, contentAs = Highlight.class)
    private Map<String, Highlight> fields;

    @JsonProperty("force_source")
    private Boolean forceSource;

    private String fragmenter;

    @JsonProperty("fragment_offset")
    private Integer fragmentOffset;

    @JsonProperty("fragment_size")
    private Integer fragmentSize;

    @JsonProperty("highlight_query")
    private SearchQuery highlightQuery;

    @JsonProperty("matched_fields")
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private List<String> matchedFields;

    @JsonProperty("no_match_size")
    private Integer noMatchSize;

    @JsonProperty("number_of_fragments")
    private Integer numberOfFragments;

    private String order;

    @JsonProperty("phrase_limit")
    private Integer phraseLimit;

    @JsonProperty("pre_tags")
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private List<String> preTags;

    @JsonProperty("post_tags")
    @JsonSerialize(contentAs = String.class)
    @JsonDeserialize(contentAs = String.class)
    private List<String> postTags;

    @JsonProperty("require_field_match")
    private Boolean requireFieldMatch;

    @JsonProperty("max_analyzed_offset")
    private Integer maxAnalyzedOffset;

    @JsonProperty("tags_schema")
    private String tagsSchema;

    private String type;

    public String getBoundaryChars() {
        return boundaryChars;
    }

    public void setBoundaryChars(final String boundaryChars) {
        this.boundaryChars = boundaryChars;
    }

    public Integer getBoundaryMaxScan() {
        return boundaryMaxScan;
    }

    public void setBoundaryMaxScan(final Integer boundaryMaxScan) {
        this.boundaryMaxScan = boundaryMaxScan;
    }

    public String getBoundaryScanner() {
        return boundaryScanner;
    }

    public void setBoundaryScanner(final String boundaryScanner) {
        this.boundaryScanner = boundaryScanner;
    }

    public String getBoundaryScannerLocale() {
        return boundaryScannerLocale;
    }

    public void setBoundaryScannerLocale(final String boundaryScannerLocale) {
        this.boundaryScannerLocale = boundaryScannerLocale;
    }

    public String getEncoder() {
        return encoder;
    }

    public void setEncoder(final String encoder) {
        this.encoder = encoder;
    }

    public Map<String, Highlight> getFields() {
        return fields;
    }

    public void setFields(final Map<String, Highlight> fields) {
        this.fields = fields;
    }

    public Boolean getForceSource() {
        return forceSource;
    }

    public void setForceSource(final Boolean forceSource) {
        this.forceSource = forceSource;
    }

    public String getFragmenter() {
        return fragmenter;
    }

    public void setFragmenter(final String fragmenter) {
        this.fragmenter = fragmenter;
    }

    public Integer getFragmentOffset() {
        return fragmentOffset;
    }

    public void setFragmentOffset(final Integer fragmentOffset) {
        this.fragmentOffset = fragmentOffset;
    }

    public Integer getFragmentSize() {
        return fragmentSize;
    }

    public void setFragmentSize(final Integer fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    public SearchQuery getHighlightQuery() {
        return highlightQuery;
    }

    public void setHighlightQuery(final SearchQuery highlightQuery) {
        this.highlightQuery = highlightQuery;
    }

    public List<String> getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(final List<String> matchedFields) {
        this.matchedFields = matchedFields;
    }

    public Integer getNoMatchSize() {
        return noMatchSize;
    }

    public void setNoMatchSize(final Integer noMatchSize) {
        this.noMatchSize = noMatchSize;
    }

    public Integer getNumberOfFragments() {
        return numberOfFragments;
    }

    public void setNumberOfFragments(final Integer numberOfFragments) {
        this.numberOfFragments = numberOfFragments;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(final String order) {
        this.order = order;
    }

    public Integer getPhraseLimit() {
        return phraseLimit;
    }

    public void setPhraseLimit(final Integer phraseLimit) {
        this.phraseLimit = phraseLimit;
    }

    public List<String> getPreTags() {
        return preTags;
    }

    public void setPreTags(final List<String> preTags) {
        this.preTags = preTags;
    }

    public List<String> getPostTags() {
        return postTags;
    }

    public void setPostTags(final List<String> postTags) {
        this.postTags = postTags;
    }

    public Boolean getRequireFieldMatch() {
        return requireFieldMatch;
    }

    public void setRequireFieldMatch(final Boolean requireFieldMatch) {
        this.requireFieldMatch = requireFieldMatch;
    }

    public Integer getMaxAnalyzedOffset() {
        return maxAnalyzedOffset;
    }

    public void setMaxAnalyzedOffset(final Integer maxAnalyzedOffset) {
        this.maxAnalyzedOffset = maxAnalyzedOffset;
    }

    public String getTagsSchema() {
        return tagsSchema;
    }

    public void setTagsSchema(final String tagsSchema) {
        this.tagsSchema = tagsSchema;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Highlight highlight = (Highlight) o;
        return Objects.equals(boundaryChars, highlight.boundaryChars) &&
                Objects.equals(boundaryMaxScan, highlight.boundaryMaxScan) &&
                Objects.equals(boundaryScanner, highlight.boundaryScanner) &&
                Objects.equals(boundaryScannerLocale, highlight.boundaryScannerLocale) &&
                Objects.equals(encoder, highlight.encoder) &&
                Objects.equals(fields, highlight.fields) &&
                Objects.equals(forceSource, highlight.forceSource) &&
                Objects.equals(fragmenter, highlight.fragmenter) &&
                Objects.equals(fragmentOffset, highlight.fragmentOffset) &&
                Objects.equals(fragmentSize, highlight.fragmentSize) &&
                Objects.equals(highlightQuery, highlight.highlightQuery) &&
                Objects.equals(matchedFields, highlight.matchedFields) &&
                Objects.equals(noMatchSize, highlight.noMatchSize) &&
                Objects.equals(numberOfFragments, highlight.numberOfFragments) &&
                Objects.equals(order, highlight.order) &&
                Objects.equals(phraseLimit, highlight.phraseLimit) &&
                Objects.equals(preTags, highlight.preTags) &&
                Objects.equals(postTags, highlight.postTags) &&
                Objects.equals(requireFieldMatch, highlight.requireFieldMatch) &&
                Objects.equals(maxAnalyzedOffset, highlight.maxAnalyzedOffset) &&
                Objects.equals(tagsSchema, highlight.tagsSchema) &&
                Objects.equals(type, highlight.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                boundaryChars,
                boundaryMaxScan,
                boundaryScanner,
                boundaryScannerLocale,
                encoder,
                fields,
                forceSource,
                fragmenter,
                fragmentOffset,
                fragmentSize,
                highlightQuery,
                matchedFields,
                noMatchSize,
                numberOfFragments,
                order,
                phraseLimit,
                preTags,
                postTags,
                requireFieldMatch,
                maxAnalyzedOffset,
                tagsSchema,
                type);
    }

    @Override
    public String toString() {
        return "Highlight{" +
                "boundaryChars='" + boundaryChars + '\'' +
                ", boundaryMaxScan=" + boundaryMaxScan +
                ", boundaryScanner='" + boundaryScanner + '\'' +
                ", boundaryScannerLocale='" + boundaryScannerLocale + '\'' +
                ", encoder='" + encoder + '\'' +
                ", fields=" + fields +
                ", forceSource=" + forceSource +
                ", fragmenter='" + fragmenter + '\'' +
                ", fragmentOffset=" + fragmentOffset +
                ", fragmentSize=" + fragmentSize +
                ", highlightQuery=" + highlightQuery +
                ", matchedFields=" + matchedFields +
                ", noMatchSize=" + noMatchSize +
                ", numberOfFragments=" + numberOfFragments +
                ", order='" + order + '\'' +
                ", phraseLimit=" + phraseLimit +
                ", preTags=" + preTags +
                ", postTags=" + postTags +
                ", requireFieldMatch=" + requireFieldMatch +
                ", maxAnalyzedOffset=" + maxAnalyzedOffset +
                ", tagsSchema='" + tagsSchema + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
