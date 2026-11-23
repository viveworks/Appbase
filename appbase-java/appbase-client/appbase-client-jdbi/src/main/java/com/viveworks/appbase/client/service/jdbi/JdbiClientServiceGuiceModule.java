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

/**
 * This package contains client services for {@code JDBC} (Java Database Connectivity).
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */

package com.viveworks.appbase.client.service.jdbi;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link JdbiClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JdbiClientServiceGuiceModule extends AbstractGenericGuiceModule<JdbiClientService> {
    public JdbiClientServiceGuiceModule() {
        super(JdbiClientService.class, DefaultJdbiClientService.class);
    }

    public JdbiClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(JdbiClientService.class, DefaultJdbiClientService.class, qualifierBindingAnnotation);
    }

    public JdbiClientServiceGuiceModule(final String qualifierBindingName) {
        super(JdbiClientService.class, DefaultJdbiClientService.class, qualifierBindingName);
    }



    public JdbiClientServiceGuiceModule(final JdbiClientService jdbiClientService) {
        super(JdbiClientService.class, jdbiClientService);
    }

    public JdbiClientServiceGuiceModule(
            final JdbiClientService jdbiClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(JdbiClientService.class, jdbiClientService, qualifierBindingAnnotation);
    }

    public JdbiClientServiceGuiceModule(
            final JdbiClientService jdbiClientService,
            final String qualifierBindingName)
    {
        super(JdbiClientService.class, jdbiClientService, qualifierBindingName);
    }



    public JdbiClientServiceGuiceModule(final Provider<? extends JdbiClientService> jdbiClientServiceProvider) {
        super(JdbiClientService.class, jdbiClientServiceProvider);
    }

    public JdbiClientServiceGuiceModule(
            final Provider<? extends JdbiClientService> jdbiClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(JdbiClientService.class, jdbiClientServiceProvider, qualifierBindingAnnotation);
    }

    public JdbiClientServiceGuiceModule(
            final Provider<? extends JdbiClientService> jdbiClientServiceProvider,
            final String qualifierBindingName)
    {
        super(JdbiClientService.class, jdbiClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultJdbiClientService.registerObjectMapperSubtypes();
    }
}
