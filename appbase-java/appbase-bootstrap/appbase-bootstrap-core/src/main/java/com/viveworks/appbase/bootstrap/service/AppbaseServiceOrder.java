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

/**
 * Initialization order for the built-in {@link AppbaseServiceFactory} implementations.
 *
 * <p>Lower values initialize first. Cross-service dependencies dictate the ordering:
 * <ul>
 *   <li>{@link #USER_SESSION_STORE} depends on {@link #ID} (IdService)</li>
 *   <li>{@link #USER_ACCOUNT_STORE} depends on {@link #PASSWORD} (PasswordService)</li>
 *   <li>{@link #USER_ACCOUNT} depends on {@link #ID}, {@link #CLUSTER}, {@link #AUTHORIZATION},
 *       {@link #PASSWORD}, {@link #USER_SESSION_STORE}, {@link #USER_ACCOUNT_STORE},
 *       and {@link #FIREBASE_AUTH}</li>
 *   <li>{@link #SUPPORT_MESSAGE} depends on {@link #ID} and {@link #SUPPORT_MESSAGE_STORE}</li>
 * </ul>
 *
 * <p>External factories may supply any {@code int} value to slot between or after built-in values.
 */
public enum AppbaseServiceOrder {

    /** {@link com.viveworks.appbase.service.authorization.AuthorizationService} */
    AUTHORIZATION(10),

    /** {@link com.viveworks.appbase.service.cluster.ClusterService} */
    CLUSTER(20),

    /** {@link com.viveworks.appbase.service.health.HealthCheckService} */
    HEALTH_CHECK(30),

    /** {@link com.viveworks.appbase.service.id.IdService} */
    ID(40),

    /** {@link com.viveworks.appbase.service.password.PasswordService} */
    PASSWORD(50),

    /** {@link com.viveworks.appbase.service.user.account.firebaseauth.FirebaseAuthService} */
    FIREBASE_AUTH(60),

    /** {@link com.viveworks.appbase.service.user.session.store.UserSessionStore} */
    USER_SESSION_STORE(70),

    /** {@link com.viveworks.appbase.service.user.account.store.UserAccountStore} */
    USER_ACCOUNT_STORE(80),

    /** {@link com.viveworks.appbase.service.user.account.UserAccountService} */
    USER_ACCOUNT(90),

    /** {@link com.viveworks.appbase.service.supportmessage.store.SupportMessageStore} */
    SUPPORT_MESSAGE_STORE(100),

    /** {@link com.viveworks.appbase.service.supportmessage.SupportMessageService} */
    SUPPORT_MESSAGE(110);

    private final int order;

    AppbaseServiceOrder(final int order) {
        this.order = order;
    }

    /**
     * Returns the numeric initialization order value.
     *
     * @return initialization order; lower values initialize first
     */
    public int order() {
        return order;
    }
}
