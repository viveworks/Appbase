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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.objectpool.Poolable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Represents a result set from a {@code Druid SQL} query. Although, {@code Avatica driver} supports the {@code JDBC API},
 * not all methods of {@link ResultSet} are implemented. This interface defines only the supported methods.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface DruidResultSet extends AutoCloseable, Poolable {
    boolean next() throws DruidSqlClientException;


    boolean isCurrentColumnNull() throws DruidSqlClientException;


    char getChar(int columnPosition) throws DruidSqlClientException;

    char getChar(String columnName) throws DruidSqlClientException;



    String getVarchar(int columnPosition) throws DruidSqlClientException;

    String getVarchar(String columnName) throws DruidSqlClientException;



    double getDecimal(int columnPosition) throws DruidSqlClientException;

    double getDecimal(String columnName) throws DruidSqlClientException;



    float getFloat(int columnPosition) throws DruidSqlClientException;

    float getFloat(String columnName) throws DruidSqlClientException;



    double getReal(int columnPosition) throws DruidSqlClientException;

    double getReal(String columnName) throws DruidSqlClientException;



    double getDouble(int columnPosition) throws DruidSqlClientException;

    double getDouble(String columnName) throws DruidSqlClientException;



    boolean getBoolean(int columnPosition) throws DruidSqlClientException;

    boolean getBoolean(String columnName) throws DruidSqlClientException;



    byte getTinyInt(int columnPosition) throws DruidSqlClientException;

    byte getTinyInt(String columnName) throws DruidSqlClientException;



    short getSmallInt(int columnPosition) throws DruidSqlClientException;

    short getSmallInt(String columnName) throws DruidSqlClientException;



    int getInteger(int columnPosition) throws DruidSqlClientException;

    int getInteger(String columnName) throws DruidSqlClientException;



    long getBigInt(int columnPosition) throws DruidSqlClientException;

    long getBigInt(String columnName) throws DruidSqlClientException;



    Timestamp getTimestamp(int columnPosition) throws DruidSqlClientException;

    Timestamp getTimestamp(String columnName) throws DruidSqlClientException;



    Date getDate(int columnPosition) throws DruidSqlClientException;

    Date getDate(String columnName) throws DruidSqlClientException;



    Object getOther(int columnPosition) throws DruidSqlClientException;

    Object getOther(String columnName) throws DruidSqlClientException;



    Object getObject(int columnPosition) throws DruidSqlClientException;

    Object getObject(String columnName) throws DruidSqlClientException;



    boolean isBeforeFirst() throws DruidSqlClientException;

    boolean isAfterLast() throws DruidSqlClientException;

    boolean isFirst() throws DruidSqlClientException;

    int getRow() throws DruidSqlClientException;



    FetchDirection getFetchDirection() throws DruidSqlClientException;

    void setFetchDirection(FetchDirection fetchDirection) throws DruidSqlClientException;



    int getFetchSize() throws DruidSqlClientException;

    void setFetchSize(int rows) throws DruidSqlClientException;



    ResultSetMetaData getResultSetMetaData() throws DruidSqlClientException;



    ResultSetType getResultSetType() throws DruidSqlClientException;

    ConcurrencyType getConcurrencyType() throws DruidSqlClientException;

    CursorHoldability getCursorHoldability() throws DruidSqlClientException;



    boolean isClosed() throws DruidSqlClientException;

    void close() throws DruidSqlClientException;



    void init(ResultSet resultSet);



    /**
     * Represents the fetch direction of the result set.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    enum FetchDirection {
        FETCH_FORWARD(ResultSet.FETCH_FORWARD),
        FETCH_REVERSE(ResultSet.FETCH_REVERSE),
        FETCH_UNKNOWN(ResultSet.FETCH_UNKNOWN);


        private final int value;

        FetchDirection(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }


        public static FetchDirection from(final int value) {
            if (value == ResultSet.FETCH_FORWARD) {
                return FETCH_FORWARD;
            } else if (value == ResultSet.FETCH_REVERSE) {
                return FETCH_REVERSE;
            } else if (value == ResultSet.FETCH_UNKNOWN) {
                return FETCH_UNKNOWN;
            }

            throw new AppbaseRuntimeException("Unknown value: " + value);
        }
    }



    /**
     * Represents the result set type.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    enum ResultSetType {
        FORWARD_ONLY(ResultSet.TYPE_FORWARD_ONLY),
        SCROLL_INSENSITIVE(ResultSet.TYPE_SCROLL_INSENSITIVE),
        SCROLL_SENSITIVE(ResultSet.TYPE_SCROLL_SENSITIVE);


        private final int value;

        ResultSetType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }


        public static ResultSetType from(final int value) {
            if (value == ResultSet.TYPE_FORWARD_ONLY) {
                return FORWARD_ONLY;
            } else if (value == ResultSet.TYPE_SCROLL_SENSITIVE) {
                return SCROLL_INSENSITIVE;
            } else if (value == ResultSet.TYPE_SCROLL_INSENSITIVE) {
                return SCROLL_INSENSITIVE;
            }

            throw new AppbaseRuntimeException("Unknown value: " + value);
        }
    }



    /**
     * Represents the concurrency type.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    enum ConcurrencyType {
        READ_ONLY(ResultSet.CONCUR_READ_ONLY),
        UPDATABLE(ResultSet.CONCUR_UPDATABLE);


        private final int value;

        ConcurrencyType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ConcurrencyType from(final int value) {
            if (value == ResultSet.CONCUR_READ_ONLY) {
                return READ_ONLY;
            } else if (value == ResultSet.CONCUR_UPDATABLE) {
                return UPDATABLE;
            }

            throw new AppbaseRuntimeException("Unknown value: " + value);
        }
    }



    /**
     * Represents the cursor holdability.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    enum CursorHoldability {
        HOLD_CURSORS_OVER_COMMIT(ResultSet.HOLD_CURSORS_OVER_COMMIT),
        CLOSE_CURSORS_AT_COMMIT(ResultSet.CLOSE_CURSORS_AT_COMMIT);


        private final int value;

        CursorHoldability(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static CursorHoldability from(final int value) {
            if (value == ResultSet.HOLD_CURSORS_OVER_COMMIT) {
                return HOLD_CURSORS_OVER_COMMIT;
            } else if (value == ResultSet.CLOSE_CURSORS_AT_COMMIT) {
                return CLOSE_CURSORS_AT_COMMIT;
            }

            throw new AppbaseRuntimeException("Unknown value: " + value);
        }
    }
}
