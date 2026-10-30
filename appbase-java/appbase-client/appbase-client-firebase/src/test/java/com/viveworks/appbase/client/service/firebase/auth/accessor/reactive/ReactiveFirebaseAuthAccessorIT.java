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

package com.viveworks.appbase.client.service.firebase.auth.accessor.reactive;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.viveworks.appbase.client.service.firebase.auth.DefaultFirebaseAuthClientService;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientService;
import com.viveworks.appbase.client.service.firebase.auth.FirebaseAuthClientServiceTestHelper;
import com.viveworks.appbase.client.service.firebase.auth.accessor.FirebaseAuthAccessor;
import com.viveworks.appbase.client.service.firebase.auth.accessor.FirebaseAuthAccessorException;
import com.viveworks.appbase.client.util.AppbaseClientObjectMapperSubTypes;
import com.viveworks.appbase.core.util.jackson.AppbaseCoreObjectMapperSubTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Gated integration test exercising both faces of the Firebase Auth accessor layer --
 * {@link ReactiveFirebaseAuthAccessor} and {@link FirebaseAuthAccessor} -- against the Firebase Auth
 * Emulator, mirroring the bootstrap of {@code DefaultFirebaseAuthClientServiceReactiveTest} via
 * {@link FirebaseAuthClientServiceTestHelper}.
 *
 * <p>Requires the local Firebase Auth Emulator reachable per the module's gated-test config: system
     * property {@code FirebaseAuthClientServiceTests.enabled=true} (defaults to {@code false} in
     * {@code appbase-client/pom.xml}). The helper defaults resolve the non-secret, emulator-only
     * {@code /appbase-client-firebase-app.yaml} and {@code /appbase-client-firebase-auth.yaml}
     * classpath fixtures added by N09. Surefire's default includes ({@code *Test}/{@code
 * *Tests}/{@code Test*}/{@code *TestCase}) do not select {@code *IT} classes, but this module now
 * binds {@code maven-failsafe-plugin} in {@code develop-profile} / {@code staging-profile} / {@code
 * release-profile} (N05, internal work item 1295), so Failsafe selects it under {@code verify} once the gate is on:
 * {@code mvn -pl appbase-java/appbase-client/appbase-client-firebase -am verify -P develop-profile
 * -DFirebaseAuthClientServiceTests.enabled=true}.
 *
 * <p><b>The Auth Emulator is the sanctioned backend, not a live project.</b> Failsafe exports {@code
 * FIREBASE_AUTH_EMULATOR_HOST} (root {@code pom.xml}), which {@code firebase-admin} composes into
 * {@code http://HOST:PORT/identitytoolkit.googleapis.com/...} -- so the value is a bare {@code
 * host:port} and must name the <i>auth</i> emulator's port, not the Emulator UI's. The container is
 * this module's {@code infra/docker/firebase} (internal work item 1299 / N09). What the emulator does <i>not</i>
 * evidence -- it issues unsigned tokens, applies no IAM, and stubs federation -- follows directly
 * from those emulator constraints. A green run of this class is
 * evidence about the accessor's user CRUD and error mapping, not about authentication security.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "FirebaseAuthClientServiceTests.enabled", matches = "true")
class ReactiveFirebaseAuthAccessorIT {

    private static FirebaseAuthClientService firebaseAuthClientService;
    private static String reactiveUid;
    private static String syncUid;

    /**
     * Boots a live {@link FirebaseAuthClientService} against the local Auth Emulator, via {@link
     * FirebaseAuthClientServiceTestHelper#firebaseAuthClientService()}, and precomputes the two
     * throwaway UIDs used by {@link #reactiveRoundTrip()} and {@link #syncRoundTrip()}.
     */
    @BeforeAll
    static void setUp() {
        AppbaseCoreObjectMapperSubTypes.register();
        AppbaseClientObjectMapperSubTypes.register();
        DefaultFirebaseAuthClientService.registerObjectMapperSubtypes();
        firebaseAuthClientService = FirebaseAuthClientServiceTestHelper.firebaseAuthClientService();

        reactiveUid = uniqueUid("reactive");
        syncUid = uniqueUid("sync");
    }

