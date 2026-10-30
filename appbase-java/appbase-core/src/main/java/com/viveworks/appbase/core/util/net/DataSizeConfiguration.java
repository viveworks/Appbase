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

package com.viveworks.appbase.core.util.net;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.core.util.AppbaseUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Represents a size.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DataSizeConfiguration.Builder.class)
public final class DataSizeConfiguration {
    private final boolean enabled;
    private final int dataSize;
    private final Unit unit;

    private DataSizeConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.enabled) {
            AppbaseUtil.positiveOrZero(builder.dataSize, "builder.dataSize");
            AppbaseUtil.notNull(builder.unit, "builder.unit");
        }

        enabled = builder.enabled;
        dataSize = builder.dataSize;
        unit = builder.unit;
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

    public int getDataSize() {
        return dataSize;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DataSizeConfiguration that = (DataSizeConfiguration) o;
        return enabled == that.enabled &&
                dataSize == that.dataSize &&
                unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, dataSize, unit);
    }

    @Override
    public String toString() {
        return "DataSizeConfiguration{" +
                "enabled=" + enabled +
                ", dataSize=" + dataSize +
                ", unit=" + unit +
                '}';
    }

    /**
     * Represents data size unit.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public enum Unit {
        BYTES(1),

        KILOBYTES(1024),

        MEGABYTES(KILOBYTES.bytes * 1024),

        GIGABYTES(MEGABYTES.bytes * 1024),

        TERABYTES(GIGABYTES.bytes * 1024),

        PETABYTES(TERABYTES.bytes * 1024),

        EXABYTES(PETABYTES.bytes * 1024);

        private final long bytes;

        Unit(final long bytes) {
            this.bytes = bytes;
        }

        public long toBytes(final long value) {
            return value * bytes;
        }
    }

    /**
     * Builder for {@link DataSizeConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private boolean DEFAULT_ENABLED = true;
        private int DEFAULT_DATA_SIZE = 512;
        private Unit DEFAULT_UNIT = Unit.MEGABYTES;

        private boolean enabled = DEFAULT_ENABLED;
        private int dataSize = DEFAULT_DATA_SIZE;
        private Unit unit = DEFAULT_UNIT;

        private Builder() {
        }

        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder dataSize(final int dataSize) {
            this.dataSize = dataSize;
            return this;
        }

        public Builder unit(final Unit unit) {
            this.unit = unit;
            return this;
        }

        public DataSizeConfiguration build() {
            return new DataSizeConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
