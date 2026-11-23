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

package com.viveworks.appbase.stacktest.test.service.user.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues emulator-owned ID tokens for the infrastructure-backed account tests only.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class FirebaseAuthEmulatorIdentity {
    private static final int CONNECT_TIMEOUT_SECONDS = 3;
    private static final int REQUEST_TIMEOUT_SECONDS = 5;
    private static final int HTTP_OK = 200;
    private static final int TOKEN_REFRESH_DELAY_MILLIS = 1100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS)).build();
    private static final Map<String, String> EMAILS_BY_UID = new ConcurrentHashMap<>();
    private static final String PASSWORD = "stacktest-password-123";

    private FirebaseAuthEmulatorIdentity() {
    }

    /**
     * Creates an email identity with an ID token containing the profile claims.
     *
     * @return the emulator-owned identity
     * @throws IllegalStateException if the emulator changes the UID during profile update or sign-in
     */
    public static Identity createEmailUser() {
        final String email = "stacktest-" + UUID.randomUUID() + "@example.com";
        final JsonNode signup = post("accounts:signUp", Map.of(
                "email", email, "password", PASSWORD, "returnSecureToken", true));
        final String uid = required(signup, "localId");
        final JsonNode updated = post("accounts:update", Map.of(
                "idToken", required(signup, "idToken"), "displayName", "name-" + uid,
                "returnSecureToken", true));
        if (!uid.equals(required(updated, "localId"))) {
            throw new IllegalStateException("Emulator returned a different UID during profile update");
        }
        // The Auth emulator persists displayName but does not return an ID token from accounts:update.
        // Sign in again so the token carries the new name claim required by Appbase's IdpToken.
        final JsonNode signedIn = post("accounts:signInWithPassword", Map.of(
                "email", email, "password", PASSWORD, "returnSecureToken", true));
        if (!uid.equals(required(signedIn, "localId"))) {
            throw new IllegalStateException("Emulator returned a different UID after profile update");
        }
        final Identity identity = new Identity(uid, email, required(signedIn, "idToken"));
        EMAILS_BY_UID.put(identity.uid(), email);
        return identity;
    }

    /**
     * Creates an emulator identity for the guest-session test path.
     *
     * @return the emulator-owned identity
     */
    public static Identity createGuestUser() {
        // Appbase's current IdpToken contract requires name and email even for a guest session.
        return createEmailUser();
    }

    /**
     * Obtains a fresh ID token for a previously created email identity.
     *
     * @param uid the previously created emulator UID
     * @return a fresh ID token for that UID
     * @throws IllegalArgumentException if the UID was not created by this helper
     * @throws IllegalStateException if token refresh is interrupted or changes the UID
     */
    public static String refreshedEmailToken(final String uid) {
        final String email = EMAILS_BY_UID.get(uid);
        if (email == null) {
            throw new IllegalArgumentException("No emulator credentials for UID " + uid);
        }
        // The emulator can issue the same JWT for two sign-ins within one second.
        try {
            Thread.sleep(TOKEN_REFRESH_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while refreshing emulator ID token", exception);
        }
        final JsonNode response = post("accounts:signInWithPassword", Map.of(
                "email", email, "password", PASSWORD,
                "returnSecureToken", true));
        if (!uid.equals(required(response, "localId"))) {
            throw new IllegalStateException("Emulator returned a different UID during sign-in");
        }
        return required(response, "idToken");
    }

    private static JsonNode post(final String endpoint, final Map<String, Object> body) {
        final String host = System.getenv("FIREBASE_AUTH_EMULATOR_HOST");
        if (host == null || !host.matches("[A-Za-z0-9.-]+:[0-9]+")) {
            throw new IllegalStateException("FIREBASE_AUTH_EMULATOR_HOST must be a bare host:port");
        }
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + "/identitytoolkit.googleapis.com/v1/" +
                            endpoint + "?key=fake-api-key"))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                    .build();
            final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                throw new IllegalStateException("Firebase Auth emulator " + endpoint +
                        " returned HTTP " + response.statusCode());
            }
            return OBJECT_MAPPER.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Auth emulator is unavailable for " + endpoint, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Firebase Auth emulator", exception);
        }
    }

    private static String required(final JsonNode response, final String field) {
        final String value = response.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Firebase Auth emulator response omitted " + field);
        }
        return value;
    }

    /**
     * Emulator-owned identity returned to account-stacktest callers.
     *
     * @param uid emulator UID
     * @param email emulator email address
     * @param idToken ID token containing account profile claims
     */
    public record Identity(String uid, String email, String idToken) {
        @Override
        public String toString() {
            return "Identity[uid=" + uid + ", email=" + email + ", idToken=[REDACTED]]";
        }
    }
}
