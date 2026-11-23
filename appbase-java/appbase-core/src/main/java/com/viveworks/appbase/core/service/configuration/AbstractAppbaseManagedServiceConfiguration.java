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

package com.viveworks.appbase.core.service.configuration;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * Abstract {@link AppbaseManagedServiceConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public abstract class AbstractAppbaseManagedServiceConfiguration implements AppbaseManagedServiceConfiguration {
    private final boolean autoStart;

    protected AbstractAppbaseManagedServiceConfiguration(final Builder<?> builder) {
        AppbaseUtil.notNull(builder, "builder");

        autoStart = builder.autoStart;
    }

    @Override
    public boolean isAutoStart() {
        return autoStart;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractAppbaseManagedServiceConfiguration that = (AbstractAppbaseManagedServiceConfiguration) o;
        return autoStart == that.autoStart;
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoStart);
    }

    @Override
    public String toString() {
        return "AbstractAppbaseManagedServiceConfiguration{" +
                "autoStart=" + autoStart +
                '}';
    }

    /**
     * Abstract {@link AppbaseManagedServiceConfiguration} builder.
     *
     * @param <T> {@link Builder} type
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public abstract static class Builder<T extends Builder<T>> {
        private static final boolean DEFAULT_AUTO_START = true;

        protected boolean autoStart = DEFAULT_AUTO_START;


        protected abstract T getThis();


        protected Builder() {
        }

        public boolean isAutoStart() {
            return autoStart;
        }

        public T autoStart(final boolean autoStart) {
            this.autoStart = autoStart;

            return getThis();
        }
    }
    // CheckStyle: Builder end
}
