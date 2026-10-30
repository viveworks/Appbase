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
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link DruidSqlClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultDruidSqlClient implements DruidSqlClient {
    private final AppbaseClientCore appbaseClientCore;
    private final DruidSqlClientSharedResources druidSqlClientSharedResources;
    private final DruidMediatorEventListener druidMediatorEventListener;

    private final Counter executeQueryErrorCounter;
    private final Counter executeParameterizedQueryErrorCounter;

    private final AtomicBoolean shutdown;


    public DefaultDruidSqlClient(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DefaultDruidSqlClientConfiguration defaultDruidSqlClientConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(defaultDruidSqlClientConfiguration, "defaultDruidSqlClientConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.druidSqlClientSharedResources = druidSqlClientSharedResources;

        this.druidMediatorEventListener =
                new DefaultDruidMediatorEventListener(appbaseClientCore, defaultDruidSqlClientConfiguration);
        registerDruidMediatorEventListener();

        final MetricService metricService = appbaseClientCore.getAppbaseCore().getMetricService();
        this.executeQueryErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "executeQuery", "error", "counter"));
        this.executeParameterizedQueryErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "executeParameterizedQuery", "error", "counter"));
        shutdown = new AtomicBoolean(false);
    }

    public DefaultDruidSqlClient(
            final AppbaseClientCore appbaseClientCore,
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidMediatorEventListener druidMediatorEventListener)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(druidSqlClientSharedResources, "druidSqlClientSharedResources");
        AppbaseUtil.notNull(druidMediatorEventListener, "druidMediatorEventListener");

        this.appbaseClientCore = appbaseClientCore;
        this.druidSqlClientSharedResources = druidSqlClientSharedResources;
        this.druidMediatorEventListener = druidMediatorEventListener;

        registerDruidMediatorEventListener();

        final MetricService metricService = appbaseClientCore.getAppbaseCore().getMetricService();
        this.executeQueryErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "executeQuery", "error", "counter"));
        this.executeParameterizedQueryErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "executeParameterizedQuery", "error", "counter"));
        shutdown = new AtomicBoolean(false);
    }

    private void registerDruidMediatorEventListener() {
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .registerMediatorEventListener(DruidQueryEvent.class, druidMediatorEventListener);
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .registerMediatorEventListener(DefaultDruidQueryEvent.class, druidMediatorEventListener);
    }

    private void unregisterDruidMediatorEventListener() {
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .unregisterMediatorEventListener(DruidQueryEvent.class, druidMediatorEventListener.getClass());
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .unregisterMediatorEventListener(DefaultDruidQueryEvent.class, druidMediatorEventListener.getClass());
    }

    @Override
    public DruidResultSet executeQuery(final String query) throws DruidSqlClientException {
        return druidMediatorEventListener.executeQuery(query);
    }

    @Override
    public DruidResultSet executeParameterizedQuery(
            final String parameterizedQuery,
            final List<DruidSqlParameter> druidSqlParameters) throws DruidSqlClientException
    {
        return druidMediatorEventListener.executeParameterizedQuery(parameterizedQuery, druidSqlParameters);
    }

    @Override
    public void asyncExecuteQuery(
            final String query,
            final DruidResultSetHandler druidResultSetHandler) throws DruidSqlClientException
    {
        AppbaseUtil.notNull(query, "query");
        AppbaseUtil.notNull(druidResultSetHandler, "druidResultSetHandler");

        final DruidQueryEvent druidQueryEvent = druidSqlClientSharedResources
                .getSharedDruidQueryEventObjectPool()
                .pollCleanObject();
        druidQueryEvent.init(query, druidResultSetHandler);
        publishEvent(druidQueryEvent, executeQueryErrorCounter);
    }

    @Override
    public void asyncExecuteParameterizedQuery(
            final String parameterizedQuery,
            final List<DruidSqlParameter> druidSqlParameters,
            final DruidResultSetHandler druidResultSetHandler) throws DruidSqlClientException
    {
        AppbaseUtil.notNull(parameterizedQuery, "parameterizedQuery");
        AppbaseUtil.notNull(druidSqlParameters, "druidSqlParameters");
        AppbaseUtil.notNull(druidResultSetHandler, "druidResultSetHandler");

        final DruidQueryEvent druidQueryEvent = druidSqlClientSharedResources
                .getSharedDruidQueryEventObjectPool()
                .pollCleanObject();
        druidQueryEvent.init(parameterizedQuery, druidSqlParameters, druidResultSetHandler);
        publishEvent(druidQueryEvent, executeParameterizedQueryErrorCounter);
    }

    private void publishEvent(
            final DruidQueryEvent druidQueryEvent,
            final Counter errorCounter) throws DruidSqlClientException
    {
        try {
            appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .publishCustomEvent(DruidQueryEvent.class, druidQueryEvent);
        } catch (MediatorException me) {
            errorCounter.inc();
            throw new DruidSqlClientException("Error publishing event: " + druidQueryEvent, me);
        }
    }

    @Override
    public void reload() throws AppbaseRuntimeException {
        shutdown();
        druidMediatorEventListener.reload();
        registerDruidMediatorEventListener();
        shutdown.set(false);
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        unregisterDruidMediatorEventListener();
        druidMediatorEventListener.shutdown();
        shutdown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get() || druidMediatorEventListener.isShutdown();
    }
}
