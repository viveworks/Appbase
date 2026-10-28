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

import com.viveworks.appbase.client.core.ClientCoreTestHelper;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.path.AppbasePathException;
import com.viveworks.appbase.service.testhelper.AppbaseServiceCoreTestHelper;
import com.viveworks.appbase.service.user.account.UserAccountService;
import com.viveworks.appbase.service.user.account.store.UserAccountStore;
import com.viveworks.appbase.service.user.session.store.UserSessionStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultAuthorizationServiceTest {
    private static AppbaseCore appbaseCore;

    @BeforeAll
    static void init() {
        appbaseCore = ClientCoreTestHelper.appbaseCore();
    }

    /**
     * Final-review F1 regression: {@link DefaultUserAccountService} gates ~24 operations via
     * {@code authorizationService.checkAuthorized(getClass(), operation, ctx)}. Under secure-by-default
     * the effective authorizer is a {@link SecureByDefaultContextAuthorizer}, which must gate only web
     * request paths and PERMIT programmatic service-class authorizations. Before the fix the class name
     * was not in {@code publicPaths} so every gated user-account operation was denied. This proves the
     * programmatic path is no longer denied: {@code checkAuthorized(SomeClass.class, op, ctx)} must NOT
     * throw under the secure authorizer (with no allow/block rules configured).
     */
    @Test
    void testCheckAuthorizedByServiceClassUnderSecureByDefaultDoesNotThrow() {
        final ContextAuthorizer secureAuthorizer =
                new SecureByDefaultContextAuthorizer(Set.of("/health"));
        final DefaultAuthorizationService authorizationService =
                new DefaultAuthorizationService(
                        appbaseCore.getLogService(),
                        appbaseCore.getMetricService(),
                        appbaseCore.getMediatorService(),
                        appbaseCore.getServiceRegistry(),
                        DefaultAuthorizationServiceConfiguration.builder().build(),
                        secureAuthorizer);

        // Mirrors DefaultUserAccountService's checkAuthorized(getClass(), operation, ctx) call.
        Assertions.assertDoesNotThrow(() -> authorizationService.checkAuthorized(
                DefaultAuthorizationServiceTest.class, "someUserAccountOperation", new AuthorizationContext()));
        // And a non-public web path must still be denied (web hardening intact).
        Assertions.assertThrows(AuthorizationServiceException.class, () -> authorizationService.checkAuthorized(
                "/configuration", "securePropertyKeys", new AuthorizationContext()));
    }

    @Test
    void testRegisterService() throws InterruptedException {
        final ContextAuthorizer contextAuthorizer = new PermitAllContextAuthorizer();
        final ServiceRegistry serviceRegistry = appbaseCore.getServiceRegistry();
        final DefaultAuthorizationService authorizationService =
                new DefaultAuthorizationService(
                        appbaseCore.getLogService(),
                        appbaseCore.getMetricService(),
                        appbaseCore.getMediatorService(),
                        serviceRegistry,
                        DefaultAuthorizationServiceConfiguration.builder().build(),
                        contextAuthorizer);
        AppbaseServiceEntry appbaseServiceEntry1 = serviceRegistry.getAppbaseService(AuthorizationService.class);
        Assertions.assertNotNull(appbaseServiceEntry1);

        appbaseServiceEntry1 = AppbaseServiceCoreTestHelper.awaitServiceStartedStatus(
                serviceRegistry,
                AuthorizationService.class,
                appbaseServiceEntry1);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry1.getServiceStatus());
        Assertions.assertEquals(authorizationService, appbaseServiceEntry1.getService());

        final AppbaseServiceEntry appbaseServiceEntry2 =
                serviceRegistry.getAppbaseService(DefaultAuthorizationService.class);
        Assertions.assertNotNull(appbaseServiceEntry2);
        Assertions.assertEquals(ServiceStatus.STARTED, appbaseServiceEntry2.getServiceStatus());
        Assertions.assertEquals(authorizationService, appbaseServiceEntry2.getService());
    }

    @Test
    void testIsAuthorized() {
        final ContextAuthorizer contextAuthorizer = new PermitAllContextAuthorizer();
        final AuthorizationContext context = new AuthorizationContext();
        final DefaultAuthorizationService authorizationService =
                new DefaultAuthorizationService(
                        appbaseCore.getLogService(),
                        appbaseCore.getMetricService(),
                        appbaseCore.getMediatorService(),
                        appbaseCore.getServiceRegistry(),
                        DefaultAuthorizationServiceConfiguration.builder().build(),
                        contextAuthorizer);
        // Allow all services except the ones with operations
        authorizationService.addAllowedServiceName(AppbaseServiceConstants.WILDCARD);

        Assertions.assertTrue(
                authorizationService.isAuthorized(UserAccountService.class, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(
                        UserAccountService.class, AuthorizationService.ALL_OPERATIONS, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(
                        UserAccountService.class, "createUserWithEmailAndPassword", context));


        authorizationService.addBlockedService(UserAccountService.class);
        Assertions.assertFalse(
                authorizationService.isAuthorized(UserAccountService.class, context));
        Assertions.assertFalse(
                authorizationService.isAuthorized(
                        UserAccountService.class, AuthorizationService.ALL_OPERATIONS, context));
        Assertions.assertFalse(
                authorizationService.isAuthorized(
                        UserAccountService.class, "createUserWithEmailAndPassword", context));


        // True because of the above line:
        // authorizationService.addAllowedServiceName(AppbaseServiceConstants.WILDCARD);
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserSessionStore.class, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserSessionStore.class, "createSession", context));


        authorizationService.addAllowedServiceOperation(UserAccountStore.class, AuthorizationService.ALL_OPERATIONS);
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserAccountStore.class, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(
                        UserAccountStore.class, AuthorizationService.ALL_OPERATIONS, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserAccountStore.class, "createUserAccount", context));


        Assertions.assertTrue(
                authorizationService.isAuthorized(UserSessionStore.class, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserSessionStore.class, "createSession", context));


        authorizationService.addAllowedService(UserSessionStore.class);
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserSessionStore.class, context));
        Assertions.assertTrue(
                authorizationService.isAuthorized(UserSessionStore.class, "createSession", context));
    }

    @Test
    void testCheckAuthorized() throws AuthorizationServiceException {
        final ContextAuthorizer contextAuthorizer = new PermitAllContextAuthorizer();
        final AuthorizationContext context = new AuthorizationContext();
        final DefaultAuthorizationService authorizationService =
                new DefaultAuthorizationService(
                        appbaseCore.getLogService(),
                        appbaseCore.getMetricService(),
                        appbaseCore.getMediatorService(),
                        appbaseCore.getServiceRegistry(),
                        DefaultAuthorizationServiceConfiguration.builder().build(),
                        contextAuthorizer);

        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserAccountService.class, context));
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(
                        UserAccountService.class, AuthorizationService.ALL_OPERATIONS, context));
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(
                        UserAccountService.class, "createUserWithEmailAndPassword", context));


        authorizationService.addBlockedService(UserAccountService.class);
        Assertions.assertThrows(AuthorizationServiceException.class, () ->
                authorizationService.checkAuthorized(UserAccountService.class, context));
        Assertions.assertThrows(AuthorizationServiceException.class, () ->
                authorizationService.checkAuthorized(
                        UserAccountService.class, AuthorizationService.ALL_OPERATIONS, context));
        Assertions.assertThrows(AuthorizationServiceException.class, () ->
                authorizationService.checkAuthorized(
                        UserAccountService.class, "createUserWithEmailAndPassword", context));


        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserSessionStore.class, context));
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserSessionStore.class, "createSession", context));


        authorizationService.addAllowedServiceOperation(UserAccountStore.class, AuthorizationService.ALL_OPERATIONS);
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserAccountStore.class, context));
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(
                        UserAccountStore.class, AuthorizationService.ALL_OPERATIONS, context));
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserAccountStore.class, "createUserAccount", context));


        // The allowlist is now populated (UserAccountStore was just added) and UserSessionStore is
        // not a member, so it must be denied until it is added. This is what makes the following
        // addAllowedService(UserSessionStore.class) line meaningful: before the allowlist-denies-
        // non-members fix, these two assertions passed via the context authorizer regardless of
        // that line, silently authorizing a non-member.
        Assertions.assertThrows(AuthorizationServiceException.class, () ->
                authorizationService.checkAuthorized(UserSessionStore.class, context));
        Assertions.assertThrows(AuthorizationServiceException.class, () ->
                authorizationService.checkAuthorized(UserSessionStore.class, "createSession", context));


        authorizationService.addAllowedService(UserSessionStore.class);
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserSessionStore.class, context));
        Assertions.assertDoesNotThrow(() ->
                authorizationService.checkAuthorized(UserSessionStore.class, "createSession", context));
    }

    /*
     * The plan's Step 1 snippet constructs the service via a two-arg
     * DefaultAuthorizationService(configuration, contextAuthorizer) constructor and populates the
     * allowlist/blocklist through DefaultAuthorizationServiceConfiguration's builder. Neither exists:
     * the only constructor is the six-arg one above, and DefaultAuthorizationServiceConfiguration
     * carries no authorization state (its never-read map fields were deleted -- see that class's
     * Javadoc). The service constructor always starts both internal maps empty; they are populated
     * only through addAllowedServiceName(...) / addBlockedServiceName(...), exactly as
     * testIsAuthorized above does. So these tests are built the same way as the pre-existing tests
     * in this class; only the test construction differs from the plan's snippet, not the assertions
     * or the semantics table.
     */

    private static DefaultAuthorizationService newAuthorizationService(final ContextAuthorizer contextAuthorizer) {
        return new DefaultAuthorizationService(
                appbaseCore.getLogService(),
                appbaseCore.getMetricService(),
                appbaseCore.getMediatorService(),
                appbaseCore.getServiceRegistry(),
                DefaultAuthorizationServiceConfiguration.builder().build(),
                contextAuthorizer);
    }

    @Test
    void aPopulatedAllowlistDeniesAPathThatIsNotAMember() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("/admin");

        Assertions.assertFalse(authorizationService.isAuthorized("/secrets", new AuthorizationContext()),
                "an allowlist that does not deny non-members is not an allowlist");
    }

    @Test
    void aPopulatedAllowlistAllowsItsMembers() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("/admin");

        Assertions.assertTrue(authorizationService.isAuthorized("/admin", new AuthorizationContext()));
    }

    @Test
    void aWildcardAllowlistAllowsAnUnlistedPath() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName(AppbaseServiceConstants.WILDCARD);

        Assertions.assertTrue(authorizationService.isAuthorized("/anything", new AuthorizationContext()));
    }

    @Test
    void aBlocklistOnlyConfigurationStillDelegatesForAnUnlistedPath() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceName("/admin");

        Assertions.assertTrue(authorizationService.isAuthorized("/public", new AuthorizationContext()),
                "a blocklist denies what it lists and delegates the rest; this is not the allowlist bug");
    }

    @Test
    void aBlocklistOnlyConfigurationDeniesAListedPath() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceName("/admin");

        Assertions.assertFalse(authorizationService.isAuthorized("/admin", new AuthorizationContext()));
    }

    @Test
    void anUnconfiguredServiceStillDelegatesSoAFreshAppDoesNotForbidEveryRequest() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());

        Assertions.assertTrue(authorizationService.isAuthorized("/anything", new AuthorizationContext()));
    }

    @Test
    void withBothListsTheBlocklistWins() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("/admin");
        authorizationService.addBlockedServiceName("/admin");

        Assertions.assertFalse(authorizationService.isAuthorized("/admin", new AuthorizationContext()));
    }

    @Test
    void withBothListsAPathInTheAllowlistOnlyIsAllowed() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("/admin");
        authorizationService.addBlockedServiceName("/other");

        Assertions.assertTrue(authorizationService.isAuthorized("/admin", new AuthorizationContext()));
    }

    @Test
    void withBothListsAPathInNeitherIsDenied() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("/admin");
        authorizationService.addBlockedServiceName("/other");

        Assertions.assertFalse(authorizationService.isAuthorized("/secrets", new AuthorizationContext()));
    }

    @Test
    void aWildcardBlocklistDeniesAnyPath() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceName(AppbaseServiceConstants.WILDCARD);

        Assertions.assertFalse(authorizationService.isAuthorized("/anything", new AuthorizationContext()));
    }

    /*
     * Task 3 (amendment A5): authorization keys never enter the canonicalizer through a configuration
     * class -- DefaultAuthorizationServiceConfiguration's maps were dead and are deleted (see that
     * class's Javadoc). The only path-shaped authorization keys are the String arguments to
     * addAllowedServiceName / addBlockedServiceName (and their *Operation siblings), guarded to
     * canonicalize only when the argument starts with "/", since these mutators also accept a bare
     * Java class name via the Class<?> overloads' serviceType.getName().
     */

    @Test
    void addAllowedServiceNameCanonicalizesAPathArgumentSoAnEncodedRequestStillMatches() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("/%61dmin");

        Assertions.assertTrue(authorizationService.isAuthorized("/admin", new AuthorizationContext()),
                "a key authored as /%61dmin must match a request for /admin");
    }

    @Test
    void addAllowedServiceNameLeavesAClassNameArgumentVerbatim() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceName("com.example.NotAPath");

        Assertions.assertTrue(authorizationService.isAuthorized("com.example.NotAPath", new AuthorizationContext()),
                "a service name that does not start with / is not a path and must not gain a leading slash");
    }

    @Test
    void addBlockedServiceNameCanonicalizesAPathArgumentSoAnEncodedRequestStillMatches() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceName("/%61dmin");

        Assertions.assertFalse(authorizationService.isAuthorized("/admin", new AuthorizationContext()),
                "a blocklist entry authored as /%61dmin must deny a request for /admin");
    }

    @Test
    void addBlockedServiceNameLeavesAClassNameArgumentVerbatim() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceName("com.example.NotAPath");

        Assertions.assertFalse(authorizationService.isAuthorized("com.example.NotAPath", new AuthorizationContext()));
    }

    @Test
    void addAllowedServiceNameOperationCanonicalizesAPathArgument() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceNameOperation("/%61dmin", AuthorizationService.ALL_OPERATIONS);

        Assertions.assertTrue(authorizationService.isAuthorized("/admin", "anyOperation", new AuthorizationContext()));
    }

    @Test
    void addBlockedServiceNameOperationCanonicalizesAPathArgument() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceNameOperation("/%61dmin", AuthorizationService.ALL_OPERATIONS);

        Assertions.assertFalse(authorizationService.isAuthorized("/admin", "anyOperation", new AuthorizationContext()));
    }

    @Test
    void nonCanonicalAndCanonicalAdminPathsHaveTheSameAuthorizationVerdict() {
        final String operation = "DELETE";
        final AuthorizationContext context = new AuthorizationContext();
        final List<String> equivalents = List.of(
                "/admin/.", "/admin/%2e", "/admin/%2E", "/admin/x/..", "/admin/x/%2e%2e",
                "//admin", "/area/../admin");

        final DefaultAuthorizationService blocked = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.copyOf(equivalents)));
        blocked.addBlockedServiceNameOperation("/admin", operation);
        Assertions.assertFalse(blocked.isAuthorized("/admin", operation, context),
                "blocklist canonical control must be denied");
        for (final String equivalent : equivalents) {
            Assertions.assertFalse(blocked.isAuthorized(equivalent, operation, context),
                    "three-argument blocklist lookup must deny canonical equivalent " + equivalent);
        }
        Assertions.assertFalse(blocked.isAuthorized("/unrelated", operation, context),
                "SecureByDefault unrelated-path negative control");
        final DefaultAuthorizationService trailingSlashControl =
                newAuthorizationService(new PermitAllContextAuthorizer());
        trailingSlashControl.addBlockedServiceNameOperation("/admin", operation);
        Assertions.assertFalse(trailingSlashControl.isAuthorized("/admin/", operation, context),
                "an explicit trailing slash must alias the /admin blocklist key");

        final DefaultAuthorizationService allowed = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.of()));
        allowed.addAllowedServiceNameOperation("/admin", operation);
        Assertions.assertTrue(allowed.isAuthorized("/admin", operation, context),
                "allowlist canonical control must be allowed");
        for (final String equivalent : equivalents) {
            Assertions.assertTrue(allowed.isAuthorized(equivalent, operation, context),
                    "three-argument allowlist lookup must allow canonical equivalent " + equivalent);
        }
        Assertions.assertFalse(allowed.isAuthorized("/unrelated", operation, context),
                "populated allowlist unrelated-path negative control");

        final DefaultAuthorizationService delegated = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.of("/admin")));
        Assertions.assertTrue(delegated.isAuthorized("/admin", operation, context),
                "empty-map delegation canonical public-path control must be allowed");
        for (final String equivalent : equivalents) {
            Assertions.assertTrue(delegated.isAuthorized(equivalent, operation, context),
                    "empty-map delegation must use the canonical public path for " + equivalent);
        }
        Assertions.assertFalse(delegated.isAuthorized("/unrelated", operation, context),
                "empty-map SecureByDefault unrelated-path negative control");
    }

    @Test
    void nonPathServiceIdentifierRemainsVerbatimAtAuthorizationLookup() {
        final String service = DefaultAuthorizationServiceTest.class.getName();
        final DefaultAuthorizationService authorizationService = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.of()));
        authorizationService.addAllowedServiceNameOperation(service, "DELETE");

        Assertions.assertTrue(authorizationService.isAuthorized(
                service, "DELETE", new AuthorizationContext()),
                "a non-path service identifier must remain an exact allowlist key");
    }

    @Test
    void twoArgumentNonCanonicalAndCanonicalAdminPathsHaveTheSameAuthorizationVerdict() {
        final AuthorizationContext context = new AuthorizationContext();
        final List<String> equivalents = List.of(
                "/admin/.", "/admin/%2e", "/admin/%2E", "/admin/x/..", "/admin/x/%2e%2e",
                "//admin", "/area/../admin");

        final DefaultAuthorizationService blocked = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.copyOf(equivalents)));
        blocked.addBlockedServiceName("/admin");
        Assertions.assertFalse(blocked.isAuthorized("/admin", context));
        for (final String equivalent : equivalents) {
            Assertions.assertFalse(blocked.isAuthorized(equivalent, context),
                    "two-argument blocklist lookup must deny canonical equivalent " + equivalent);
        }
        Assertions.assertFalse(blocked.isAuthorized("/unrelated", context));
        final DefaultAuthorizationService trailingSlashControl =
                newAuthorizationService(new PermitAllContextAuthorizer());
        trailingSlashControl.addBlockedServiceName("/admin");
        Assertions.assertFalse(trailingSlashControl.isAuthorized("/admin/", context),
                "an explicit trailing slash must alias the /admin blocklist key");

        final DefaultAuthorizationService delegated = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.of("/admin")));
        Assertions.assertTrue(delegated.isAuthorized("/admin", context));
        for (final String equivalent : equivalents) {
            Assertions.assertTrue(delegated.isAuthorized(equivalent, context),
                    "two-argument delegation must use the canonical public path for " + equivalent);
        }
        Assertions.assertFalse(delegated.isAuthorized("/unrelated", context));
    }

    @Test
    void malformedPathLookupFailsClosedBeforeAuthorizationRules() {
        final DefaultAuthorizationService authorizationService = newAuthorizationService(
                new SecureByDefaultContextAuthorizer(Set.of("/public")));

        Assertions.assertThrows(AppbasePathException.class,
                () -> authorizationService.isAuthorized("/admin%2fescape", new AuthorizationContext()));
        Assertions.assertThrows(AppbasePathException.class,
                () -> authorizationService.isAuthorized(
                        "/admin%2fescape", "DELETE", new AuthorizationContext()));
    }

    @Test
    void nonCanonicalConfiguredRuleKeysMatchCanonicalRequestsForBothStringOverloads() {
        final AuthorizationContext context = new AuthorizationContext();
        for (final String configured : List.of(
                "/admin/.", "/admin/%2e", "/admin/x/..", "/admin/x/%2e%2e")) {
            final DefaultAuthorizationService operationBlock =
                    newAuthorizationService(new PermitAllContextAuthorizer());
            operationBlock.addBlockedServiceNameOperation(configured, "DELETE");
            Assertions.assertFalse(operationBlock.isAuthorized("/admin", "DELETE", context), configured);

            final DefaultAuthorizationService operationAllow =
                    newAuthorizationService(new SecureByDefaultContextAuthorizer(Set.of()));
            operationAllow.addAllowedServiceNameOperation(configured, "DELETE");
            Assertions.assertTrue(operationAllow.isAuthorized("/admin", "DELETE", context), configured);

            final DefaultAuthorizationService serviceBlock =
                    newAuthorizationService(new PermitAllContextAuthorizer());
            serviceBlock.addBlockedServiceName(configured);
            Assertions.assertFalse(serviceBlock.isAuthorized("/admin", context), configured);

            final DefaultAuthorizationService serviceAllow =
                    newAuthorizationService(new PermitAllContextAuthorizer());
            serviceAllow.addAllowedServiceName(configured);
            Assertions.assertTrue(serviceAllow.isAuthorized("/admin", context), configured);
        }
    }

    @Test
    void blockedOperationMatchingIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            final DefaultAuthorizationService authorizationService =
                newAuthorizationService(new PermitAllContextAuthorizer());
            authorizationService.addBlockedServiceNameOperation("/admin", "WRITE");

            Assertions.assertFalse(
                authorizationService.isAuthorized("/admin", "write", new AuthorizationContext()));
        } finally {
            Locale.setDefault(original);
        }
    }

    /*
     * AUD-03 (CWE-178), allowlist site. blockedOperationMatchingIsLocaleIndependent above covers the
     * blocklist lookup, where a locale-induced miss turned a DENY into a PERMIT (the bypass
     * direction). The allowlist site's defect is the mirror image and was previously untested: a
     * Turkish-locale miss on the allowlist falls into populated-allowlist-denies-non-members, so
     * an operation the operator explicitly ALLOWED is silently denied -- a fail-closed
     * availability defect rather than a bypass. "write" carries a dotted lowercase i, so
     * toUpperCase() under tr-TR yields "WRİTE", not "WRITE".
     */
    @Test
    void allowedOperationMatchingIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            final DefaultAuthorizationService authorizationService =
                newAuthorizationService(new PermitAllContextAuthorizer());
            authorizationService.addAllowedServiceNameOperation("/admin", "WRITE");

            Assertions.assertTrue(
                authorizationService.isAuthorized("/admin", "write", new AuthorizationContext()),
                "an allowlisted operation must match under any default locale: under tr-TR the "
                        + "lookup uppercased 'write' to 'WRİTE' and denied an explicitly allowed "
                        + "operation (AUD-03, allowlist site)");
        } finally {
            Locale.setDefault(original);
        }
    }

    /*
     * P-042 E01 N08 (internal work item 1379) -- the three-argument overload's blocklist branch.
     *
     * Earlier three-argument arms cover blocked operations, but not an unblocked operation falling
     * through to the allowlist or context authorizer. That overload is the one filters route to whenever an
     * operation is present -- which ALL THREE request faces supply: the servlet filter
     * (request.getMethod()), the reactive filter (exchange.method()), and the JAX-RS filter
     * (resourceMethod.getName(), falling back to request.getMethod()). So the suite thoroughly
     * covered the overload that was right and never the one that was wrong. These four arms close
     * that gap.
     *
     * Note the JAX-RS face keys the blocklist on a JAVA METHOD NAME rather than an HTTP verb, so one
     * map is consulted with two different key vocabularies. That is pre-existing and untouched here;
     * it is recorded because it widens who is affected by the fall-through, not what the fix does.
     *
     * Registered as AUD-01/AUD-02 in docs/spec/1.0.0/P05/decisions/E01-audit-triage.md.
     */

    @Test
    void aBlocklistedPathStillHonoursSecureByDefaultForAnUnblockedOperation() {
        // AUD-01. Real SecureByDefaultContextAuthorizer, not a double: "/admin" is not a public path,
        // so secure-by-default must deny it whatever the blocklist says about some OTHER operation.
        final DefaultAuthorizationService authorizationService =
                newAuthorizationService(new SecureByDefaultContextAuthorizer(Set.of("/health")));
        authorizationService.addBlockedServiceNameOperation("/admin", "POST");

        Assertions.assertFalse(
                authorizationService.isAuthorized("/admin", "GET", new AuthorizationContext()),
                "blocking POST on /admin must not make GET /admin public: configuring a blocklist rule "
                        + "cannot widen access to the path it protects");
    }

    @Test
    void aBlocklistedPathStillHonoursTheAllowlistForAnUnblockedOperation() {
        // AUD-02. The allowlist lists GET only. DELETE is in neither list, so the allowlist must deny
        // it -- the blocklist entry must not short-circuit the allowlist for every other operation.
        final DefaultAuthorizationService authorizationService =
                newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addAllowedServiceNameOperation("/admin", "GET");
        authorizationService.addBlockedServiceNameOperation("/admin", "POST");

        Assertions.assertFalse(
                authorizationService.isAuthorized("/admin", "DELETE", new AuthorizationContext()),
                "a populated allowlist denies what it does not list; a blocklist entry on the same "
                        + "path must not make the allowlist unreachable");
    }

    @Test
    void aBlockedOperationOnABlocklistedPathIsStillDenied() {
        // The blocklist's own job, asserted so the fall-through cannot be achieved by weakening it.
        final DefaultAuthorizationService authorizationService =
                newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceNameOperation("/admin", "POST");

        Assertions.assertFalse(
                authorizationService.isAuthorized("/admin", "POST", new AuthorizationContext()));
    }

    @Test
    void anUnblockedOperationOnABlocklistedPathIsStillAllowedWhenNothingElseDenies() {
        /*
         * THE PRESERVATION ARM, and the reason it exists is worth stating.
         *
         * Before the fix the blocklist branch returned true for an unblocked operation. For THIS
         * configuration -- blocklist only, permissive authorizer, no allowlist -- true is the CORRECT
         * answer, and the class's own comment says so: "a blocklist denies what it lists and delegates
         * the rest."
         *
         * So the old code reached the right answer here by short-circuiting, and the fix must now
         * reach it deliberately, by delegating. Without this arm the obvious over-correction -- making
         * blocklist presence deny outright, as the two-argument overload does -- would pass every
         * other arm in this file.
         */
        final DefaultAuthorizationService authorizationService =
                newAuthorizationService(new PermitAllContextAuthorizer());
        authorizationService.addBlockedServiceNameOperation("/admin", "POST");

        Assertions.assertTrue(
                authorizationService.isAuthorized("/admin", "GET", new AuthorizationContext()),
                "a blocklist denies what it lists and delegates the rest -- the fix must delegate, "
                        + "not deny outright");
    }
}
