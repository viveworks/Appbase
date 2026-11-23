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

package com.viveworks.appbase.client.service.druid.sql.client.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.viveworks.appbase.client.service.druid.sql.client.DruidResultSet;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration used during query execution with {@link java.sql.PreparedStatement} and {@link java.sql.Statement}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = DruidSqlQueryConfiguration.Builder.class)
public final class DruidSqlQueryConfiguration {
    private final Integer fetchSize;
    private final Long largeMaxRows;
    private final Integer maxRows;
    private final TimeoutConfiguration queryTimeoutConfiguration;
    private final DruidResultSet.FetchDirection fetchDirection;
    private final DruidResultSet.ResultSetType resultSetType;
    private final DruidResultSet.ConcurrencyType concurrencyType;
    private final DruidResultSet.CursorHoldability cursorHoldability;


    private DruidSqlQueryConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        if (builder.fetchSize != null) {
            AppbaseUtil.positiveOrZero(builder.fetchSize, "builder.fetchSize");
        }
        if (builder.largeMaxRows != null) {
            AppbaseUtil.positiveOrZero(builder.largeMaxRows, "builder.largeMaxRows");
        }
        if (builder.maxRows != null) {
            AppbaseUtil.positiveOrZero(builder.maxRows, "builder.maxRows");
        }

        fetchSize = builder.fetchSize;
        largeMaxRows = builder.largeMaxRows;
        maxRows = builder.maxRows;
        queryTimeoutConfiguration = builder.queryTimeoutConfiguration;
        fetchDirection = builder.fetchDirection;
        resultSetType = builder.resultSetType;
        concurrencyType = builder.concurrencyType;
        cursorHoldability = builder.cursorHoldability;
    }


    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public Long getLargeMaxRows() {
        return largeMaxRows;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public TimeoutConfiguration getQueryTimeoutConfiguration() {
        return queryTimeoutConfiguration;
    }

    public DruidResultSet.FetchDirection getFetchDirection() {
        return fetchDirection;
    }

    public DruidResultSet.ResultSetType getResultSetType() {
        return resultSetType;
    }

    public DruidResultSet.ConcurrencyType getConcurrencyType() {
        return concurrencyType;
    }

    public DruidResultSet.CursorHoldability getCursorHoldability() {
        return cursorHoldability;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DruidSqlQueryConfiguration that = (DruidSqlQueryConfiguration) o;
        return Objects.equals(fetchSize, that.fetchSize) &&
                Objects.equals(largeMaxRows, that.largeMaxRows) &&
                Objects.equals(maxRows, that.maxRows) &&
                Objects.equals(queryTimeoutConfiguration, that.queryTimeoutConfiguration) &&
                fetchDirection == that.fetchDirection &&
                resultSetType == that.resultSetType &&
                concurrencyType == that.concurrencyType &&
                cursorHoldability == that.cursorHoldability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                fetchSize,
                largeMaxRows,
                maxRows,
                queryTimeoutConfiguration,
                fetchDirection,
                resultSetType,
                concurrencyType,
                cursorHoldability);
    }

    @Override
    public String toString() {
        return "DruidSqlQueryConfiguration{" +
                "fetchSize=" + fetchSize +
                ", largeMaxRows=" + largeMaxRows +
                ", maxRows=" + maxRows +
                ", queryTimeoutConfiguration=" + queryTimeoutConfiguration +
                ", fetchDirection=" + fetchDirection +
                ", resultSetType=" + resultSetType +
                ", concurrencyType=" + concurrencyType +
                ", cursorHoldability=" + cursorHoldability +
                '}';
    }


    /**
     * Builder for {@link DruidSqlQueryConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final int DEFAULT_FETCH_SIZE = 100;
        private static final long DEFAULT_LARGE_MAX_ROWS = 0;
        private static final int DEFAULT_MAX_ROWS = 0;
        private static final TimeoutConfiguration DEFAULT_QUERY_TIMEOUT_CONFIGURATION = TimeoutConfiguration
                .builder()
                .enabled(true)
                .timeout(5)
                .timeoutTimeUnit(TimeUnit.SECONDS)
                .build();
        private static final DruidResultSet.FetchDirection DEFAULT_FETCH_DIRECTION =
                DruidResultSet.FetchDirection.FETCH_FORWARD;
        private static final DruidResultSet.ResultSetType DEFAULT_RESULT_SET_TYPE =
                DruidResultSet.ResultSetType.FORWARD_ONLY;
        private static final DruidResultSet.ConcurrencyType DEFAULT_CONCURRENCY_TYPE =
                DruidResultSet.ConcurrencyType.READ_ONLY;
        private static final DruidResultSet.CursorHoldability DEFAULT_CURSOR_HOLDABILITY =
                DruidResultSet.CursorHoldability.CLOSE_CURSORS_AT_COMMIT;


        private Integer fetchSize = DEFAULT_FETCH_SIZE;
        private Long largeMaxRows = DEFAULT_LARGE_MAX_ROWS;
        private Integer maxRows = DEFAULT_MAX_ROWS;
        private TimeoutConfiguration queryTimeoutConfiguration = DEFAULT_QUERY_TIMEOUT_CONFIGURATION;
        private DruidResultSet.FetchDirection fetchDirection = DEFAULT_FETCH_DIRECTION;
        private DruidResultSet.ResultSetType resultSetType = DEFAULT_RESULT_SET_TYPE;
        private DruidResultSet.ConcurrencyType concurrencyType = DEFAULT_CONCURRENCY_TYPE;
        private DruidResultSet.CursorHoldability cursorHoldability = DEFAULT_CURSOR_HOLDABILITY;


        private Builder() {
        }

        public Builder fetchSize(final Integer fetchSize) {
            this.fetchSize = fetchSize;
            return this;
        }

        public Builder largeMaxRows(final Long largeMaxRows) {
            this.largeMaxRows = largeMaxRows;
            return this;
        }

        public Builder maxRows(final Integer maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public Builder queryTimeoutConfiguration(final TimeoutConfiguration queryTimeoutConfiguration) {
            this.queryTimeoutConfiguration = queryTimeoutConfiguration;
            return this;
        }

        public Builder fetchDirection(final DruidResultSet.FetchDirection fetchDirection) {
            this.fetchDirection = fetchDirection;
            return this;
        }

        public Builder resultSetType(final DruidResultSet.ResultSetType resultSetType) {
            this.resultSetType = resultSetType;
            return this;
        }

        public Builder concurrencyType(final DruidResultSet.ConcurrencyType concurrencyType) {
            this.concurrencyType = concurrencyType;
            return this;
        }

        public Builder cursorHoldability(final DruidResultSet.CursorHoldability cursorHoldability) {
            this.cursorHoldability = cursorHoldability;
            return this;
        }

        public DruidSqlQueryConfiguration build() {
            return new DruidSqlQueryConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
