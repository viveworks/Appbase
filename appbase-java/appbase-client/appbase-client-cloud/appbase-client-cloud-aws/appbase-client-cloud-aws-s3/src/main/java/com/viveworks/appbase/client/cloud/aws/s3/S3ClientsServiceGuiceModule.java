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

package com.viveworks.appbase.client.cloud.aws.s3;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link S3ClientsService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class S3ClientsServiceGuiceModule extends AbstractGenericGuiceModule<S3ClientsService> {
    /**
     * Creates an unqualified module binding the service interface to its default implementation.
     */
    public S3ClientsServiceGuiceModule() {
        super(S3ClientsService.class, DefaultS3ClientsService.class);
    }

    /**
     * Creates a module whose default-implementation binding uses an annotation qualifier.
     *
     * @param qualifierBindingAnnotation annotation qualifying the binding
     */
    public S3ClientsServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(S3ClientsService.class, DefaultS3ClientsService.class, qualifierBindingAnnotation);
    }

    /**
     * Creates a module whose default-implementation binding uses a named qualifier.
     *
     * @param qualifierBindingName name qualifying the binding
     */
    public S3ClientsServiceGuiceModule(final String qualifierBindingName) {
        super(S3ClientsService.class, DefaultS3ClientsService.class, qualifierBindingName);
    }



    /**
     * Creates an unqualified module binding the supplied service instance.
     *
     * @param s3ClientsService service instance to bind
     */
    public S3ClientsServiceGuiceModule(final S3ClientsService s3ClientsService) {
        super(S3ClientsService.class, s3ClientsService);
    }

    /**
     * Creates a module binding the supplied service instance with an annotation qualifier.
     *
     * @param s3ClientsService service instance to bind
     * @param qualifierBindingAnnotation annotation qualifying the binding
     */
    public S3ClientsServiceGuiceModule(final S3ClientsService s3ClientsService, final Annotation qualifierBindingAnnotation) {
        super(S3ClientsService.class, s3ClientsService, qualifierBindingAnnotation);
    }

    /**
     * Creates a module binding the supplied service instance with a named qualifier.
     *
     * @param s3ClientsService service instance to bind
     * @param qualifierBindingName name qualifying the binding
     */
    public S3ClientsServiceGuiceModule(final S3ClientsService s3ClientsService, final String qualifierBindingName) {
        super(S3ClientsService.class, s3ClientsService, qualifierBindingName);
    }



    /**
     * Creates an unqualified module binding the supplied service provider.
     *
     * @param s3ClientServiceProvider provider to bind
     */
    public S3ClientsServiceGuiceModule(final Provider<? extends S3ClientsService> s3ClientServiceProvider) {
        super(S3ClientsService.class, s3ClientServiceProvider);
    }

    /**
     * Creates a module binding the supplied service provider with an annotation qualifier.
     *
     * @param s3ClientServiceProvider provider to bind
     * @param qualifierBindingAnnotation annotation qualifying the binding
     */
    public S3ClientsServiceGuiceModule(
            final Provider<? extends S3ClientsService> s3ClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(S3ClientsService.class, s3ClientServiceProvider, qualifierBindingAnnotation);
    }

    /**
     * Creates a module binding the supplied service provider with a named qualifier.
     *
     * @param s3ClientServiceProvider provider to bind
     * @param qualifierBindingName name qualifying the binding
     */
    public S3ClientsServiceGuiceModule(
            final Provider<? extends S3ClientsService> s3ClientServiceProvider,
            final String qualifierBindingName)
    {
        super(S3ClientsService.class, s3ClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultS3ClientsService.registerObjectMapperSubtypes();
    }
}
