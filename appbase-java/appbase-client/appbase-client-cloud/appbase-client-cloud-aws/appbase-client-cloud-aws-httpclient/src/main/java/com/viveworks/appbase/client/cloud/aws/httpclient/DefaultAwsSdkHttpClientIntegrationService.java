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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.AwsSdkApacheProxyConfiguration;
import com.viveworks.appbase.client.cloud.aws.httpclient.configuration.DefaultAwsSdkHttpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.cloud.aws.common.configuration.tls.AwsSdkTlsKeyManagersProviderConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.di.guice.GuiceInjectorService;
import com.viveworks.appbase.core.util.net.InetAddressConfiguration;
import com.viveworks.appbase.core.util.net.InetAddressVersion;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link AwsSdkHttpClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultAwsSdkHttpClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <SdkHttpClient, TextValue, DefaultAwsSdkHttpClientIntegrationServiceConfiguration>
        implements AwsSdkHttpClientIntegrationService
{
    private final AtomicReference<SdkHttpClient> sdkHttpClientRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
            defaultAwsSdkHttpClientIntegrationServiceConfiguration;

    /**
     * Creates a DefaultAwsSdkHttpClientIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SdkHttpClient, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final SdkHttpClient sdkHttpClient)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SdkHttpClient.class);

        this.sdkHttpClientRef = new AtomicReference<>(sdkHttpClient);
        this.defaultAwsSdkHttpClientIntegrationServiceConfiguration = null;
    }

    /**
     * Creates a DefaultAwsSdkHttpClientIntegrationService using the supplied configuration and collaborators.
     */
    public DefaultAwsSdkHttpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SdkHttpClient, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultAwsSdkHttpClientIntegrationServiceConfiguration
                    defaultAwsSdkHttpClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SdkHttpClient.class);

        AppbaseUtil.notNull(
                defaultAwsSdkHttpClientIntegrationServiceConfiguration,
                "defaultAwsSdkHttpClientIntegrationServiceConfiguration");
        this.defaultAwsSdkHttpClientIntegrationServiceConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration;

        this.sdkHttpClientRef = new AtomicReference<>();
        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    @Override
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultAwsSdkHttpClientIntegrationServiceConfiguration,
                "defaultAwsSdkHttpClientIntegrationServiceConfiguration");

        final ApacheHttpClient.Builder apacheHttpClientBuilder = ApacheHttpClient.builder();

        final TimeoutConfiguration connectionAcquisitionTimeoutConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getConnectionAcquisitionTimeoutConfiguration();
        if (connectionAcquisitionTimeoutConfiguration != null) {
            apacheHttpClientBuilder.connectionAcquisitionTimeout(
                    AppbaseClientUtil.duration(connectionAcquisitionTimeoutConfiguration));
        }

        final TimeoutConfiguration connectionTimeoutConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getConnectionTimeoutConfiguration();
        if (connectionTimeoutConfiguration != null) {
            apacheHttpClientBuilder.connectionTimeout(AppbaseClientUtil.duration(connectionTimeoutConfiguration));
        }

        final TimeoutConfiguration connectionMaxIdleTimeoutConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getConnectionMaxIdleTimeoutConfiguration();
        if (connectionMaxIdleTimeoutConfiguration != null) {
            apacheHttpClientBuilder.connectionMaxIdleTime(AppbaseClientUtil.duration(connectionMaxIdleTimeoutConfiguration));
        }

        final TimeUnitConfiguration connectionTimeToLiveConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getConnectionTimeToLiveConfiguration();
        if (connectionTimeToLiveConfiguration != null) {
            apacheHttpClientBuilder.connectionTimeToLive(AppbaseClientUtil.duration(connectionTimeToLiveConfiguration));
        }

        final Boolean expectContinueEnabled =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getExpectContinueEnabled();
        if (expectContinueEnabled != null) {
            apacheHttpClientBuilder.expectContinueEnabled(expectContinueEnabled);
        }

        final Integer maxConnections = defaultAwsSdkHttpClientIntegrationServiceConfiguration.getMaxConnections();
        if (maxConnections != null) {
            apacheHttpClientBuilder.maxConnections(maxConnections);
        }

        final Boolean tcpKeepAlive = defaultAwsSdkHttpClientIntegrationServiceConfiguration.getTcpKeepAlive();
        if (tcpKeepAlive != null) {
            apacheHttpClientBuilder.tcpKeepAlive(tcpKeepAlive);
        }

        final TimeoutConfiguration socketTimeoutConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getSocketTimeoutConfiguration();
        if (socketTimeoutConfiguration != null) {
            apacheHttpClientBuilder.socketTimeout(AppbaseClientUtil.duration(socketTimeoutConfiguration));
        }

        final Boolean useIdleConnectionReaper =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getUseIdleConnectionReaper();
        if (useIdleConnectionReaper != null) {
            apacheHttpClientBuilder.useIdleConnectionReaper(useIdleConnectionReaper);
        }

        final InetAddressConfiguration localAddressConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getLocalAddressConfiguration();
        if (localAddressConfiguration != null) {
            final InetAddressVersion inetAddressVersion = localAddressConfiguration.getInetAddressVersion();
            try {
                if (inetAddressVersion == InetAddressVersion.IPV4) {
                    apacheHttpClientBuilder.localAddress(Inet4Address.getByName(localAddressConfiguration.getHost()));
                } else {
                    apacheHttpClientBuilder.localAddress(Inet6Address.getByName(localAddressConfiguration.getHost()));
                }
            } catch (UnknownHostException uhe) {
                throw new AppbaseClientException("Failed to resolve local address: " + localAddressConfiguration, uhe);
            }
        }

        final GuiceInjectorService guiceInjectorService = appbaseClientCore.getAppbaseCore().getGuiceInjectorService();

        final InjectedTypeConfiguration<? extends CredentialsProvider> credentialsProviderInjectedTypeConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getCredentialsProviderInjectedTypeConfiguration();
        if (credentialsProviderInjectedTypeConfiguration != null) {
            apacheHttpClientBuilder.credentialsProvider(
                    AppbaseUtil.injectedInstance(guiceInjectorService, credentialsProviderInjectedTypeConfiguration));
        }

        final InjectedTypeConfiguration<? extends DnsResolver> dnsResolverInjectedTypeConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getDnsResolverInjectedTypeConfiguration();
        if (dnsResolverInjectedTypeConfiguration != null) {
            apacheHttpClientBuilder.dnsResolver(
                    AppbaseUtil.injectedInstance(guiceInjectorService, dnsResolverInjectedTypeConfiguration));
        }

        final InjectedTypeConfiguration<? extends HttpRoutePlanner> httpRoutePlannerInjectedTypeConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getHttpRoutePlannerInjectedTypeConfiguration();
        if (httpRoutePlannerInjectedTypeConfiguration != null) {
            apacheHttpClientBuilder.httpRoutePlanner(
                    AppbaseUtil.injectedInstance(guiceInjectorService, httpRoutePlannerInjectedTypeConfiguration));
        }

        final InjectedTypeConfiguration<? extends ConnectionSocketFactory> connectionSocketFactoryInjectedTypeConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getConnectionSocketFactoryInjectedTypeConfiguration();
        if (connectionSocketFactoryInjectedTypeConfiguration != null) {
            apacheHttpClientBuilder.socketFactory(
                    AppbaseUtil.injectedInstance(guiceInjectorService, connectionSocketFactoryInjectedTypeConfiguration));
        }

        final InjectedTypeConfiguration<? extends TlsTrustManagersProvider>
                tlsTrustManagersProviderInjectedTypeConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getTlsTrustManagersProviderInjectedTypeConfiguration();
        if (tlsTrustManagersProviderInjectedTypeConfiguration != null) {
            apacheHttpClientBuilder.tlsTrustManagersProvider(
                    AppbaseUtil.injectedInstance(guiceInjectorService, tlsTrustManagersProviderInjectedTypeConfiguration));
        }

        final AwsSdkApacheProxyConfiguration awsSdkApacheProxyConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getAwsSdkApacheProxyConfiguration();
        if (awsSdkApacheProxyConfiguration != null) {
            apacheHttpClientBuilder.proxyConfiguration(proxyConfiguration(awsSdkApacheProxyConfiguration));
        }

        final AwsSdkTlsKeyManagersProviderConfiguration awsSdkTlsKeyManagersProviderConfiguration =
                defaultAwsSdkHttpClientIntegrationServiceConfiguration.getAwsSdkTlsKeyManagersProviderConfiguration();
        if (awsSdkTlsKeyManagersProviderConfiguration != null) {
            final AwsSdkTlsKeyManagersProviderFactory awsSdkTlsKeyManagersProviderFactory =
                    new DefaultAwsSdkTlsKeyManagersProviderFactory(awsSdkTlsKeyManagersProviderConfiguration);
            final TlsKeyManagersProvider tlsKeyManagersProvider = awsSdkTlsKeyManagersProviderFactory.getInstance();
            apacheHttpClientBuilder.tlsKeyManagersProvider(tlsKeyManagersProvider);
        }

        final SdkHttpClient sdkHttpClient = apacheHttpClientBuilder.build();
        sdkHttpClientRef.set(sdkHttpClient);
    }

    private ProxyConfiguration proxyConfiguration(final AwsSdkApacheProxyConfiguration awsSdkApacheProxyConfiguration) {
        final ProxyConfiguration.Builder builder = ProxyConfiguration.builder();

        final Set<String> nonProxyHosts = awsSdkApacheProxyConfiguration.getNonProxyHosts();
        if (nonProxyHosts != null) {
            builder.nonProxyHosts(nonProxyHosts);
        }

        final URI endpoint = awsSdkApacheProxyConfiguration.getEndpoint();
        if (endpoint != null) {
            builder.endpoint(endpoint);
        }

        final String username = awsSdkApacheProxyConfiguration.getUsername();
        if (username != null) {
            builder.username(username);
        }

        final String password = awsSdkApacheProxyConfiguration.getPassword();
        if (password != null) {
            builder.password(password);
        }

        final String ntlmDomain = awsSdkApacheProxyConfiguration.getNtlmDomain();
        if (ntlmDomain != null) {
            builder.ntlmDomain(ntlmDomain);
        }

        final String ntlmWorkstation = awsSdkApacheProxyConfiguration.getNtlmWorkstation();
        if (ntlmWorkstation != null) {
            builder.ntlmWorkstation(ntlmWorkstation);
        }

        final Boolean preemptiveBasicAuthenticationEnabled =
                awsSdkApacheProxyConfiguration.getPreemptiveBasicAuthenticationEnabled();
        if (preemptiveBasicAuthenticationEnabled != null) {
            builder.preemptiveBasicAuthenticationEnabled(preemptiveBasicAuthenticationEnabled);
        }

        final Boolean useSystemPropertyValues = awsSdkApacheProxyConfiguration.getUseSystemPropertyValues();
        if (useSystemPropertyValues != null) {
            builder.useSystemPropertyValues(useSystemPropertyValues);
        }

        return builder.build();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultAwsSdkHttpClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final SdkHttpClient sdkHttpClient = sdkHttpClientRef.get();
        if (sdkHttpClient != null) {
            sdkHttpClient.close();
        }

        sdkHttpClientRef.set(null);
    }

    @Override
    public SdkHttpClient getClient() {
        return sdkHttpClientRef.get();
    }

    @Override
    public boolean isHealthy() {
        final SdkHttpClient sdkHttpClient = getClient();
        return sdkHttpClient != null;
    }
}
