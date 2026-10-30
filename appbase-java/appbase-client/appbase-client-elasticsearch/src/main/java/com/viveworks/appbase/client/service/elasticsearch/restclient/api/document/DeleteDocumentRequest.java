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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api.document;

import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DeleteDocumentRequest {
    private final String index;

    private final String id;

    @Nullable
    private Integer ifSeqNo;

    @Nullable
    private Integer ifPrimaryTerm;

    @Nullable
    private String refresh;

    @Nullable
    private String timeout;

    @Nullable
    private Integer version;

    @Nullable
    private String versionType;

    @Nullable
    private String waitForActiveShards;


    public DeleteDocumentRequest(final String index, final String id) {
        AppbaseUtil.notBlank(index, "index");
        AppbaseUtil.notBlank(id, "id");

        this.index = index;
        this.id = id;
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
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
    public String getRefresh() {
        return refresh;
    }

    public void setRefresh(@Nullable final String refresh) {
        this.refresh = refresh;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DeleteDocumentRequest that = (DeleteDocumentRequest) o;
        return Objects.equals(index, that.index) &&
                Objects.equals(id, that.id) &&
                Objects.equals(ifSeqNo, that.ifSeqNo) &&
                Objects.equals(ifPrimaryTerm, that.ifPrimaryTerm) &&
                Objects.equals(refresh, that.refresh) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(version, that.version) &&
                Objects.equals(versionType, that.versionType) &&
                Objects.equals(waitForActiveShards, that.waitForActiveShards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                index,
                id,
                ifSeqNo,
                ifPrimaryTerm,
                refresh,
                timeout,
                version,
                versionType,
                waitForActiveShards);
    }

    @Override
    public String toString() {
        return "DeleteDocumentRequest{" +
                "index='" + index + '\'' +
                ", id='" + id + '\'' +
                ", ifSeqNo=" + ifSeqNo +
                ", ifPrimaryTerm=" + ifPrimaryTerm +
                ", refresh='" + refresh + '\'' +
                ", timeout='" + timeout + '\'' +
                ", version=" + version +
                ", versionType='" + versionType + '\'' +
                ", waitForActiveShards='" + waitForActiveShards + '\'' +
                '}';
    }
}
