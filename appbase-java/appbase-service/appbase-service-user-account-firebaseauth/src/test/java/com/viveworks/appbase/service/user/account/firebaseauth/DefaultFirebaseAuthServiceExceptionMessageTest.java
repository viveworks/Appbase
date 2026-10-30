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

package com.viveworks.appbase.service.user.account.firebaseauth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientIntegrationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.IdpToken;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifying a user token must never put the token itself into an exception message or a log line. A short
 * one-way fingerprint is allowed so an operator can correlate an error with a request.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultFirebaseAuthServiceExceptionMessageTest {
    private static final String TOKEN = "eyJhbGciOi-SECRETTOKEN-signature";

    private FirebaseAuth firebaseAuth;
    private Logger logger;
    private DefaultFirebaseAuthService service;

    @BeforeEach
    void setUp() {
        firebaseAuth = Mockito.mock(FirebaseAuth.class);
        logger = Mockito.mock(Logger.class);

        final LogService logService = Mockito.mock(LogService.class);
        Mockito.when(logService.getLogger(Mockito.any(Class.class))).thenReturn(logger);

        final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
        final Timer timer = Mockito.mock(Timer.class);
        Mockito.when(timer.time()).thenReturn(Mockito.mock(Timer.Context.class));
        Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
        Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(Mockito.mock(Counter.class));
        final MetricService metricService = Mockito.mock(MetricService.class);
        Mockito.when(metricService.getMetricRegistry()).thenReturn(metricRegistry);
        Mockito.when(metricService.name(Mockito.any(Class.class), Mockito.anyString())).thenReturn("metric");

        final FirebaseAuthClientIntegrationService integrationService =
                Mockito.mock(FirebaseAuthClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(firebaseAuth);

        service = new DefaultFirebaseAuthService(
                logService,
                Mockito.mock(ValidationService.class),
                metricService,
                Mockito.mock(MediatorService.class, Mockito.RETURNS_DEEP_STUBS),
                Mockito.mock(ServiceRegistry.class),
                DefaultFirebaseAuthServiceConfiguration.builder().build(),
                integrationService);
    }

    private static String fingerprint(final String token) throws NoSuchAlgorithmException {
        final byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest).substring(0, 8);
    }

    @Test
    void absentFirebaseTokenDoesNotEchoTheUserToken() throws Exception {
        Mockito.when(firebaseAuth.verifyIdToken(Mockito.eq(TOKEN), Mockito.anyBoolean())).thenReturn(null);

        final AuthenticationServiceException thrown =
                Assertions.assertThrows(AuthenticationServiceException.class, () -> service.verifyUserToken(TOKEN));

        Assertions.assertFalse(thrown.getMessage().contains("SECRETTOKEN"),
                "message echoed the token: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains(fingerprint(TOKEN)), "message lost its correlation hint");
    }

    @Test
    void firebaseVerificationFailureDoesNotEchoTheUserTokenInTheMessageOrTheLog() throws Exception {
        final FirebaseAuthException failure = Mockito.mock(FirebaseAuthException.class);
        Mockito.when(firebaseAuth.verifyIdToken(Mockito.eq(TOKEN), Mockito.anyBoolean())).thenThrow(failure);

        final AuthenticationServiceException thrown =
                Assertions.assertThrows(AuthenticationServiceException.class, () -> service.verifyUserToken(TOKEN));

        Assertions.assertFalse(thrown.getMessage().contains("SECRETTOKEN"),
                "message echoed the token: " + thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains(fingerprint(TOKEN)), "message lost its correlation hint");
        Assertions.assertSame(failure, thrown.getCause());

        final ArgumentCaptor<String> logged = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger).error(logged.capture(), Mockito.eq(failure));
        Assertions.assertFalse(logged.getValue().contains("SECRETTOKEN"), "log line echoed the token: " + logged.getValue());
    }

    @Test
    void verifiedTokenStillReturnsTheIdentity() throws Exception {
        final FirebaseToken firebaseToken = Mockito.mock(FirebaseToken.class);
        Mockito.when(firebaseToken.getUid()).thenReturn("uid-1");
        Mockito.when(firebaseToken.getName()).thenReturn("Test Name");
        Mockito.when(firebaseToken.getEmail()).thenReturn("a@example.invalid");
        Mockito.when(firebaseToken.getIssuer()).thenReturn("issuer");
        Mockito.when(firebaseAuth.verifyIdToken(Mockito.eq(TOKEN), Mockito.anyBoolean())).thenReturn(firebaseToken);

        final IdpToken idpToken = service.verifyUserToken(TOKEN);

        Assertions.assertEquals("uid-1", idpToken.getUid());
        Assertions.assertEquals("a@example.invalid", idpToken.getEmail());
    }
}
