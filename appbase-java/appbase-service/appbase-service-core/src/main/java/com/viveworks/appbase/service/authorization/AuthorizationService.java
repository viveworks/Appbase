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

import com.viveworks.appbase.core.service.AppbaseService;

/**
 * Service-level allow/block authorization contract; the wildcard operation token matches every operation.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface AuthorizationService extends AppbaseService {
    /** Wildcard operation name used to grant or block every operation for one service. */
    String ALL_OPERATIONS = "*";

    /**
     * Tests authorization for a named service.
     *
     * @param service service name to test
     * @param context request context used by the policy
     * @return whether access is authorized
     */
    boolean isAuthorized(String service, AuthorizationContext context);

    /**
     * Tests authorization for a service type.
     *
     * @param serviceType service class to test
     * @param context request context used by the policy
     * @return whether access is authorized
     */
    boolean isAuthorized(Class<?> serviceType, AuthorizationContext context);

    /**
     * Tests authorization for an operation on a named service.
     *
     * @param service service name to test
     * @param operation operation name to test
     * @param context request context used by the policy
     * @return whether access is authorized
     */
    boolean isAuthorized(String service, String operation, AuthorizationContext context);

    /**
     * Tests authorization for an operation on a service type.
     *
     * @param serviceType service class to test
     * @param operation operation name to test
     * @param context request context used by the policy
     * @return whether access is authorized
     */
    boolean isAuthorized(Class<?> serviceType, String operation, AuthorizationContext context);

    /**
     * Requires authorization for a named service.
     *
     * @param service service name to check
     * @param context request context used by the policy
     * @throws AuthorizationServiceException when access is denied
     */
    void checkAuthorized(String service, AuthorizationContext context) throws AuthorizationServiceException;

    /**
     * Requires authorization for a service type.
     *
     * @param serviceType service class to check
     * @param context request context used by the policy
     * @throws AuthorizationServiceException when access is denied
     */
    void checkAuthorized(Class<?> serviceType, AuthorizationContext context) throws AuthorizationServiceException;

    /**
     * Requires authorization for an operation on a named service.
     *
     * @param service service name to check
     * @param operation operation name to check
     * @param context request context used by the policy
     * @throws AuthorizationServiceException when access is denied
     */
    void checkAuthorized(String service, String operation, AuthorizationContext context)
            throws AuthorizationServiceException;

    /**
     * Requires authorization for an operation on a service type.
     *
     * @param serviceType service class to check
     * @param operation operation name to check
     * @param context request context used by the policy
     * @throws AuthorizationServiceException when access is denied
     */
    void checkAuthorized(Class<?> serviceType, String operation, AuthorizationContext context)
            throws AuthorizationServiceException;

    /**
     * Adds a service type to the allow rules for all operations.
     *
     * @param serviceType service class to allow
     */
    void addAllowedService(Class<?> serviceType);

    /**
     * Adds an operation to the allow rules for a service type.
     * @param serviceType service class to allow
     * @param operation operation name to allow
     */
    void addAllowedServiceOperation(Class<?> serviceType, String operation);

    /**
     * Adds a named service to the allow rules for all operations.
     *
     * @param serviceName service name to allow
     */
    void addAllowedServiceName(String serviceName);

    /**
     * Adds an operation to the allow rules for a named service.
     * @param serviceName service name to allow; a name starting with {@code /} is treated as a request path and
     *        canonicalized, any other name is stored as given
     * @param operation operation name to allow, stored as given
     */
    void addAllowedServiceNameOperation(String serviceName, String operation);

    /**
     * Adds a service type to the block rules for all operations.
     *
     * @param serviceType service class to block
     */
    void addBlockedService(Class<?> serviceType);

    /**
     * Adds an operation to the block rules for a service type.
     * @param serviceType service class to block
     * @param operation operation name to block
     */
    void addBlockedServiceOperation(Class<?> serviceType, String operation);

    /**
     * Adds a named service to the block rules for all operations.
     *
     * @param serviceName service name to block
     */
    void addBlockedServiceName(String serviceName);

    /**
     * Adds an operation to the block rules for a named service.
     * @param serviceName service name to block; a name starting with {@code /} is treated as a request path and
     *        canonicalized, any other name is stored as given
     * @param operation operation name to block, stored as given
     */
    void addBlockedServiceNameOperation(String serviceName, String operation);
}
