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

package com.viveworks.appbase.client.service.clamav.client;

import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientConfiguration;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVPingEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVScanPathEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVScanStreamEvent;
import com.viveworks.appbase.client.service.clamav.client.event.DefaultClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVPingResultHandler;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVScanResultHandler;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.eclipse.microprofile.metrics.Counter;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link ClamAVClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVClient implements ClamAVClient {
    private final AppbaseClientCore appbaseClientCore;
    private final ClamAVClientSharedResources clamAVClientSharedResources;
    private final ClamAVMediatorEventListener clamAVMediatorEventListener;

    private final Counter pingErrorCounter;
    private final Counter scanPathErrorCounter;
    private final Counter scanStreamErrorCounter;

    private final AtomicBoolean shutdown;

    public DefaultClamAVClient(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        AppbaseUtil.notNull(defaultClamAVClientConfiguration, "defaultClamAVClientConfiguration");

        this.appbaseClientCore = appbaseClientCore;
        this.clamAVClientSharedResources = clamAVClientSharedResources;
        this.clamAVMediatorEventListener =
                new DefaultClamAVMediatorEventListener(clamAVClientSharedResources, defaultClamAVClientConfiguration);

        registerClamAVMediatorEventListener();

        final MetricService metricService = appbaseClientCore.getAppbaseCore().getMetricService();
        pingErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "ping", "error", "counter"));
        scanPathErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "scanPath", "error", "counter"));
        scanStreamErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "scanStream", "error", "counter"));
        shutdown = new AtomicBoolean(false);
    }

    public DefaultClamAVClient(
            final AppbaseClientCore appbaseClientCore,
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final ClamAVMediatorEventListener clamAVMediatorEventListener)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(clamAVClientSharedResources, "clamAVClientSharedResources");
        AppbaseUtil.notNull(clamAVMediatorEventListener, "clamAVMediatorEventListener");

        this.appbaseClientCore = appbaseClientCore;
        this.clamAVClientSharedResources = clamAVClientSharedResources;
        this.clamAVMediatorEventListener = clamAVMediatorEventListener;

        registerClamAVMediatorEventListener();

        final MetricService metricService = appbaseClientCore.getAppbaseCore().getMetricService();
        pingErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "ping", "error", "counter"));
        scanPathErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "scanPath", "error", "counter"));
        scanStreamErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "scanStream", "error", "counter"));
        shutdown = new AtomicBoolean(false);
    }

    private void registerClamAVMediatorEventListener() {
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .registerMediatorEventListener(ClamAVRequestEvent.class, clamAVMediatorEventListener);
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .registerMediatorEventListener(DefaultClamAVRequestEvent.class, clamAVMediatorEventListener);
    }

    private void unregisterClamAVMediatorEventListener() {
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .unregisterMediatorEventListener(ClamAVRequestEvent.class, clamAVMediatorEventListener.getClass());
        appbaseClientCore
                .getAppbaseCore()
                .getMediatorService()
                .getMediator()
                .getSharedMediatorEventDelegatorRegistry()
                .unregisterMediatorEventListener(DefaultClamAVRequestEvent.class, clamAVMediatorEventListener.getClass());
    }

    @Override
    public void ping(final ClamAVPingResultHandler clamAVPingResultHandler) throws ClamAVClientException {
        AppbaseUtil.notNull(clamAVPingResultHandler, "clamAVPingResultHandler");

        final ClamAVRequestEvent clamAVRequestEvent = clamAVClientSharedResources
                .getClamAVRequestEventObjectPool()
                .pollCleanObject();
        clamAVRequestEvent.init(ClamAVPingEvent.class);
        clamAVRequestEvent
                .getClamAVPingEvent()
                .init(clamAVPingResultHandler);

        publishEvent(clamAVRequestEvent, pingErrorCounter);
    }

    @Override
    public void scan(final Path path, final ClamAVScanResultHandler clamAVScanResultHandler) throws ClamAVClientException {
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(clamAVScanResultHandler, "clamAVScanResultHandler");

        final ClamAVRequestEvent clamAVRequestEvent = clamAVClientSharedResources
                .getClamAVRequestEventObjectPool()
                .pollCleanObject();
        clamAVRequestEvent.init(ClamAVScanPathEvent.class);
        clamAVRequestEvent
                .getClamAVScanPathEvent()
                .init(path, clamAVScanResultHandler);

        publishEvent(clamAVRequestEvent, scanPathErrorCounter);
    }

    @Override
    public void scan(
            final InputStream inputStream,
            final boolean closeInputStream,
            final ClamAVScanResultHandler clamAVScanResultHandler) throws ClamAVClientException
    {
        AppbaseUtil.notNull(inputStream, "inputStream");
        AppbaseUtil.notNull(clamAVScanResultHandler, "clamAVScanResultHandler");

        final ClamAVRequestEvent clamAVRequestEvent = clamAVClientSharedResources
                .getClamAVRequestEventObjectPool()
                .pollCleanObject();
        clamAVRequestEvent.init(ClamAVScanStreamEvent.class);
        clamAVRequestEvent
                .getClamAVScanStreamEvent()
                .init(inputStream, closeInputStream, clamAVScanResultHandler);

        publishEvent(clamAVRequestEvent, scanStreamErrorCounter);
    }

    private void publishEvent(
            final ClamAVRequestEvent clamAVRequestEvent,
            final Counter errorCounter) throws ClamAVClientException
    {
        try {
            appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .publishCustomEvent(ClamAVRequestEvent.class, clamAVRequestEvent);
        } catch (MediatorException me) {
            errorCounter.inc();
            throw new ClamAVClientException("Error publishing event: " + clamAVRequestEvent, me);
        }
    }

    @Override
    public void reload() throws AppbaseRuntimeException {
        shutdown();
        clamAVMediatorEventListener.reload();
        registerClamAVMediatorEventListener();
        shutdown.set(false);
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        unregisterClamAVMediatorEventListener();
        clamAVMediatorEventListener.shutdown();
        shutdown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get() || clamAVMediatorEventListener.isShutdown();
    }
}
