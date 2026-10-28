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
package com.viveworks.appbase.service.user.account;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.cluster.ClusterService;
import com.viveworks.appbase.service.id.IdService;
import com.viveworks.appbase.service.password.HashAlgorithm;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.AfterIdpCreateUserWithEmailPasswordRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpEmailPasswordSignInRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentRequest;
import com.viveworks.appbase.service.user.account.data.AfterIdpPasswordResetEmailSentResponse;
import com.viveworks.appbase.service.user.account.data.UpdateAccountRequest;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import com.viveworks.appbase.service.user.account.store.UserAccountStore;
import com.viveworks.appbase.service.user.account.store.UserAccountStoreException;
import com.viveworks.appbase.service.user.account.web.endpoint.jaxrs.UserAccountJaxrsResource;
import com.viveworks.appbase.service.user.session.data.UserSession;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStoreException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.Logger;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Account-flow regressions using in-memory collaborators, not an external IdP or store.
 * @author Navaneeth Mysore G
 * @author Claude
 */
class DefaultUserAccountServiceFlowTest {
    private final UserAccountStore accountStore = mock(UserAccountStore.class);
    private final UserSessionStore sessionStore = mock(UserSessionStore.class);
    private final IdpTokenVerifier tokenVerifier = mock(IdpTokenVerifier.class);
    private DefaultUserAccountService service;

