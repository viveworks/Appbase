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

/**
 * A {@link ContextAuthorizer} that authorizes every request.
 *
 * <p><strong>This authorizes everything.</strong> It was previously named
 * {@code DefaultContextAuthorizer}, which read as though it were the safe thing you get without
 * thinking about it. It is not, and it is not wired by default -- an application must pass it
 * explicitly to {@code AppbaseServices.Builder.withContextAuthorizer(...)}. Useful for local
 * development and tests. Passing it in production disables path authorization entirely.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class PermitAllContextAuthorizer implements ContextAuthorizer {
    /** Authorizes a named service without inspecting its context. */
    @Override
    public boolean isAuthorizedContext(
            final String service,
            final AuthorizationContext context)
    {
        return isAuthorizedContext(service, AuthorizationService.ALL_OPERATIONS, context);
    }

    /** Authorizes a named service operation unconditionally. */
    @Override
    public boolean isAuthorizedContext(
            final String service,
            final String operation,
            final AuthorizationContext context)
    {
        return true;
    }

    /** Authorizes a service type unconditionally. */
    @Override
    public boolean isAuthorizedContext(final Class<?> serviceType, final AuthorizationContext context) {
        return isAuthorizedContext(serviceType, AuthorizationService.ALL_OPERATIONS, context);
    }

    /** Authorizes a service-type operation unconditionally. */
    @Override
    public boolean isAuthorizedContext(
            final Class<?> serviceType,
            final String operation,
            final AuthorizationContext context)
    {
        return true;
    }
}
