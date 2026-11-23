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

package com.viveworks.appbase.core.service.mediator;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import javax.annotation.concurrent.Immutable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * Configuration for {@link java.util.concurrent.ThreadFactory}. See
 * https://netty.io/wiki/thread-affinity.html for more information (including tuning kernel parameters).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@ThreadFactoryConfiguration.Valid
@JsonDeserialize(builder = ThreadFactoryConfiguration.Builder.class)
public final class ThreadFactoryConfiguration {
    private final String nameFormat;
    private final boolean daemon;
    private final boolean enableThreadAffinity;

    private ThreadFactoryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        ThreadFactoryConfigurationValidator.validate(builder.nameFormat);

        nameFormat = builder.nameFormat;
        daemon = builder.daemon;
        enableThreadAffinity = builder.enableThreadAffinity;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public boolean isEnableThreadAffinity() {
        return enableThreadAffinity;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ThreadFactoryConfiguration that = (ThreadFactoryConfiguration) o;
        return daemon == that.daemon &&
                enableThreadAffinity == that.enableThreadAffinity &&
                Objects.equals(nameFormat, that.nameFormat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameFormat, daemon, enableThreadAffinity);
    }

    @Override
    public String toString() {
        return "ThreadFactoryConfiguration{" +
                "nameFormat='" + nameFormat + '\'' +
                ", daemon=" + daemon +
                ", enableThreadAffinity=" + enableThreadAffinity +
                '}';
    }

    /**
     * Builder for {@link ThreadFactoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private String DEFAULT_NAME_FORMAT = "appbase-thread-%d";
        private boolean DEFAULT_DAEMON = false;
        private final boolean DEFAULT_ENABLE_THREAD_AFFINITY = true;

        private String nameFormat = DEFAULT_NAME_FORMAT;
        private boolean daemon = DEFAULT_DAEMON;
        private boolean enableThreadAffinity = DEFAULT_ENABLE_THREAD_AFFINITY;

        private Builder() {
        }

        public Builder nameFormat(final String nameFormat) {
            this.nameFormat = nameFormat;
            return this;
        }

        public Builder daemon(final boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder enableThreadAffinity(final boolean enableThreadAffinity) {
            this.enableThreadAffinity = enableThreadAffinity;
            return this;
        }

        public ThreadFactoryConfiguration build() {
            return new ThreadFactoryConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link ThreadFactoryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ThreadFactoryConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.mediator.ThreadFactoryConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
