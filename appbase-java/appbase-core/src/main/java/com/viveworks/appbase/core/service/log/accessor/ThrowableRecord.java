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

package com.viveworks.appbase.core.service.log.accessor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a throwable record.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ThrowableRecord.Builder.class)
public final class ThrowableRecord {
    private final String message;
    private final String className;
    private final String stackTrace;
    private final ThrowableRecord cause;

    private ThrowableRecord(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        message = builder.message;
        className = builder.className;
        stackTrace = builder.stackTrace;
        cause = builder.cause;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getMessage() {
        return message;
    }

    public String getClassName() {
        return className;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public ThrowableRecord getCause() {
        return cause;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ThrowableRecord)) {
            return false;
        }

        final ThrowableRecord that = (ThrowableRecord) o;
        return Objects.equals(message, that.message) &&
                Objects.equals(className, that.className) &&
                Objects.equals(stackTrace, that.stackTrace) &&
                Objects.equals(cause, that.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, className, stackTrace, cause);
    }

    @Override
    public String toString() {
        return "ThrowableRecord{" +
                "message='" + message + '\'' +
                ", className='" + className + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                ", cause=" + cause +
                '}';
    }

    /**
     * Builder for {@link ThrowableRecord}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String message;
        private String className;
        private String stackTrace;
        private ThrowableRecord cause;

        private Builder() {
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder className(final String className) {
            this.className = className;
            return this;
        }

        public Builder stackTrace(final String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder cause(final ThrowableRecord cause) {
            this.cause = cause;
            return this;
        }

        public ThrowableRecord build() {
            return new ThrowableRecord(this);
        }
    }
    // CheckStyle: Builder end
}
