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

package com.viveworks.appbase.client.service.druid.sql.client;

import java.sql.Connection;

/**
 * Representation of {@link java.sql.Connection#setTransactionIsolation(int)}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum DruidTransactionIsolation {
    TRANSACTION_NONE(Connection.TRANSACTION_NONE),

    TRANSACTION_READ_UNCOMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    TRANSACTION_READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    TRANSACTION_REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

    TRANSACTION_SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);


    private final int value;

    DruidTransactionIsolation(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