    @BeforeEach
    void setUp() throws Exception {
        final LogService logService = mock(LogService.class);
        when(logService.getLogger(any())).thenReturn(mock(Logger.class));
        final MetricService metricService = mock(MetricService.class);
        final MetricRegistry metricRegistry = mock(MetricRegistry.class);
        final Timer timer = mock(Timer.class);
        when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        when(metricRegistry.timer(nullable(String.class))).thenReturn(timer);
        when(metricRegistry.counter(nullable(String.class))).thenReturn(mock(Counter.class));
        when(timer.time()).thenReturn(mock(Timer.Context.class));
        service = new DefaultUserAccountService(
                logService, metricService, mock(MediatorService.class, RETURNS_DEEP_STUBS),
                mock(ValidationService.class),
                mock(ServiceRegistry.class), mock(IdService.class), mock(ClusterService.class),
                mock(AuthorizationService.class), sessionStore, accountStore,
                tokenVerifier, mock(PasswordService.class),
                new DefaultUserAccountServiceConfiguration());
        when(tokenVerifier.isUserTokenVerificationEnabled()).thenReturn(true);
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("idp-1").issuer("issuer").name("User")
                .email("user@example.test").emailVerified(false).build());
    }

    @Test
    void signInRejectsTokenForAnotherUidBeforeAccountMutationOrSession() throws Exception {
        when(accountStore.readByIdpUserId("idp-1")).thenReturn(Optional.of(account()));
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("attacker-idp").issuer("issuer").name("Attacker")
                .email("attacker@example.test").emailVerified(true).build());
        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", clientHash(), "idp-1", "token",
                "app", "en", null);

        assertThrows(AuthenticationServiceException.class, () -> service.afterIdpEmailPasswordSignIn(request));
        verify(accountStore, never()).updateUserAccount(any());
        verify(sessionStore, never()).createSession(any());
    }

    @Test
    void signInFailsClosedWhenTokenVerificationDisabled() throws Exception {
        when(tokenVerifier.isUserTokenVerificationEnabled()).thenReturn(false);
        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", clientHash(), "idp-1", "token",
                "app", "en", null);

        assertThrows(AuthenticationServiceException.class, () -> service.afterIdpEmailPasswordSignIn(request));
        verify(accountStore, never()).updateUserAccount(any());
        verify(sessionStore, never()).createSession(any());
    }

    @Test
    void signInRejectsTokenForAnotherEmailBeforeSession() throws Exception {
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("idp-1").issuer("issuer").name("User")
                .email("other@example.test").emailVerified(true).build());
        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", clientHash(), "idp-1", "token",
                "app", "en", null);

        assertThrows(AuthenticationServiceException.class, () -> service.afterIdpEmailPasswordSignIn(request));
        verify(accountStore, never()).updateUserAccount(any());
        verify(sessionStore, never()).createSession(any());
    }

    @Test
    void createUserRejectsTokenForAnotherUidBeforePreclaim() throws Exception {
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("attacker-idp").issuer("issuer").name("Attacker")
                .email("user@example.test").emailVerified(false).build());
        final AfterIdpCreateUserWithEmailPasswordRequest request =
                new AfterIdpCreateUserWithEmailPasswordRequest("trace", "user-1", "idp-1", "token",
                        "User", "user@example.test", clientHash(), "en", "app", null);

        assertThrows(AuthenticationServiceException.class,
                () -> service.afterIdpCreateUserWithEmailPassword(request));
        verify(accountStore, never()).createUserAccount(any());
        verify(sessionStore, never()).createSession(any());
    }

    @Test
    void createUserRejectsTokenForAnotherEmailBeforePreclaim() throws Exception {
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("idp-1").issuer("issuer").name("User")
                .email("other@example.test").emailVerified(true).build());
        final AfterIdpCreateUserWithEmailPasswordRequest request =
                new AfterIdpCreateUserWithEmailPasswordRequest("trace", "user-1", "idp-1", "token",
                        "User", "user@example.test", clientHash(), "en", "app", null);

        assertThrows(AuthenticationServiceException.class,
                () -> service.afterIdpCreateUserWithEmailPassword(request));
        verify(accountStore, never()).createUserAccount(any());
        verify(sessionStore, never()).createSession(any());
    }

    @Test
    void createUserFailsClosedWhenTokenVerificationDisabled() throws Exception {
        when(tokenVerifier.isUserTokenVerificationEnabled()).thenReturn(false);
        final AfterIdpCreateUserWithEmailPasswordRequest request =
                new AfterIdpCreateUserWithEmailPasswordRequest("trace", "user-1", "idp-1", "token",
                        "User", "user@example.test", clientHash(), "en", "app", null);

        assertThrows(AuthenticationServiceException.class,
                () -> service.afterIdpCreateUserWithEmailPassword(request));
        verify(accountStore, never()).createUserAccount(any());
    }

    @Test
    void signInWithResetEmailFlagStillCreatesSession() throws Exception {
        final UserAccount account = account();
        account.setPasswordResetEmailSent(true);
        when(accountStore.readByIdpUserId("idp-1")).thenReturn(Optional.of(account));
        persistAccountChanges(account);
        when(sessionStore.generateSessionId()).thenReturn("session-new");

        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", clientHash(), "idp-1", "token",
                "app", "en", null);
        final UserSession signedIn = service.afterIdpEmailPasswordSignIn(request).getUserSession();
        assertNotNull(signedIn);
        assertEquals(false, signedIn.getUserAccount().getPasswordResetEmailSent());

        final ArgumentCaptor<UserAccount> updated = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountStore).updateUserAccount(updated.capture());
        assertEquals(false, updated.getValue().getPasswordResetEmailSent());
        verify(sessionStore).createSession(any(UserSession.class));
    }

    @Test
    void signInClearsResetCodeHash() throws Exception {
        final UserAccount account = account();
        account.setPasswordResetCodeHash(clientHash());
        when(accountStore.readByIdpUserId("idp-1")).thenReturn(Optional.of(account));
        persistAccountChanges(account);
        when(sessionStore.generateSessionId()).thenReturn("session-new");

        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", clientHash(), "idp-1", "token",
                "app", "en", null);
        final UserSession signedIn = service.afterIdpEmailPasswordSignIn(request).getUserSession();
        assertNotNull(signedIn);
        assertEquals(null, signedIn.getUserAccount().getPasswordResetCodeHash());

        final ArgumentCaptor<UserAccount> updated = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountStore).updateUserAccount(updated.capture());
        assertEquals(null, updated.getValue().getPasswordResetCodeHash());
    }

    @Test
    void signInWithChangedClientHashUpdatesAccount() throws Exception {
        final UserAccount account = account();
        when(accountStore.readByIdpUserId("idp-1")).thenReturn(Optional.of(account));
        persistAccountChanges(account);
        when(sessionStore.generateSessionId()).thenReturn("session-new");

        final PasswordHash changedHash = PasswordHash.builder().hash("changed-hash").salt("salt")
                .hashAlgorithm(HashAlgorithm.SHA512).shaRounds(80).build();
        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", changedHash, "idp-1", "token",
                "app", "en", null);
        final UserSession signedIn = service.afterIdpEmailPasswordSignIn(request).getUserSession();
        assertNotNull(signedIn);
        assertEquals(changedHash, signedIn.getUserAccount().getPasswordHashes().getClientPasswordHash());

        final ArgumentCaptor<UserAccount> updated = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountStore).updateUserAccount(updated.capture());
        assertEquals(changedHash, updated.getValue().getPasswordHashes().getClientPasswordHash());
    }

    @Test
    void signInWithoutMaintenanceCreatesSessionWithoutAccountWrite() throws Exception {
        final UserAccount account = account();
        when(accountStore.readByIdpUserId("idp-1")).thenReturn(Optional.of(account));
        when(sessionStore.generateSessionId()).thenReturn("session-new");

        final AfterIdpEmailPasswordSignInRequest request = new AfterIdpEmailPasswordSignInRequest(
                "trace", "user-1", "user@example.test", clientHash(), "idp-1", "token",
                "app", "en", null);
        assertNotNull(service.afterIdpEmailPasswordSignIn(request).getUserSession());
        verify(accountStore, never()).updateUserAccount(any());
        verify(sessionStore).createSession(any(UserSession.class));
    }

    @Test
    void resetEmailCallbackPersistsTrueAndFalseViaJaxrs() throws Exception {
        final UserAccount account = account();
        account.setName("Untouched");
        when(accountStore.readByEmail("user@example.test")).thenReturn(Optional.of(account));
        persistAccountChanges(account);
        final UserAccountJaxrsResource resource = new UserAccountJaxrsResource() {
            @Override
            protected UserAccountService userAccountService(final ServletContext servletContext) {
                return service;
            }
        };

        final AfterIdpPasswordResetEmailSentRequest enableRequest =
                new AfterIdpPasswordResetEmailSentRequest("trace-1", "spoofed-requester", "user@example.test", true);
        enableRequest.setIdpUserToken("token");
        final AfterIdpPasswordResetEmailSentRequest disableRequest =
                new AfterIdpPasswordResetEmailSentRequest("trace-2", "spoofed-requester", "user@example.test", false);
        disableRequest.setIdpUserToken("token");
        final Response enabled = resource.afterIdpPasswordResetEmailSent(enableRequest, "trace-1", null);
        final Response disabled = resource.afterIdpPasswordResetEmailSent(disableRequest, "trace-2", null);
        assertEquals(200, enabled.getStatus());
        assertEquals(200, disabled.getStatus());
        assertTrue(((AfterIdpPasswordResetEmailSentResponse) enabled.getEntity()).isUpdated());
        assertTrue(((AfterIdpPasswordResetEmailSentResponse) disabled.getEntity()).isUpdated());

        final ArgumentCaptor<UserAccount> updated = ArgumentCaptor.forClass(UserAccount.class);
        verify(accountStore, org.mockito.Mockito.times(2)).updateUserAccount(updated.capture());
        assertEquals(true, updated.getAllValues().get(0).getPasswordResetEmailSent());
        assertEquals(false, updated.getAllValues().get(1).getPasswordResetEmailSent());
        assertEquals("Untouched", updated.getAllValues().get(0).getName());
        assertEquals("Untouched", updated.getAllValues().get(1).getName());
        assertEquals(false, accountStore.readByAppbaseUserId("user-1").orElseThrow()
                .getPasswordResetEmailSent());
    }

    @Test
    void resetEmailCallbackRejectsMissingTokenBeforeAccountWrite() throws Exception {
        final UserAccount account = account();
        when(accountStore.readByEmail("user@example.test")).thenReturn(Optional.of(account));
        persistAccountChanges(account);
        final AfterIdpPasswordResetEmailSentRequest request =
                new AfterIdpPasswordResetEmailSentRequest("trace", "user-1", "user@example.test", true);

        assertThrows(Exception.class, () -> service.afterIdpPasswordResetEmailSent(request));
        verify(accountStore, never()).updateUserAccount(any());
    }

    @Test
    void resetEmailCallbackRejectsTokenForDifferentEmailBeforeAccountRead() throws Exception {
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("idp-1").issuer("issuer").name("User")
                .email("other@example.test").emailVerified(true).build());
        final AfterIdpPasswordResetEmailSentRequest request =
                new AfterIdpPasswordResetEmailSentRequest("trace", "user-1", "user@example.test", true);
        request.setIdpUserToken("token");

        assertThrows(AuthenticationServiceException.class,
                () -> service.afterIdpPasswordResetEmailSent(request));
        verify(accountStore, never()).readByEmail(any());
        verify(accountStore, never()).updateUserAccount(any());
    }

    @Test
    void resetEmailCallbackRejectsTokenForDifferentUidBeforeAccountWrite() throws Exception {
        when(tokenVerifier.verifyUserToken("token")).thenReturn(IdpToken.builder()
                .uid("attacker-idp").issuer("issuer").name("Attacker")
                .email("user@example.test").emailVerified(true).build());
        when(accountStore.readByEmail("user@example.test")).thenReturn(Optional.of(account()));
        final AfterIdpPasswordResetEmailSentRequest request =
                new AfterIdpPasswordResetEmailSentRequest("trace", "user-1", "user@example.test", true);
        request.setIdpUserToken("token");

        assertThrows(AuthenticationServiceException.class,
                () -> service.afterIdpPasswordResetEmailSent(request));
        verify(accountStore, never()).updateUserAccount(any());
    }

    @Test
    void resetEmailCallbackFailsClosedWhenVerificationDisabled() throws Exception {
        when(tokenVerifier.isUserTokenVerificationEnabled()).thenReturn(false);
        final AfterIdpPasswordResetEmailSentRequest request =
                new AfterIdpPasswordResetEmailSentRequest("trace", "user-1", "user@example.test", true);
        request.setIdpUserToken("token");

        assertThrows(AuthenticationServiceException.class,
                () -> service.afterIdpPasswordResetEmailSent(request));
        verify(accountStore, never()).readByEmail(any());
        verify(accountStore, never()).updateUserAccount(any());
    }

    @Test
    void accountStoreFailureDoesNotDeleteExistingSession() throws Exception {
        final UserAccount account = account();
        final UserSession session = new UserSession();
        session.setAppbaseSessionId("session-old");
        session.setUserAccount(account);
        when(accountStore.readByAppbaseUserId("user-1")).thenReturn(Optional.of(account));
        when(sessionStore.readSession("user-1", "session-old")).thenReturn(Optional.of(session));
        when(accountStore.updateUserAccount(any())).thenThrow(new UserAccountStoreException("injected"));

        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAppbaseRequesterId("user-1");
        request.setAppbaseUserId("user-1");
        request.setAppbaseSessionId("session-old");
        request.setName("Updated");
        assertThrows(UserAccountServiceException.class, () -> service.updateAccount(request));

        verify(sessionStore, never()).deleteSession(eq("user-1"), eq("session-old"));
    }

    @Test
    void successfulAccountWritePrecedesSessionRefreshWithoutDeletion() throws Exception {
        final UserAccount account = account();
        final UserSession session = new UserSession();
        session.setAppbaseSessionId("session-old");
        session.setUserAccount(account);
        when(accountStore.readByAppbaseUserId("user-1")).thenReturn(Optional.of(account));
        when(sessionStore.readSession("user-1", "session-old")).thenReturn(Optional.of(session));
        when(accountStore.updateUserAccount(any())).thenReturn(1);

        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAppbaseRequesterId("user-1");
        request.setAppbaseUserId("user-1");
        request.setAppbaseSessionId("session-old");
        request.setName("Updated");
        assertTrue(service.updateAccount(request).isUpdated());

        final InOrder order = inOrder(accountStore, sessionStore);
        order.verify(accountStore).updateUserAccount(any());
        order.verify(sessionStore).updateSession(any());
        verify(sessionStore, never()).deleteSession(eq("user-1"), eq("session-old"));
    }

    @Test
    void sessionRefreshFailureInvalidatesExistingSessionAfterAccountCommit() throws Exception {
        final UserAccount account = account();
        final UserSession session = new UserSession();
        session.setAppbaseSessionId("session-old");
        session.setUserAccount(account);
        when(accountStore.readByAppbaseUserId("user-1")).thenReturn(Optional.of(account));
        when(sessionStore.readSession("user-1", "session-old")).thenReturn(Optional.of(session));
        when(accountStore.updateUserAccount(any())).thenReturn(1);
        org.mockito.Mockito.doThrow(new UserSessionStoreException("injected"))
                .when(sessionStore).updateSession(any());
        when(sessionStore.deleteSession("user-1", "session-old")).thenReturn(true);

        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAppbaseRequesterId("user-1");
        request.setAppbaseUserId("user-1");
        request.setAppbaseSessionId("session-old");
        request.setName("Updated");
        final UserAccountServiceException error =
                assertThrows(UserAccountServiceException.class, () -> service.updateAccount(request));
        assertTrue(error.getMessage().contains("account was updated"));
        assertTrue(error.getMessage().contains("reauthenticate"));
        final InOrder order = inOrder(accountStore, sessionStore);
        order.verify(accountStore).updateUserAccount(any());
        order.verify(sessionStore).updateSession(any());
        order.verify(sessionStore).deleteSession("user-1", "session-old");
    }

    @Test
    void sessionRefreshAndInvalidationFailureReportsBothErrors() throws Exception {
        final UserAccount account = account();
        final UserSession session = new UserSession();
        session.setAppbaseSessionId("session-old");
        session.setUserAccount(account);
        when(accountStore.readByAppbaseUserId("user-1")).thenReturn(Optional.of(account));
        when(sessionStore.readSession("user-1", "session-old")).thenReturn(Optional.of(session));
        when(accountStore.updateUserAccount(any())).thenReturn(1);
        org.mockito.Mockito.doThrow(new UserSessionStoreException("refresh failed"))
                .when(sessionStore).updateSession(any());
        when(sessionStore.deleteSession("user-1", "session-old"))
                .thenThrow(new UserSessionStoreException("delete failed"));

        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAppbaseRequesterId("user-1");
        request.setAppbaseUserId("user-1");
        request.setAppbaseSessionId("session-old");
        request.setName("Updated");
        final UserAccountServiceException error =
                assertThrows(UserAccountServiceException.class, () -> service.updateAccount(request));
        assertTrue(error.getMessage().contains("invalidation failed"));
        assertTrue(error.getMessage().contains("reauthenticate"));
        assertEquals(1, error.getSuppressed().length);
        assertEquals("delete failed", error.getSuppressed()[0].getMessage());
    }

    @Test
    void unconfirmedSessionInvalidationRequiresReauthentication() throws Exception {
        final UserAccount account = account();
        final UserSession session = new UserSession();
        session.setAppbaseSessionId("session-old");
        session.setUserAccount(account);
        when(accountStore.readByAppbaseUserId("user-1")).thenReturn(Optional.of(account));
        when(sessionStore.readSession("user-1", "session-old")).thenReturn(Optional.of(session));
        when(accountStore.updateUserAccount(any())).thenReturn(1);
        org.mockito.Mockito.doThrow(new UserSessionStoreException("refresh failed"))
                .when(sessionStore).updateSession(any());
        when(sessionStore.deleteSession("user-1", "session-old")).thenReturn(false);

        final UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAppbaseRequesterId("user-1");
        request.setAppbaseUserId("user-1");
        request.setAppbaseSessionId("session-old");
        request.setName("Updated");
        final UserAccountServiceException error =
                assertThrows(UserAccountServiceException.class, () -> service.updateAccount(request));
        assertTrue(error.getMessage().contains("invalidation could not be confirmed"));
        assertTrue(error.getMessage().contains("reauthenticate"));
        verify(sessionStore).deleteSession("user-1", "session-old");
    }

    private static UserAccount account() {
        final UserAccount account = new UserAccount();
        account.setAppbaseUserId("user-1");
        account.setIdpUserId("idp-1");
        account.setEmail("user@example.test");
        account.setName("Original");
        account.setPasswordHashes(PasswordHashes.builder().clientPasswordHash(clientHash()).build());
        account.setPasswordResetEmailSent(false);
        account.setCreationTimestamp(LocalDateTime.of(2026, 10, 20, 1, 0));
        account.setLastAccessTimestamp(LocalDateTime.of(2026, 10, 20, 1, 0));
        return account;
    }

    private static PasswordHash clientHash() {
        return PasswordHash.builder().hash("hash").salt("salt")
                .hashAlgorithm(HashAlgorithm.SHA512).shaRounds(80).build();
    }

    private void persistAccountChanges(final UserAccount initial) throws Exception {
        final AtomicReference<UserAccount> stored = new AtomicReference<>(initial);
        when(accountStore.readByAppbaseUserId("user-1"))
                .thenAnswer(invocation -> Optional.of(stored.get()));
        when(accountStore.updateUserAccount(any())).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return 1;
        });
    }
}
