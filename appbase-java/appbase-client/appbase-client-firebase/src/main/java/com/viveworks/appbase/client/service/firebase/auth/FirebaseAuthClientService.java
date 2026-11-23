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

package com.viveworks.appbase.client.service.firebase.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;
import com.viveworks.appbase.client.service.firebase.auth.accessor.FirebaseAuthAccessor;
import com.viveworks.appbase.client.service.firebase.auth.accessor.reactive.ReactiveFirebaseAuthAccessor;

/**
 * Represents {@link AppbaseClientService} for {@link com.google.firebase.auth.FirebaseAuth}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface FirebaseAuthClientService extends AppbaseClientService<FirebaseAuth, TextValue> {
    @Override
    FirebaseAuthClientIntegrationService getClientIntegrationService()
            throws AppbaseClientIntegrationService.NotFoundException;


    @Override
    FirebaseAuthClientIntegrationService getClientIntegrationService(AppbaseClientKey<TextValue> appbaseClientKey)
            throws AppbaseClientIntegrationService.NotFoundException;

    /**
     * Returns a {@link ReactiveFirebaseAuthAccessor} bound to this service's default Firebase Auth client.
     *
     * @return reactive accessor for the default Firebase Auth client
     */
    ReactiveFirebaseAuthAccessor getReactiveAccessor();

    /**
     * Returns a {@link ReactiveFirebaseAuthAccessor} bound to the Firebase Auth client for the given key.
     *
     * @param appbaseClientKey key identifying the target Firebase Auth client
     * @return reactive accessor for the keyed Firebase Auth client
     */
    ReactiveFirebaseAuthAccessor getReactiveAccessor(AppbaseClientKey<TextValue> appbaseClientKey);

    /**
     * Returns a {@link FirebaseAuthAccessor} bound to this service's default Firebase Auth client.
     *
     * @return synchronous accessor for the default Firebase Auth client
     */
    FirebaseAuthAccessor getAccessor();

    /**
     * Returns a {@link FirebaseAuthAccessor} bound to the Firebase Auth client for the given key.
     *
     * @param appbaseClientKey key identifying the target Firebase Auth client
     * @return synchronous accessor for the keyed Firebase Auth client
     */
    FirebaseAuthAccessor getAccessor(AppbaseClientKey<TextValue> appbaseClientKey);
}
