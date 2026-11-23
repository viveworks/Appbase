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

package com.viveworks.appbase.client.service.jpa;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link JpaClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JpaClientServiceGuiceModule extends AbstractGenericGuiceModule<JpaClientService> {
    public JpaClientServiceGuiceModule() {
        super(JpaClientService.class, DefaultJpaClientService.class);
    }

    public JpaClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(JpaClientService.class, DefaultJpaClientService.class, qualifierBindingAnnotation);
    }

    public JpaClientServiceGuiceModule(final String qualifierBindingName) {
        super(JpaClientService.class, DefaultJpaClientService.class, qualifierBindingName);
    }



    public JpaClientServiceGuiceModule(final JpaClientService jpaClientService) {
        super(JpaClientService.class, jpaClientService);
    }

    public JpaClientServiceGuiceModule(
            final JpaClientService jpaClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(JpaClientService.class, jpaClientService, qualifierBindingAnnotation);
    }

    public JpaClientServiceGuiceModule(
            final JpaClientService jpaClientService,
            final String qualifierBindingName)
    {
        super(JpaClientService.class, jpaClientService, qualifierBindingName);
    }



    public JpaClientServiceGuiceModule(final Provider<? extends JpaClientService> jpaClientServiceProvider) {
        super(JpaClientService.class, jpaClientServiceProvider);
    }

    public JpaClientServiceGuiceModule(
            final Provider<? extends JpaClientService> jpaClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(JpaClientService.class, jpaClientServiceProvider, qualifierBindingAnnotation);
    }

    public JpaClientServiceGuiceModule(
            final Provider<? extends JpaClientService> jpaClientServiceProvider,
            final String qualifierBindingName)
    {
        super(JpaClientService.class, jpaClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultJpaClientService.registerObjectMapperSubtypes();
    }
}
