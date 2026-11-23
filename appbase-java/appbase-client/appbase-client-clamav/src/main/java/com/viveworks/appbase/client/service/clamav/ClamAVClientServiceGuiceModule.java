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

package com.viveworks.appbase.client.service.clamav;

import com.viveworks.appbase.client.core.AppbaseClientService;
import com.viveworks.appbase.client.service.clamav.client.ClamAVClientSharedResources;
import com.viveworks.appbase.client.service.clamav.client.DefaultClamAVClientSharedResources;
import com.viveworks.appbase.core.util.di.guice.AbstractGeneric2GuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Represents {@link AppbaseClientService} for {@code ClamAV}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ClamAVClientServiceGuiceModule
        extends AbstractGeneric2GuiceModule<ClamAVClientSharedResources, ClamAVClientService>
{
    public ClamAVClientServiceGuiceModule() {
        super(ClamAVClientSharedResources.class,
                DefaultClamAVClientSharedResources.class,
                ClamAVClientService.class,
                DefaultClamAVClientService.class);
    }

    public ClamAVClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(ClamAVClientSharedResources.class,
                DefaultClamAVClientSharedResources.class,
                ClamAVClientService.class,
                DefaultClamAVClientService.class,
                qualifierBindingAnnotation);
    }

    public ClamAVClientServiceGuiceModule(final String qualifierBindingName) {
        super(ClamAVClientSharedResources.class,
                DefaultClamAVClientSharedResources.class,
                ClamAVClientService.class,
                DefaultClamAVClientService.class,
                qualifierBindingName);
    }



    public ClamAVClientServiceGuiceModule(
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final ClamAVClientService clamAVClientService)
    {
        super(ClamAVClientSharedResources.class,
                clamAVClientSharedResources,
                ClamAVClientService.class,
                clamAVClientService);
    }

    public ClamAVClientServiceGuiceModule(
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final ClamAVClientService clamAVClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(ClamAVClientSharedResources.class,
                clamAVClientSharedResources,
                ClamAVClientService.class,
                clamAVClientService,
                qualifierBindingAnnotation);
    }

    public ClamAVClientServiceGuiceModule(
            final ClamAVClientSharedResources clamAVClientSharedResources,
            final ClamAVClientService clamAVClientService,
            final String qualifierBindingName)
    {
        super(ClamAVClientSharedResources.class,
                clamAVClientSharedResources,
                ClamAVClientService.class,
                clamAVClientService,
                qualifierBindingName);
    }



    public ClamAVClientServiceGuiceModule(
            final Provider<? extends ClamAVClientSharedResources> clamAVClientSharedResourcesProvider,
            final Provider<? extends ClamAVClientService> clamAVClientServiceProvider)
    {
        super(ClamAVClientSharedResources.class,
                clamAVClientSharedResourcesProvider,
                ClamAVClientService.class,
                clamAVClientServiceProvider);
    }

    public ClamAVClientServiceGuiceModule(
            final Provider<? extends ClamAVClientSharedResources> clamAVClientSharedResourcesProvider,
            final Provider<? extends ClamAVClientService> clamAVClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(ClamAVClientSharedResources.class,
                clamAVClientSharedResourcesProvider,
                ClamAVClientService.class,
                clamAVClientServiceProvider,
                qualifierBindingAnnotation);
    }

    public ClamAVClientServiceGuiceModule(
            final Provider<? extends ClamAVClientSharedResources> clamAVClientSharedResourcesProvider,
            final Provider<? extends ClamAVClientService> clamAVClientServiceProvider,
            final String qualifierBindingName)
    {
        super(ClamAVClientSharedResources.class,
                clamAVClientSharedResourcesProvider,
                ClamAVClientService.class,
                clamAVClientServiceProvider,
                qualifierBindingName);
    }



    static {
        DefaultClamAVClientSharedResources.registerObjectMapperSubtypes();
        DefaultClamAVClientService.registerObjectMapperSubtypes();
    }
}
