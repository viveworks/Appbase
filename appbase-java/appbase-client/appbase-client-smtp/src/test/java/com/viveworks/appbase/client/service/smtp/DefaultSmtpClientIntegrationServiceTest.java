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
import com.hubspot.smtp.client.SmtpClientResponse;
import com.hubspot.smtp.client.SmtpSession;
import com.viveworks.appbase.client.core.AppbaseClientCore;
import com.viveworks.appbase.client.core.AppbaseClientException;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.client.service.smtp.configuration.DefaultSmtpClientIntegrationServiceConfiguration;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpAuthConfiguration;
import com.viveworks.appbase.client.service.smtp.configuration.SmtpAuthMechanism;
import io.netty.handler.codec.smtp.DefaultSmtpResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SEC-2 / AUD-07 (CWE-319, cleartext transmission of credentials): {@code initImpl()} started TLS
 * only "if configured" ({@code isStartTls()}) and then unconditionally ran
 * {@code authenticateIfConfigured}, with no check that TLS had actually started before sending
 * SMTP {@code AUTH}. An operator who configured {@code smtpAuthConfiguration} (credentials) but
 * left {@code startTls} unset — or any future caller of the gate logic under the same
 * misconfiguration — would transmit credentials in cleartext with no error.
 *
 * <p>{@link DefaultSmtpClientIntegrationService#refuseAuthenticationWithoutTls} is extracted as a
 * pure, package-private static check specifically so this is testable without a live SMTP
 * server or network I/O — this module's other test classes are all gated behind
 * {@code @EnabledIfSystemProperty} and need a live backend; this one needs neither.
 *
 * <p>AUD-502 delta: the gate itself was covered, but the brief's named coverage —
 * {@code authenticateIfConfigured} mechanism dispatch, {@code startTlsIfConfigured}, and the
 * ordering "no credentials before TLS" — was only javadoc-deep. The service now has a
 * package-private test-seam constructor (configuration + stubbed {@link SmtpSession}, no
 * {@code init()}), and these tests drive the real dispatch code against a Mockito stub:
 * LOGIN/PLAIN/X_OAUTH2 each reach the matching {@code session.auth*} method with the configured
 * credentials and no other; a capability the server never advertised is refused before anything
 * is sent; and when auth is configured but TLS did not start, <em>zero</em> methods are invoked
 * on the session — the credentials never leave the service.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class DefaultSmtpClientIntegrationServiceTest {
    private static final String USERNAME = "support.devenv.local";
    private static final String SECRET = "correct horse battery staple";

    @Test
    void refuseAuthenticationWithoutTls_authConfiguredButTlsNotStarted_throws() {
        assertThrows(AppbaseClientException.class,
                () -> DefaultSmtpClientIntegrationService.refuseAuthenticationWithoutTls(true, false),
                "auth must never be allowed to proceed over a connection that did not start TLS");
    }

    @Test
    void refuseAuthenticationWithoutTls_authConfiguredAndTlsStarted_doesNotThrow() {
        assertDoesNotThrow(
                () -> DefaultSmtpClientIntegrationService.refuseAuthenticationWithoutTls(true, true));
    }

    @Test
    void refuseAuthenticationWithoutTls_authNotConfigured_doesNotThrowRegardlessOfTls() {
        assertDoesNotThrow(
                () -> DefaultSmtpClientIntegrationService.refuseAuthenticationWithoutTls(false, false));
        assertDoesNotThrow(
                () -> DefaultSmtpClientIntegrationService.refuseAuthenticationWithoutTls(false, true));
    }

    @Test
    void authenticateIfConfigured_loginDispatchesToAuthLoginWithTheConfiguredCredentials()
            throws Exception
    {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service =
                authService(SmtpAuthMechanism.LOGIN, smtpSession);

        service.authenticateIfConfigured(smtpSession, ehloAdvertising("LOGIN", "PLAIN", "XOAUTH2"), true);

        verify(smtpSession).authLogin(USERNAME, SECRET);
        verify(smtpSession, never()).authPlain(anyString(), anyString());
        verify(smtpSession, never()).authXoauth2(anyString(), anyString());
    }

    @Test
    void authenticateIfConfigured_plainDispatchesToAuthPlainWithTheConfiguredCredentials()
            throws Exception
    {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service =
                authService(SmtpAuthMechanism.PLAIN, smtpSession);

        service.authenticateIfConfigured(smtpSession, ehloAdvertising("LOGIN", "PLAIN", "XOAUTH2"), true);

        verify(smtpSession).authPlain(USERNAME, SECRET);
        verify(smtpSession, never()).authLogin(anyString(), anyString());
        verify(smtpSession, never()).authXoauth2(anyString(), anyString());
    }

    @Test
    void authenticateIfConfigured_xoauth2DispatchesToAuthXoauth2WithTheConfiguredCredentials()
            throws Exception
    {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service =
                authService(SmtpAuthMechanism.X_OAUTH2, smtpSession);

        service.authenticateIfConfigured(smtpSession, ehloAdvertising("LOGIN", "PLAIN", "XOAUTH2"), true);

        verify(smtpSession).authXoauth2(USERNAME, SECRET);
        verify(smtpSession, never()).authLogin(anyString(), anyString());
        verify(smtpSession, never()).authPlain(anyString(), anyString());
    }

    @Test
    void authenticateIfConfigured_authConfiguredButTlsNotStarted_sendsNoCredentials() {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service =
                authService(SmtpAuthMechanism.PLAIN, smtpSession);

        assertThrows(AppbaseClientException.class,
                () -> service.authenticateIfConfigured(smtpSession, ehloAdvertising("PLAIN"), false),
                "the gate must fire before any AUTH command leaves the service");

        verifyNoInteractions(smtpSession);
    }

    @Test
    void authenticateIfConfigured_noAuthConfigured_neverTouchesTheSessionRegardlessOfTls()
            throws Exception
    {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service = service(
                DefaultSmtpClientIntegrationServiceConfiguration.builder()
                        .smtpAuthConfiguration(null)
                        .build(),
                smtpSession);

        assertDoesNotThrow(
                () -> service.authenticateIfConfigured(smtpSession, ehloAdvertising("PLAIN"), false));

        verifyNoInteractions(smtpSession);
    }

    @Test
    void authenticateIfConfigured_loginNotAdvertisedByServer_refusesWithoutSending() {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service =
                authService(SmtpAuthMechanism.LOGIN, smtpSession);

        assertThrows(AppbaseClientException.class,
                () -> service.authenticateIfConfigured(smtpSession, ehloAdvertising("PLAIN"), true),
                "a mechanism the server never advertised must be refused before any AUTH command");

        verifyNoInteractions(smtpSession);
    }

    @Test
    void authenticateIfConfigured_xoauth2WithNoAuthLineAtAll_refusesWithoutSending() {
        final SmtpSession smtpSession = authSessionStub();
        final DefaultSmtpClientIntegrationService service =
                authService(SmtpAuthMechanism.X_OAUTH2, smtpSession);

        assertThrows(AppbaseClientException.class,
                () -> service.authenticateIfConfigured(smtpSession, ehloWithoutAuth(), true),
                "a server that advertised no AUTH capability must not receive credentials");

        verifyNoInteractions(smtpSession);
    }

    @Test
    void startTlsIfConfigured_configured_startsTlsOnTheSessionAndReportsTrue() throws Exception {
        final SmtpSession smtpSession = mock(SmtpSession.class);
        when(smtpSession.startTls())
                .thenReturn(CompletableFuture.completedFuture(successResponse(smtpSession)));
        final DefaultSmtpClientIntegrationService service = service(
                DefaultSmtpClientIntegrationServiceConfiguration.builder()
                        .startTls(true)
                        .build(),
                smtpSession);

        assertTrue(service.startTlsIfConfigured(smtpSession),
                "a successful STARTTLS must report true so auth is allowed to proceed");

        verify(smtpSession).startTls();
    }

    @Test
    void startTlsIfConfigured_notConfigured_neverTouchesTheSessionAndReportsFalse() throws Exception {
        final SmtpSession smtpSession = mock(SmtpSession.class);
        final DefaultSmtpClientIntegrationService service = service(
                DefaultSmtpClientIntegrationServiceConfiguration.builder().build(),
                smtpSession);

        assertFalse(service.startTlsIfConfigured(smtpSession),
                "STARTTLS unset must report false -- which is what makes the AUD-07 gate refuse auth");

        verify(smtpSession, never()).startTls();
    }

    private static DefaultSmtpClientIntegrationService authService(
            final SmtpAuthMechanism smtpAuthMechanism,
            final SmtpSession smtpSession)
    {
        return service(DefaultSmtpClientIntegrationServiceConfiguration.builder()
                .smtpAuthConfiguration(SmtpAuthConfiguration.builder()
                        .smtpAuthMechanism(smtpAuthMechanism)
                        .username(USERNAME)
                        .passwordOrAccessToken(SECRET)
                        .build())
                .build(),
                smtpSession);
    }

    private static DefaultSmtpClientIntegrationService service(
            final DefaultSmtpClientIntegrationServiceConfiguration configuration,
            final SmtpSession smtpSession)
    {
        // Deep stubs: the superclass constructor chains
        // core.getAppbaseCore().getLogService().getLogger(...) to build its logger.
        return new DefaultSmtpClientIntegrationService(
                mock(AppbaseClientCore.class, RETURNS_DEEP_STUBS),
                clientPool(),
                clientKey(),
                configuration,
                smtpSession);
    }

    private static SmtpSession authSessionStub() {
        final SmtpSession smtpSession = mock(SmtpSession.class);
        when(smtpSession.authLogin(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResponse(smtpSession)));
        when(smtpSession.authPlain(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResponse(smtpSession)));
        when(smtpSession.authXoauth2(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResponse(smtpSession)));
        return smtpSession;
    }

    // A real EhloResponse is parsed (not stubbed) so the dispatch tests also exercise the
    // hubspot capability parser; this is only possible because the pom pins a test-scoped Guava
    // that still has the CharMatcher.WHITESPACE field EhloResponse's static initializer needs.
    private static EhloResponse ehloAdvertising(final String... authMechanisms) {
        return EhloResponse.parse(
                "mail.devenv.local",
                Collections.singletonList("AUTH " + String.join(" ", authMechanisms)));
    }

    private static EhloResponse ehloWithoutAuth() {
        return EhloResponse.parse(
                "mail.devenv.local",
                Collections.singletonList("SIZE 52428800"));
    }

    private static SmtpClientResponse successResponse(final SmtpSession smtpSession) {
        return new SmtpClientResponse(smtpSession, new DefaultSmtpResponse(235));
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientPool<SmtpSession, TextValue> clientPool() {
        return mock(AppbaseClientPool.class);
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientKey<TextValue> clientKey() {
        return mock(AppbaseClientKey.class);
    }
}
