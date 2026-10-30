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

package com.viveworks.appbase.client.service.firebase.app.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * Verifies that Firebase access tokens remain redacted through nested configurations.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
final class FirebaseCredentialConfigurationToStringTest {
    private static final String SECRET = "firebase-access-token-secret-2558";

    private static FirebaseAccessTokenConfiguration tokenConfiguration() {
        return FirebaseAccessTokenConfiguration.builder()
                .tokenValue(SECRET)
                .expirationTimeMillis(new Date(0))
                .build();
    }

    @Test
    void accessTokenRedactsTokenValue() {
        final String rendered = tokenConfiguration().toString();

        Assertions.assertFalse(rendered.contains(SECRET));
        Assertions.assertTrue(rendered.contains("tokenValue='********'"));
    }

    @Test
    void googleCredentialsRedactsNestedAccessToken() {
        final String rendered = GoogleCredentialsConfiguration.builder()
                .firebaseAccessTokenConfiguration(tokenConfiguration())
                .build()
                .toString();

        Assertions.assertFalse(rendered.contains(SECRET));
        Assertions.assertTrue(rendered.contains("firebaseAccessTokenConfiguration=********"));
    }

    @Test
    void appConfigurationRedactsNestedGoogleCredentials() {
        final GoogleCredentialsConfiguration credentials = GoogleCredentialsConfiguration.builder()
                .firebaseAccessTokenConfiguration(tokenConfiguration())
                .build();
        final String rendered = DefaultFirebaseAppClientIntegrationServiceConfiguration.builder()
                .googleCredentialsConfiguration(credentials)
                .build()
                .toString();

        Assertions.assertFalse(rendered.contains(SECRET));
        Assertions.assertTrue(rendered.contains("googleCredentialsConfiguration=********"));
    }
}
