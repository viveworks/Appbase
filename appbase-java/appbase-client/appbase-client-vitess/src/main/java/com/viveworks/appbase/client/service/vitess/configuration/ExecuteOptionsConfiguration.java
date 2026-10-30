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
import io.vitess.proto.Query;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Configuration for {@link Query.ExecuteOptions}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
@JsonDeserialize(builder = ExecuteOptionsConfiguration.Builder.class)
public final class ExecuteOptionsConfiguration {
    private final Boolean clientFoundRows;
    private final Boolean skipQueryPlanCache;
    private final Long sqlSelectLimit;
    private final VitessTransactionIsolation vitessTransactionIsolation;
    private final VitessWorkload vitessWorkload;

    private ExecuteOptionsConfiguration(final Builder builder) {
        AppbaseUtil.notNull(builder, "builder");

        clientFoundRows = builder.clientFoundRows;
        skipQueryPlanCache = builder.skipQueryPlanCache;
        sqlSelectLimit = builder.sqlSelectLimit;
        vitessTransactionIsolation = builder.vitessTransactionIsolation;
        vitessWorkload = builder.vitessWorkload;
    }

    /**
     * Get the builder used to build this object.
     *
     * @return Builder for this object
     */
    public static Builder builder() {
        return new Builder();
    }

    public Boolean getClientFoundRows() {
        return clientFoundRows;
    }

    public Boolean getSkipQueryPlanCache() {
        return skipQueryPlanCache;
    }

    public Long getSqlSelectLimit() {
        return sqlSelectLimit;
    }

    public VitessTransactionIsolation getVitessTransactionIsolation() {
        return vitessTransactionIsolation;
    }

    public VitessWorkload getVitessWorkload() {
        return vitessWorkload;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ExecuteOptionsConfiguration that = (ExecuteOptionsConfiguration) o;
        return Objects.equals(clientFoundRows, that.clientFoundRows) &&
                Objects.equals(skipQueryPlanCache, that.skipQueryPlanCache) &&
                Objects.equals(sqlSelectLimit, that.sqlSelectLimit) &&
                vitessTransactionIsolation == that.vitessTransactionIsolation &&
                vitessWorkload == that.vitessWorkload;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientFoundRows, skipQueryPlanCache, sqlSelectLimit, vitessTransactionIsolation, vitessWorkload);
    }

    @Override
    public String toString() {
        return "ExecuteOptionsConfiguration{" +
                "clientFoundRows=" + clientFoundRows +
                ", skipQueryPlanCache=" + skipQueryPlanCache +
                ", sqlSelectLimit=" + sqlSelectLimit +
                ", vitessTransactionIsolation=" + vitessTransactionIsolation +
                ", vitessWorkload=" + vitessWorkload +
                '}';
    }


    /**
     * Builder for {@link ExecuteOptionsConfiguration}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    // CheckStyle: Builder start
    @JsonPOJOBuilder(withPrefix = "")
    @SuppressFBWarnings("FCBL_FIELD_COULD_BE_LOCAL")
    public static final class Builder {
        private static final boolean DEFAULT_CLIENT_FOUND_ROWS = false;
        private static final boolean DEFAULT_SKIP_QUERY_PLAN_CACHE = false;
        private static final long DEFAULT_SQL_SELECT_LIMIT = 1000L;
        private static final VitessTransactionIsolation DEFAULT_VITESS_TRANSACTION_ISOLATION =
                VitessTransactionIsolation.REPEATABLE_READ;
        private static final VitessWorkload DEFAULT_VITESS_WORKLOAD =
                VitessWorkload.UNSPECIFIED;


        private Boolean clientFoundRows = DEFAULT_CLIENT_FOUND_ROWS;
        private Boolean skipQueryPlanCache = DEFAULT_SKIP_QUERY_PLAN_CACHE;
        private Long sqlSelectLimit = DEFAULT_SQL_SELECT_LIMIT;
        private VitessTransactionIsolation vitessTransactionIsolation = DEFAULT_VITESS_TRANSACTION_ISOLATION;
        private VitessWorkload vitessWorkload = DEFAULT_VITESS_WORKLOAD;


        private Builder() {
        }

        public Builder clientFoundRows(final Boolean clientFoundRows) {
            this.clientFoundRows = clientFoundRows;
            return this;
        }

        public Builder skipQueryPlanCache(final Boolean skipQueryPlanCache) {
            this.skipQueryPlanCache = skipQueryPlanCache;
            return this;
        }

        public Builder sqlSelectLimit(final Long sqlSelectLimit) {
            this.sqlSelectLimit = sqlSelectLimit;
            return this;
        }

        public Builder vitessTransactionIsolation(final VitessTransactionIsolation vitessTransactionIsolation) {
            this.vitessTransactionIsolation = vitessTransactionIsolation;
            return this;
        }

        public Builder vitessWorkload(final VitessWorkload vitessWorkload) {
            this.vitessWorkload = vitessWorkload;
            return this;
        }

        public ExecuteOptionsConfiguration build() {
            return new ExecuteOptionsConfiguration(this);
        }
    }
    // CheckStyle: Builder end
}
