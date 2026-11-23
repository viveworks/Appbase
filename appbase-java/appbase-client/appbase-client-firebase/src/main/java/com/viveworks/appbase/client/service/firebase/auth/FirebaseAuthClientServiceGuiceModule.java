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

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link FirebaseAuthClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class FirebaseAuthClientServiceGuiceModule extends AbstractGenericGuiceModule<FirebaseAuthClientService> {
    public FirebaseAuthClientServiceGuiceModule() {
        super(FirebaseAuthClientService.class, DefaultFirebaseAuthClientService.class);
    }

    public FirebaseAuthClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(FirebaseAuthClientService.class, DefaultFirebaseAuthClientService.class, qualifierBindingAnnotation);
    }

    public FirebaseAuthClientServiceGuiceModule(final String qualifierBindingName) {
        super(FirebaseAuthClientService.class, DefaultFirebaseAuthClientService.class, qualifierBindingName);
    }



    public FirebaseAuthClientServiceGuiceModule(final FirebaseAuthClientService firebaseAuthClientService) {
        super(FirebaseAuthClientService.class, firebaseAuthClientService);
    }

    public FirebaseAuthClientServiceGuiceModule(
            final FirebaseAuthClientService firebaseAuthClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(FirebaseAuthClientService.class, firebaseAuthClientService, qualifierBindingAnnotation);
    }

    public FirebaseAuthClientServiceGuiceModule(
            final FirebaseAuthClientService firebaseAuthClientService,
            final String qualifierBindingName)
    {
        super(FirebaseAuthClientService.class, firebaseAuthClientService, qualifierBindingName);
    }



    public FirebaseAuthClientServiceGuiceModule(
            final Provider<? extends FirebaseAuthClientService> firebaseAuthClientServiceProvider)
    {
        super(FirebaseAuthClientService.class, firebaseAuthClientServiceProvider);
    }

    public FirebaseAuthClientServiceGuiceModule(
            final Provider<? extends FirebaseAuthClientService> firebaseAuthClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(FirebaseAuthClientService.class, firebaseAuthClientServiceProvider, qualifierBindingAnnotation);
    }

    public FirebaseAuthClientServiceGuiceModule(
            final Provider<? extends FirebaseAuthClientService> firebaseAuthClientServiceProvider,
            final String qualifierBindingName)
    {
        super(FirebaseAuthClientService.class, firebaseAuthClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultFirebaseAuthClientService.registerObjectMapperSubtypes();
    }
}
