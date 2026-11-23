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

package com.viveworks.appbase.core.service.log.filter.burst;

import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.service.log.filter.duplicate.DefaultLogDeduplicator;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Marker;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a log entry used by {@link DefaultLogDeduplicator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class LogEntry {
    private final String threadGroupName;
    private final String threadName;
    private final String formattedMessage;
    private final Marker marker;
    private final LogLevel logLevel;
    private final Object[] params;
    private final Throwable throwable;

    private LogEntry(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.logLevel, "builder.logLevel");

        threadGroupName = builder.threadGroupName;
        threadName = builder.threadName;
        formattedMessage = builder.formattedMessage;
        marker = builder.marker;
        logLevel = builder.logLevel;
        params = builder.params;
        throwable = builder.throwable;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getThreadGroupName() {
        return threadGroupName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public Marker getMarker() {
        return marker;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public Object[] getParams() {
        return params;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LogEntry)) {
            return false;
        }

        final LogEntry logEntry = (LogEntry) o;
        return Objects.equals(threadGroupName, logEntry.threadGroupName) &&
                Objects.equals(threadName, logEntry.threadName) &&
                Objects.equals(formattedMessage, logEntry.formattedMessage) &&
                Objects.equals(marker, logEntry.marker) &&
                logLevel == logEntry.logLevel &&
                Arrays.equals(params, logEntry.params) &&
                Objects.equals(throwable, logEntry.throwable);
    }

    @Override
    @SuppressWarnings("MagicNumber")
    public int hashCode() {
        int result = Objects.hash(threadGroupName, threadName, formattedMessage, marker, logLevel, throwable);
        result = 31 * result + Arrays.hashCode(params);
        return result;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "threadGroupName='" + threadGroupName + '\'' +
                ", threadName='" + threadName + '\'' +
                ", formattedMessage='" + formattedMessage + '\'' +
                ", marker=" + marker +
                ", logLevel=" + logLevel +
                ", params=" + Arrays.toString(params) +
                ", throwable=" + throwable +
                '}';
    }

    /**
     * {@link LogEntry} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String threadGroupName;
        private String threadName;
        private String formattedMessage;
        private Marker marker;
        private LogLevel logLevel;
        private Object[] params;
        private Throwable throwable;

        private Builder() {
        }

        public Builder threadGroupName(final String threadGroupName) {
            this.threadGroupName = threadGroupName;
            return this;
        }

        public Builder threadName(final String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder formattedMessage(final String formattedMessage) {
            this.formattedMessage = formattedMessage;
            return this;
        }

        public Builder marker(final Marker marker) {
            this.marker = marker;
            return this;
        }

        public Builder logLevel(final LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder params(final Object[] params) {
            this.params = params;
            return this;
        }

        public Builder throwable(final Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
    // CheckStyle: Builder end
}
