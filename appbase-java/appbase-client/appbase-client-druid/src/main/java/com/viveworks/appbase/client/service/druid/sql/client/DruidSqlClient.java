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

import com.viveworks.appbase.core.service.Reloadable;
import com.viveworks.appbase.core.service.configuration.ConfigurationKey;

import java.util.List;

/**
 * Represents a {@code SQL client} to {@code Druid}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface DruidSqlClient extends Reloadable {
    ConfigurationKey SQL_QUERY_ID_PROPERTY_KEY = ConfigurationKey.fromPropertyKey("sqlQueryId");
    ConfigurationKey SQL_TIME_ZONE_PROPERTY_KEY = ConfigurationKey.fromPropertyKey("sqlTimeZone");
    ConfigurationKey SQL_STRINGIFY_ARRAYS_PROPERTY_KEY = ConfigurationKey.fromPropertyKey("sqlStringifyArrays");
    ConfigurationKey USE_APPROXIMATE_COUNT_DISTINCT_PROPERTY_KEY =
            ConfigurationKey.fromPropertyKey("useApproximateCountDistinct");
    ConfigurationKey USE_GROUPING_SET_FOR_EXACT_DISTINCT_PROPERTY_KEY =
            ConfigurationKey.fromPropertyKey("useGroupingSetForExactDistinct");
    ConfigurationKey USE_APPROXIMATE_TOP_N_PROPERTY_KEY = ConfigurationKey.fromPropertyKey("useApproximateTopN");
    ConfigurationKey USER_PROPERTY_KEY = ConfigurationKey.fromPropertyKey("user");
    ConfigurationKey PASSWORD_PROPERTY_KEY = ConfigurationKey.fromPropertyKey("password");


    DruidResultSet executeQuery(String query) throws DruidSqlClientException;

    DruidResultSet executeParameterizedQuery(
            String parameterizedQuery,
            List<DruidSqlParameter> druidSqlParameters) throws DruidSqlClientException;

    /**
     * Execute query.
     *
     * @param query SQL query (non-parameterized)
     * @param druidResultSetHandler {@link DruidResultSetHandler} to handle the result
     * @throws DruidSqlClientException if there are errors before executing the query
     */
    void asyncExecuteQuery(
            String query,
            DruidResultSetHandler druidResultSetHandler) throws DruidSqlClientException;


    /**
     * Execute parameterized queries.
     *
     * @param parameterizedQuery Parameterized SQL query
     * @param druidSqlParameters {@link DruidSqlParameter}s for the parameterized query
     * @param druidResultSetHandler {@link DruidResultSetHandler} to handle the result
     * @throws DruidSqlClientException if there are errors before executing the query
     */
    void asyncExecuteParameterizedQuery(
            String parameterizedQuery,
            List<DruidSqlParameter> druidSqlParameters,
            DruidResultSetHandler druidResultSetHandler) throws DruidSqlClientException;
}
