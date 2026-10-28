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

package com.viveworks.appbase.client.core.pool.manager;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents max repair count for an {@link com.viveworks.appbase.client.core.AppbaseClientIntegrationService} represented
 * by {@link AppbaseClientKeyConfiguration}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = MaxRepairAttempsConfiguration.Builder.class)
public final class MaxRepairAttempsConfiguration {
    private final int count;
    private final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration;

    private MaxRepairAttempsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");
        AppbaseUtil.positiveOrZero(builder.count, "builder.count");
        AppbaseUtil.notNull(builder.appbaseClientKeyConfiguration, "builder.appbaseClientKeyConfiguration");

        count = builder.count;
        appbaseClientKeyConfiguration = builder.appbaseClientKeyConfiguration;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getCount() {
        return count;
    }

    public AppbaseClientKeyConfiguration<?> getAppbaseClientKeyConfiguration() {
        return appbaseClientKeyConfiguration;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MaxRepairAttempsConfiguration that = (MaxRepairAttempsConfiguration) o;
        return count == that.count &&
                Objects.equals(appbaseClientKeyConfiguration, that.appbaseClientKeyConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, appbaseClientKeyConfiguration);
    }

    @Override
    public String toString() {
        return "MaxRepairAttempsConfiguration{" +
                "count=" + count +
                ", appbaseClientKeyConfiguration=" + appbaseClientKeyConfiguration +
                '}';
    }

    /**
     * Builder for {@link MaxRepairAttempsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final int DEFAULT_COUNT = 3;

        private int count = DEFAULT_COUNT;
        private AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration;

        private Builder() {
        }

        public Builder count(final int count) {
            this.count = count;
            return this;
        }

        public Builder appbaseClientKeyConfiguration(final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration) {
            this.appbaseClientKeyConfiguration = appbaseClientKeyConfiguration;
            return this;
        }

        public MaxRepairAttempsConfiguration build() {
            return new MaxRepairAttempsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
