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

package com.viveworks.appbase.client.service.jasync;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link JasyncClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JasyncClientServiceGuiceModule extends AbstractGenericGuiceModule<JasyncClientService> {
    public JasyncClientServiceGuiceModule() {
        super(JasyncClientService.class, DefaultJasyncClientService.class);
    }

    public JasyncClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(JasyncClientService.class, DefaultJasyncClientService.class, qualifierBindingAnnotation);
    }

    public JasyncClientServiceGuiceModule(final String qualifierBindingName) {
        super(JasyncClientService.class, DefaultJasyncClientService.class, qualifierBindingName);
    }



    public JasyncClientServiceGuiceModule(final JasyncClientService jasyncClientService) {
        super(JasyncClientService.class, jasyncClientService);
    }

    public JasyncClientServiceGuiceModule(
            final JasyncClientService jasyncClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(JasyncClientService.class, jasyncClientService, qualifierBindingAnnotation);
    }

    public JasyncClientServiceGuiceModule(
            final JasyncClientService jasyncClientService,
            final String qualifierBindingName)
    {
        super(JasyncClientService.class, jasyncClientService, qualifierBindingName);
    }



    public JasyncClientServiceGuiceModule(final Provider<? extends JasyncClientService> jasyncClientServiceProvider) {
        super(JasyncClientService.class, jasyncClientServiceProvider);
    }

    public JasyncClientServiceGuiceModule(
            final Provider<? extends JasyncClientService> jasyncClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(JasyncClientService.class, jasyncClientServiceProvider, qualifierBindingAnnotation);
    }

    public JasyncClientServiceGuiceModule(
            final Provider<? extends JasyncClientService> jasyncClientServiceProvider,
            final String qualifierBindingName)
    {
        super(JasyncClientService.class, jasyncClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultJasyncClientService.registerObjectMapperSubtypes();
    }
}
