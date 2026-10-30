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

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientIntegrationService;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.user.account.AuthenticationServiceException;
import com.viveworks.appbase.service.user.account.IdpToken;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultFirebaseAuthService
        extends AbstractStatelessAppbaseService<DefaultFirebaseAuthServiceConfiguration>
        implements FirebaseAuthService
{
    private final Logger logger;
    private final ValidationService validationService;
    private final FirebaseAuthClientIntegrationService firebaseAuthClientIntegrationService;

    private final Timer createUserTimer;
    private final Counter createUserErrorCounter;
    private final Timer verifyUserTokenTimer;
    private final Counter verifyUserTokenErrorCounter;

    public DefaultFirebaseAuthService(
            final LogService logService,
            final ValidationService validationService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultFirebaseAuthServiceConfiguration serviceConfiguration,
            final FirebaseAuthClientIntegrationService firebaseAuthClientIntegrationService)
    {
        super(logService, metricService, mediatorService, serviceRegistry, serviceConfiguration);

        AppbaseUtil.notNull(validationService, "validationService");
        AppbaseUtil.notNull(firebaseAuthClientIntegrationService, "firebaseAuthClientIntegrationService");

        this.logger = logService.getLogger(getClass());
        this.validationService = validationService;
        this.firebaseAuthClientIntegrationService = firebaseAuthClientIntegrationService;

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createUserTimer =
                metricRegistry.timer(metricService.name(getClass(), "createUserTimer"));
        this.createUserErrorCounter =
                metricRegistry.counter(metricService.name(getClass(), "createUserErrorCounter"));
        this.verifyUserTokenTimer =
                metricRegistry.timer(metricService.name(getClass(), "verifyUserTokenTimer"));
        this.verifyUserTokenErrorCounter =
                metricRegistry.counter(metricService.name(getClass(), "verifyUserTokenErrorCounter"));
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(FirebaseAuthService.class, this);
        serviceRegistry.registerAppbaseService(DefaultFirebaseAuthService.class, this);
    }

    @Override
    public FirebaseAuthCreateResponse createUser(
            final FirebaseAuthCreateRequest firebaseAuthCreateRequest)
            throws AuthenticationServiceException
    {
        AppbaseUtil.notNull(firebaseAuthCreateRequest, "firebaseAuthCreateRequest");
        AppbaseUtil.validate(validationService, firebaseAuthCreateRequest);

        final Timer.Context context = createUserTimer.time();
        try {
            final UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setDisplayName(firebaseAuthCreateRequest.getName())
                    .setEmail(firebaseAuthCreateRequest.getEmail())
                    .setEmailVerified(false)
                    .setPassword(firebaseAuthCreateRequest.getPassword())
                    .setDisabled(false);
            final UserRecord userRecord = firebaseAuthClientIntegrationService.getClient().createUser(createRequest);

            return new FirebaseAuthCreateResponse(firebaseAuthCreateRequest.getTraceId(), userRecord.getUid());
        } catch (FirebaseAuthException fae) {
            createUserErrorCounter.inc();

            final String message = "Failed to create user in FirebaseAuth" + firebaseAuthCreateRequest;
            logger.error(message, fae);
            throw new AuthenticationServiceException(message, fae);
        } finally {
            context.stop();
        }
    }

    @Override
    public boolean isUserTokenVerificationEnabled() {
        return serviceConfiguration.isUserTokenVerificationEnabled();
    }

    @Override
    public IdpToken verifyUserToken(final String userToken) throws AuthenticationServiceException {
        AppbaseUtil.notBlank(userToken, "userToken");

        final Timer.Context context = verifyUserTokenTimer.time();

        try {
            final FirebaseToken firebaseToken = firebaseAuthClientIntegrationService
                    .getClient()
                    .verifyIdToken(userToken, serviceConfiguration.isCheckUserTokenRevocation());
            if (firebaseToken == null) {
                throw new AuthenticationServiceException(
                        "No firebase token for the supplied user token " + describeToken(userToken));
            }

            return IdpToken
                    .builder()
                    .uid(firebaseToken.getUid())
                    .name(firebaseToken.getName())
                    .issuer(firebaseToken.getIssuer())
                    .email(firebaseToken.getEmail())
                    .emailVerified(firebaseToken.isEmailVerified())
                    .build();
        } catch (FirebaseAuthException e) {
            verifyUserTokenErrorCounter.inc();

            final String message = "Error verifying ID token " + describeToken(userToken);
            logger.error(message, e);
            throw new AuthenticationServiceException(message, e);
        } finally {
            context.stop();
        }
    }

    /**
     * Describes a user token for an error message or log line without revealing it: a bearer token must
     * never be echoed, but a short one-way fingerprint lets an operator correlate an error with a request.
     */
    private static String describeToken(final String userToken) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(userToken.getBytes(StandardCharsets.UTF_8));
            return "(fingerprint " + HexFormat.of().formatHex(digest, 0, 4) + ")";
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every Java platform; fail closed rather than fall back to the token.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public FirebaseAuthCheckUserTokenResponse checkUserToken(
            final FirebaseAuthCheckUserTokenRequest firebaseAuthCheckUserTokenRequest)
            throws AuthenticationServiceException
    {
        AppbaseUtil.notNull(firebaseAuthCheckUserTokenRequest, "firebaseAuthCheckUserTokenRequest");
        AppbaseUtil.validate(validationService, firebaseAuthCheckUserTokenRequest);

        final IdpToken idpToken = verifyUserToken(firebaseAuthCheckUserTokenRequest.getToken());
        if (!firebaseAuthCheckUserTokenRequest.getEmail().equalsIgnoreCase(idpToken.getEmail())) {
            throw new AuthenticationServiceException("Email does not match");
        }

        if (!firebaseAuthCheckUserTokenRequest.getUid().equals(idpToken.getUid())) {
            throw new AuthenticationServiceException("UID does not match");
        }

        return new FirebaseAuthCheckUserTokenResponse(firebaseAuthCheckUserTokenRequest.getTraceId(), idpToken);
    }
}
