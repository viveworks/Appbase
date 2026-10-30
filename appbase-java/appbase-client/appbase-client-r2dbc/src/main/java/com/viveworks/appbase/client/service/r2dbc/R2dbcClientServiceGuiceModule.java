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

package com.viveworks.appbase.client.service.r2dbc;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link R2dbcClientService}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class R2dbcClientServiceGuiceModule extends AbstractGenericGuiceModule<R2dbcClientService> {
    public R2dbcClientServiceGuiceModule() {
        super(R2dbcClientService.class, DefaultR2dbcClientService.class);
    }

    public R2dbcClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(R2dbcClientService.class, DefaultR2dbcClientService.class, qualifierBindingAnnotation);
    }

    public R2dbcClientServiceGuiceModule(final String qualifierBindingName) {
        super(R2dbcClientService.class, DefaultR2dbcClientService.class, qualifierBindingName);
    }



    public R2dbcClientServiceGuiceModule(final R2dbcClientService r2dbcClientService) {
        super(R2dbcClientService.class, r2dbcClientService);
    }

    public R2dbcClientServiceGuiceModule(
            final R2dbcClientService r2dbcClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(R2dbcClientService.class, r2dbcClientService, qualifierBindingAnnotation);
    }

    public R2dbcClientServiceGuiceModule(
            final R2dbcClientService r2dbcClientService,
            final String qualifierBindingName)
    {
        super(R2dbcClientService.class, r2dbcClientService, qualifierBindingName);
    }



    public R2dbcClientServiceGuiceModule(final Provider<? extends R2dbcClientService> r2dbcClientServiceProvider) {
        super(R2dbcClientService.class, r2dbcClientServiceProvider);
    }

    public R2dbcClientServiceGuiceModule(
            final Provider<? extends R2dbcClientService> r2dbcClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(R2dbcClientService.class, r2dbcClientServiceProvider, qualifierBindingAnnotation);
    }

    public R2dbcClientServiceGuiceModule(
            final Provider<? extends R2dbcClientService> r2dbcClientServiceProvider,
            final String qualifierBindingName)
    {
        super(R2dbcClientService.class, r2dbcClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultR2dbcClientService.registerObjectMapperSubtypes();
    }
}
