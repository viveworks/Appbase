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

package com.viveworks.appbase.client.service.spark;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link SparkClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SparkClientServiceGuiceModule extends AbstractGenericGuiceModule<SparkClientService> {
    public SparkClientServiceGuiceModule() {
        super(SparkClientService.class, DefaultSparkClientService.class);
    }

    public SparkClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(SparkClientService.class, DefaultSparkClientService.class, qualifierBindingAnnotation);
    }

    public SparkClientServiceGuiceModule(final String qualifierBindingName) {
        super(SparkClientService.class, DefaultSparkClientService.class, qualifierBindingName);
    }



    public SparkClientServiceGuiceModule(final SparkClientService sparkClientService) {
        super(SparkClientService.class, sparkClientService);
    }

    public SparkClientServiceGuiceModule(
            final SparkClientService sparkClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(SparkClientService.class, sparkClientService, qualifierBindingAnnotation);
    }

    public SparkClientServiceGuiceModule(
            final SparkClientService sparkClientService,
            final String qualifierBindingName)
    {
        super(SparkClientService.class, sparkClientService, qualifierBindingName);
    }



    public SparkClientServiceGuiceModule(final Provider<? extends SparkClientService> sparkClientServiceProvider) {
        super(SparkClientService.class, sparkClientServiceProvider);
    }

    public SparkClientServiceGuiceModule(
            final Provider<? extends SparkClientService> sparkClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(SparkClientService.class, sparkClientServiceProvider, qualifierBindingAnnotation);
    }

    public SparkClientServiceGuiceModule(
            final Provider<? extends SparkClientService> sparkClientServiceProvider,
            final String qualifierBindingName)
    {
        super(SparkClientService.class, sparkClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultSparkClientService.registerObjectMapperSubtypes();
    }
}
