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

/**
 * This package contains client services for {@code JDBC} (Java Database Connectivity).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.v3.core.Handles;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * {@link Handles} configuration.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = HandlesConfiguration.Builder.class)
public final class HandlesConfiguration {
    private final boolean forceEndTransactions;

    private HandlesConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        forceEndTransactions = builder.forceEndTransactions;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isForceEndTransactions() {
        return forceEndTransactions;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HandlesConfiguration that = (HandlesConfiguration) o;
        return forceEndTransactions == that.forceEndTransactions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(forceEndTransactions);
    }

    @Override
    public String toString() {
        return "HandlesConfiguration{" +
                "forceEndTransactions=" + forceEndTransactions +
                '}';
    }

    /**
     * Builder for {@link HandlesConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_FORCE_END_TRANSACTIONS = true;

        private boolean forceEndTransactions = DEFAULT_FORCE_END_TRANSACTIONS;

        private Builder() {
        }

        public Builder forceEndTransactions(final boolean forceEndTransactions) {
            this.forceEndTransactions = forceEndTransactions;
            return this;
        }

        public HandlesConfiguration build() {
            return new HandlesConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
