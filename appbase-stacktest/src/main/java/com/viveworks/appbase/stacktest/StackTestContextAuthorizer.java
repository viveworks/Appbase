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

package com.viveworks.appbase.stacktest;

import com.viveworks.appbase.service.authorization.AuthorizationContext;
import com.viveworks.appbase.service.authorization.AuthorizationService;
import com.viveworks.appbase.service.authorization.ContextAuthorizer;

import java.util.UUID;

/**
 * Stack-test {@link ContextAuthorizer}: authorizes or rejects based on known requester-ID constants.
 * Unknown requesters are allowed by default.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class StackTestContextAuthorizer implements ContextAuthorizer {

    /** A pre-known requester ID that is always authorized. */
    public static final String AUTHORIZED_DUMMY_REQUESTER_ID = UUID.randomUUID().toString();

    /** A pre-known requester ID that is always denied. */
    public static final String UNAUTHORIZED_DUMMY_REQUESTER_ID = UUID.randomUUID().toString();

    @Override
    public boolean isAuthorizedContext(final String service, final AuthorizationContext context) {
        return isAuthorizedContext(service, AuthorizationService.ALL_OPERATIONS, context);
    }

    @Override
    public boolean isAuthorizedContext(final Class<?> serviceType, final AuthorizationContext context) {
        return isAuthorizedContext(serviceType, AuthorizationService.ALL_OPERATIONS, context);
    }

    @Override
    public boolean isAuthorizedContext(
            final Class<?> serviceType,
            final String operation,
            final AuthorizationContext context)
    {
        return isAuthorizedContext(serviceType.getName(), operation, context);
    }

    @Override
    public boolean isAuthorizedContext(
            final String service,
            final String operation,
            final AuthorizationContext context)
    {
        if (context != null) {
            final String requesterId = context.getAppbaseRequesterId();
            if (requesterId != null) {
                if (AUTHORIZED_DUMMY_REQUESTER_ID.equals(requesterId)) {
                    return true;
                }
                return !UNAUTHORIZED_DUMMY_REQUESTER_ID.equals(requesterId);
            }
        }
        return true;
    }
}
