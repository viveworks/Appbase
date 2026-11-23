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

package com.viveworks.appbase.client.service.vitess.configuration;

import io.vitess.proto.Query;

/**
 * Serialization / De-serialization friendly wrapper for {@link io.vitess.proto.Query.ExecuteOptions.TransactionIsolation}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum VitessTransactionIsolation {
    DEFAULT(Query.ExecuteOptions.TransactionIsolation.DEFAULT),

    REPEATABLE_READ(Query.ExecuteOptions.TransactionIsolation.REPEATABLE_READ),

    READ_COMMITTED(Query.ExecuteOptions.TransactionIsolation.READ_COMMITTED),

    READ_UNCOMMITTED(Query.ExecuteOptions.TransactionIsolation.READ_UNCOMMITTED),

    SERIALIZABLE(Query.ExecuteOptions.TransactionIsolation.SERIALIZABLE),

    CONSISTENT_SNAPSHOT_READ_ONLY(Query.ExecuteOptions.TransactionIsolation.CONSISTENT_SNAPSHOT_READ_ONLY),

    AUTOCOMMIT(Query.ExecuteOptions.TransactionIsolation.AUTOCOMMIT),

    UNRECOGNIZED(Query.ExecuteOptions.TransactionIsolation.UNRECOGNIZED);


    private final Query.ExecuteOptions.TransactionIsolation transactionIsolation;

    VitessTransactionIsolation(final Query.ExecuteOptions.TransactionIsolation transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public Query.ExecuteOptions.TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }
}
