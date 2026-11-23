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

package com.viveworks.appbase.core.service.log.common;

import com.viveworks.appbase.core.service.log.filter.LogLevel;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.Marker;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents turbo log filter arguments in <code>logback</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class TurboLog {
    private final long timestamp;
    private final Logger logger;
    private final LogLevel logLevel;
    private final String formattedMessage;
    private final Object[] argumentArray;
    private final Throwable throwable;
    private final Marker marker;

    private TurboLog(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        timestamp = builder.timestamp;
        logger = builder.logger;
        logLevel = builder.logLevel;
        formattedMessage = builder.formattedMessage;
        argumentArray = builder.argumentArray;
        throwable = builder.throwable;
        marker = builder.marker;
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

    public Logger getLogger() {
        return logger;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public Object[] getArgumentArray() {
        return argumentArray;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Marker getMarker() {
        return marker;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TurboLog)) {
            return false;
        }

        final TurboLog turboLog = (TurboLog) o;
        return timestamp == turboLog.timestamp &&
                Objects.equals(logger, turboLog.logger) &&
                logLevel == turboLog.logLevel &&
                Objects.equals(formattedMessage, turboLog.formattedMessage) &&
                Arrays.equals(argumentArray, turboLog.argumentArray) &&
                Objects.equals(throwable, turboLog.throwable) &&
                Objects.equals(marker, turboLog.marker);
    }

    @Override
    @SuppressWarnings("MagicNumber")
    public int hashCode() {
        int result = Objects.hash(timestamp, logger, logLevel, formattedMessage, throwable, marker);
        result = 31 * result + Arrays.hashCode(argumentArray);
        return result;
    }

    @Override
    public String toString() {
        return "TurboLog{" +
                "timestamp=" + timestamp +
                ", logLevel=" + logLevel +
                ", formattedMessage='" + formattedMessage + '\'' +
                ", argumentArray=" + Arrays.toString(argumentArray) +
                ", throwable=" + throwable +
                ", marker=" + marker +
                '}';
    }

    /**
     * {@link TurboLog} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private long timestamp;
        private Logger logger;
        private LogLevel logLevel;
        private String formattedMessage;
        private Object[] argumentArray;
        private Throwable throwable;
        private Marker marker;

        private Builder() {
        }

        public Builder timestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder logger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder logLevel(final LogLevel logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder formattedMessage(final String formattedMessage) {
            this.formattedMessage = formattedMessage;
            return this;
        }

        public Builder argumentArray(final Object[] argumentArray) {
            this.argumentArray = argumentArray;
            return this;
        }

        public Builder throwable(final Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder marker(final Marker marker) {
            this.marker = marker;
            return this;
        }

        public TurboLog build() {
            return new TurboLog(this);
        }
    }
    // CheckStyle: Builder end
}
