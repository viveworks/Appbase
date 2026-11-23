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

import com.viveworks.appbase.client.service.elasticsearch.restclient.api.search.response.ErrorResponseBody;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DeleteDocumentResponse {
    @Nullable
    private final DeleteDocumentResponseBody deleteDocumentResponseBody;

    @Nullable
    private final ErrorResponseBody errorResponseBody;

    private final boolean error;

    public DeleteDocumentResponse() {
        this.deleteDocumentResponseBody = null;
        this.errorResponseBody = null;
        this.error = false;
    }

    public DeleteDocumentResponse(
            final DeleteDocumentResponseBody deleteDocumentResponseBody)
    {
        AppbaseUtil.notNull(deleteDocumentResponseBody, "deleteDocumentResponseBody");

        this.deleteDocumentResponseBody = deleteDocumentResponseBody;
        this.errorResponseBody = null;
        this.error = false;
    }

    public DeleteDocumentResponse(
            final ErrorResponseBody errorResponseBody)
    {
        AppbaseUtil.notNull(errorResponseBody, "errorResponseBody");

        this.error = true;
        this.errorResponseBody = errorResponseBody;
        this.deleteDocumentResponseBody = null;
    }

    public DeleteDocumentResponse(final boolean error) {
        this.error = error;
        this.errorResponseBody = null;
        this.deleteDocumentResponseBody = null;
    }

    @Nullable
    public DeleteDocumentResponseBody getDeleteDocumentResponseBody() {
        return deleteDocumentResponseBody;
    }

    @Nullable
    public ErrorResponseBody getErrorResponseBody() {
        return errorResponseBody;
    }

    public boolean isError() {
        return error;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DeleteDocumentResponse that = (DeleteDocumentResponse) o;
        return error == that.error &&
                Objects.equals(
                        deleteDocumentResponseBody, that.deleteDocumentResponseBody) &&
                Objects.equals(errorResponseBody, that.errorResponseBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                deleteDocumentResponseBody,
                errorResponseBody,
                error);
    }

    @Override
    public String toString() {
        return "DeleteDocumentResponse{" +
                "deleteDocumentResponseBody=" + deleteDocumentResponseBody +
                ", errorResponseBody=" + errorResponseBody +
                ", error=" + error +
                '}';
    }
}
