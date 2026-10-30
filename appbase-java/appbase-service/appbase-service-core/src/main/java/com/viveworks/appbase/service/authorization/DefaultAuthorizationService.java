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

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.AppbaseServiceConstants;
import com.viveworks.appbase.service.path.AppbasePathCanonicalizer;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe authorization service combining concurrent allow/block maps with a contextual policy fallback.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class DefaultAuthorizationService
        extends AbstractStatelessAppbaseService<DefaultAuthorizationServiceConfiguration>
        implements AuthorizationService
{
    private final ContextAuthorizer contextAuthorizer;
    private final Map<String, Map<String, String>> allowedOperationsByServiceTypeNameMap;
    private final Map<String, Map<String, String>> blockedOperationsByServiceTypeNameMap;

    /**
     * Creates an authorization service with empty in-memory allow and block maps.
     * @param logService logging service
     * @param metricService metrics service
     * @param mediatorService mediator service
     * @param serviceRegistry service registry
     * @param defaultAuthorizationServiceConfiguration service configuration
     * @param contextAuthorizer fallback contextual policy
     */
    public DefaultAuthorizationService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultAuthorizationServiceConfiguration defaultAuthorizationServiceConfiguration,
            final ContextAuthorizer contextAuthorizer)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultAuthorizationServiceConfiguration);

        this.contextAuthorizer = Objects.requireNonNull(contextAuthorizer);
        this.allowedOperationsByServiceTypeNameMap = new ConcurrentHashMap<>();
        this.blockedOperationsByServiceTypeNameMap = new ConcurrentHashMap<>();
    }

    /** Registers this instance under both the authorization interface and concrete service class. */
    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(AuthorizationService.class, this);
        serviceRegistry.registerAppbaseService(DefaultAuthorizationService.class, this);
    }

    /**
     * Applies the service-wide policy without inspecting any operation map. A wildcard or exact
     * service block entry denies immediately. With a nonempty allowlist, an exact service key leaves
     * the decision to the context authorizer regardless of that entry's operation contents; an
     * otherwise unlisted service is permitted by a wildcard key regardless of the wildcard entry's
     * contents, and is denied without one. Every undecided request delegates only the service and
     * context to the context authorizer.
     */
    @Override
    public boolean isAuthorized(
            final String service,
            final AuthorizationContext context)
    {
        Objects.requireNonNull(service);
        Objects.requireNonNull(context);
        final String authorizationKey = canonicalizeServiceIdentifier(service);

        if (allowedOperationsByServiceTypeNameMap.isEmpty() &&
                blockedOperationsByServiceTypeNameMap.isEmpty())
        {
            return contextAuthorizer.isAuthorizedContext(authorizationKey, context);
        }

        if (blockedOperationsByServiceTypeNameMap.containsKey(AppbaseServiceConstants.WILDCARD)) {
            return false;
        }

        if (blockedOperationsByServiceTypeNameMap.containsKey(authorizationKey)) {
            return false;
        }

        // A populated allowlist denies whatever it does not list. Reaching contextAuthorizer here
        // would make "configured but unmatched" indistinguishable from "unconfigured", which is how
        // an allowlist silently stopped denying anything. A blocklist-only configuration falls
        // through on purpose: a blocklist denies what it lists and delegates the rest.
        if (!allowedOperationsByServiceTypeNameMap.isEmpty() &&
                !allowedOperationsByServiceTypeNameMap.containsKey(authorizationKey))
        {
            return allowedOperationsByServiceTypeNameMap.containsKey(AppbaseServiceConstants.WILDCARD);
        }

        return contextAuthorizer.isAuthorizedContext(authorizationKey, context);
    }

    /**
     * Resolves the service type's binary name and applies the service-wide policy of
     * {@link #isAuthorized(String, AuthorizationContext)}, including its denial of a service that a
     * nonempty allowlist does not list (unless a wildcard key permits it). Operation entries are not
     * inspected; undecided requests delegate to the context authorizer.
     */
    @Override
    public boolean isAuthorized(final Class<?> serviceType, final AuthorizationContext context) {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(context);

        return isAuthorized(serviceType.getName(), context);
    }

    /** Resolves the service type's binary name and applies operation-specific authorization rules. */
    @Override
    public boolean isAuthorized(
            final Class<?> serviceType,
            final String operation,
            final AuthorizationContext context)
    {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(context);

        return isAuthorized(serviceType.getName(), operation, context);
    }

    /**
     * Applies the configured maps in implementation order. A wildcard block denies immediately. If
     * the service has a block entry, that blocklist alone decides: listed operations deny and every
     * other operation is allowed, without consulting the allowlist or context authorizer. Otherwise,
     * a configured allowlist permits matching operations and denies nonmatches. A wildcard service
     * entry permits every operation on every otherwise unlisted service; its own operation map is
     * not inspected. An operation matches an entry when the entry is the all-operations marker, the
     * operation exactly, or the operation converted to upper case. When neither map supplies a
     * decision, delegation uses only the service and
     * context, so the context authorizer does not receive the requested operation.
     */
    @Override
    public boolean isAuthorized(
            final String service,
            final String operation,
            final AuthorizationContext context)
    {
        Objects.requireNonNull(service);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(context);
        final String authorizationKey = canonicalizeServiceIdentifier(service);

        if (allowedOperationsByServiceTypeNameMap.isEmpty() &&
                blockedOperationsByServiceTypeNameMap.isEmpty())
        {
            return contextAuthorizer.isAuthorizedContext(authorizationKey, context);
        }

        if (blockedOperationsByServiceTypeNameMap.containsKey(AppbaseServiceConstants.WILDCARD)) {
            return false;
        }

        // A blocked operation denies outright. An operation this service's blocklist does NOT name must
        // FALL THROUGH to the allowlist and then the contextAuthorizer -- it must not return permitted
        // from here. Returning true here made configuring a blocklist rule WIDEN access to the path it
        // protects: the contextAuthorizer below (where secure-by-default lives) was never consulted for
        // that service again, and a populated allowlist was skipped entirely. The three-argument
        // contract and the allowlist/context-authorizer tests require this fall-through; the
        // two-argument overload intentionally has different service-level blocklist semantics.
        // (P-042 E01 N08, internal work item 1379 -- register rows AUD-01/AUD-02.)
        if (blockedOperationsByServiceTypeNameMap.containsKey(authorizationKey)) {
            final Map<String, String> blockedOperations =
                    blockedOperationsByServiceTypeNameMap.get(authorizationKey);
            if (blockedOperations != null &&
                    (blockedOperations.containsKey(ALL_OPERATIONS) ||
                            blockedOperations.containsKey(operation) ||
                            blockedOperations.containsKey(operation.toUpperCase(Locale.ROOT))))
            {
                return false;
            }
        }

        // Operation-specific precedence: a wildcard block key or an existing block entry for this
        // service has already decided above, without delegating to contextAuthorizer. From here a
        // populated allowlist denies whatever it does not list: reaching contextAuthorizer would make
        // "configured but unmatched" indistinguishable from "unconfigured", which is how an allowlist
        // silently stopped denying anything. Only a blocklist-only configuration with no block entry
        // for this service, and no allowlist at all, delegates the rest to contextAuthorizer.
        if (!allowedOperationsByServiceTypeNameMap.isEmpty()) {
            final Map<String, String> allowedOperations =
                    allowedOperationsByServiceTypeNameMap.get(authorizationKey);

            // Service type is allowed, check if the operation is allowed
            if (allowedOperations != null) {
                return allowedOperations.containsKey(ALL_OPERATIONS) ||
                        allowedOperations.containsKey(operation) ||
                        allowedOperations.containsKey(operation.toUpperCase(Locale.ROOT));
            }

            // No entry for this service in the allowed map: deny, unless a wildcard permits everything.
            return allowedOperationsByServiceTypeNameMap.containsKey(AppbaseServiceConstants.WILDCARD);
        }

        return contextAuthorizer.isAuthorizedContext(authorizationKey, context);
    }

    private static String canonicalizeServiceIdentifier(final String service) {
        if (!service.startsWith("/")) {
            return service;
        }
        return AppbasePathCanonicalizer.canonicalizeAuthorizationPath(service);
    }

    /** Checks a named service and reports that service in the exception when denied. */
    @Override
    public void checkAuthorized(
            final String service,
            final AuthorizationContext context)
            throws AuthorizationServiceException
    {
        Objects.requireNonNull(service);
        Objects.requireNonNull(context);

        if (!isAuthorized(service, context)) {
            throw new AuthorizationServiceException("Unauthorized [service=" + service + ", context=" + context + "]");
        }
    }

    /** Checks a service type and reports that type in the exception when denied. */
    @Override
    public void checkAuthorized(
            final Class<?> serviceType,
            final AuthorizationContext context) throws AuthorizationServiceException
    {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(context);

        if (!isAuthorized(serviceType, context)) {
            throw new AuthorizationServiceException(
                    "Unauthorized [serviceType=" + serviceType + ", context=" + context + "]");
        }
    }

    /** Checks a named service operation and reports both names in the exception when denied. */
    @Override
    public void checkAuthorized(
            final String service,
            final String operation,
            final AuthorizationContext context)
            throws AuthorizationServiceException
    {
        Objects.requireNonNull(service);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(context);

        if (!isAuthorized(service, operation, context)) {
            throw new AuthorizationServiceException("Unauthorized [service=" + service +
                    ", operation=" + operation + ", context=" + context + "]");
        }
    }

    /** Checks a service-type operation and reports both values in the exception when denied. */
    @Override
    public void checkAuthorized(
            final Class<?> serviceType,
            final String operation,
            final AuthorizationContext context) throws AuthorizationServiceException
    {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(context);

        if (!isAuthorized(serviceType, operation, context)) {
            throw new AuthorizationServiceException("Unauthorized [serviceType=" + serviceType +
                    ", operation=" + operation + ", context=" + context + "]");
        }
    }

    /** Allows every operation for the supplied service class by its binary class name. */
    @Override
    public void addAllowedService(final Class<?> serviceType) {
        addAllowedServiceOperation(serviceType, ALL_OPERATIONS);
    }

    /** Adds one allowed operation to the concurrent map for a service class; mutation is synchronized. */
    @Override
    public synchronized void addAllowedServiceOperation(final Class<?> serviceType, final String operation) {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(operation);

        final String serviceTypeName = serviceType.getName();
        Map<String, String> operations = allowedOperationsByServiceTypeNameMap.get(serviceTypeName);
        if (operations == null) {
            operations = new ConcurrentHashMap<>();
        }
        operations.put(operation, operation);
        allowedOperationsByServiceTypeNameMap.put(serviceTypeName, operations);
    }

    /** Allows every operation for a supplied service name or request path. */
    @Override
    public void addAllowedServiceName(final String serviceName) {
        addAllowedServiceNameOperation(serviceName, ALL_OPERATIONS);
    }

    /** Adds an allowed operation, canonicalizing names that begin with a slash as request paths. */
    @Override
    public synchronized void addAllowedServiceNameOperation(final String serviceName, final String operation) {
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(operation);

        // addAllowedServiceName(serviceName) delegates straight here with no transformation, so
        // canonicalizing in this single spot covers both entry points -- the same "one point every
        // path reaches" rule the configuration classes' private constructors follow.
        //
        // This map is keyed on EITHER a request path (this overload) OR a Class#getName() (the
        // Class<?> overload, via addAllowedServiceOperation), so canonicalizeKeys's own startsWith("/")
        // guard cannot protect a bare String argument here -- it is guarded explicitly instead. A class
        // name never starts with "/", so it is never canonicalized; a path always does.
        final String canonicalServiceName = canonicalizeServiceIdentifier(serviceName);

        Map<String, String> operations = allowedOperationsByServiceTypeNameMap.get(canonicalServiceName);
        if (operations == null) {
            operations = new ConcurrentHashMap<>();
        }
        operations.put(operation, operation);
        allowedOperationsByServiceTypeNameMap.put(canonicalServiceName, operations);
    }

    /** Blocks every operation for the supplied service class by its binary class name. */
    @Override
    public void addBlockedService(final Class<?> serviceType) {
        addBlockedServiceOperation(serviceType, ALL_OPERATIONS);
    }

    /** Adds one blocked operation to the concurrent map for a service class; mutation is synchronized. */
    @Override
    public synchronized void addBlockedServiceOperation(
            final Class<?> serviceType,
            final String operation)
    {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(operation);

        final String serviceTypeName = serviceType.getName();
        Map<String, String> operations = blockedOperationsByServiceTypeNameMap.get(serviceTypeName);
        if (operations == null) {
            operations = new ConcurrentHashMap<>();
        }
        operations.put(operation, operation);
        blockedOperationsByServiceTypeNameMap.put(serviceTypeName, operations);
    }

    /** Blocks every operation for a supplied service name or request path. */
    @Override
    public void addBlockedServiceName(final String serviceName) {
        addBlockedServiceNameOperation(serviceName, ALL_OPERATIONS);
    }

    /** Adds a blocked operation, canonicalizing names that begin with a slash as request paths. */
    @Override
    public synchronized void addBlockedServiceNameOperation(final String serviceName, final String operation) {
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(operation);

        // See addAllowedServiceNameOperation's Javadoc-style comment: single canonicalization point,
        // guarded to skip anything that is not a path, mirrored here for the blocklist.
        final String canonicalServiceName = canonicalizeServiceIdentifier(serviceName);

        Map<String, String> operations = blockedOperationsByServiceTypeNameMap.get(canonicalServiceName);
        if (operations == null) {
            operations = new ConcurrentHashMap<>();
        }
        operations.put(operation, operation);
        blockedOperationsByServiceTypeNameMap.put(canonicalServiceName, operations);
    }
}
