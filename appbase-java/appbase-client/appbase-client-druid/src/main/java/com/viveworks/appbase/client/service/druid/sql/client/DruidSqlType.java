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

package com.viveworks.appbase.client.service.druid.sql.client;

import java.sql.Types;

/**
 * Represents {@code SQL} types supported by Druid.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public enum DruidSqlType {
    NULL(Types.NULL),
    CHAR(Types.CHAR),
    VARCHAR(Types.VARCHAR),
    DECIMAL(Types.DECIMAL),
    FLOAT(Types.FLOAT),
    REAL(Types.REAL),
    DOUBLE(Types.DOUBLE),
    BOOLEAN(Types.BOOLEAN),
    TINYINT(Types.TINYINT),
    SMALLINT(Types.SMALLINT),
    INTEGER(Types.INTEGER),
    BIGINT(Types.BIGINT),
    TIMESTAMP(Types.TIMESTAMP),
    DATE(Types.DATE),
    OTHER(Types.OTHER);

    private final int value;

    DruidSqlType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
