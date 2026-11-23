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

package com.viveworks.appbase.core.service.log.store.criteria;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.TimestampCriteria;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a log record criteria to search for logs.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = LogRecordCriteria.Builder.class)
public final class LogRecordCriteria {
    private final TimestampCriteria timestampCriteria;
    private final String threadName;
    private final LogLevelCriteria logLevelCriteria;
    private final String message;
    private final String formattedMessage;
    private final String loggerName;
    private final String markerName;
    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> mdcPropertyMap;
    private final String callerData;
    private final String throwableMessage;
    private final String throwableTypeName;
    private final String throwableStackTrace;
    private final String throwableCauseMessage;
    private final String throwableCauseTypeName;
    private final String throwableCauseStackTrace;

    private LogRecordCriteria(final Builder builder) {
        timestampCriteria = builder.timestampCriteria;
        threadName = builder.threadName;
        logLevelCriteria = builder.logLevelCriteria;
        message = builder.message;
        formattedMessage = builder.formattedMessage;
        loggerName = builder.loggerName;
        markerName = builder.markerName;
        mdcPropertyMap = builder.mdcPropertyMap;
        callerData = builder.callerData;
        throwableMessage = builder.throwableMessage;
        throwableTypeName = builder.throwableTypeName;
        throwableStackTrace = builder.throwableStackTrace;
        throwableCauseMessage = builder.throwableCauseMessage;
        throwableCauseTypeName = builder.throwableCauseTypeName;
        throwableCauseStackTrace = builder.throwableCauseStackTrace;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public TimestampCriteria getTimestampCriteria() {
        return timestampCriteria;
    }

    public String getThreadName() {
        return threadName;
    }

    public LogLevelCriteria getLogLevelCriteria() {
        return logLevelCriteria;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getMarkerName() {
        return markerName;
    }

    public Map<String, String> getMdcPropertyMap() {
        return mdcPropertyMap;
    }

    public String getCallerData() {
        return callerData;
    }

    public String getThrowableMessage() {
        return throwableMessage;
    }

    public String getThrowableTypeName() {
        return throwableTypeName;
    }

    public String getThrowableStackTrace() {
        return throwableStackTrace;
    }

    public String getThrowableCauseMessage() {
        return throwableCauseMessage;
    }

    public String getThrowableCauseTypeName() {
        return throwableCauseTypeName;
    }

    public String getThrowableCauseStackTrace() {
        return throwableCauseStackTrace;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LogRecordCriteria)) {
            return false;
        }

        final LogRecordCriteria that = (LogRecordCriteria) o;
        return Objects.equals(timestampCriteria, that.timestampCriteria) &&
                Objects.equals(threadName, that.threadName) &&
                Objects.equals(logLevelCriteria, that.logLevelCriteria) &&
                Objects.equals(message, that.message) &&
                Objects.equals(formattedMessage, that.formattedMessage) &&
                Objects.equals(loggerName, that.loggerName) &&
                Objects.equals(markerName, that.markerName) &&
                Objects.equals(mdcPropertyMap, that.mdcPropertyMap) &&
                Objects.equals(callerData, that.callerData) &&
                Objects.equals(throwableMessage, that.throwableMessage) &&
                Objects.equals(throwableTypeName, that.throwableTypeName) &&
                Objects.equals(throwableStackTrace, that.throwableStackTrace) &&
                Objects.equals(throwableCauseMessage, that.throwableCauseMessage) &&
                Objects.equals(throwableCauseTypeName, that.throwableCauseTypeName) &&
                Objects.equals(throwableCauseStackTrace, that.throwableCauseStackTrace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                timestampCriteria,
                threadName,
                logLevelCriteria,
                message,
                formattedMessage,
                loggerName,
                markerName,
                mdcPropertyMap,
                callerData,
                throwableMessage,
                throwableTypeName,
                throwableStackTrace,
                throwableCauseMessage,
                throwableCauseTypeName,
                throwableCauseStackTrace);
    }

    @Override
    public String toString() {
        return "LogRecordCriteria{" +
                "timestampCriteria=" + timestampCriteria +
                ", threadName='" + threadName + '\'' +
                ", logLevelCriteria=" + logLevelCriteria +
                ", message='" + message + '\'' +
                ", formattedMessage='" + formattedMessage + '\'' +
                ", loggerName='" + loggerName + '\'' +
                ", markerName='" + markerName + '\'' +
                ", mdcPropertyMap='" + mdcPropertyMap + '\'' +
                ", callerData='" + callerData + '\'' +
                ", throwableMessage='" + throwableMessage + '\'' +
                ", throwableTypeName='" + throwableTypeName + '\'' +
                ", throwableStackTrace='" + throwableStackTrace + '\'' +
                ", throwableCauseMessage='" + throwableCauseMessage + '\'' +
                ", throwableCauseTypeName='" + throwableCauseTypeName + '\'' +
                ", throwableCauseStackTrace='" + throwableCauseStackTrace + '\'' +
                '}';
    }

    /**
     * Builder for {@link LogRecordCriteria}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private TimestampCriteria timestampCriteria;
        private String threadName;
        private LogLevelCriteria logLevelCriteria;
        private String message;
        private String formattedMessage;
        private String loggerName;
        private String markerName;
        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> mdcPropertyMap;
        private String callerData;
        private String throwableMessage;
        private String throwableTypeName;
        private String throwableStackTrace;
        private String throwableCauseMessage;
        private String throwableCauseTypeName;
        private String throwableCauseStackTrace;

        private Builder() {
        }

        public Builder timestampCriteria(final TimestampCriteria timestampCriteria) {
            this.timestampCriteria = timestampCriteria;
            return this;
        }

        public Builder threadName(final String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder logLevelCriteria(final LogLevelCriteria logLevelCriteria) {
            this.logLevelCriteria = logLevelCriteria;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder formattedMessage(final String formattedMessage) {
            this.formattedMessage = formattedMessage;
            return this;
        }

        public Builder loggerName(final String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder markerName(final String markerName) {
            this.markerName = markerName;
            return this;
        }

        public Builder mdcPropertyMap(
                @JsonSerialize(keyAs = String.class, contentAs = String.class)
                @JsonDeserialize(keyAs = String.class, contentAs = String.class)
                final Map<String, String> mdcPropertyMap)
        {
            this.mdcPropertyMap = mdcPropertyMap;
            return this;
        }

        public Builder callerData(final String callerData) {
            this.callerData = callerData;
            return this;
        }

        public Builder throwableMessage(final String throwableMessage) {
            this.throwableMessage = throwableMessage;
            return this;
        }

        public Builder throwableTypeName(final String throwableTypeName) {
            this.throwableTypeName = throwableTypeName;
            return this;
        }

        public Builder throwableStackTrace(final String throwableStackTrace) {
            this.throwableStackTrace = throwableStackTrace;
            return this;
        }

        public Builder throwableCauseMessage(final String throwableCauseMessage) {
            this.throwableCauseMessage = throwableCauseMessage;
            return this;
        }

        public Builder throwableCauseTypeName(final String throwableCauseTypeName) {
            this.throwableCauseTypeName = throwableCauseTypeName;
            return this;
        }

        public Builder throwableCauseStackTrace(final String throwableCauseStackTrace) {
            this.throwableCauseStackTrace = throwableCauseStackTrace;
            return this;
        }

        public LogRecordCriteria build() {
            return new LogRecordCriteria(this);
        }
    }
    // CheckStyle: Builder end
}
