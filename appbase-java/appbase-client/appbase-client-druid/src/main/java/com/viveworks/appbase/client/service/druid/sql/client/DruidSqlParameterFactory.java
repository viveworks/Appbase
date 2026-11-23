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

import java.sql.Timestamp;
import java.util.Date;

/**
 * A factory to create {@link DruidSqlParameter}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface DruidSqlParameterFactory {
    DruidSqlParameter nullParameter(int position, DruidSqlType druidSqlType);

    DruidSqlParameter charParameter(int position, char value);

    DruidSqlParameter varcharParameter(int position, String value);

    DruidSqlParameter decimalParameter(int position, double value);

    DruidSqlParameter floatParameter(int position, float value);

    DruidSqlParameter realParameter(int position, double value);

    DruidSqlParameter doubleParameter(int position, double value);

    DruidSqlParameter booleanParameter(int position, boolean value);

    DruidSqlParameter tinyIntParameter(int position, byte value);

    DruidSqlParameter smallIntParameter(int position, short value);

    DruidSqlParameter integerParameter(int position, int value);

    DruidSqlParameter bigIntParameter(int position, long value);

    DruidSqlParameter timestampParameter(int position, Timestamp value);

    DruidSqlParameter dateParameter(int position, Date value);

    DruidSqlParameter otherParameter(int position, Object value);
}
