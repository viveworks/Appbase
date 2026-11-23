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

package com.viveworks.appbase.core.service.log.store.lucene;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.log.store.LogStore;
import com.viveworks.appbase.core.service.log.store.LogStoreConfiguration;
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
 * Configuration for {@link LuceneLogStore}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@LuceneLogStoreConfiguration.Valid
@JsonDeserialize(builder = LuceneLogStoreConfiguration.Builder.class)
public final class LuceneLogStoreConfiguration implements LogStoreConfiguration {
    private final boolean enabled;

    private LuceneLogStoreConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        enabled = builder.enabled;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends LogStore> Class<S> getLogStoreType() {
        return (Class<S>) LuceneLogStore.class;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LuceneLogStoreConfiguration)) {
            return false;
        }

        final LuceneLogStoreConfiguration that = (LuceneLogStoreConfiguration) o;
        return enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled);
    }

    @Override
    public String toString() {
        return "LuceneLogStoreConfiguration{" +
                "enabled=" + enabled +
                '}';
    }

    /**
     * Builder for {@link LuceneLogStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings({"FCBL_FIELD_COULD_BE_LOCAL"})
    public static final class Builder {
        // R3 (internal work item 1073, spec §10): the Lucene log store does file I/O on the hot log path — off by default;
        // users who want indexed/searchable logs opt in explicitly.
        private boolean DEFAULT_ENABLED = false;

        private boolean enabled = DEFAULT_ENABLED;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public LuceneLogStoreConfiguration build() {
            return new LuceneLogStoreConfiguration(this);
        }
    }
    // CheckStyle: Builder end

    /**
     * JSR 303 validation constraint annotation for {@link LuceneLogStoreConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = LuceneLogStoreConfigurationValidator.class)
    @Documented
    public @interface Valid {
        String message() default "{" +
                "com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration.message" +
                "}";

        Class<?>[] groups() default { };

        Class<? extends Payload>[] payload() default { };
    }
}
