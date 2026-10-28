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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
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
import java.util.List;
import java.util.Objects;

/**
 * Default implemenation of {@link LogAccessorConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@DefaultLogAccessorConfiguration.Valid
@JsonDeserialize(builder = DefaultLogAccessorConfiguration.Builder.class)
public final class DefaultLogAccessorConfiguration implements LogAccessorConfiguration {
    private final boolean enabled;

    @JsonSerialize(contentAs = LogStoreConfiguration.class)
    @JsonDeserialize(contentAs = LogStoreConfiguration.class)
    private final List<LogStoreConfiguration> logStoreConfigurations;

    private DefaultLogAccessorConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        DefaultLogAccessorConfigurationValidator.validate(
                builder.enabled,
                builder.logStoreConfigurations);

        enabled = builder.enabled;
        logStoreConfigurations = builder.logStoreConfigurations;
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

    public List<LogStoreConfiguration> getLogStoreConfigurations() {
        return logStoreConfigurations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultLogAccessorConfiguration)) {
            return false;
        }

        final DefaultLogAccessorConfiguration that = (DefaultLogAccessorConfiguration) o;
        return enabled == that.enabled &&
                Objects.equals(logStoreConfigurations, that.logStoreConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, logStoreConfigurations);
    }

    @Override
    public String toString() {
        return "DefaultLogAccessorConfiguration{" +
                "enabled=" + enabled +
                ", logStoreConfigurations=" + logStoreConfigurations +
                '}';
    }

    /**
     * Builder for {@link DefaultLogAccessorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        private static final boolean DEFAULT_ENABLED = true;
        private static final List<LogStoreConfiguration> DEFAULT_LOG_STORE_CONFIGURATIONS =
                List.of(LuceneLogStoreConfiguration.builder().build());

        private boolean enabled = DEFAULT_ENABLED;

        @JsonSerialize(contentAs = LogStoreConfiguration.class)
        @JsonDeserialize(contentAs = LogStoreConfiguration.class)
        private List<LogStoreConfiguration> logStoreConfigurations = DEFAULT_LOG_STORE_CONFIGURATIONS;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder logStoreConfigurations(
                @JsonSerialize(contentAs = LogStoreConfiguration.class)
                @JsonDeserialize(contentAs = LogStoreConfiguration.class)
                final List<LogStoreConfiguration> logStoreConfigurations)
        {
            this.logStoreConfigurations = logStoreConfigurations;
            return this;
        }

        public DefaultLogAccessorConfiguration build() {
            return new DefaultLogAccessorConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link DefaultLogAccessorConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = DefaultLogAccessorConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.accessor.DefaultLogAccessorConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
