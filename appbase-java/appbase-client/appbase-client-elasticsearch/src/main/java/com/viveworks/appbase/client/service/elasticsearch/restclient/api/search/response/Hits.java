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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
// https://www.elastic.co/guide/en/elasticsearch/reference/current/search-fields.html#search-fields-param
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Hits {
    @JsonProperty("_index")
    private String index;

    @JsonProperty("_id")
    private String id;

    @JsonProperty("_score")
    private Float score;

    @JsonRawValue
    @JsonProperty("_source")
    private Object sourceJson;

    @JsonRawValue
    @JsonProperty("fields")
    private String fieldsJson;

    @JsonProperty("inner_hits")
    @JsonSerialize(keyAs = String.class, contentAs = SearchHits.class)
    @JsonDeserialize(keyAs = String.class, contentAs = SearchHits.class)
    private Map<String, SearchHits> innerHits;

    public String getIndex() {
        return index;
    }

    public void setIndex(final String index) {
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(final Float score) {
        this.score = score;
    }

    public Object getSourceJson() {
        return sourceJson;
    }

    public void setSourceJson(final Object sourceJson) {
        this.sourceJson = sourceJson;
    }

    public String getFieldsJson() {
        return fieldsJson;
    }

    public void setFieldsJson(final String fieldsJson) {
        this.fieldsJson = fieldsJson;
    }

    public Map<String, SearchHits> getInnerHits() {
        return innerHits;
    }

    public void setInnerHits(final Map<String, SearchHits> innerHits) {
        this.innerHits = innerHits;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Hits hits = (Hits) o;
        return Objects.equals(index, hits.index) &&
                Objects.equals(id, hits.id) &&
                Objects.equals(score, hits.score) &&
                Objects.equals(sourceJson, hits.sourceJson) &&
                Objects.equals(fieldsJson, hits.fieldsJson) &&
                Objects.equals(innerHits, hits.innerHits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, id, score, sourceJson, fieldsJson, innerHits);
    }

    @Override
    public String toString() {
        return "Hits{" +
                "index='" + index + '\'' +
                ", id='" + id + '\'' +
                ", score=" + score +
                ", sourceJson='" + sourceJson + '\'' +
                ", fieldsJson='" + fieldsJson + '\'' +
                ", innerHits=" + innerHits +
                '}';
    }
}