    /**
     * Best-effort deletes both test users, so a failed assertion in either round trip does not
     * orphan a user on the live Firebase project.
     */
    @AfterAll
    static void tearDown() {
        if (firebaseAuthClientService != null) {
            final FirebaseAuthAccessor accessor = firebaseAuthClientService.getAccessor();
            deleteQuietly(accessor, reactiveUid);
            deleteQuietly(accessor, syncUid);
            firebaseAuthClientService.shutdown();
        }
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    private static String uniqueUid(final String prefix) {
        return (prefix + "-" + ReactiveFirebaseAuthAccessorIT.class.getSimpleName() +
                "-" + UUID.randomUUID()).toLowerCase();
    }

    private static String emailFor(final String uid) {
        return uid + "@example.com";
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void deleteQuietly(final FirebaseAuthAccessor accessor, final String uid) {
        try {
            accessor.deleteUser("cleanupDelete", uid);
        } catch (Exception ex) {
            // Best-effort: tolerate user-not-found (already deleted by the test itself) and any
            // other failure here, so cleanup never masks the test's own assertion failures.
        }
    }

    private static void assertFirebaseAuthError(final Throwable throwable, final AuthErrorCode expected) {
        Assertions.assertInstanceOf(FirebaseAuthAccessorException.class, throwable);
        final Throwable cause = throwable.getCause();
        Assertions.assertInstanceOf(FirebaseAuthException.class, cause);
        Assertions.assertEquals(expected, ((FirebaseAuthException) cause).getAuthErrorCode());
    }

    private static void assertUserNotFound(final Throwable throwable) {
        assertFirebaseAuthError(throwable, AuthErrorCode.USER_NOT_FOUND);
    }

    private static String expiredEmulatorIdToken() {
        final long now = Instant.now().getEpochSecond();
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        final String payload = encoder.encodeToString((
                "{\"aud\":\"appbase-project\","
                        + "\"iss\":\"https://securetoken.google.com/appbase-project\","
                        + "\"sub\":\"expired-emulator-user\","
                        + "\"iat\":" + (now - 7200) + ","
                        + "\"exp\":" + (now - 3600) + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }

    /**
     * Reactive round trip via {@link ReactiveFirebaseAuthAccessor}: {@code createUser} with a
     * random uid/email -&gt; {@code getUser} (asserting the uid/email match) -&gt; {@code
     * setCustomUserClaims} -&gt; {@code getUser} (asserting the claim is present) -&gt; {@code
     * deleteUser} -&gt; {@code getUser} (asserting the resulting error is a {@link
     * FirebaseAuthAccessorException} wrapping a {@link FirebaseAuthException} with {@link
     * AuthErrorCode#USER_NOT_FOUND}). Cleanup is handled unconditionally by {@link #tearDown()}, so
     * a failed assertion here does not orphan the user on the live Firebase project.
     */
    @Test
    void reactiveRoundTrip() {
        final ReactiveFirebaseAuthAccessor reactive = firebaseAuthClientService.getReactiveAccessor();
        final String email = emailFor(reactiveUid);
        final UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setUid(reactiveUid)
                .setEmail(email);

        StepVerifier.create(reactive.createUser("createUser", createRequest))
                .assertNext(created -> {
                    Assertions.assertEquals(reactiveUid, created.getUid());
                    Assertions.assertEquals(email, created.getEmail());
                })
                .verifyComplete();

        StepVerifier.create(reactive.getUser("getUser", reactiveUid))
                .assertNext(fetched -> {
                    Assertions.assertEquals(reactiveUid, fetched.getUid());
                    Assertions.assertEquals(email, fetched.getEmail());
                })
                .verifyComplete();

        final Map<String, Object> claims = Collections.singletonMap("role", "reactive-it");
        StepVerifier.create(reactive.setCustomUserClaims("setCustomUserClaims", reactiveUid, claims))
                .verifyComplete();

        StepVerifier.create(reactive.getUser("getUser", reactiveUid))
                .assertNext(withClaims ->
                        Assertions.assertEquals("reactive-it", withClaims.getCustomClaims().get("role")))
                .verifyComplete();

        StepVerifier.create(reactive.deleteUser("deleteUser", reactiveUid))
                .verifyComplete();

        StepVerifier.create(reactive.getUser("getUser", reactiveUid))
                .expectErrorSatisfies(ReactiveFirebaseAuthAccessorIT::assertUserNotFound)
                .verify();
    }

    /**
     * Synchronous round trip via {@link FirebaseAuthAccessor}, mirroring the same {@code
     * createUser} -&gt; {@code getUser} -&gt; {@code setCustomUserClaims} -&gt; {@code getUser}
     * -&gt; {@code deleteUser} -&gt; {@code getUser} (not-found) sequence as {@link
     * #reactiveRoundTrip()}. Cleanup is handled unconditionally by {@link #tearDown()}, so a failed
     * assertion here does not orphan the user on the live Firebase project.
     *
     * @throws Exception the mapped exception, if a non-terminal operation fails
     */
    @Test
    void syncRoundTrip() throws Exception {
        final FirebaseAuthAccessor sync = firebaseAuthClientService.getAccessor();
        final String email = emailFor(syncUid);
        final UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setUid(syncUid)
                .setEmail(email);

        final UserRecord created = sync.createUser("createUser", createRequest);
        Assertions.assertEquals(syncUid, created.getUid());
        Assertions.assertEquals(email, created.getEmail());

        final UserRecord fetched = sync.getUser("getUser", syncUid);
        Assertions.assertEquals(syncUid, fetched.getUid());
        Assertions.assertEquals(email, fetched.getEmail());

        final Map<String, Object> claims = Collections.singletonMap("role", "sync-it");
        sync.setCustomUserClaims("setCustomUserClaims", syncUid, claims);

        final UserRecord withClaims = sync.getUser("getUser", syncUid);
        Assertions.assertEquals("sync-it", withClaims.getCustomClaims().get("role"));

        sync.deleteUser("deleteUser", syncUid);

        final Exception notFound = Assertions.assertThrows(Exception.class, () -> sync.getUser("getUser", syncUid));
        assertUserNotFound(notFound);
    }

    /** Proves both accessor faces reject a malformed ID token with the precise mapped error. */
    @Test
    void rejectsMalformedIdToken() {
        final ReactiveFirebaseAuthAccessor reactive = firebaseAuthClientService.getReactiveAccessor();
        StepVerifier.create(reactive.verifyIdToken("verifyMalformed", "not-a-jwt"))
                .expectErrorSatisfies(error ->
                        assertFirebaseAuthError(error, AuthErrorCode.INVALID_ID_TOKEN))
                .verify();

        final FirebaseAuthAccessor sync = firebaseAuthClientService.getAccessor();
        final Exception error = Assertions.assertThrows(
                Exception.class, () -> sync.verifyIdToken("verifyMalformed", "not-a-jwt"));
        assertFirebaseAuthError(error, AuthErrorCode.INVALID_ID_TOKEN);
    }

    /** Proves emulator-mode verification still enforces expiry for both accessor faces. */
    @Test
    void rejectsExpiredIdToken() {
        final String expiredToken = expiredEmulatorIdToken();
        final ReactiveFirebaseAuthAccessor reactive = firebaseAuthClientService.getReactiveAccessor();
        StepVerifier.create(reactive.verifyIdToken("verifyExpired", expiredToken))
                .expectErrorSatisfies(error ->
                        assertFirebaseAuthError(error, AuthErrorCode.EXPIRED_ID_TOKEN))
                .verify();

        final FirebaseAuthAccessor sync = firebaseAuthClientService.getAccessor();
        final Exception error = Assertions.assertThrows(
                Exception.class, () -> sync.verifyIdToken("verifyExpired", expiredToken));
        assertFirebaseAuthError(error, AuthErrorCode.EXPIRED_ID_TOKEN);
    }
}
