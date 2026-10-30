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

package com.viveworks.appbase.client.service.smtp;

import com.hubspot.smtp.client.EhloResponse;
import com.hubspot.smtp.client.Extension;
import com.hubspot.smtp.client.SendInterceptor;
import com.hubspot.smtp.client.SmtpClientResponse;
import com.hubspot.smtp.client.SmtpSession;
import com.hubspot.smtp.client.SmtpSessionConfig;
import com.hubspot.smtp.client.SmtpSessionFactory;
import com.hubspot.smtp.client.SmtpSessionFactoryConfig;
import com.viveworks.appbase.client.core.AbstractAppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.smtp.configuration.DefaultSmtpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpAuthConfiguration;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpAuthMechanism;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpExtension;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpSessionConfiguration;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpSessionFactoryConfiguration;
import com.viveworks.appbase.client.util.AppbaseClientUtil;
import com.viveworks.appbase.client.util.BlockingOperationConfiguration;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceConfiguration;
import com.viveworks.appbase.core.service.mediator.executor.ExecutorServiceFactory;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.NullableWhenUnused;
import com.viveworks.appbase.core.util.di.InjectedTypeConfiguration;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import com.viveworks.appbase.core.util.net.HostPortConfiguration;
import com.viveworks.appbase.core.util.net.TimeoutConfiguration;
import com.viveworks.appbase.core.util.net.buffer.BufferAllocator;
import com.viveworks.appbase.core.util.net.buffer.BufferAllocatorConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupConfiguration;
import com.viveworks.appbase.core.util.net.eventloopgroup.EventLoopGroupFactory;
import com.viveworks.appbase.core.util.net.ssl.SslClientContextConfiguration;
import com.viveworks.appbase.core.util.net.ssl.SslClientContextFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.smtp.DefaultSmtpRequest;
import io.netty.handler.codec.smtp.SmtpCommand;
import io.netty.handler.codec.smtp.SmtpRequest;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of {@link SmtpClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultSmtpClientIntegrationService
        extends AbstractAppbaseClientIntegrationService
        <SmtpSession, TextValue, DefaultSmtpClientIntegrationServiceConfiguration>
        implements SmtpClientIntegrationService
{
    private final AtomicReference<SmtpSession> smtpSessionRef;

    @NullableWhenUnused(info = "init() / repair() will fail. The dependencies must be set using one of the constructors")
    private final DefaultSmtpClientIntegrationServiceConfiguration defaultSmtpClientIntegrationServiceConfiguration;


    public DefaultSmtpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SmtpSession, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final SmtpSession smtpSession)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SmtpSession.class);

        AppbaseUtil.notNull(smtpSession, "smtpSession");

        this.smtpSessionRef = new AtomicReference<>(smtpSession);
        this.defaultSmtpClientIntegrationServiceConfiguration = null;
    }

    public DefaultSmtpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SmtpSession, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultSmtpClientIntegrationServiceConfiguration defaultSmtpClientIntegrationServiceConfiguration)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SmtpSession.class);

        AppbaseUtil.notNull(
                defaultSmtpClientIntegrationServiceConfiguration, "defaultSmtpClientIntegrationServiceConfiguration");

        this.smtpSessionRef = new AtomicReference<>();
        this.defaultSmtpClientIntegrationServiceConfiguration = defaultSmtpClientIntegrationServiceConfiguration;

        try {
            super.init();
        } catch (AppbaseClientException ace) {
            throw new AppbaseRuntimeException("Error initializing " + getClass().getName(), ace);
        }
    }

    /**
     * Offline unit-test seam (AUD-502): constructs the service with both a configuration and a
     * caller-supplied {@link SmtpSession} and deliberately skips {@code init()} -- no connection,
     * EHLO, STARTTLS or AUTH is attempted, so tests can drive
     * {@link #startTlsIfConfigured} / {@link #authenticateIfConfigured} against a stubbed
     * session without a live SMTP server. Package-private so only tests in this package can
     * reach it; production callers must use the two public constructors above.
     *
     * @param appbaseClientCore the client core
     * @param appbaseClientPool the client pool
     * @param appbaseClientKey the client key
     * @param defaultSmtpClientIntegrationServiceConfiguration the service configuration
     * @param smtpSession the pre-existing session the service wraps
     */
    DefaultSmtpClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<SmtpSession, TextValue> appbaseClientPool,
            final AppbaseClientKey<TextValue> appbaseClientKey,
            final DefaultSmtpClientIntegrationServiceConfiguration defaultSmtpClientIntegrationServiceConfiguration,
            final SmtpSession smtpSession)
    {
        super(appbaseClientCore, appbaseClientPool, appbaseClientKey, SmtpSession.class);

        AppbaseUtil.notNull(
                defaultSmtpClientIntegrationServiceConfiguration, "defaultSmtpClientIntegrationServiceConfiguration");
        AppbaseUtil.notNull(smtpSession, "smtpSession");

        this.smtpSessionRef = new AtomicReference<>(smtpSession);
        this.defaultSmtpClientIntegrationServiceConfiguration = defaultSmtpClientIntegrationServiceConfiguration;
    }

    @Override
    @SuppressWarnings("checkstyle:MethodLength")
    protected void initImpl() throws AppbaseClientException {
        AppbaseUtil.notNull(
                defaultSmtpClientIntegrationServiceConfiguration, "defaultSmtpClientIntegrationServiceConfiguration");

        final SmtpSessionFactory smtpSessionFactory = smtpSessionFactory();
        final SmtpSessionConfig smtpSessionConfig = smtpSessionConfig(smtpSessionFactory);
        final SmtpSession smtpSession = smtpSession(smtpSessionFactory, smtpSessionConfig);
        final EhloResponse ehloResponse = sendEhloRequest(smtpSession);
        final boolean tlsStarted = startTlsIfConfigured(smtpSession);
        authenticateIfConfigured(smtpSession, ehloResponse, tlsStarted);

        this.smtpSessionRef.set(smtpSession);
    }

    /**
     * SEC-2 / AUD-07 (CWE-319, cleartext transmission of credentials): authentication must never
     * proceed unless TLS has actually started on this connection -- "configured" is not
     * "started". Extracted as a pure, package-private static check so it is unit-testable
     * without a live SMTP server or any network I/O.
     *
     * @param authConfigured whether {@code smtpAuthConfiguration} is set (i.e. SMTP AUTH would
     *                        otherwise be attempted)
     * @param tlsStarted      whether {@link #startTlsIfConfigured} actually started TLS on this
     *                        connection ({@code false} when {@code isStartTls()} was unset)
     * @throws AppbaseClientException when authentication is configured but TLS did not start
     */
    static void refuseAuthenticationWithoutTls(final boolean authConfigured, final boolean tlsStarted)
            throws AppbaseClientException
    {
        if (authConfigured && !tlsStarted) {
            throw new AppbaseClientException(
                    "Refusing SMTP AUTH over a connection that has not started TLS -- "
                            + "sending credentials would transmit them in cleartext. "
                            + "Enable startTls in the SMTP client configuration to use SMTP AUTH.");
        }
    }

    /**
     * Sends SMTP AUTH over {@code smtpSession} when credentials are configured. Package-private
     * (AUD-502), like {@link #refuseAuthenticationWithoutTls} above, so the mechanism dispatch
     * and the no-credentials-before-TLS ordering are unit-testable against a stubbed session
     * without a live SMTP server.
     */
    void authenticateIfConfigured(
            final SmtpSession smtpSession,
            final EhloResponse ehloResponse,
            final boolean tlsStarted)
            throws AppbaseClientException
    {
        AppbaseUtil.notNull(smtpSession, "smtpSession");
        AppbaseUtil.notNull(ehloResponse, "ehloResponse");

        final SmtpAuthConfiguration smtpAuthConfiguration =
                defaultSmtpClientIntegrationServiceConfiguration.getSmtpAuthConfiguration();
        refuseAuthenticationWithoutTls(smtpAuthConfiguration != null, tlsStarted);
        if (smtpAuthConfiguration != null) {
            final SmtpAuthMechanism smtpAuthMechanism = smtpAuthConfiguration.getSmtpAuthMechanism();

            final CompletableFuture<SmtpClientResponse> authResponseCompletableFuture;
            switch (smtpAuthMechanism) {
                case LOGIN:
                    if (!ehloResponse.isAuthLoginSupported()) {
                        throw new AppbaseClientException("LOGIN account is not supported by the SMTP server");
                    }
                    logger.info("Authenticating using LOGIN");
                    authResponseCompletableFuture = smtpSession.authLogin(
                            smtpAuthConfiguration.getUsername(),
                            smtpAuthConfiguration.getPasswordOrAccessToken());
                    break;
                case PLAIN:
                    if (!ehloResponse.isAuthPlainSupported()) {
                        throw new AppbaseClientException("PLAIN account is not supported by the SMTP server");
                    }
                    logger.info("Authenticating using PLAIN");
                    authResponseCompletableFuture = smtpSession.authPlain(
                            smtpAuthConfiguration.getUsername(),
                            smtpAuthConfiguration.getPasswordOrAccessToken());
                    break;
                case X_OAUTH2:
                    if (!ehloResponse.isAuthXoauth2Supported()) {
                        throw new AppbaseClientException("X_OAUTH2 account is not supported by the SMTP server");
                    }
                    logger.info("Authenticating using X_OAUTH2");
                    authResponseCompletableFuture = smtpSession.authXoauth2(
                            smtpAuthConfiguration.getUsername(),
                            smtpAuthConfiguration.getPasswordOrAccessToken());
                    break;
                default:
                    throw new AppbaseClientException("Unsupported SMTP auth mechanism: " + smtpAuthMechanism);
            }

            processClientResponse(
                    authResponseCompletableFuture,
                    defaultSmtpClientIntegrationServiceConfiguration.getAuthBlockingOperationConfiguration());
        }
    }

    /**
     * @return {@code true} when TLS was actually started on {@code smtpSession} on this call,
     *         {@code false} when {@code isStartTls()} was unset and nothing was attempted. A
     *         failed start throws (via {@code processClientResponse}) rather than returning
     *         {@code false} -- a negotiation failure must abort {@code initImpl}, not silently
     *         fall through to {@link #authenticateIfConfigured} as though TLS were still pending.
     *         Package-private (AUD-502) so the dispatch is unit-testable with a stubbed session.
     */
    boolean startTlsIfConfigured(final SmtpSession smtpSession) throws SmtpClientResponseException {
        AppbaseUtil.notNull(smtpSession, "smtpSession");

        if (defaultSmtpClientIntegrationServiceConfiguration.isStartTls()) {
            logger.info("Starting TLS");
            final CompletableFuture<SmtpClientResponse> startTlsResponseCompletableFuture = smtpSession.startTls();
            final BlockingOperationConfiguration startTlsBlockingOperationConfiguration =
                    defaultSmtpClientIntegrationServiceConfiguration.getStartTlsBlockingOperationConfiguration();
            processClientResponse(startTlsResponseCompletableFuture, startTlsBlockingOperationConfiguration);
            return true;
        }
        return false;
    }

    private EhloResponse sendEhloRequest(final SmtpSession smtpSession) throws SmtpClientResponseException {
        AppbaseUtil.notNull(smtpSession, "smtpSession");

        final String senderDomainName =
                defaultSmtpClientIntegrationServiceConfiguration.getSenderDomainName();
        logger.info("Sending EHLO request using domain name={}", senderDomainName);

        final SmtpRequest ehloRequest = new DefaultSmtpRequest(SmtpCommand.EHLO, senderDomainName);
        final CompletableFuture<SmtpClientResponse> ehloResponseCompletableFuture = smtpSession.send(ehloRequest);
        final BlockingOperationConfiguration ehloRequestBlockingOperationConfiguration =
                defaultSmtpClientIntegrationServiceConfiguration.getEhloRequestBlockingOperationConfiguration();
        processClientResponse(ehloResponseCompletableFuture, ehloRequestBlockingOperationConfiguration);

        final EhloResponse ehloResponse = smtpSession.getEhloResponse();
        AppbaseUtil.notNull(ehloResponse, "ehloResponse");

        logger.debug("SMTP EHLO Response: [" +
                "domain={}, " +
                "maxMessageSize={}, " +
                "supportedExtensions={}, " +
                "isAuthLoginSupported={}, " +
                "isAuthPlainSupported={}, " +
                "isAuthXoauth2Supported={}, " +
                "]",
                ehloResponse.getEhloDomain(),
                ehloResponse.getMaxMessageSize(),
                ehloResponse.getSupportedExtensions(),
                ehloResponse.isAuthLoginSupported(),
                ehloResponse.isAuthPlainSupported(),
                ehloResponse.isAuthXoauth2Supported());

        return ehloResponse;
    }

    private SmtpSession smtpSession(
            final SmtpSessionFactory smtpSessionFactory,
            final SmtpSessionConfig smtpSessionConfig)
            throws SmtpClientResponseException
    {
        AppbaseUtil.notNull(smtpSessionFactory, "smtpSessionFactory");
        AppbaseUtil.notNull(smtpSessionConfig, "smtpSessionConfig");

        logger.info("Connecting to SMTP server using configuration: {}", smtpSessionConfig);

        final CompletableFuture<SmtpClientResponse> connectResponseCompletableFuture =
                smtpSessionFactory.connect(smtpSessionConfig);
        final BlockingOperationConfiguration connectBlockingOperationConfiguration =
                defaultSmtpClientIntegrationServiceConfiguration.getConnectBlockingOperationConfiguration();
        final SmtpClientResponse connectResponse =
                processClientResponse(connectResponseCompletableFuture, connectBlockingOperationConfiguration);

        final SmtpSession smtpSession = connectResponse.getSession();
        AppbaseUtil.notNull(smtpSession, "smtpSession");

        return smtpSession;
    }

    private SmtpClientResponse processClientResponse(
            final CompletableFuture<SmtpClientResponse> smtpClientResponseCompletableFuture,
            final BlockingOperationConfiguration blockingOperationConfiguration)
            throws SmtpClientResponseException
    {
        AppbaseUtil.notNull(smtpClientResponseCompletableFuture, "smtpClientResponseCompletableFuture");
        AppbaseUtil.notNull(blockingOperationConfiguration, "blockingOperationConfiguration");

        final SmtpClientResponse smtpClientResponse = AppbaseClientUtil.waitUninterruptiblyAsConfigured(
                smtpClientResponseCompletableFuture, blockingOperationConfiguration);
        AppbaseUtil.notNull(smtpClientResponse, "smtpClientResponse");

        if (smtpClientResponse.containsError()) {
            throw new SmtpClientResponseException(smtpClientResponse.getResponses());
        }

        return smtpClientResponse;
    }

    private SmtpSessionFactory smtpSessionFactory() {
        final SmtpSessionFactoryConfiguration smtpSessionFactoryConfiguration =
                defaultSmtpClientIntegrationServiceConfiguration.getSmtpSessionFactoryConfiguration();
        AppbaseUtil.notNull(smtpSessionFactoryConfiguration, "smtpSessionFactoryConfiguration");

        final SmtpSessionFactoryConfig.Builder builder = SmtpSessionFactoryConfig.builder();

        builder.channelClass(smtpSessionFactoryConfiguration.getChannelType());

        if (smtpSessionFactoryConfiguration.isUseSharedExecutorServiceConfiguration()) {
            builder.executor(appbaseClientCore
                    .getAppbaseCore()
                    .getMediatorService()
                    .getMediator()
                    .getSharedExecutorService());
        } else {
            final ExecutorServiceConfiguration customExecutorServiceConfiguration =
                    smtpSessionFactoryConfiguration.getCustomExecutorServiceConfiguration();
            final ExecutorService executorService = ExecutorServiceFactory.newExecutorService(
                    customExecutorServiceConfiguration,
                    AppbaseUtil.defaultUncaughtExceptionHandler(logger));
            builder.executor(executorService);
        }

        final SslClientContextConfiguration sslClientContextConfiguration =
                smtpSessionFactoryConfiguration.getSslClientContextConfiguration();
        if (sslClientContextConfiguration != null) {
            final SslClientContextFactory sslClientContextFactory =
                    new SslClientContextFactory(sslClientContextConfiguration);
            final SslContext sslContext = sslClientContextFactory.getInstance();
            builder.sslEngineSupplier(() -> sslContext.newEngine(ByteBufAllocator.DEFAULT));
        }

        return new SmtpSessionFactory(builder.build());
    }

    private SmtpSessionConfig smtpSessionConfig(final SmtpSessionFactory smtpSessionFactory) {
        AppbaseUtil.notNull(smtpSessionFactory, "smtpSessionFactory");

        final SmtpSessionConfiguration smtpSessionConfiguration =
                defaultSmtpClientIntegrationServiceConfiguration.getSmtpSessionConfiguration();
        AppbaseUtil.notNull(smtpSessionConfiguration, "smtpSessionConfiguration");


        final SmtpSessionConfig.Builder builder = SmtpSessionConfig.builder();


        final HostPortConfiguration localAddressHostPortConfiguration =
                smtpSessionConfiguration.getLocalAddressHostPortConfiguration();
        if (localAddressHostPortConfiguration != null) {
            final InetSocketAddress localAddress = InetSocketAddress.createUnresolved(
                    localAddressHostPortConfiguration.getHost(),
                    localAddressHostPortConfiguration.getPort());
            builder.localAddress(localAddress);
        }


        final HostPortConfiguration remoteAddressHostPortConfiguration =
                smtpSessionConfiguration.getRemoteAddressHostPortConfiguration();
        if (remoteAddressHostPortConfiguration != null) {
            final InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved(
                    remoteAddressHostPortConfiguration.getHost(),
                    remoteAddressHostPortConfiguration.getPort());
            builder.remoteAddress(remoteAddress);
        }


        final String connectionId = smtpSessionConfiguration.getConnectionId();
        if (connectionId != null) {
            builder.connectionId(connectionId);
        }


        final Set<SmtpExtension> disabledSmtpExtensions = smtpSessionConfiguration.getDisabledSmtpExtensions();
        if (disabledSmtpExtensions != null) {
            final Set<Extension> disabledExtensions = new HashSet<>(disabledSmtpExtensions.size());
            disabledSmtpExtensions.forEach(smtpExtension -> disabledExtensions.add(smtpExtension.getExtension()));
            builder.disabledExtensions(EnumSet.copyOf(disabledExtensions));
        }

        final TimeoutConfiguration initialResponseTimeoutConfiguration =
                smtpSessionConfiguration.getInitialResponseTimeoutConfiguration();
        if (initialResponseTimeoutConfiguration != null && initialResponseTimeoutConfiguration.isEnabled()) {
            final Duration duration = AppbaseClientUtil.duration(initialResponseTimeoutConfiguration);
            builder.initialResponseTimeout(duration);
        }


        final TimeoutConfiguration keepAliveTimeoutConfiguration =
                smtpSessionConfiguration.getKeepAliveTimeoutConfiguration();
        if (keepAliveTimeoutConfiguration != null && keepAliveTimeoutConfiguration.isEnabled()) {
            final Duration duration = AppbaseClientUtil.duration(keepAliveTimeoutConfiguration);
            builder.keepAliveTimeout(duration);
        }


        final TimeoutConfiguration readTimeoutConfiguration = smtpSessionConfiguration.getReadTimeoutConfiguration();
        if (readTimeoutConfiguration != null && readTimeoutConfiguration.isEnabled()) {
            final Duration duration = AppbaseClientUtil.duration(readTimeoutConfiguration);
            builder.readTimeout(duration);
        }


        final InjectedTypeConfiguration<SmtpExceptionHandler> smtpExceptionHandlerInjectedTypeConfiguration =
                smtpSessionConfiguration.getSmtpExceptionHandlerInjectedTypeConfiguration();
        if (smtpExceptionHandlerInjectedTypeConfiguration != null) {
            final SmtpExceptionHandler smtpExceptionHandler = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    smtpExceptionHandlerInjectedTypeConfiguration);
            builder.exceptionHandler(smtpExceptionHandler);
        } else {
            builder.exceptionHandler(throwable -> logger.error("Error in " + SmtpSession.class.getName(), throwable));
        }


        final InjectedTypeConfiguration<SendInterceptor> sendInterceptorInjectedTypeConfiguration =
                smtpSessionConfiguration.getSendInterceptorInjectedTypeConfiguration();
        if (sendInterceptorInjectedTypeConfiguration != null) {
            final SendInterceptor sendInterceptor = AppbaseUtil.injectedInstance(
                    appbaseClientCore.getAppbaseCore().getGuiceInjectorService(),
                    sendInterceptorInjectedTypeConfiguration);
            builder.sendInterceptor(sendInterceptor);
        }


        return builder.build();
    }

    @Override
    protected void repairImpl(final RepairListener repairListener) throws AppbaseClientException {
        defaultRepairImpl(repairListener, defaultSmtpClientIntegrationServiceConfiguration);
    }

    @Override
    protected void shutdownImpl() {
        final SmtpSession smtpSession = getClient();
        if (smtpSession != null) {
            final CompletableFuture<Void> close = smtpSession.close();

            if (defaultSmtpClientIntegrationServiceConfiguration != null) {
                AppbaseClientUtil.waitAsConfigured(
                        close, defaultSmtpClientIntegrationServiceConfiguration.getCloseBlockingOperationConfiguration());
            }
        }

        this.smtpSessionRef.set(null);
    }

    @Override
    public SmtpSession getClient() {
        return smtpSessionRef.get();
    }

    @Override
    public boolean isHealthy() {
        final SmtpSession smtpSession = getClient();
        return smtpSession != null;
    }
}
