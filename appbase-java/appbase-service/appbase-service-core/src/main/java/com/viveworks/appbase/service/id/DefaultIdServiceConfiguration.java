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
package com.viveworks.appbase.service.id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DefaultIdServiceConfiguration.Builder.class)
public class DefaultIdServiceConfiguration implements AppbaseConfiguration {
    private final int randomStringMinimumCodePoint;

    private final int randomStringMaximumCodePoint;

    private DefaultIdServiceConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        randomStringMinimumCodePoint = builder.randomStringMinimumCodePoint;
        randomStringMaximumCodePoint = builder.randomStringMaximumCodePoint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getRandomStringMinimumCodePoint() {
        return randomStringMinimumCodePoint;
    }

    public int getRandomStringMaximumCodePoint() {
        return randomStringMaximumCodePoint;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultIdServiceConfiguration that = (DefaultIdServiceConfiguration) o;
        return randomStringMinimumCodePoint == that.randomStringMinimumCodePoint &&
                randomStringMaximumCodePoint == that.randomStringMaximumCodePoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomStringMinimumCodePoint, randomStringMaximumCodePoint);
    }

    @Override
    public String toString() {
        return "DefaultIdServiceConfiguration{" +
                "randomStringMinimumCodePoint=" + randomStringMinimumCodePoint +
                ", randomStringMaximumCodePoint=" + randomStringMaximumCodePoint +
                '}';
    }

    /**
     * Builder for {@link DefaultIdServiceConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final int DEFAULT_RANDOM_STRING_MINIMUM_CODE_POINT = 'a';
        private static final int DEFAULT_RANDOM_STRING_MAXIMUM_CODE_POINT = 'z';

        private int randomStringMinimumCodePoint = DEFAULT_RANDOM_STRING_MINIMUM_CODE_POINT;
        private int randomStringMaximumCodePoint = DEFAULT_RANDOM_STRING_MAXIMUM_CODE_POINT;

        private Builder() {
        }

        public Builder randomStringMinimumCodePoint(final int randomStringMinimumCodePoint) {
            this.randomStringMinimumCodePoint = randomStringMinimumCodePoint;
            return this;
        }

        public Builder randomStringMaximumCodePoint(final int randomStringMaximumCodePoint) {
            this.randomStringMaximumCodePoint = randomStringMaximumCodePoint;
            return this;
        }

        public DefaultIdServiceConfiguration build() {
            return new DefaultIdServiceConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
