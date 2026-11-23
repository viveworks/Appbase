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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link VitessGrpcClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class VitessGrpcClientServiceGuiceModule extends AbstractGenericGuiceModule<VitessGrpcClientService> {
    public VitessGrpcClientServiceGuiceModule() {
        super(VitessGrpcClientService.class, DefaultVitessGrpcClientService.class);
    }

    public VitessGrpcClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(VitessGrpcClientService.class, DefaultVitessGrpcClientService.class, qualifierBindingAnnotation);
    }

    public VitessGrpcClientServiceGuiceModule(final String qualifierBindingName) {
        super(VitessGrpcClientService.class, DefaultVitessGrpcClientService.class, qualifierBindingName);
    }



    public VitessGrpcClientServiceGuiceModule(final VitessGrpcClientService vitessGrpcClientService) {
        super(VitessGrpcClientService.class, vitessGrpcClientService);
    }

    public VitessGrpcClientServiceGuiceModule(
            final VitessGrpcClientService vitessGrpcClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(VitessGrpcClientService.class, vitessGrpcClientService, qualifierBindingAnnotation);
    }

    public VitessGrpcClientServiceGuiceModule(
            final VitessGrpcClientService vitessGrpcClientService,
            final String qualifierBindingName)
    {
        super(VitessGrpcClientService.class, vitessGrpcClientService, qualifierBindingName);
    }



    public VitessGrpcClientServiceGuiceModule(
            final Provider<? extends VitessGrpcClientService> vitessGrpcClientService)
    {
        super(VitessGrpcClientService.class, vitessGrpcClientService);
    }

    public VitessGrpcClientServiceGuiceModule(
            final Provider<? extends VitessGrpcClientService> vitessGrpcClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(VitessGrpcClientService.class, vitessGrpcClientService, qualifierBindingAnnotation);
    }

    public VitessGrpcClientServiceGuiceModule(
            final Provider<? extends VitessGrpcClientService> vitessGrpcClientService,
            final String qualifierBindingName)
    {
        super(VitessGrpcClientService.class, vitessGrpcClientService, qualifierBindingName);
    }



    static {
        DefaultVitessGrpcClientService.registerObjectMapperSubtypes();
    }
}
