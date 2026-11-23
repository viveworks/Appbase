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

package com.viveworks.appbase.client.cloud.aws.httpclient;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link AwsSdkAsyncHttpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AwsSdkAsyncHttpClientServiceGuiceModule extends AbstractGenericGuiceModule<AwsSdkAsyncHttpClientService> {
    /**
     * Creates an unqualified module binding the service interface to its default implementation.
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule() {
        super(AwsSdkAsyncHttpClientService.class, DefaultAwsSdkAsyncHttpClientService.class);
    }

    /**
     * Creates a module whose default-implementation binding uses an annotation qualifier.
     *
     * @param qualifierBindingAnnotation annotation qualifying the binding
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(AwsSdkAsyncHttpClientService.class, DefaultAwsSdkAsyncHttpClientService.class, qualifierBindingAnnotation);
    }

    /**
     * Creates a module whose default-implementation binding uses a named qualifier.
     *
     * @param qualifierBindingName name qualifying the binding
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(final String qualifierBindingName) {
        super(AwsSdkAsyncHttpClientService.class, DefaultAwsSdkAsyncHttpClientService.class, qualifierBindingName);
    }



    /**
     * Creates an unqualified module binding the supplied service instance.
     *
     * @param awsSdkAsyncHttpClientService service instance to bind
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService) {
        super(AwsSdkAsyncHttpClientService.class, awsSdkAsyncHttpClientService);
    }

    /**
     * Creates a module binding the supplied service instance with an annotation qualifier.
     *
     * @param awsSdkAsyncHttpClientService service instance to bind
     * @param qualifierBindingAnnotation annotation qualifying the binding
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(AwsSdkAsyncHttpClientService.class, awsSdkAsyncHttpClientService, qualifierBindingAnnotation);
    }

    /**
     * Creates a module binding the supplied service instance with a named qualifier.
     *
     * @param awsSdkAsyncHttpClientService service instance to bind
     * @param qualifierBindingName name qualifying the binding
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(
            final AwsSdkAsyncHttpClientService awsSdkAsyncHttpClientService,
            final String qualifierBindingName)
    {
        super(AwsSdkAsyncHttpClientService.class, awsSdkAsyncHttpClientService, qualifierBindingName);
    }



    /**
     * Creates an unqualified module binding the supplied service provider.
     *
     * @param awsSdkAsyncHttpClientServiceProvider provider to bind
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(
            final Provider<? extends AwsSdkAsyncHttpClientService> awsSdkAsyncHttpClientServiceProvider)
    {
        super(AwsSdkAsyncHttpClientService.class, awsSdkAsyncHttpClientServiceProvider);
    }

    /**
     * Creates a module binding the supplied service provider with an annotation qualifier.
     *
     * @param awsSdkAsyncHttpClientServiceProvider provider to bind
     * @param qualifierBindingAnnotation annotation qualifying the binding
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(
            final Provider<? extends AwsSdkAsyncHttpClientService> awsSdkAsyncHttpClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(AwsSdkAsyncHttpClientService.class, awsSdkAsyncHttpClientServiceProvider, qualifierBindingAnnotation);
    }

    /**
     * Creates a module binding the supplied service provider with a named qualifier.
     *
     * @param awsSdkAsyncHttpClientServiceProvider provider to bind
     * @param qualifierBindingName name qualifying the binding
     */
    public AwsSdkAsyncHttpClientServiceGuiceModule(
            final Provider<? extends AwsSdkAsyncHttpClientService> awsSdkAsyncHttpClientServiceProvider,
            final String qualifierBindingName)
    {
        super(AwsSdkAsyncHttpClientService.class, awsSdkAsyncHttpClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultAwsSdkAsyncHttpClientService.registerObjectMapperSubtypes();
    }
}
