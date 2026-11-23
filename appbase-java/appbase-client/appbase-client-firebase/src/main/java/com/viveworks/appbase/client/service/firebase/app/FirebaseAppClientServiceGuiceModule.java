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

package com.viveworks.appbase.client.service.firebase.app;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link FirebaseAppClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class FirebaseAppClientServiceGuiceModule extends AbstractGenericGuiceModule<FirebaseAppClientService> {
    public FirebaseAppClientServiceGuiceModule() {
        super(FirebaseAppClientService.class, DefaultFirebaseAppClientService.class);
    }

    public FirebaseAppClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(FirebaseAppClientService.class, DefaultFirebaseAppClientService.class, qualifierBindingAnnotation);
    }

    public FirebaseAppClientServiceGuiceModule(final String qualifierBindingName) {
        super(FirebaseAppClientService.class, DefaultFirebaseAppClientService.class, qualifierBindingName);
    }



    public FirebaseAppClientServiceGuiceModule(final FirebaseAppClientService firebaseAppClientService) {
        super(FirebaseAppClientService.class, firebaseAppClientService);
    }

    public FirebaseAppClientServiceGuiceModule(
            final FirebaseAppClientService firebaseAppClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(FirebaseAppClientService.class, firebaseAppClientService, qualifierBindingAnnotation);
    }

    public FirebaseAppClientServiceGuiceModule(
            final FirebaseAppClientService firebaseAppClientService,
            final String qualifierBindingName)
    {
        super(FirebaseAppClientService.class, firebaseAppClientService, qualifierBindingName);
    }



    public FirebaseAppClientServiceGuiceModule(
            final Provider<? extends FirebaseAppClientService> firebaseAppClientServiceProvider)
    {
        super(FirebaseAppClientService.class, firebaseAppClientServiceProvider);
    }

    public FirebaseAppClientServiceGuiceModule(
            final Provider<? extends FirebaseAppClientService> firebaseAppClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(FirebaseAppClientService.class, firebaseAppClientServiceProvider, qualifierBindingAnnotation);
    }

    public FirebaseAppClientServiceGuiceModule(
            final Provider<? extends FirebaseAppClientService> firebaseAppClientServiceProvider,
            final String qualifierBindingName)
    {
        super(FirebaseAppClientService.class, firebaseAppClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultFirebaseAppClientService.registerObjectMapperSubtypes();
    }
}
