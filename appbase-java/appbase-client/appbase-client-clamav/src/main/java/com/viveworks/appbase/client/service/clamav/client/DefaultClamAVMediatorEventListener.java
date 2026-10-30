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

import com.viveworks.appbase.client.service.clamav.client.configuration.DefaultClamAVClientConfiguration;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVPingEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVRequestEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVRequestEventType;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVScanPathEvent;
import com.viveworks.appbase.client.service.clamav.client.event.ClamAVScanStreamEvent;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVPingResultHandler;
import com.viveworks.appbase.client.service.clamav.client.handler.ClamAVScanResultHandler;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVPingResult;
import com.viveworks.appbase.client.service.clamav.client.result.ClamAVScanResult;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.client.util.ClientHandler;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.event.delegator.AbstractMediatorCustomEventListener;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import org.eclipse.microprofile.metrics.Counter;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link ClamAVMediatorEventListener}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultClamAVMediatorEventListener
        extends AbstractMediatorCustomEventListener<ClamAVRequestEvent> implements ClamAVMediatorEventListener
{
    private static final Charset RESPONSE_CHARSET = StandardCharsets.US_ASCII;
    private static final int INT_SIZE = 4;
    private static final byte[] PING_COMMAND = "zPING\0".getBytes(RESPONSE_CHARSET);
    private static final byte[] PONG = "PONG".getBytes(RESPONSE_CHARSET);
    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(RESPONSE_CHARSET);
    private static final int PONG_REPLY_LENGTH = 4;
    private static final String INSTREAM_SIZE_EXCEEDED = "INSTREAM size limit exceeded.";
    private static final String OK = "OK";
    private static final String FOUND = "FOUND";


    private final ClamAVClientSharedResources clamAVClientSharedResources;
    private final Logger logger;
    private final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration;
    private final int instreamChunkSize;
    private final Counter socketConnectionErrorCounter;
    private final Counter unsupportedClamAVRequestEventTypeErrorCounter;
    private final Counter pingErrorCounter;
    private final Counter pingFailedCounter;
    private final Counter scanPathErrorCounter;
    private final Counter scanStreamErrorCounter;
    private final Counter instreamSizeExceededErrorCounter;
    private final Counter closeSocketErrorCounter;
    private final AtomicReference<Socket> socketRef;


    public DefaultClamAVMediatorEventListener(
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final DefaultClamAVClientConfiguration defaultClamAVClientConfiguration)
    {
        super(clamAVClientSharedResources.getAppbaseClientCore().getAppbaseCore());

        AppbaseUtil.notNull(defaultClamAVClientConfiguration, "defaultClamAVClientConfiguration");

        this.clamAVClientSharedResources = clamAVClientSharedResources;
        this.logger = clamAVClientSharedResources
                .getAppbaseClientCore()
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.defaultClamAVClientConfiguration = defaultClamAVClientConfiguration;
        final DataSizeConfiguration chunkSizeConfiguration = defaultClamAVClientConfiguration.getChunkSizeConfiguration();
        this.instreamChunkSize = (int) chunkSizeConfiguration.getUnit().toBytes(chunkSizeConfiguration.getDataSize());

        final MetricService metricService = clamAVClientSharedResources
                .getAppbaseClientCore()
                .getAppbaseCore()
                .getMetricService();
        this.socketConnectionErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "socketConnection", "error", "counter"));
        this.unsupportedClamAVRequestEventTypeErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "unsupportedClamAVRequestEventType", "error", "counter"));
        this.pingErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "ping", "error", "counter"));
        this.pingFailedCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "pingFailed", "counter"));
        this.scanPathErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "scanPath", "error", "counter"));
        this.scanStreamErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "scanStream", "error", "counter"));
        this.instreamSizeExceededErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "instreamSizeExceeded", "error", "counter"));
        this.closeSocketErrorCounter = metricService
                .getMetricRegistry()
                .counter(metricService.name(getClass(), "closeSocket", "error", "counter"));

        this.socketRef = new AtomicReference<>();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void handleCustomEvent(final ClamAVRequestEvent clamAVRequestEvent) {
        final Class<? extends ClamAVRequestEventType> clamAVRequestEventType =
                clamAVRequestEvent.getClamAVRequestEventType();

        if (clamAVRequestEventType.isAssignableFrom(ClamAVPingEvent.class)) {
            final ClamAVPingEvent clamAVPingEvent = clamAVRequestEvent.getClamAVPingEvent();
            try {
                handleClamAVPingEvent(clamAVPingEvent);
            } catch (Exception ex) {
                pingErrorCounter.inc();
                clamAVPingEvent.getClamAVPingResultHandler().completeWithException(ex);
            } finally {
                try {
                    closeSocket();
                } catch (IOException ioe1) {
                    closeSocketErrorCounter.inc();
                    socketRef.set(null);
                }
            }
        } else if (clamAVRequestEventType.isAssignableFrom(ClamAVScanPathEvent.class)) {
            final ClamAVScanPathEvent clamAVScanPathEvent = clamAVRequestEvent.getClamAVScanPathEvent();
            try {
                handleClamAVScanPathEvent(clamAVScanPathEvent);
            } catch (Exception ex) {
                scanPathErrorCounter.inc();
                clamAVScanPathEvent.getClamAVScanResultHandler().completeWithException(ex);
            } finally {
                try {
                    closeSocket();
                } catch (IOException ioe1) {
                    closeSocketErrorCounter.inc();
                    socketRef.set(null);
                }
            }
        } else if (clamAVRequestEventType.isAssignableFrom(ClamAVScanStreamEvent.class)) {
            final ClamAVScanStreamEvent clamAVScanStreamEvent = clamAVRequestEvent.getClamAVScanStreamEvent();
            try {
                handleClamAVScanStreamEvent(clamAVScanStreamEvent);
            } catch (Exception ex) {
                scanStreamErrorCounter.inc();
                clamAVScanStreamEvent.getClamAVScanResultHandler().completeWithException(ex);
            } finally {
                try {
                    closeSocket();
                } catch (IOException ioe1) {
                    closeSocketErrorCounter.inc();
                    socketRef.set(null);
                }
            }
        } else {
            unsupportedClamAVRequestEventTypeErrorCounter.inc();
            throw new IllegalArgumentException(
                    "Unsupported " + ClamAVRequestEventType.class.getName() + ": " + clamAVRequestEvent);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void handleClamAVPingEvent(final ClamAVPingEvent clamAVPingEvent) throws IOException {
        AppbaseUtil.notNull(clamAVPingEvent, "clamAVPingEvent");

        final ClamAVPingResultHandler clamAVPingResultHandler = clamAVPingEvent.getClamAVPingResultHandler();
        final Socket socket = socket(clamAVPingResultHandler);
        if (socket != null) {
            final OutputStream socketOutputStream = socket.getOutputStream();
            socketOutputStream.write(PING_COMMAND);
            socketOutputStream.flush();

            final byte[] response = new byte[PONG_REPLY_LENGTH];
            final InputStream socketInputStream = socket.getInputStream();

            int offset = 0;
            int bytesRead;
            do {
                bytesRead = socketInputStream.read(response, offset, Math.max(response.length - offset, 0));
                offset += bytesRead;
            } while (bytesRead > 0);

            final boolean success = Arrays.equals(response, PONG);
            final ClamAVPingResult clamAVPingResult = clamAVClientSharedResources
                    .getClamAVPingResultObjectPool()
                    .pollCleanObject();
            if (!success) {
                pingFailedCounter.inc();
                clamAVPingResult.init(false);

                try {
                    closeSocket();
                } catch (Exception ex) {
                    closeSocketErrorCounter.inc();
                    throw ex;
                }
            } else {
                clamAVPingResult.init(true);
            }

            clamAVPingResultHandler.complete(clamAVPingResult);
        }
    }

    private void handleClamAVScanPathEvent(final ClamAVScanPathEvent clamAVScanPathEvent) throws IOException {
        AppbaseUtil.notNull(clamAVScanPathEvent, "clamAVScanPathEvent");

        final File file = clamAVScanPathEvent.getPath().toFile();
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        handleClamAVScanEvent(
                bufferedInputStream,
                clamAVScanPathEvent.getClamAVScanResultHandler(),
                true);
    }

    private void handleClamAVScanStreamEvent(final ClamAVScanStreamEvent clamAVScanStreamEvent) throws IOException {
        AppbaseUtil.notNull(clamAVScanStreamEvent, "clamAVScanStreamEvent");

        handleClamAVScanEvent(
                clamAVScanStreamEvent.getInputStream(),
                clamAVScanStreamEvent.getClamAVScanResultHandler(),
                clamAVScanStreamEvent.closeInputStream());
    }

    private void handleClamAVScanEvent(
            final InputStream inputStream,
            final ClamAVScanResultHandler clamAVScanResultHandler,
            final boolean closeInputStream) throws IOException
    {
        AppbaseUtil.notNull(inputStream, "inputStream");
        AppbaseUtil.notNull(clamAVScanResultHandler, "clamAVScanResultHandler");

        try {
            final Socket socket = socket(clamAVScanResultHandler);
            if (socket != null) {
                final OutputStream socketOutputStream = socket.getOutputStream();
                socketOutputStream.write(INSTREAM_COMMAND);

                final byte[] chunk = new byte[instreamChunkSize];
                final InputStream socketInputStream = socket.getInputStream();
                int inputRead = inputStream.read(chunk);
                while (inputRead >= 0) {
                    final byte[] chunkSize = ByteBuffer
                            .allocate(INT_SIZE)
                            .putInt(inputRead)
                            .array();
                    socketOutputStream.write(chunkSize);
                    socketOutputStream.write(chunk, 0, inputRead);

                    if (socketInputStream.available() > 0) {
                        final byte[] response = readSocketStream(socketInputStream);
                        if (sizeExceeded(response, clamAVScanResultHandler)) {
                            return;
                        }

                        abort(response, clamAVScanResultHandler);
                        return;
                    }

                    inputRead = inputStream.read(chunk);
                }
                socketOutputStream.write(new byte[]{0, 0, 0, 0});
                socketOutputStream.flush();

                final byte[] response = readSocketStream(socketInputStream);
                completeResponse(response, clamAVScanResultHandler);
            }
        } finally {
            if (closeInputStream) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    clamAVScanResultHandler.completeWithException(ioe);
                }
            }
        }
    }

    private void abort(
            final byte[] response,
            final ClamAVScanResultHandler clamAVScanResultHandler)
    {
        final String responseString = responseString(response);
        final ClamAVScanAbortedException clamAVScanAbortedException = new ClamAVScanAbortedException(responseString);
        clamAVScanResultHandler.completeWithException(clamAVScanAbortedException);
    }

    private void completeResponse(
            final byte[] response,
            final ClamAVScanResultHandler clamAVScanResultHandler) throws IOException
    {
        if (sizeExceeded(response, clamAVScanResultHandler)) {
            return;
        }

        final ClamAVScanResult clamAVScanResult = clamAVClientSharedResources
                .getClamAVScanResultObjectPool()
                .pollCleanObject();
        if (notInfected(response)) {
            clamAVScanResult.init(false);
        } else {
            clamAVScanResult.init(true);
        }

        clamAVScanResultHandler.complete(clamAVScanResult);
    }

    private boolean notInfected(final byte[] response) {
        final String responseString = responseString(response);
        return (responseString.contains(OK) && !responseString.contains(FOUND));
    }

    private String responseString(final byte[] response) {
        return new String(response, RESPONSE_CHARSET);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private boolean sizeExceeded(final byte[] response, final ClamAVScanResultHandler clamAVScanResultHandler) {
        AppbaseUtil.notNull(response, "response");
        AppbaseUtil.notNull(clamAVScanResultHandler, "clamAVScanResultHandler");

        final String responseString = responseString(response);
        if (responseString.startsWith(INSTREAM_SIZE_EXCEEDED)) {
            instreamSizeExceededErrorCounter.inc();

            final ClamAVSizeLimitException clamAVSizeLimitException = new ClamAVSizeLimitException();
            clamAVScanResultHandler.completeWithException(clamAVSizeLimitException);
            return true;
        }

        return false;
    }

    private byte[] readSocketStream(final InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    @Nullable
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Socket socket(final ClientHandler<?> clientHandler) {
        final Socket socket = socketRef.get();
        if (socket != null && socket.isConnected()) {
            return socket;
        }

        final HostPortConfiguration hostPortConfiguration = defaultClamAVClientConfiguration.getHostPortConfiguration();
        try {
            final Socket newSocket = new Socket(hostPortConfiguration.getHost(), hostPortConfiguration.getPort());
            AppbaseClientUtil.configureSocket(newSocket, defaultClamAVClientConfiguration.getSocketConfiguration());
            socketRef.set(newSocket);
            return newSocket;
        } catch (Exception ex) {
            socketConnectionErrorCounter.inc();
            logger.warn("Unable to connect to {}", defaultClamAVClientConfiguration.getHostPortConfiguration());
            clientHandler.completeWithException(ex);
        }

        return null;
    }

    private void closeSocket() throws IOException {
        final Socket socket = socketRef.get();
        if (socket != null && socket.isConnected()) {
            socket.close();
            socketRef.set(null);
        }
    }

    @Override
    protected Class<ClamAVRequestEvent> getEventType() {
        return ClamAVRequestEvent.class;
    }

    @Override
    public void reload() throws AppbaseRuntimeException {
        shutdown();
    }

    @Override
    public boolean isShutdown() {
        final Socket socket = socketRef.get();
        if (socket == null) {
            return true;
        }

        return socket.isClosed();
    }

    @Override
    public void shutdown() throws AppbaseRuntimeException {
        try {
            closeSocket();
        } catch (IOException ioe) {
            closeSocketErrorCounter.inc();
            throw new AppbaseRuntimeException("Failed to close socket", ioe);
        }
    }
}
