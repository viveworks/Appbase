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

package com.viveworks.appbase.client.service.jdbc.configuration;

import java.sql.Connection;

/**
 * Represents transaction isolation values defined in {@link Connection}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum JdbcTransactionIsolation {
    TRANSACTION_NONE("TRANSACTION_NONE", Connection.TRANSACTION_NONE),

    TRANSACTION_READ_UNCOMMITTED("TRANSACTION_READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED),

    TRANSACTION_READ_COMMITTED("TRANSACTION_READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED),

    TRANSACTION_REPEATABLE_READ("TRANSACTION_REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ),

    TRANSACTION_SERIALIZABLE("TRANSACTION_SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);


    private final String transactionName;

    private final int transactionValue;


    JdbcTransactionIsolation(final String transactionName, final int transactionValue) {
        this.transactionName = transactionName;
        this.transactionValue = transactionValue;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public int getTransactionValue() {
        return transactionValue;
    }
}
