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

package com.viveworks.appbase.client.service.vitess.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link io.vitess.client.VTSession}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = VtSessionConfiguration.Builder.class)
public final class VtSessionConfiguration {
    private final ExecuteOptionsConfiguration executeOptionsConfiguration;
    private final Boolean autoCommit;

    private VtSessionConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.notNull(builder.executeOptionsConfiguration, "builder.executeOptionsConfiguration");

        executeOptionsConfiguration = builder.executeOptionsConfiguration;
        autoCommit = builder.autoCommit;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public ExecuteOptionsConfiguration getExecuteOptionsConfiguration() {
        return executeOptionsConfiguration;
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final VtSessionConfiguration that = (VtSessionConfiguration) o;
        return autoCommit == that.autoCommit &&
                Objects.equals(executeOptionsConfiguration, that.executeOptionsConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executeOptionsConfiguration, autoCommit);
    }

    @Override
    public String toString() {
        return "VtSessionConfiguration{" +
                "executeOptionsConfiguration=" + executeOptionsConfiguration +
                ", autocommit=" + autoCommit +
                '}';
    }


    /**
     * Builder for {@link VtSessionConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final ExecuteOptionsConfiguration DEFAULT_EXECUTE_OPTIONS_CONFIGURATION =
                ExecuteOptionsConfiguration.builder().build();
        private static final boolean DEFAULT_AUTO_COMMIT = true;

        private ExecuteOptionsConfiguration executeOptionsConfiguration = DEFAULT_EXECUTE_OPTIONS_CONFIGURATION;
        private Boolean autoCommit = DEFAULT_AUTO_COMMIT;


        private Builder() {
        }

        public Builder executeOptionsConfiguration(final ExecuteOptionsConfiguration executeOptionsConfiguration) {
            this.executeOptionsConfiguration = executeOptionsConfiguration;
            return this;
        }

        public Builder autoCommit(final Boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public VtSessionConfiguration build() {
            return new VtSessionConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
