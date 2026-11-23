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

package com.viveworks.appbase.service.authorization;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;

import java.util.Set;

/**
 * A {@link ContextAuthorizer} that gates web request paths (deny unless in {@code publicPaths}) and permits
 * non-path (programmatic/service-class) authorizations, which secure-by-default does not target.
 *
 * <p>This is the secure-by-default counterpart to {@link PermitAllContextAuthorizer}. The web filters invoke the
 * {@code String}-service variants with {@code service} set to the request path (always starting with {@code "/"}),
 * so those variants deny unless the path is in {@code publicPaths}. A {@code String} service that is NOT a path
 * (a {@code Class#getName()} passed through {@code DefaultAuthorizationService}) is a programmatic authorization
 * and is PERMITTED -- secure-by-default only hardens web request paths, and denying programmatic service-class
 * authorizations (which were permit-all before) would break normal authenticated flows such as the gated
 * user-account operations. The {@link Class}-service variants are by definition non-path and likewise PERMIT.
 *
 * <p>The path-vs-classname distinction is the same {@code startsWith("/")} guard convention used elsewhere in the
 * authorization stack (see {@code DefaultAuthorizationService}'s canonicalization guards).
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class SecureByDefaultContextAuthorizer implements ContextAuthorizer {
    private final Set<String> publicPaths;

    /**
     * Create a new instance.
     *
     * @param publicPaths Set of request paths that are authorized without further checks; must not be {@code null}
     */
    public SecureByDefaultContextAuthorizer(final Set<String> publicPaths) {
        AppbaseUtil.notNull(publicPaths, "publicPaths");
        this.publicPaths = publicPaths.stream()
                .map(SecureByDefaultContextAuthorizer::canonicalizeServiceIdentifier)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Allows non-path names and allows request paths only when they are in the public-path set. */
    @Override
    public boolean isAuthorizedContext(
            final String service,
            final AuthorizationContext context)
    {
        return isAuthorizedService(service);
    }

    /** Applies path policy to a named operation without inspecting the operation or context. */
    @Override
    public boolean isAuthorizedContext(
            final String service,
            final String operation,
            final AuthorizationContext context)
    {
        return isAuthorizedService(service);
    }

    /** Permits a programmatic service type. */
    @Override
    public boolean isAuthorizedContext(final Class<?> serviceType, final AuthorizationContext context) {
        // A Class service is by definition programmatic/non-path (a Class is never a web path), so it is
        // permitted: secure-by-default gates web request paths, not programmatic service-class authorizations.
        return true;
    }

    /** Permits a programmatic service-type operation. */
    @Override
    public boolean isAuthorizedContext(
            final Class<?> serviceType,
            final String operation,
            final AuthorizationContext context)
    {
        return true;
    }

    /**
     * Gates only web request paths: a {@code service} starting with {@code "/"} is a request path, permitted iff
     * it is in {@code publicPaths}. Any other {@code service} identifier is a non-path (programmatic/service-class)
     * authorization, which secure-by-default does not target, so it is permitted.
     */
    private boolean isAuthorizedService(final String service) {
        return service != null && service.startsWith("/")
                ? publicPaths.contains(canonicalizeServiceIdentifier(service))
                : true;
    }

    private static String canonicalizeServiceIdentifier(final String service) {
        if (!service.startsWith("/")) {
            return service;
        }
        return AppbasePathCanonicalizer.canonicalizeAuthorizationPath(service);
    }
}
