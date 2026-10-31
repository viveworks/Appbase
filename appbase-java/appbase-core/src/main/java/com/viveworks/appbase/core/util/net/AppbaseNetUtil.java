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

package com.viveworks.appbase.core.util.net;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseConstants;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.buffer.BufferAllocator;
import com.viveworks.appbase.core.util.net.buffer.BufferAllocatorConfiguration;
import com.viveworks.appbase.core.util.net.buffer.BufferPoolingStrategy;
import com.viveworks.appbase.core.util.net.buffer.BufferSource;
import com.viveworks.appbase.core.util.net.buffer.PooledDirectBufferAllocator;
import com.viveworks.appbase.core.util.net.buffer.PooledHeapBufferAllocator;
import com.viveworks.appbase.core.util.net.buffer.UnpooledDirectBufferAllocator;
import com.viveworks.appbase.core.util.net.buffer.UnpooledHeapBufferAllocator;
import com.viveworks.appbase.core.util.net.ssl.SslApplicationProtocol;
import com.viveworks.appbase.core.util.net.ssl.SslApplicationProtocolConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslApplicationProtocolNegotiation;
import com.viveworks.appbase.core.util.net.ssl.SslClientAuth;
import com.viveworks.appbase.core.util.net.ssl.SslContextCommonConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslImplementation;
import com.viveworks.appbase.core.util.net.ssl.SslKeyStore;
import com.viveworks.appbase.core.util.net.ssl.SslSelectedListenerFailureBehavior;
import com.viveworks.appbase.core.util.net.ssl.SslSelectorFailureBehavior;
import com.viveworks.appbase.core.util.net.ssl.SslTrustManagerConfiguration;
import io.netty.channel.Channel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.internal.PlatformDependent;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Net utilities.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class AppbaseNetUtil {
    /**
     * Size of a byte in bytes.
     */
    public static final int SIZE_OF_BYTE = 1;
    /**
     * Size of a boolean in bytes.
     */
    public static final int SIZE_OF_BOOLEAN = 1;
    /**
     * Size of a char in bytes.
     */
    public static final int SIZE_OF_CHAR = 2;
    /**
     * Size of a short in bytes.
     */
    public static final int SIZE_OF_SHORT = 2;
    /**
     * Size of a medium int in bytes.
     */
    public static final int SIZE_OF_MEDIUM = 3;
    /**
     * Size of an int in bytes.
     */
    public static final int SIZE_OF_INT = 4;
    /**
     * Size of a float in bytes.
     */
    public static final int SIZE_OF_FLOAT = 4;
    /**
     * Size of a long in bytes.
     */
    public static final int SIZE_OF_LONG = 8;
    /**
     * Size of a double in bytes.
     */
    public static final int SIZE_OF_DOUBLE = 8;

    /**
     * Maximum buffer length.
     */
    public static final int MAX_BUFFER_LENGTH = Integer.MAX_VALUE - 8;

    public static final String APPBASE_NET_BASE_DIRECTORY =
            AppbaseConstants.APPBASE_BASE_DIRECTORY + File.separator + "net";
    public static final String SSL_DIRECTORY = APPBASE_NET_BASE_DIRECTORY + File.separator + "ssl";

    private AppbaseNetUtil() {
    }

    /**
     * Calculate length from start and end indices.
     *
     * @param startIndexInclusive Start index (inclusive)
     * @param endIndexExclusive End index (exclusive)
     * @return length
     */
    public static int lengthByIndices(final int startIndexInclusive, final int endIndexExclusive) {
        return (endIndexExclusive - startIndexInclusive);
    }

    /**
     * Check if the given start and end indices are valid.
     *
     * @param startIndex Start index
     * @param endIndex End index
     */
    public static void checkIndices(final int startIndex, final int endIndex) {
        AppbaseUtil.positive(startIndex, "startIndex");
        AppbaseUtil.positive(endIndex, "endIndex");

        if (endIndex < startIndex) {
            throw new IndexOutOfBoundsException("End index must be greater than start index");
        }
    }

    /**
     * Check if the OS is <code>Linux</code>.
     *
     * @return true if Linux, false otherwise
     */
    public static boolean isLinux() {
        return os().toLowerCase().contains("linux");
    }

    /**
     * Check if the OS is <code>OSx</code>.
     *
     * @return true if OSx, false otherwise
     */
    public static boolean isOsx() {
        return PlatformDependent.isOsx();
    }

    /**
     * Get the OS name.
     *
     * @return OS name
     */
    private static String os() {
        return System.getProperty("os.name");
    }

    /**
     * Build {@link SslContextBuilder} using {@link SslContextCommonConfiguration}.
     *
     * @param sslContextBuilder SSL context builder to use
     * @param sslContextCommonConfiguration Configuration for the builder
     */
    public static void sslContext(final SslContextBuilder sslContextBuilder,
                                  final SslContextCommonConfiguration sslContextCommonConfiguration)
    {
        AppbaseUtil.notNull(sslContextBuilder, "sslContextBuilder");
        AppbaseUtil.notNull(sslContextCommonConfiguration, "sslContextCommonConfiguration");

        final SslProvider sslProvider =
                SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
        sslContextBuilder.sslProvider(sslProvider);

        final Set<String> ciphers = sslContextCommonConfiguration.getCiphers();
        if (ciphers != null) {
            sslContextBuilder.ciphers(ciphers, SupportedCipherSuiteFilter.INSTANCE);
        }

        final Set<String> protocols = sslContextCommonConfiguration.getProtocols();
        if (protocols != null) {
            sslContextBuilder.protocols(protocols);
        }

        final int sessionCacheSize = sslContextCommonConfiguration.getSessionCacheSize();
        if (sessionCacheSize > 0) {
            sslContextBuilder.sessionCacheSize(sessionCacheSize);
        }

        final long sessionTimeoutSeconds = sslContextCommonConfiguration.getSessionTimeoutSeconds();
        if (sessionTimeoutSeconds > 0) {
            sslContextBuilder.sessionTimeout(sessionTimeoutSeconds);
        }

        final SslClientAuth sslClientAuth = sslContextCommonConfiguration.getSslClientAuth();
        if (sslClientAuth != null) {
            sslContextBuilder.clientAuth(sslClientAuth.getClientAuth());
        }

        final SslKeyStore sslKeyStore = sslContextCommonConfiguration.getSslKeyStore();
        if (sslKeyStore != null) {
            sslContextBuilder.keyStoreType(sslKeyStore.getKeyStoreName());
        }

        final SslImplementation sslImplementation = sslContextCommonConfiguration.getSslImplementation();
        if (sslImplementation != null) {
            sslContextBuilder.sslProvider(sslImplementation.getSslProvider());
        }

        sslContextBuilder.enableOcsp(sslContextCommonConfiguration.isEnableOcsp());
        sslContextBuilder.startTls(sslContextCommonConfiguration.isStartTls());

        final SslApplicationProtocolConfiguration sslApplicationProtocolConfiguration =
                sslContextCommonConfiguration.getSslApplicationProtocolConfiguration();
        if (sslApplicationProtocolConfiguration != null) {
            final SslApplicationProtocolNegotiation sslApplicationProtocolNegotiation =
                    sslApplicationProtocolConfiguration.getSslApplicationProtocolNegotiation();

            final SslSelectorFailureBehavior sslSelectorFailureBehavior =
                    sslApplicationProtocolConfiguration.getSslSelectorFailureBehavior();

            final SslSelectedListenerFailureBehavior sslSelectedListenerFailureBehavior =
                    sslApplicationProtocolConfiguration.getSslSelectedListenerFailureBehavior();

            final Set<SslApplicationProtocol> sslApplicationProtocolNames =
                    sslApplicationProtocolConfiguration.getSslApplicationProtocols();

            if (sslApplicationProtocolNegotiation != null &&
                    sslSelectorFailureBehavior != null &&
                    sslSelectedListenerFailureBehavior != null &&
                    sslApplicationProtocolNames != null)
            {
                final List<String> supportedProtocols = new ArrayList<>();
                sslApplicationProtocolNames.forEach(sslApplicationProtocol ->
                        supportedProtocols.add(sslApplicationProtocol.getProtocolName()));

                final ApplicationProtocolConfig applicationProtocolConfig = new ApplicationProtocolConfig(
                        sslApplicationProtocolNegotiation.getProtocol(),
                        sslSelectorFailureBehavior.getSelectorFailureBehavior(),
                        sslSelectedListenerFailureBehavior.getSelectedListenerFailureBehavior(),
                        supportedProtocols);

                sslContextBuilder.applicationProtocolConfig(applicationProtocolConfig);
            }
        }

        final SslTrustManagerConfiguration sslTrustManagerConfiguration =
                sslContextCommonConfiguration.getSslTrustManagerConfiguration();
        if (sslTrustManagerConfiguration != null) {
            final String trustedCertificatesFilename = sslTrustManagerConfiguration.getTrustedCertificatesFilename();
            sslContextBuilder.trustManager(new File(trustedCertificatesFilename));
        }
    }

    /**
     * Get the appropriate Netty {@link Channel} type for the current platform.
     *
     * @return Channel type class
     */
    public static Class<? extends Channel> channelType() {
        if (isLinux()) {
            return EpollSocketChannel.class;
        } else if (isOsx()) {
            return KQueueSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }

    /**
     * Get the local hostname.
     *
     * @return hostname
     */
    public static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            throw new AppbaseRuntimeException("Error while retrieving localhost hostname", uhe);
        }
    }

    /**
     * Convert a {@link TimeoutConfiguration} to the desired {@link TimeUnit}.
     *
     * @param timeoutConfiguration Timeout configuration to convert
     * @param targetTimeUnit Target time unit
     * @return Converted timeout value
     */
    public static long convertTimeoutConfiguration(
            final TimeoutConfiguration timeoutConfiguration,
            final TimeUnit targetTimeUnit)
    {
        AppbaseUtil.notNull(timeoutConfiguration, "timeoutConfiguration");
        AppbaseUtil.notNull(targetTimeUnit, "targetTimeUnit");
        return targetTimeUnit.convert(timeoutConfiguration.getTimeout(),
                timeoutConfiguration.getTimeoutTimeUnit());
    }

    /**
     * Build a {@link BufferAllocator} using {@link BufferAllocatorConfiguration}.
     *
     * @param bufferAllocatorConfiguration {@link BufferAllocatorConfiguration}
     * @param appbaseCore {@link AppbaseCore}
     * @return {@link BufferAllocator}
     */
    public static BufferAllocator bufferAllocator(
            final BufferAllocatorConfiguration bufferAllocatorConfiguration,
            final AppbaseCore appbaseCore)
    {
        AppbaseUtil.notNull(bufferAllocatorConfiguration, "bufferAllocatorConfiguration");
        AppbaseUtil.notNull(appbaseCore, "appbaseCore");

        final BufferPoolingStrategy bufferPoolingStrategy =
                bufferAllocatorConfiguration.getBufferPoolingStrategy();
        final BufferSource bufferSource =
                bufferAllocatorConfiguration.getBufferSource();
        switch (bufferPoolingStrategy) {
            case POOLED:
                switch (bufferSource) {
                    case HEAP:
                        return new PooledHeapBufferAllocator(
                                appbaseCore,
                                bufferAllocatorConfiguration.getPooledBufferAllocatorConfiguration());
                    case DIRECT:
                        return new PooledDirectBufferAllocator(
                                appbaseCore,
                                bufferAllocatorConfiguration.getPooledBufferAllocatorConfiguration());
                    default:
                        throw new AppbaseRuntimeException(
                                "Unsupported " + BufferSource.class.getName() + ": " + bufferSource);
                }
            case UNPOOLED:
                switch (bufferSource) {
                    case HEAP:
                        return new UnpooledHeapBufferAllocator(
                                appbaseCore,
                                bufferAllocatorConfiguration.getUnpooledBufferAllocatorConfiguration());
                    case DIRECT:
                        return new UnpooledDirectBufferAllocator(
                                appbaseCore,
                                bufferAllocatorConfiguration.getUnpooledBufferAllocatorConfiguration());
                    default:
                        throw new AppbaseRuntimeException(
                                "Unsupported " + BufferSource.class.getName() + ": " + bufferSource);
                }
            default:
                throw new AppbaseRuntimeException(
                        "Unsupported " + BufferPoolingStrategy.class.getName() + ": " + bufferPoolingStrategy);
        }
    }
}
