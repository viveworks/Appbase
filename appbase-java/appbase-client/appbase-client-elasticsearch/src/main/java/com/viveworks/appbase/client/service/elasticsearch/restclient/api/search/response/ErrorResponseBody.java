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
import com.fasterxml.jackson.databind.JsonNode;
import com.viveworks.appbase.core.util.jackson.JsonUtil;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseBody {
    @JsonProperty("root_cause")
    private ErrorCause rootCause;

    @Nullable
    private String type;

    @Nullable
    private String reason;

    @Nullable
    @JsonProperty("caused_by")
    private ErrorCause causedBy;

    @Nullable
    private Integer status;

    /**
     * Elasticsearch nests the error under an {@code error} object, whose {@code root_cause} is an array:
     * {@code {"error":{"root_cause":[{"type":"index_not_found_exception","reason":"no such index [x]"}],
     * "type":"index_not_found_exception","reason":"no such index [x]"},"status":404}}. Read that envelope
     * so the type and reason reach the caller; the flat fields above remain for the unwrapped form.
     *
     * @param error the {@code error} node of the response body
     */
    @JsonProperty("error")
    private void setErrorEnvelope(@Nullable final JsonNode error) {
        if (error == null || !error.isObject()) {
            return;
        }

        final JsonNode envelopeType = error.get("type");
        if (envelopeType != null && envelopeType.isTextual()) {
            this.type = envelopeType.asText();
        }

        final JsonNode envelopeReason = error.get("reason");
        if (envelopeReason != null && envelopeReason.isTextual()) {
            this.reason = envelopeReason.asText();
        }

        final JsonNode causes = error.get("root_cause");
        if (causes != null && causes.isArray() && causes.size() > 0) {
            this.rootCause = JsonUtil.getJsonMapper().convertValue(causes.get(0), ErrorCause.class);
        }
    }

    public ErrorCause getRootCause() {
        return rootCause;
    }

    public void setRootCause(final ErrorCause rootCause) {
        this.rootCause = rootCause;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public ErrorCause getCausedBy() {
        return causedBy;
    }

    public void setCausedBy(final ErrorCause causedBy) {
        this.causedBy = causedBy;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ErrorResponseBody that = (ErrorResponseBody) o;
        return Objects.equals(rootCause, that.rootCause) &&
                Objects.equals(type, that.type) &&
                Objects.equals(reason, that.reason) &&
                Objects.equals(causedBy, that.causedBy) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootCause, type, reason, causedBy, status);
    }

    @Override
    public String toString() {
        return "ErrorResponseBody{" +
                "rootCause=" + rootCause +
                ", type='" + type + '\'' +
                ", reason='" + reason + '\'' +
                ", causedBy=" + causedBy +
                ", status=" + status +
                '}';
    }
}
