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

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AddOrUpdateDocumentRequest {
    private final String requestJson;

    private final String target;

    @Nullable
    private String documentId;

    @Nullable
    private Integer ifSeqNo;

    @Nullable
    private Integer ifPrimaryTerm;

    @Nullable
    private String opType;

    @Nullable
    private String pipeline;

    @Nullable
    private String refresh;

    @Nullable
    private String routing;

    @Nullable
    private String timeout;

    @Nullable
    private Integer version;

    @Nullable
    private String versionType;

    @Nullable
    private String waitForActiveShards;

    @Nullable
    private Boolean requireAlias;

    public AddOrUpdateDocumentRequest(
            final String requestJson,
            final String target)
    {
        AppbaseUtil.notBlank(requestJson, "requestJson");
        AppbaseUtil.notBlank(target, "target");

        this.requestJson = requestJson;
        this.target = target;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public String getTarget() {
        return target;
    }

    @Nullable
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(@Nullable final String documentId) {
        this.documentId = documentId;
    }

    @Nullable
    public Integer getIfSeqNo() {
        return ifSeqNo;
    }

    public void setIfSeqNo(@Nullable final Integer ifSeqNo) {
        this.ifSeqNo = ifSeqNo;
    }

    @Nullable
    public Integer getIfPrimaryTerm() {
        return ifPrimaryTerm;
    }

    public void setIfPrimaryTerm(@Nullable final Integer ifPrimaryTerm) {
        this.ifPrimaryTerm = ifPrimaryTerm;
    }

    @Nullable
    public String getOpType() {
        return opType;
    }

    public void setOpType(@Nullable final String opType) {
        this.opType = opType;
    }

    @Nullable
    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(@Nullable final String pipeline) {
        this.pipeline = pipeline;
    }

    @Nullable
    public String getRefresh() {
        return refresh;
    }

    public void setRefresh(@Nullable final String refresh) {
        this.refresh = refresh;
    }

    @Nullable
    public String getRouting() {
        return routing;
    }

    public void setRouting(@Nullable final String routing) {
        this.routing = routing;
    }

    @Nullable
    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(@Nullable final String timeout) {
        this.timeout = timeout;
    }

    @Nullable
    public Integer getVersion() {
        return version;
    }

    public void setVersion(@Nullable final Integer version) {
        this.version = version;
    }

    @Nullable
    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(@Nullable final String versionType) {
        this.versionType = versionType;
    }

    @Nullable
    public String getWaitForActiveShards() {
        return waitForActiveShards;
    }

    public void setWaitForActiveShards(@Nullable final String waitForActiveShards) {
        this.waitForActiveShards = waitForActiveShards;
    }

    @Nullable
    public Boolean getRequireAlias() {
        return requireAlias;
    }

    public void setRequireAlias(@Nullable final Boolean requireAlias) {
        this.requireAlias = requireAlias;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AddOrUpdateDocumentRequest that = (AddOrUpdateDocumentRequest) o;
        return Objects.equals(requestJson, that.requestJson) &&
                Objects.equals(target, that.target) &&
                Objects.equals(documentId, that.documentId) &&
                Objects.equals(ifSeqNo, that.ifSeqNo) &&
                Objects.equals(ifPrimaryTerm, that.ifPrimaryTerm) &&
                Objects.equals(opType, that.opType) &&
                Objects.equals(pipeline, that.pipeline) &&
                Objects.equals(refresh, that.refresh) &&
                Objects.equals(routing, that.routing) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(version, that.version) &&
                Objects.equals(versionType, that.versionType) &&
                Objects.equals(
                        waitForActiveShards,
                        that.waitForActiveShards) &&
                Objects.equals(requireAlias, that.requireAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                requestJson,
                target,
                documentId,
                ifSeqNo,
                ifPrimaryTerm,
                opType,
                pipeline,
                refresh,
                routing,
                timeout,
                version,
                versionType,
                waitForActiveShards,
                requireAlias);
    }

    @Override
    public String toString() {
        return "AddDocumentRequest{" +
                "requestJson='" + requestJson + '\'' +
                ", target='" + target + '\'' +
                ", documentId='" + documentId + '\'' +
                ", ifSeqNo=" + ifSeqNo +
                ", ifPrimaryTerm=" + ifPrimaryTerm +
                ", opType='" + opType + '\'' +
                ", pipeline='" + pipeline + '\'' +
                ", refresh='" + refresh + '\'' +
                ", routing='" + routing + '\'' +
                ", timeout='" + timeout + '\'' +
                ", version=" + version +
                ", versionType='" + versionType + '\'' +
                ", waitForActiveShards='" + waitForActiveShards + '\'' +
                ", requireAlias=" + requireAlias +
                '}';
    }
}
