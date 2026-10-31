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

package com.viveworks.appbase.client.service.aerospike;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link AerospikeClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AerospikeClientServiceGuiceModule extends AbstractGenericGuiceModule<AerospikeClientService> {
    public AerospikeClientServiceGuiceModule() {
        super(AerospikeClientService.class, DefaultAerospikeClientService.class);
    }

    public AerospikeClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(AerospikeClientService.class, DefaultAerospikeClientService.class, qualifierBindingAnnotation);
    }

    public AerospikeClientServiceGuiceModule(final String qualifierBindingName) {
        super(AerospikeClientService.class, DefaultAerospikeClientService.class, qualifierBindingName);
    }



    public AerospikeClientServiceGuiceModule(
            final AerospikeClientService aerospikeClientService)
    {
        super(AerospikeClientService.class, aerospikeClientService);
    }

    public AerospikeClientServiceGuiceModule(
            final AerospikeClientService aerospikeClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(AerospikeClientService.class, aerospikeClientService, qualifierBindingAnnotation);
    }

    public AerospikeClientServiceGuiceModule(
            final AerospikeClientService aerospikeClientService,
            final String qualifierBindingName)
    {
        super(AerospikeClientService.class, aerospikeClientService, qualifierBindingName);
    }



    public AerospikeClientServiceGuiceModule(
            final Provider<? extends AerospikeClientService> aerospikeClientServiceProvider)
    {
        super(AerospikeClientService.class, aerospikeClientServiceProvider);
    }

    public AerospikeClientServiceGuiceModule(
            final Provider<? extends AerospikeClientService> aerospikeClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(AerospikeClientService.class, aerospikeClientServiceProvider, qualifierBindingAnnotation);
    }

    public AerospikeClientServiceGuiceModule(
            final Provider<? extends AerospikeClientService> aerospikeClientServiceProvider,
            final String qualifierBindingName)
    {
        super(AerospikeClientService.class, aerospikeClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultAerospikeClientService.registerObjectMapperSubtypes();
    }
}
