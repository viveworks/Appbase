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

package com.viveworks.appbase.client.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientCoreSharedResources;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKeyConfiguration;
import com.viveworks.appbase.client.core.clientkey.DefaultMutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.MutableAppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValueConfiguration;
import com.viveworks.appbase.client.core.clientkey.clusterid.DefaultClusterIdValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValueConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.DataSizeConfiguration;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;

import javax.annotation.Nullable;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utilities used in this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbaseClientUtil {
    private AppbaseClientUtil() {
    }


    /**
     * Get {@link AppbaseClientKey} from {@link AppbaseClientKeyConfiguration} using
     * {@link AppbaseClientCoreSharedResources#getSharedMutableAppbaseClientKeyObjectPool()}.
     *
     * @param appbaseClientCore {@link AppbaseClientCore}
     * @param appbaseClientKeyConfiguration {@link AppbaseClientKeyConfiguration}
     * @return {@link AppbaseClientKey}
     */
    @SuppressWarnings("unchecked")
    public static AppbaseClientKey<?> appbaseClientKeyFromPool(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientKeyConfiguration<?> appbaseClientKeyConfiguration)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(appbaseClientKeyConfiguration, "appbaseClientKeyConfiguration");

        final ComparableValue value = appbaseClientKeyConfiguration.getValue();
        AppbaseUtil.notNull(value, "value");

        final MutableAppbaseClientKey<ComparableValue> mutableAppbaseClientKey =
                (MutableAppbaseClientKey<ComparableValue>)
                        appbaseClientCore
                                .getAppbaseClientCoreSharedResources()
                                .getSharedMutableAppbaseClientKeyObjectPool()
                                .pollCleanObject();
        mutableAppbaseClientKey.initValue(value);

        return mutableAppbaseClientKey;
    }


    /**
     * Get {@code appbaseClientKey} using {@link ClusterIdValueConfiguration}.
     *
     * @param clusterIdValueConfiguration {@link ClusterIdValueConfiguration}
     * @return {@link MutableAppbaseClientKey} of {@link ClusterIdValue}
     */
    public static MutableAppbaseClientKey<ClusterIdValue> clusterIdValueKey(
            final ClusterIdValueConfiguration clusterIdValueConfiguration)
    {
        AppbaseUtil.notNull(clusterIdValueConfiguration, "clusterIdValueConfiguration");

        final String clusterId = clusterIdValueConfiguration.getClusterId();
        AppbaseUtil.notNull(clusterIdValueConfiguration, "clusterIdValueConfiguration");

        final DefaultClusterIdValue clusterIdValue = new DefaultClusterIdValue();
        clusterIdValue.init(clusterId);

        final MutableAppbaseClientKey<ClusterIdValue> mutableAppbaseClientKey = new DefaultMutableAppbaseClientKey<>();
        mutableAppbaseClientKey.initValue(clusterIdValue);

        return mutableAppbaseClientKey;
    }


    /**
     * Get {@code appbaseClientKey} using {@link TextValueConfiguration}.
     *
     * @param textValueConfiguration {@link TextValueConfiguration}
     * @return {@link MutableAppbaseClientKey} of {@link TextValue}
     */
    public static MutableAppbaseClientKey<TextValue> textValueKey(final TextValueConfiguration textValueConfiguration) {
        AppbaseUtil.notNull(textValueConfiguration, "textValueConfiguration");

        final String text = textValueConfiguration.getText();
        AppbaseUtil.notNull(text, "text");

        final TextValue textValue = new TextValue();
        textValue.init(text);

        return textValueKey(textValue);
    }


    /**
     * Get {@code appbaseClientKey} using {@link TextValue}.
     *
     * @param textValue {@link TextValue}
     * @return {@link MutableAppbaseClientKey} of {@link TextValue}
     */
    public static MutableAppbaseClientKey<TextValue> textValueKey(final TextValue textValue) {
        AppbaseUtil.notNull(textValue, "textValue");

        final MutableAppbaseClientKey<TextValue> mutableAppbaseClientKey = new DefaultMutableAppbaseClientKey<>();
        mutableAppbaseClientKey.initValue(textValue);

        return mutableAppbaseClientKey;
    }


    /**
     * Get {@code host:port} from {@link HostPortConfiguration}.
     *
     * @param hostPortConfiguration {@link HostPortConfiguration}
     * @return {@code host:port}
     */
    public static String hostPort(final HostPortConfiguration hostPortConfiguration) {
        AppbaseUtil.notNull(hostPortConfiguration, "hostPortConfiguration");

        final String host = hostPortConfiguration.getHost();
        final int port = hostPortConfiguration.getPort();

        return host + AppbaseUtil.COLON + port;
    }


    /**
     * Convert collection of {@link HostPortConfiguration}s to a comma-separated string of {@code host:port}.
     *
     * @param hostPortConfigurations Set of {@link HostPortConfiguration}
     * @return CSV of {@code host:port}
     */
    public static String hostPortCsv(final Collection<HostPortConfiguration> hostPortConfigurations) {
        AppbaseUtil.notNull(hostPortConfigurations, "hostPortConfigurations");

        final StringBuilder csvBuilder = new StringBuilder();
        boolean first = true;

        for (final HostPortConfiguration hostPortConfiguration : hostPortConfigurations) {
            final String hostPort = hostPort(hostPortConfiguration);

            if (!first) {
                csvBuilder.append(AppbaseUtil.COMMA).append(hostPort);
            } else {
                csvBuilder.append(hostPort);
                first = false;
            }
        }

        return csvBuilder.toString();
    }


    /**
     * Convert {@link HostPortConfiguration} to {@link TextValue}.
     *
     * @param hostPortConfiguration {@link HostPortConfiguration}
     * @return {@link TextValue}
     */
    public static TextValue convertHostPortConfiguration(final HostPortConfiguration hostPortConfiguration) {
        AppbaseUtil.notNull(hostPortConfiguration, "hostPortConfiguration");

        final String text = hostPort(hostPortConfiguration);
        return new TextValue(text);
    }


    /**
     * Convert {@link HostPortConfiguration} to {@link TextValue}.
     *
     * @param hostPortConfiguration {@link HostPortConfiguration}
     * @param appbaseClientCore {@link AppbaseClientCore}
     * @return {@link TextValue}
     */
    public static TextValue convertHostPortConfiguration(
            final HostPortConfiguration hostPortConfiguration,
            final AppbaseClientCore appbaseClientCore)
    {
        AppbaseUtil.notNull(hostPortConfiguration, "hostPortConfiguration");
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");

        final String text = hostPort(hostPortConfiguration);
        final TextValue textValue = appbaseClientCore
                .getAppbaseClientCoreSharedResources()
                .getSharedTextValueObjectPool()
                .pollCleanObject();
        textValue.init(text);

        return textValue;
    }

    /**
     * Convert {@link TimeUnitConfiguration} to milliseconds as {@link String}.
     *
     * @param timeUnitConfiguration {@link TimeUnitConfiguration}
     * @return Milliseconds as {@link String}
     */
    public static String toStringMillis(final TimeUnitConfiguration timeUnitConfiguration) {
        AppbaseUtil.notNull(timeUnitConfiguration, "timeUnitConfiguration");

        return Long.toString(timeUnitConfiguration
                .getTimeUnit()
                .toMillis(timeUnitConfiguration.getTime()));
    }

    /**
     * Convert {@link TimeoutConfiguration} to milliseconds as {@link String}.
     *
     * @param timeoutConfiguration {@link TimeoutConfiguration}
     * @return Milliseconds as {@link String}
     */
    public static String toStringMillis(final TimeoutConfiguration timeoutConfiguration) {
        AppbaseUtil.notNull(timeoutConfiguration, "timeoutConfiguration");

        return Long.toString(timeoutConfiguration
                .getTimeoutTimeUnit()
                .toMillis(timeoutConfiguration.getTimeout()));
    }

    /**
     * Convert {@link DataSizeConfiguration} to bytes as {@link String}.
     *
     * @param dataSizeConfiguration {@link DataSizeConfiguration}
     * @return Bytes as {@link String}
     */
    public static String toStringBytes(final DataSizeConfiguration dataSizeConfiguration) {
        AppbaseUtil.notNull(dataSizeConfiguration, "dataSizeConfiguration");

        return Long.toString(dataSizeConfiguration
                .getUnit()
                .toBytes(dataSizeConfiguration.getDataSize()));
    }


    /**
     * Convert a collection of {@link Class} instances to comma-separated values of class names.
     *
     * @param classes Collection of {@link Class} instances
     * @param <T> Class type
     * @return Class names CSV
     */
    public static <T> String classNamesCsv(final Collection<Class<? extends T>> classes) {
        AppbaseUtil.notNull(classes, "classes");

        final StringBuilder csvBuilder = new StringBuilder();
        boolean first = true;

        for (final Class<? extends T> type : classes) {
            if (type != null) {
                if (!first) {
                    csvBuilder.append(AppbaseUtil.COMMA).append(type.getName());
                } else {
                    csvBuilder.append(type.getName());
                    first = false;
                }
            }
        }

        return csvBuilder.toString();
    }


    /**
     * Wait for a {@link Future} if configured.
     *
     * @param future {@link Future}
     * @param blockingOperationConfiguration {@link BlockingOperationConfiguration}
     * @param <T> Type of the future result
     * @return T Result of the future
     * @throws BlockingOperationRuntimeException if the future completes with an exception
     */
    @CanIgnoreReturnValue
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> T waitAsConfigured(
            final Future<T> future,
            final BlockingOperationConfiguration blockingOperationConfiguration)
            throws BlockingOperationRuntimeException
    {
        AppbaseUtil.notNull(future, "future");
        AppbaseUtil.notNull(blockingOperationConfiguration, "blockingOperationConfiguration");
        AppbaseUtil.notFalse(blockingOperationConfiguration.isEnabled(), "blockingOperationConfiguration.isEnabled()");

        try {
            if (blockingOperationConfiguration.isWaitUntilComplete()) {
                return future.get();
            } else {
                return future.get(
                        blockingOperationConfiguration.getWaitTimeout(),
                        blockingOperationConfiguration.getWaitTimeoutTimeUnit());
            }
        } catch (Exception ex) {
            throw new BlockingOperationRuntimeException("Error with future completion", ex);
        }
    }


    /**
     * Wait uninterruptiby for a {@link Future} if configured.
     *
     * @param future {@link Future}
     * @param blockingOperationConfiguration {@link BlockingOperationConfiguration}
     * @param <T> Type of the future result
     * @return T Result of the future
     * @throws BlockingOperationRuntimeException if the future completes with an exception
     */
    @CanIgnoreReturnValue
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static <T> T waitUninterruptiblyAsConfigured(
            final Future<T> future,
            final BlockingOperationConfiguration blockingOperationConfiguration)
            throws BlockingOperationRuntimeException
    {
        AppbaseUtil.notNull(future, "future");
        AppbaseUtil.notNull(blockingOperationConfiguration, "blockingOperationConfiguration");
        AppbaseUtil.notFalse(blockingOperationConfiguration.isEnabled(), "blockingOperationConfiguration.isEnabled()");

        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException ie) {
                    interrupted = true;
                } catch (ExecutionException ee) {
                    final Throwable cause = ee.getCause();
                    throw new BlockingOperationRuntimeException(cause);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Get {@link Duration} from {@link TimeoutConfiguration}.
     *
     * @param timeoutConfiguration {@link TimeoutConfiguration}
     * @return {@link Duration}
     */
    public static Duration duration(final TimeoutConfiguration timeoutConfiguration) {
        AppbaseUtil.notNull(timeoutConfiguration, "timeoutConfiguration");
        AppbaseUtil.notFalse(timeoutConfiguration.isEnabled(), "timeoutConfiguration.isEnabled()");

        return Duration.ofNanos(timeoutConfiguration.getTimeoutTimeUnit().toNanos(timeoutConfiguration.getTimeout()));
    }


    /**
     * Get {@link Duration} from {@link TimeUnitConfiguration}.
     *
     * @param timeUnitConfiguration {@link TimeUnitConfiguration}
     * @return {@link Duration}
     */
    public static Duration duration(final TimeUnitConfiguration timeUnitConfiguration) {
        AppbaseUtil.notNull(timeUnitConfiguration, "timeUnitConfiguration");
        AppbaseUtil.notFalse(timeUnitConfiguration.isEnabled(), "timeUnitConfiguration.isEnabled()");

        return Duration.ofNanos(timeUnitConfiguration.getTimeUnit().toNanos(timeUnitConfiguration.getTime()));
    }

    /**
     * Get value if not null, else default value (which can be null).
     *
     * @param value Value
     * @param defaultValue Default value
     * @param <T> Value type
     * @return value or default value
     */
    @Nullable
    public static <T> T valueOrDefault(@Nullable final T value, @Nullable final T defaultValue) {
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    /**
     * Configuration {@link Socket} using {@link SocketConfiguration}.
     *
     * @param socket {@link Socket}
     * @param socketConfiguration {@link SocketConfiguration}
     * @throws SocketException if there were errors while configuring the socket
     */
    public static void configureSocket(
            final Socket socket,
            final SocketConfiguration socketConfiguration) throws SocketException
    {
        AppbaseUtil.notNull(socket, "socket");
        AppbaseUtil.notNull(socketConfiguration, "socketConfiguration");

        final Boolean keepAlive = socketConfiguration.getKeepAlive();
        if (keepAlive != null) {
            socket.setKeepAlive(keepAlive);
        }

        final Boolean reuseAddress = socketConfiguration.getReuseAddress();
        if (reuseAddress != null) {
            socket.setReuseAddress(reuseAddress);
        }

        final Boolean tcpNoDelay = socketConfiguration.getTcpNoDelay();
        if (tcpNoDelay != null) {
            socket.setTcpNoDelay(tcpNoDelay);
        }

        final DataSizeConfiguration sendBufferDataSizeConfiguration =
                socketConfiguration.getSendBufferDataSizeConfiguration();
        if (sendBufferDataSizeConfiguration != null && sendBufferDataSizeConfiguration.isEnabled()) {
            socket.setSendBufferSize((int) sendBufferDataSizeConfiguration
                    .getUnit()
                    .toBytes(sendBufferDataSizeConfiguration.getDataSize()));
        }

        final DataSizeConfiguration receiveBufferDataSizeConfiguration =
                socketConfiguration.getReceiveBufferDataSizeConfiguration();
        if (receiveBufferDataSizeConfiguration != null && receiveBufferDataSizeConfiguration.isEnabled()) {
            socket.setReceiveBufferSize((int) receiveBufferDataSizeConfiguration
                    .getUnit()
                    .toBytes(receiveBufferDataSizeConfiguration.getDataSize()));
        }

        final TimeoutConfiguration connectTimeoutConfiguration = socketConfiguration.getConnectTimeoutConfiguration();
        if (connectTimeoutConfiguration != null) {
            socket.setSoTimeout((int) connectTimeoutConfiguration
                    .getTimeoutTimeUnit()
                    .toMillis(connectTimeoutConfiguration.getTimeout()));
        }

        final Boolean linger = socketConfiguration.getLinger();
        final TimeUnitConfiguration lingerTimeUnitConfiguration = socketConfiguration.getLingerTimeUnitConfiguration();
        if (linger != null) {
            if (!linger) {
                socket.setSoLinger(false, 0);
            } else {
                socket.setSoLinger(
                        true,
                        (int) lingerTimeUnitConfiguration.getTimeUnit().toMillis(lingerTimeUnitConfiguration.getTime()));
            }
        }
    }
}
