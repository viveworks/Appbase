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

package com.viveworks.appbase.service.authorization;

import com.viveworks.appbase.core.AppbaseRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class SecureByDefaultContextAuthorizerTest {
    private static final String PUBLIC_PATH = "/health";
    private static final String PRIVATE_PATH = "/account";
    private static final String OPERATION = "someOperation";
    private static final String NON_PATH_SERVICE_ID = "com.example.SomeService";

    @Test
    void testPublicPathIsAuthorizedForServiceVariant() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized = authorizer.isAuthorizedContext(PUBLIC_PATH, new AuthorizationContext());

        Assertions.assertTrue(authorized);
    }

    @Test
    void testPublicPathIsAuthorizedForServiceOperationVariant() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized =
                authorizer.isAuthorizedContext(PUBLIC_PATH, OPERATION, new AuthorizationContext());

        Assertions.assertTrue(authorized);
    }

    @Test
    void testNonPublicPathIsDeniedForServiceVariant() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized = authorizer.isAuthorizedContext(PRIVATE_PATH, new AuthorizationContext());

        Assertions.assertFalse(authorized);
    }

    @Test
    void testNonPublicPathIsDeniedForServiceOperationVariant() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized =
                authorizer.isAuthorizedContext(PRIVATE_PATH, OPERATION, new AuthorizationContext());

        Assertions.assertFalse(authorized);
    }

    @Test
    void testNonPathServiceIdIsPermittedForServiceVariant() {
        // A non-path service identifier (a Class#getName(), never a web path) is a programmatic
        // authorization, which secure-by-default does not target -- it must be PERMITTED, not denied.
        // Regression: denying it broke every DefaultUserAccountService-gated operation (final-review F1).
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized =
                authorizer.isAuthorizedContext(NON_PATH_SERVICE_ID, new AuthorizationContext());

        Assertions.assertTrue(authorized);
    }

    @Test
    void testNonPathServiceIdIsPermittedForServiceOperationVariant() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized =
                authorizer.isAuthorizedContext(NON_PATH_SERVICE_ID, OPERATION, new AuthorizationContext());

        Assertions.assertTrue(authorized);
    }

    @Test
    void testClassServiceVariantIsPermitted() {
        // A Class service is by definition programmatic/non-path; secure-by-default permits it.
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized = authorizer.isAuthorizedContext(String.class, new AuthorizationContext());

        Assertions.assertTrue(authorized);
    }

    @Test
    void testClassServiceOperationVariantIsPermitted() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of(PUBLIC_PATH));

        final boolean authorized =
                authorizer.isAuthorizedContext(String.class, OPERATION, new AuthorizationContext());

        Assertions.assertTrue(authorized);
    }

    @Test
    void testEmptyPublicPathsDeniesEverything() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of());

        final boolean authorized = authorizer.isAuthorizedContext(PUBLIC_PATH, new AuthorizationContext());

        Assertions.assertFalse(authorized);
    }

    @Test
    void testNullPublicPathsThrows() {
        Assertions.assertThrows(
                AppbaseRuntimeException.class,
                () -> new SecureByDefaultContextAuthorizer(null));
    }

    @Test
    void testStoredPublicPathsAreDefensivelyCopied() {
        final Set<String> mutablePublicPaths = new HashSet<>(Set.of(PUBLIC_PATH));
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(mutablePublicPaths);

        mutablePublicPaths.add(PRIVATE_PATH);

        final boolean authorized = authorizer.isAuthorizedContext(PRIVATE_PATH, new AuthorizationContext());

        Assertions.assertFalse(authorized);
    }

    @Test
    void nonCanonicalAndCanonicalPublicPathsHaveTheSameVerdictForBothStringOverloads() {
        final SecureByDefaultContextAuthorizer authorizer =
                new SecureByDefaultContextAuthorizer(Set.of("/health/."));
        final AuthorizationContext context = new AuthorizationContext();

        Assertions.assertTrue(authorizer.isAuthorizedContext("/health", context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/.", context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/%2e", context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/x/..", context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/x/%2e%2e", context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("//health", context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health", OPERATION, context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/%2E", OPERATION, context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/x/%2E%2E", OPERATION, context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("//health", OPERATION, context));
        Assertions.assertTrue(authorizer.isAuthorizedContext("/health/", context),
                "an explicit trailing slash aliases the same authorization key");
        Assertions.assertFalse(authorizer.isAuthorizedContext("/unrelated", context));
        Assertions.assertFalse(authorizer.isAuthorizedContext("/unrelated", OPERATION, context));
    }
}
