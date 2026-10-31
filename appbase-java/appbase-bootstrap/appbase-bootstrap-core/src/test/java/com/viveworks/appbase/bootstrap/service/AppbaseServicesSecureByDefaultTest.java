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

package com.viveworks.appbase.bootstrap.service;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;
import com.viveworks.appbase.service.authorization.AuthorizationServiceExceptionConfiguration;
import com.viveworks.appbase.service.authorization.ContextAuthorizer;
import com.viveworks.appbase.service.authorization.DefaultAuthorizationServiceConfiguration;
import com.viveworks.appbase.service.authorization.PermitAllContextAuthorizer;
import com.viveworks.appbase.service.authorization.SecureByDefaultContextAuthorizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies secure-by-default authorization wiring in {@link AppbaseServices.Builder}: the effective
 * {@link ContextAuthorizer} selection and the FORBIDDEN
 * {@link AuthorizationServiceExceptionConfiguration} registration that lets the web filters answer
 * HTTP 403.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class AppbaseServicesSecureByDefaultTest {

    /**
     * A mock {@link AppbaseCoreServices} rich enough to build a real
     * {@link com.viveworks.appbase.service.authorization.DefaultAuthorizationService}: every
     * unstubbed accessor returns a mock ({@code RETURNS_MOCKS}), and a backing map gives the
     * {@link ConfigurationService} real put/get typed-configuration semantics. A default
     * {@link DefaultAuthorizationServiceConfiguration} is pre-registered so the authorization factory
     * can construct its service.
     */
    private static AppbaseCoreServices richCore(final Map<Class<?>, AppbaseConfiguration> typedStore) {
        typedStore.put(DefaultAuthorizationServiceConfiguration.class,
            mock(DefaultAuthorizationServiceConfiguration.class));

        final ConfigurationService cs = mock(ConfigurationService.class);
        when(cs.getTypedConfiguration(any())).thenAnswer(inv -> typedStore.get(inv.getArgument(0)));
        doAnswer(inv -> {
            typedStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(cs).putTypedConfiguration(any(), any());

        final AppbaseCoreServices core = mock(AppbaseCoreServices.class, RETURNS_MOCKS);
        when(core.getConfigurationService()).thenReturn(cs);
        return core;
    }

    // ── Authorizer selection (pure, via package-private resolveContextAuthorizer) ───────────────

    @Test
    void defaultBuilder_resolvesSecureByDefaultAuthorizer() {
        final ContextAuthorizer authorizer = AppbaseServices.builder().resolveContextAuthorizer();
        assertInstanceOf(SecureByDefaultContextAuthorizer.class, authorizer);
    }

    @Test
    void secureByDefaultFalse_resolvesPermitAllAuthorizer() {
        final ContextAuthorizer authorizer =
            AppbaseServices.builder().secureByDefault(false).resolveContextAuthorizer();
        assertInstanceOf(PermitAllContextAuthorizer.class, authorizer);
    }

    @Test
    void explicitAuthorizer_winsRegardlessOfSecureByDefaultTrue() {
        final ContextAuthorizer custom = mock(ContextAuthorizer.class);
        final ContextAuthorizer resolved = AppbaseServices.builder()
            .secureByDefault(true)
            .withContextAuthorizer(custom)
            .resolveContextAuthorizer();
        assertSame(custom, resolved);
    }

    @Test
    void explicitAuthorizer_winsRegardlessOfSecureByDefaultFalse() {
        final ContextAuthorizer custom = mock(ContextAuthorizer.class);
        final ContextAuthorizer resolved = AppbaseServices.builder()
            .secureByDefault(false)
            .withContextAuthorizer(custom)
            .resolveContextAuthorizer();
        assertSame(custom, resolved);
    }

    @Test
    void publicPaths_areCarriedIntoSecureByDefaultAuthorizer() {
        final ContextAuthorizer authorizer = AppbaseServices.builder()
            .publicPaths(Set.of("/health", "/metric"))
            .resolveContextAuthorizer();
        assertInstanceOf(SecureByDefaultContextAuthorizer.class, authorizer);
        // The public paths permit /metric; anything else is denied.
        assertTrue(authorizer.isAuthorizedContext("/metric", null));
        assertFalse(authorizer.isAuthorizedContext("/admin", null));
    }

    @Test
    void publicPaths_areCanonicalizedIntoSecureByDefaultAuthorizer() {
        // Final-review F2: a non-canonical operator public-path entry must still match the canonical
        // request path the authorizer compares against, or it is silently denied. "/a/../health"
        // canonicalizes to "/health", which the authorizer must therefore permit.
        final ContextAuthorizer authorizer = AppbaseServices.builder()
            .publicPaths(Set.of("/a/../health"))
            .resolveContextAuthorizer();
        assertInstanceOf(SecureByDefaultContextAuthorizer.class, authorizer);
        assertTrue(authorizer.isAuthorizedContext("/health", null));
    }

    // ── 403 configuration registration (via build) ──────────────────────────────────────────────

    @Test
    void secureByDefaultBuild_registersForbiddenExceptionConfiguration() {
        final Map<Class<?>, AppbaseConfiguration> typedStore = new HashMap<>();
        final AppbaseCoreServices core = richCore(typedStore);

        AppbaseServices.builder().from(core).build();

        final AuthorizationServiceExceptionConfiguration cfg = core.getConfigurationService()
            .getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        assertNotNull(cfg);
        assertTrue(cfg.isRespondForbidden());
    }

    @Test
    void secureByDefaultFalseBuild_doesNotRegisterExceptionConfiguration() {
        final Map<Class<?>, AppbaseConfiguration> typedStore = new HashMap<>();
        final AppbaseCoreServices core = richCore(typedStore);

        AppbaseServices.builder().from(core).secureByDefault(false).build();

        assertFalse(typedStore.containsKey(AuthorizationServiceExceptionConfiguration.class));
    }

    @Test
    void secureByDefaultBuild_preservesRealOperatorExceptionConfiguration() {
        // A config with a genuine action (here: a redirect) is an operator choice and must be kept.
        final Map<Class<?>, AppbaseConfiguration> typedStore = new HashMap<>();
        final AppbaseCoreServices core = richCore(typedStore);
        final AuthorizationServiceExceptionConfiguration operatorConfig =
            AuthorizationServiceExceptionConfiguration.builder()
                .respondForbidden(false).redirectUrl("/login").build();
        typedStore.put(AuthorizationServiceExceptionConfiguration.class, operatorConfig);

        AppbaseServices.builder().from(core).build();

        final AuthorizationServiceExceptionConfiguration cfg = core.getConfigurationService()
            .getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        assertSame(operatorConfig, cfg);
        assertFalse(cfg.isRespondForbidden());
    }

    @Test
    void secureByDefaultBuild_replacesNeutralOperatorExceptionConfigurationWithForbidden() {
        // A NEUTRAL pre-registered config (respondForbidden=false, blank redirectUrl, non-positive
        // status) is replaced by an explicit forbidden policy in secure-by-default builds. The web
        // authorization filters also deny by default if no response action remains configured.
        final Map<Class<?>, AppbaseConfiguration> typedStore = new HashMap<>();
        final AppbaseCoreServices core = richCore(typedStore);
        final AuthorizationServiceExceptionConfiguration neutralConfig =
            AuthorizationServiceExceptionConfiguration.builder().respondForbidden(false).build();
        typedStore.put(AuthorizationServiceExceptionConfiguration.class, neutralConfig);

        AppbaseServices.builder().from(core).build();

        final AuthorizationServiceExceptionConfiguration cfg = core.getConfigurationService()
            .getTypedConfiguration(AuthorizationServiceExceptionConfiguration.class);
        assertNotSame(neutralConfig, cfg);
        assertTrue(cfg.isRespondForbidden());
    }
}
