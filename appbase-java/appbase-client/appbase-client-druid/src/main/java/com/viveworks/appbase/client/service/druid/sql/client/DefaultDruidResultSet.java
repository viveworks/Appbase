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
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.objectpool.ObjectFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DruidResultSet}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MethodCount")
public class DefaultDruidResultSet implements DruidResultSet {
    private final AtomicReference<ResultSet> resultSetRef;


    public DefaultDruidResultSet() {
        this.resultSetRef = new AtomicReference<>();
    }

    public DefaultDruidResultSet(final ResultSet resultSet) {
        this();
        init(resultSet);
    }


    @Override
    public void init(final ResultSet resultSet) {
        AppbaseUtil.notNull(resultSet, "resultSet");

        this.resultSetRef.set(resultSet);
    }



    @Override
    public boolean next() throws DruidSqlClientException {
        try {
            return resultSet().next();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean isCurrentColumnNull() throws DruidSqlClientException {
        try {
            return resultSet().wasNull();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public char getChar(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getString(columnPosition).charAt(0);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public char getChar(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getString(columnName).charAt(0);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public String getVarchar(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getString(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public String getVarchar(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getString(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public double getDecimal(final int columnPosition) throws DruidSqlClientException {
        return getDouble(columnPosition);
    }

    @Override
    public double getDecimal(final String columnName) throws DruidSqlClientException {
        return getDouble(columnName);
    }

    @Override
    public float getFloat(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getFloat(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public float getFloat(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getFloat(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public double getReal(final int columnPosition) throws DruidSqlClientException {
        return getDouble(columnPosition);
    }

    @Override
    public double getReal(final String columnName) throws DruidSqlClientException {
        return getDouble(columnName);
    }

    @Override
    public double getDouble(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getDouble(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public double getDouble(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getDouble(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean getBoolean(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getBoolean(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean getBoolean(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getBoolean(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public byte getTinyInt(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getByte(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public byte getTinyInt(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getByte(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public short getSmallInt(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getShort(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public short getSmallInt(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getShort(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public int getInteger(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getInt(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public int getInteger(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getInt(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public long getBigInt(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getLong(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public long getBigInt(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getLong(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public Timestamp getTimestamp(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getTimestamp(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public Timestamp getTimestamp(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getTimestamp(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public Date getDate(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getDate(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public Date getDate(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getDate(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public Object getOther(final int columnPosition) throws DruidSqlClientException {
        return getObject(columnPosition);
    }

    @Override
    public Object getOther(final String columnName) throws DruidSqlClientException {
        return getObject(columnName);
    }

    @Override
    public Object getObject(final int columnPosition) throws DruidSqlClientException {
        try {
            return resultSet().getObject(columnPosition);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public Object getObject(final String columnName) throws DruidSqlClientException {
        try {
            return resultSet().getObject(columnName);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean isBeforeFirst() throws DruidSqlClientException {
        try {
            return resultSet().isBeforeFirst();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean isAfterLast() throws DruidSqlClientException {
        try {
            return resultSet().isAfterLast();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean isFirst() throws DruidSqlClientException {
        try {
            return resultSet().isFirst();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public int getRow() throws DruidSqlClientException {
        try {
            return resultSet().getRow();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public FetchDirection getFetchDirection() throws DruidSqlClientException {
        try {
            final int fetchDirection = resultSet().getFetchDirection();
            return FetchDirection.from(fetchDirection);
        } catch (SQLException | AppbaseRuntimeException ex) {
            throw new DruidSqlClientException(ex);
        }
    }

    @Override
    public void setFetchDirection(final FetchDirection fetchDirection) throws DruidSqlClientException {
        AppbaseUtil.notNull(fetchDirection, "fetchDirection");

        try {
            //noinspection MagicConstant
            resultSet().setFetchDirection(fetchDirection.getValue());
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public int getFetchSize() throws DruidSqlClientException {
        try {
            return resultSet().getFetchSize();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public void setFetchSize(final int rows) throws DruidSqlClientException {
        try {
            resultSet().setFetchSize(rows);
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public ResultSetMetaData getResultSetMetaData() throws DruidSqlClientException {
        try {
            return resultSet().getMetaData();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public ResultSetType getResultSetType() throws DruidSqlClientException {
        try {
            final int type = resultSet().getType();
            return ResultSetType.from(type);
        } catch (SQLException | AppbaseRuntimeException ex) {
            throw new DruidSqlClientException(ex);
        }
    }

    @Override
    public ConcurrencyType getConcurrencyType() throws DruidSqlClientException {
        try {
            final int concurrency = resultSet().getConcurrency();
            return ConcurrencyType.from(concurrency);
        } catch (SQLException | AppbaseRuntimeException ex) {
            throw new DruidSqlClientException(ex);
        }
    }

    @Override
    public CursorHoldability getCursorHoldability() throws DruidSqlClientException {
        try {
            final int holdability = resultSet().getHoldability();
            return CursorHoldability.from(holdability);
        } catch (SQLException | AppbaseRuntimeException ex) {
            throw new DruidSqlClientException(ex);
        }
    }

    @Override
    public boolean isClosed() throws DruidSqlClientException {
        try {
            return resultSet().isClosed();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public void close() throws DruidSqlClientException {
        try {
            resultSet().close();
        } catch (SQLException se) {
            throw new DruidSqlClientException(se);
        }
    }

    @Override
    public boolean isValid() {
        return resultSetRef.get() != null;
    }

    @Override
    public boolean isClean() {
        return resultSetRef.get() == null;
    }

    @Override
    public void clean() {
        resultSetRef.set(null);
    }


    private ResultSet resultSet() {
        final ResultSet resultSet = resultSetRef.get();
        AppbaseUtil.notNull(resultSet, "resultSet");

        return resultSet;
    }

    @Override
    public String toString() {
        return "DefaultDruidResultSet{" +
                "resultSet=" + resultSetRef.get() +
                "} " + super.toString();
    }

    /**
     * {@link ObjectFactory} for {@link DruidResultSet}.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    public static class Factory implements ObjectFactory<DruidResultSet> {
        @Override
        public DruidResultSet newObject() {
            return new DefaultDruidResultSet();
        }
    }
}
