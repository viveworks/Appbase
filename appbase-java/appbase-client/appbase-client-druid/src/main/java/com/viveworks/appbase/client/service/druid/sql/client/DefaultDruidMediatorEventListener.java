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

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DefaultDruidSqlClientConfiguration;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DruidConnectionConfiguration;
import com.viveworks.appbase.client.service.druid.sql.client.configuration.DruidSqlQueryConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.event.delegator.AbstractMediatorCustomEventListener;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.MutableReference;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import org.apache.calcite.avatica.BuiltInConnectionProperty;
import org.eclipse.microprofile.metrics.Counter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link DruidMediatorEventListener}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidMediatorEventListener
        extends AbstractMediatorCustomEventListener<DruidQueryEvent> implements DruidMediatorEventListener
{
    private final AppbaseClientCore appbaseClientCore;
    private final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration;
    private final Counter executeQueryErrorCounter;
    private final Counter connectionErrorCounter;
    private final AtomicReference<Connection> connectionRef;
    private final AtomicBoolean shutdown;

    public DefaultDruidMediatorEventListener(
            final AppbaseClientCore appbaseClientCore,
            final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration)
    {
        super(appbaseClientCore.getAppbaseCore());

        AppbaseUtil.notNull(defaultDruidSqlClientConfiguration, "defaultDruidSqlClientConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.defaultDruidSqlClientConfiguration = defaultDruidSqlClientConfiguration;

        final MetricService metricService = appbaseClientCore.getAppbaseCore().getMetricService();
        this.executeQueryErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "executeQuery", "error", "counter"));
        this.connectionErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "connection", "error", "counter"));

        this.connectionRef = new AtomicReference<>();
        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public DruidResultSet executeQuery(final String query) throws DruidSqlClientException {
        final MutableReference<DruidResultSet> druidResultSetRef = new MutableReference<>();
        final MutableReference<Throwable> throwableRef = new MutableReference<>();

        final DruidResultSetHandler druidResultSetHandler = new DruidResultSetHandler() {
            @Override
            public void complete(final DruidResultSet result) {
                druidResultSetRef.setReference(result);
            }

            @Override
            public void completeWithException(final Throwable throwable) {
                throwableRef.setReference(throwable);
            }
        };

        final DruidQueryEvent druidQueryEvent = new DefaultDruidQueryEvent(query, druidResultSetHandler);
        handleCustomEvent(druidQueryEvent);
        if (throwableRef.getReference() != null) {
            throw new DruidSqlClientException("Error while executing query: " + query, throwableRef.getReference());
        }

        return druidResultSetRef.getReference();
    }

    @Override
    public DruidResultSet executeParameterizedQuery(
            final String parameterizedQuery,
            final List<DruidSqlParameter> druidSqlParameters) throws DruidSqlClientException
    {
        final MutableReference<DruidResultSet> druidResultSetRef = new MutableReference<>();
        final MutableReference<Throwable> throwableRef = new MutableReference<>();

        final DruidResultSetHandler druidResultSetHandler = new DruidResultSetHandler() {
            @Override
            public void complete(final DruidResultSet result) {
                druidResultSetRef.setReference(result);
            }

            @Override
            public void completeWithException(final Throwable throwable) {
                throwableRef.setReference(throwable);
            }
        };

        final DruidQueryEvent druidQueryEvent = new DefaultDruidQueryEvent(parameterizedQuery, druidSqlParameters, druidResultSetHandler);
        handleCustomEvent(druidQueryEvent);
        if (throwableRef.getReference() != null) {
            throw new DruidSqlClientException("Error while executing query=[" + parameterizedQuery + "], parameters=[" + druidSqlParameters + "]",
                    throwableRef.getReference());
        }

        return druidResultSetRef.getReference();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void handleCustomEvent(final DruidQueryEvent druidQueryEvent) {
        final Connection connection;
        try {
            connection = connection();
            AppbaseUtil.notNull(connection, "connection");
        } catch (Exception ex) {
            connectionErrorCounter.inc();
            druidQueryEvent.getDruidResultSetHandler().completeWithException(ex);
            return;
        }

        try {
            execute(druidQueryEvent, connection);
        } catch (Exception ex) {
            executeQueryErrorCounter.inc();
            druidQueryEvent.getDruidResultSetHandler().completeWithException(ex);
            return;
        }
    }

    private Connection connection() throws SQLException {
        final Connection connection = connectionRef.get();
        if (connection != null && !connection.isClosed()) {
            return connection;
        }


        final Properties properties = new Properties();
        final DruidConnectionConfiguration druidConnectionConfiguration =
                defaultDruidSqlClientConfiguration.getDruidConnectionConfiguration();

        final String authentication = druidConnectionConfiguration.getAuthentication();
        if (authentication != null) {
            properties.put(BuiltInConnectionProperty.AUTHENTICATION, authentication);
        }


        final String keyPassword = druidConnectionConfiguration.getKeyPassword();
        if (keyPassword != null) {
            properties.put(BuiltInConnectionProperty.KEY_PASSWORD, keyPassword);
        }


        final String keystore = druidConnectionConfiguration.getKeystore();
        if (keystore != null) {
            properties.put(BuiltInConnectionProperty.KEYSTORE, keystore);
        }


        final String keystorePassword = druidConnectionConfiguration.getKeystorePassword();
        if (keyPassword != null) {
            properties.put(BuiltInConnectionProperty.KEYSTORE_PASSWORD, keystorePassword);
        }


        final String trustStore = druidConnectionConfiguration.getTrustStore();
        if (trustStore != null) {
            properties.put(BuiltInConnectionProperty.TRUSTSTORE, trustStore);
        }


        final String trustStorePassword = druidConnectionConfiguration.getTrustStorePassword();
        if (trustStorePassword != null) {
            properties.put(BuiltInConnectionProperty.TRUSTSTORE_PASSWORD, trustStorePassword);
        }


        final String sqlQueryId = druidConnectionConfiguration.getSqlQueryId();
        if (sqlQueryId != null) {
            properties.put(DruidSqlClient.SQL_QUERY_ID_PROPERTY_KEY, sqlQueryId);
        }


        final Boolean sqlStringifyArrays = druidConnectionConfiguration.getSqlStringifyArrays();
        if (sqlStringifyArrays != null) {
            properties.put(DruidSqlClient.SQL_STRINGIFY_ARRAYS_PROPERTY_KEY, sqlStringifyArrays);
        }


        final String sqlTimeZone = druidConnectionConfiguration.getSqlTimeZone();
        if (sqlTimeZone != null) {
            properties.put(DruidSqlClient.SQL_TIME_ZONE_PROPERTY_KEY, sqlTimeZone);
        }


        final Boolean useApproximateCountDistinct = druidConnectionConfiguration.getUseApproximateCountDistinct();
        if (useApproximateCountDistinct != null) {
            properties.put(
                    DruidSqlClient.USE_APPROXIMATE_COUNT_DISTINCT_PROPERTY_KEY.getPropertyKey(),
                    useApproximateCountDistinct);
        }


        final Boolean useApproximateTopN = druidConnectionConfiguration.getUseApproximateTopN();
        if (useApproximateTopN != null) {
            properties.put(DruidSqlClient.USE_APPROXIMATE_TOP_N_PROPERTY_KEY, useApproximateTopN);
        }


        final Boolean useGroupingSetForExactDistinct = druidConnectionConfiguration.getUseGroupingSetForExactDistinct();
        if (useGroupingSetForExactDistinct != null) {
            properties.put(
                    DruidSqlClient.USE_GROUPING_SET_FOR_EXACT_DISTINCT_PROPERTY_KEY.getPropertyKey(),
                    useGroupingSetForExactDistinct);
        }


        final Map<String, String> additionalProperties = druidConnectionConfiguration.getAdditionalProperties();
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }


        final String url = druidConnectionConfiguration.getUrl();
        final String user = druidConnectionConfiguration.getUser();
        final String password = druidConnectionConfiguration.getPassword();

        final Connection localConnection;
        if (properties.isEmpty()) {
            if (user != null && password != null) {
                localConnection = DriverManager.getConnection(url, user, password);
            } else {
                localConnection = DriverManager.getConnection(url);
            }
        } else {
            if (user != null && password != null) {
                properties.put(DruidSqlClient.USER_PROPERTY_KEY.getPropertyKey(), user);
                properties.put(DruidSqlClient.PASSWORD_PROPERTY_KEY.getPropertyKey(), password);
            }

            localConnection = DriverManager.getConnection(url, properties);
        }


        final Boolean autoCommit = druidConnectionConfiguration.getAutoCommit();
        if (autoCommit != null) {
            localConnection.setAutoCommit(autoCommit);
        }


        final String catalog = druidConnectionConfiguration.getCatalog();
        if (catalog != null) {
            localConnection.setCatalog(catalog);
        }


        final Map<String, String> clientInfo = druidConnectionConfiguration.getClientInfo();
        if (clientInfo != null) {
            final Properties clientInfoProperties = new Properties(clientInfo.size());
            clientInfoProperties.putAll(clientInfo);
            localConnection.setClientInfo(clientInfoProperties);
        }


        final DruidResultSet.CursorHoldability defaultCursorHoldability =
                druidConnectionConfiguration.getDefaultCursorHoldability();
        if (defaultCursorHoldability != null) {
            localConnection.setHoldability(defaultCursorHoldability.getValue());
        }


        final Boolean readOnly = druidConnectionConfiguration.getReadOnly();
        if (readOnly != null) {
            localConnection.setReadOnly(readOnly);
        }


        final DruidTransactionIsolation druidTransactionIsolation =
                druidConnectionConfiguration.getDruidTransactionIsolation();
        if (druidTransactionIsolation != null) {
            //noinspection MagicConstant
            localConnection.setTransactionIsolation(druidTransactionIsolation.getValue());
        }


        final TimeoutConfiguration networkTimeoutConfiguration =
                druidConnectionConfiguration.getNetworkTimeoutConfiguration();
        if (networkTimeoutConfiguration != null && networkTimeoutConfiguration.isEnabled()) {
            localConnection.setNetworkTimeout(
                    appbaseClientCore
                            .getAppbaseCore()
                            .getMediatorService()
                            .getMediator()
                            .getSharedExecutorService(),
                    (int) networkTimeoutConfiguration
                            .getTimeoutTimeUnit()
                            .toSeconds(networkTimeoutConfiguration.getTimeout()));
        }


        final String schema = druidConnectionConfiguration.getSchema();
        if (schema != null) {
            localConnection.setSchema(schema);
        }

        connectionRef.set(localConnection);
        return connectionRef.get();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void execute(final DruidQueryEvent druidQueryEvent, final Connection connection) {
        AppbaseUtil.notNull(druidQueryEvent, "druidQueryEvent");
        AppbaseUtil.notNull(connection, "connection");

        final String query = druidQueryEvent.getQuery();
        final List<DruidSqlParameter> druidSqlParameters = druidQueryEvent.getDruidSqlParameters();

        try {
            final DruidSqlQueryConfiguration druidSqlQueryConfiguration =
                    defaultDruidSqlClientConfiguration.getDruidSqlQueryConfiguration();
            final DruidConnectionConfiguration druidConnectionConfiguration =
                    defaultDruidSqlClientConfiguration.getDruidConnectionConfiguration();

            final DruidResultSet.ConcurrencyType finalConcurrencyType = AppbaseClientUtil.valueOrDefault(
                    druidSqlQueryConfiguration.getConcurrencyType(),
                    druidConnectionConfiguration.getDefaultConcurrencyType());
            final DruidResultSet.CursorHoldability finalCursorHoldability = AppbaseClientUtil.valueOrDefault(
                    druidSqlQueryConfiguration.getCursorHoldability(),
                    druidConnectionConfiguration.getDefaultCursorHoldability());
            final DruidResultSet.FetchDirection finalFetchDirection = AppbaseClientUtil.valueOrDefault(
                    druidSqlQueryConfiguration.getFetchDirection(),
                    druidConnectionConfiguration.getDefaultFetchDirection());
            final DruidResultSet.ResultSetType finalResultSetType = AppbaseClientUtil.valueOrDefault(
                    druidSqlQueryConfiguration.getResultSetType(),
                    druidConnectionConfiguration.getDefaultResultSetType());

            final ResultSet resultSet;
            if (druidSqlParameters != null) {
                final PreparedStatement preparedStatement;

                if (finalResultSetType != null && finalCursorHoldability != null && finalConcurrencyType != null) {
                    //noinspection MagicConstant
                    preparedStatement = connection.prepareStatement(
                            query,
                            finalResultSetType.getValue(),
                            finalConcurrencyType.getValue(),
                            finalCursorHoldability.getValue());
                } else if (finalResultSetType != null && finalConcurrencyType != null) {
                    //noinspection MagicConstant
                    preparedStatement = connection.prepareStatement(
                            query,
                            finalResultSetType.getValue(),
                            finalConcurrencyType.getValue());
                } else {
                    preparedStatement = connection.prepareStatement(query);
                }

                configure(preparedStatement, finalFetchDirection);
                setParameters(preparedStatement, druidSqlParameters);
                resultSet = preparedStatement.executeQuery();
            } else {
                final Statement statement;

                if (finalResultSetType != null && finalCursorHoldability != null && finalConcurrencyType != null) {
                    //noinspection MagicConstant
                    statement = connection.createStatement(
                            finalResultSetType.getValue(),
                            finalConcurrencyType.getValue(),
                            finalCursorHoldability.getValue());
                } else if (finalResultSetType != null && finalConcurrencyType != null) {
                    //noinspection MagicConstant
                    statement = connection.createStatement(finalResultSetType.getValue(), finalConcurrencyType.getValue());
                } else {
                    statement = connection.createStatement();
                }

                configure(statement, finalFetchDirection);
                resultSet = statement.executeQuery(query);
            }

            final DruidResultSet druidResultSet = new DefaultDruidResultSet(resultSet);
            druidQueryEvent.getDruidResultSetHandler().complete(druidResultSet);
        } catch (Exception ex) {
            executeQueryErrorCounter.inc();
            druidQueryEvent.getDruidResultSetHandler().completeWithException(ex);
        }
    }

    private void configure(
            final Statement statement,
            final DruidResultSet.FetchDirection fetchDirection) throws SQLException
    {
        final DruidSqlQueryConfiguration druidSqlQueryConfiguration =
                defaultDruidSqlClientConfiguration.getDruidSqlQueryConfiguration();

        final TimeoutConfiguration queryTimeoutConfiguration =
                druidSqlQueryConfiguration.getQueryTimeoutConfiguration();
        if (queryTimeoutConfiguration != null && queryTimeoutConfiguration.isEnabled()) {
            statement.setQueryTimeout((int) queryTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toSeconds(queryTimeoutConfiguration.getTimeout()));
        }


        final Integer fetchSize = druidSqlQueryConfiguration.getFetchSize();
        if (fetchSize != null) {
            statement.setFetchSize(fetchSize);
        }


        final Long largeMaxRows = druidSqlQueryConfiguration.getLargeMaxRows();
        if (largeMaxRows != null) {
            statement.setLargeMaxRows(largeMaxRows);
        }


        final Integer maxRows = druidSqlQueryConfiguration.getMaxRows();
        if (maxRows != null) {
            statement.setMaxRows(maxRows);
        }


        if (fetchDirection != null) {
            //noinspection MagicConstant
            statement.setFetchDirection(fetchDirection.getValue());
        }
    }

    private void setParameters(
            final PreparedStatement preparedStatement,
            final List<DruidSqlParameter> druidSqlParameters)
            throws SQLException
    {
        AppbaseUtil.notNull(preparedStatement, "preparedStatement");
        AppbaseUtil.notNull(druidSqlParameters, "druidSqlParameters");

        for (final DruidSqlParameter druidSqlParameter : druidSqlParameters) {
            if (!druidSqlParameter.isValid()) {
                throw new AppbaseRuntimeException("Invalid parameter: " + druidSqlParameter);
            }

            final DruidSqlType druidSqlType = druidSqlParameter.getDruidSqlType();
            final Object value = druidSqlParameter.getValue();
            final int position = druidSqlParameter.getPosition();

            switch (druidSqlType) {
                case CHAR:
                case VARCHAR:
                    preparedStatement.setString(position, (String) value);
                    break;

                case TINYINT:
                    preparedStatement.setByte(position, (byte) value);
                    break;

                case SMALLINT:
                    preparedStatement.setShort(position, (short) value);
                    break;

                case INTEGER:
                    preparedStatement.setInt(position, (int) value);
                    break;

                case BIGINT:
                    preparedStatement.setLong(position, (long) value);
                    break;

                case FLOAT:
                    preparedStatement.setFloat(position, (float) value);
                    break;

                case REAL:
                case DOUBLE:
                case DECIMAL:
                    preparedStatement.setDouble(position, (double) value);
                    break;

                case BOOLEAN:
                    preparedStatement.setBoolean(position, (boolean) value);
                    break;

                case DATE:
                    preparedStatement.setDate(position, (Date) value);
                    break;

                case TIMESTAMP:
                    preparedStatement.setTimestamp(position, (Timestamp) value);
                    break;

                case OTHER:
                    preparedStatement.setObject(position, value);
                    break;

                default:
                case NULL:
                    preparedStatement.setNull(position, druidSqlType.getValue());
                    break;
            }
        }
    }

    @Override
    protected Class<DruidQueryEvent> getEventType() {
        return DruidQueryEvent.class;
    }

    @Override
    public void reload() throws AppbaseRuntimeException {
        shutdown();
        shutdown.set(false);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        final Connection connection = connectionRef.get();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException sqe) {
            throw new AppbaseRuntimeException("Error shutting down " + getClass().getName(), sqe);
        }
    }
}
