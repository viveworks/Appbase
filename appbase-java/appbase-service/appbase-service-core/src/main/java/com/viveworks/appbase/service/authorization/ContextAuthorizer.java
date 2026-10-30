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

/**
 * Application policy hook invoked when service allow/block maps do not make the decision.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ContextAuthorizer {
    /**
     * Applies contextual policy to a named service.
     * @param service service name to test
     * @param context authorization context
     * @return whether the context permits access
     */
    boolean isAuthorizedContext(String service, AuthorizationContext context);

    /**
     * Applies contextual policy to a named service operation.
     * @param service service name to test
     * @param operation operation name to test
     * @param context authorization context
     * @return whether the context permits access
     */
    boolean isAuthorizedContext(String service, String operation, AuthorizationContext context);

    /**
     * Applies contextual policy to a service type.
     * @param serviceType service class to test
     * @param context authorization context
     * @return whether the context permits access
     */
    boolean isAuthorizedContext(Class<?> serviceType, AuthorizationContext context);

    /**
     * Applies contextual policy to a service-type operation.
     * @param serviceType service class to test
     * @param operation operation name to test
     * @param context authorization context
     * @return whether the context permits access
     */
    boolean isAuthorizedContext(Class<?> serviceType, String operation, AuthorizationContext context);
}
