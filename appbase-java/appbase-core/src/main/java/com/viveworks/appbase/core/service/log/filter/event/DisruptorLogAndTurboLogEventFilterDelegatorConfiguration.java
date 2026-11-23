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

package com.viveworks.appbase.core.service.log.filter.event;

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
 * Configuration for {@link DisruptorLogAndTurboLogEventFilterDelegator}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.Valid
@JsonDeserialize(builder = DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.Builder.class)
public final class DisruptorLogAndTurboLogEventFilterDelegatorConfiguration {
    private final boolean enabled;
    private final boolean acceptLogAfterTurboLogEventListeners;
    private final boolean acceptLogAfterLogEventListeners;

    private DisruptorLogAndTurboLogEventFilterDelegatorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enabled = builder.enabled;
        acceptLogAfterTurboLogEventListeners = builder.acceptLogAfterTurboLogEventListeners;
        acceptLogAfterLogEventListeners = builder.acceptLogAfterLogEventListeners;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAcceptLogAfterTurboLogEventListeners() {
        return acceptLogAfterTurboLogEventListeners;
    }

    public boolean isAcceptLogAfterLogEventListeners() {
        return acceptLogAfterLogEventListeners;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DisruptorLogAndTurboLogEventFilterDelegatorConfiguration that =
                (DisruptorLogAndTurboLogEventFilterDelegatorConfiguration) o;
        return enabled == that.enabled &&
                acceptLogAfterTurboLogEventListeners == that.acceptLogAfterTurboLogEventListeners &&
                acceptLogAfterLogEventListeners == that.acceptLogAfterLogEventListeners;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, acceptLogAfterTurboLogEventListeners, acceptLogAfterLogEventListeners);
    }

    @Override
    public String toString() {
        return "DisruptorLogAndTurboLogEventFilterDelegatorConfiguration{" +
                "enabled=" + enabled +
                ", acceptLogAfterTurboLogEventListeners=" + acceptLogAfterTurboLogEventListeners +
                ", acceptLogAfterLogEventListeners=" + acceptLogAfterLogEventListeners +
                '}';
    }

    /**
     * {@link DisruptorLogAndTurboLogEventFilterDelegatorConfiguration} builder.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final boolean DEFAULT_ACCEPT_LOG_AFTER_TURBO_LOG_EVENT_LISTENERS = true;
        private static final boolean DEFAULT_ACCEPT_LOG_AFTER_LOG_EVENT_LISTENERS = true;

        private boolean enabled = DEFAULT_ENABLED;
        private boolean acceptLogAfterTurboLogEventListeners = DEFAULT_ACCEPT_LOG_AFTER_TURBO_LOG_EVENT_LISTENERS;
        private boolean acceptLogAfterLogEventListeners = DEFAULT_ACCEPT_LOG_AFTER_LOG_EVENT_LISTENERS;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder acceptLogAfterTurboLogEventListeners(final boolean acceptLogAfterTurboLogEventListeners) {
            this.acceptLogAfterTurboLogEventListeners = acceptLogAfterTurboLogEventListeners;
            return this;
        }

        public Builder acceptLogAfterLogEventListeners(final boolean acceptLogAfterLogEventListeners) {
            this.acceptLogAfterLogEventListeners = acceptLogAfterLogEventListeners;
            return this;
        }

        public DisruptorLogAndTurboLogEventFilterDelegatorConfiguration build() {
            return new DisruptorLogAndTurboLogEventFilterDelegatorConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DisruptorLogAndTurboLogEventFilterDelegatorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DisruptorLogAndTurboLogEventFilterDelegatorConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.filter.event." +
                "DisruptorLogAndTurboLogEventFilterDelegatorConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
