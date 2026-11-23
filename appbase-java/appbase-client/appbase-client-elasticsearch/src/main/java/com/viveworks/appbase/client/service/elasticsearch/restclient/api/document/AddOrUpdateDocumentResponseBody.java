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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.Shards;

import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddOrUpdateDocumentResponseBody {
    @JsonProperty("_shards")
    private Shards shards;

    @JsonProperty("_index")
    private String index;

    @JsonProperty("_id")
    private String id;

    @JsonProperty("_version")
    private Integer version;

    @JsonProperty("_seq_no")
    private Integer sequenceNumber;

    @JsonProperty("_primary_term")
    private Integer primaryTerm;

    private String result;

    public Shards getShards() {
        return shards;
    }

    public void setShards(final Shards shards) {
        this.shards = shards;
    }

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getPrimaryTerm() {
        return primaryTerm;
    }

    public void setPrimaryTerm(final Integer primaryTerm) {
        this.primaryTerm = primaryTerm;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
        this.result = result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AddOrUpdateDocumentResponseBody that = (AddOrUpdateDocumentResponseBody) o;
        return Objects.equals(shards, that.shards) &&
                Objects.equals(index, that.index) &&
                Objects.equals(id, that.id) &&
                Objects.equals(version, that.version) &&
                Objects.equals(sequenceNumber, that.sequenceNumber) &&
                Objects.equals(primaryTerm, that.primaryTerm) &&
                Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                shards,
                index,
                id,
                version,
                sequenceNumber,
                primaryTerm,
                result);
    }

    @Override
    public String toString() {
        return "AddDocumentResponseBody{" +
                "shards=" + shards +
                ", index='" + index + '\'' +
                ", id='" + id + '\'' +
                ", version=" + version +
                ", sequenceNumber=" + sequenceNumber +
                ", primaryTerm=" + primaryTerm +
                ", result='" + result + '\'' +
                '}';
    }
}
