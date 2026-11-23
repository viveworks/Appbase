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

package com.viveworks.appbase.core.service.log.accessor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a log record in a {@link LogStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = LogRecord.Builder.class)
public final class LogRecord {
    private final long timestamp;

    private final String threadName;

    private final LogLevel logLevel;

    private final String message;

    private final String formattedMessage;

    private final String loggerName;

    private final String markerName;

    @JsonSerialize(keyAs = String.class, contentAs = String.class)
    @JsonDeserialize(keyAs = String.class, contentAs = String.class)
    private final Map<String, String> mdcPropertyMap;

    private final String callerData;

    private final ThrowableRecord throwableRecord;

    private LogRecord(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        timestamp = builder.timestamp;
        threadName = builder.threadName;
        logLevel = builder.logLevel;
        message = builder.message;
        formattedMessage = builder.formattedMessage;
        loggerName = builder.loggerName;
        markerName = builder.markerName;
        mdcPropertyMap = builder.mdcPropertyMap;
        callerData = builder.callerData;
        throwableRecord = builder.throwableRecord;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThreadName() {
        return threadName;
    }

    public LogLevel getLogLevel() {
        return logLevel;
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

    public ThrowableRecord getThrowableRecord() {
        return throwableRecord;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LogRecord)) {
            return false;
        }

        final LogRecord logRecord = (LogRecord) o;
        return timestamp == logRecord.timestamp &&
                Objects.equals(threadName, logRecord.threadName) &&
                logLevel == logRecord.logLevel &&
                Objects.equals(message, logRecord.message) &&
                Objects.equals(formattedMessage, logRecord.formattedMessage) &&
                Objects.equals(loggerName, logRecord.loggerName) &&
                Objects.equals(markerName, logRecord.markerName) &&
                Objects.equals(mdcPropertyMap, logRecord.mdcPropertyMap) &&
                Objects.equals(callerData, logRecord.callerData) &&
                Objects.equals(throwableRecord, logRecord.throwableRecord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                timestamp,
                threadName,
                logLevel,
                message,
                formattedMessage,
                loggerName,
                markerName,
                mdcPropertyMap,
                callerData,
                throwableRecord);
    }

    @Override
    public String toString() {
        return "LogRecord{" +
                "timestamp=" + timestamp +
                ", threadName='" + threadName + '\'' +
                ", logLevel=" + logLevel +
                ", message='" + message + '\'' +
                ", formattedMessage='" + formattedMessage + '\'' +
                ", loggerName='" + loggerName + '\'' +
                ", markerName='" + markerName + '\'' +
                ", mdcPropertyMap=" + mdcPropertyMap +
                ", callerData='" + callerData + '\'' +
                ", throwableRecord=" + throwableRecord +
                '}';
    }

    /**
     * Builder for {@link LogRecord}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private long timestamp;

        private String threadName;

        private LogLevel logLevel;

        private String message;

        private String formattedMessage;

        private String loggerName;

        private String markerName;

        @JsonSerialize(keyAs = String.class, contentAs = String.class)
        @JsonDeserialize(keyAs = String.class, contentAs = String.class)
        private Map<String, String> mdcPropertyMap;

        private String callerData;

        private ThrowableRecord throwableRecord;

        private Builder() {
        }

        public Builder timestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder threadName(final String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder logLevel(final LogLevel logLevel) {
            this.logLevel = logLevel;
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

        public Builder throwableRecord(final ThrowableRecord throwableRecord) {
            this.throwableRecord = throwableRecord;
            return this;
        }

        public LogRecord build() {
            return new LogRecord(this);
        }
    }
    // CheckStyle: Builder end
}
