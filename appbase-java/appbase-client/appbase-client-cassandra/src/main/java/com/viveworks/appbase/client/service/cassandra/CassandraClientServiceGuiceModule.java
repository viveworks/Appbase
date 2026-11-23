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

package com.viveworks.appbase.client.service.cassandra;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link CassandraClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class CassandraClientServiceGuiceModule extends AbstractGenericGuiceModule<CassandraClientService> {
    public CassandraClientServiceGuiceModule() {
        super(CassandraClientService.class, DefaultCassandraClientService.class);
    }

    public CassandraClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(CassandraClientService.class, DefaultCassandraClientService.class, qualifierBindingAnnotation);
    }

    public CassandraClientServiceGuiceModule(final String qualifierBindingName) {
        super(CassandraClientService.class, DefaultCassandraClientService.class, qualifierBindingName);
    }



    public CassandraClientServiceGuiceModule(final CassandraClientService cassandraClientService) {
        super(CassandraClientService.class, cassandraClientService);
    }

    public CassandraClientServiceGuiceModule(
            final CassandraClientService cassandraClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(CassandraClientService.class, cassandraClientService, qualifierBindingAnnotation);
    }

    public CassandraClientServiceGuiceModule(
            final CassandraClientService cassandraClientService,
            final String qualifierBindingName)
    {
        super(CassandraClientService.class, cassandraClientService, qualifierBindingName);
    }



    public CassandraClientServiceGuiceModule(
            final Provider<? extends CassandraClientService> cassandraClientServiceProvider)
    {
        super(CassandraClientService.class, cassandraClientServiceProvider);
    }

    public CassandraClientServiceGuiceModule(
            final Provider<? extends CassandraClientService> cassandraClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(CassandraClientService.class, cassandraClientServiceProvider, qualifierBindingAnnotation);
    }

    public CassandraClientServiceGuiceModule(
            final Provider<? extends CassandraClientService> cassandraClientServiceProvider,
            final String qualifierBindingName)
    {
        super(CassandraClientService.class, cassandraClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultCassandraClientService.registerObjectMapperSubtypes();
    }
}
