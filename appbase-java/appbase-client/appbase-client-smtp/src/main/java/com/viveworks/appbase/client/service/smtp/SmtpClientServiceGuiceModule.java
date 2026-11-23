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

package com.viveworks.appbase.client.service.smtp;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link SmtpClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class SmtpClientServiceGuiceModule extends AbstractGenericGuiceModule<SmtpClientService> {
    public SmtpClientServiceGuiceModule() {
        super(SmtpClientService.class, DefaultSmtpClientService.class);
    }

    public SmtpClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(SmtpClientService.class, DefaultSmtpClientService.class, qualifierBindingAnnotation);
    }

    public SmtpClientServiceGuiceModule(final String qualifierBindingName) {
        super(SmtpClientService.class, DefaultSmtpClientService.class, qualifierBindingName);
    }



    public SmtpClientServiceGuiceModule(final SmtpClientService smtpClientService) {
        super(SmtpClientService.class, smtpClientService);
    }

    public SmtpClientServiceGuiceModule(
            final SmtpClientService smtpClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(SmtpClientService.class, smtpClientService, qualifierBindingAnnotation);
    }

    public SmtpClientServiceGuiceModule(
            final SmtpClientService smtpClientService,
            final String qualifierBindingName)
    {
        super(SmtpClientService.class, smtpClientService, qualifierBindingName);
    }



    public SmtpClientServiceGuiceModule(final Provider<? extends SmtpClientService> smtpClientServiceProvider) {
        super(SmtpClientService.class, smtpClientServiceProvider);
    }

    public SmtpClientServiceGuiceModule(
            final Provider<? extends SmtpClientService> smtpClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(SmtpClientService.class, smtpClientServiceProvider, qualifierBindingAnnotation);
    }

    public SmtpClientServiceGuiceModule(
            final Provider<? extends SmtpClientService> smtpClientServiceProvider,
            final String qualifierBindingName)
    {
        super(SmtpClientService.class, smtpClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultSmtpClientService.registerObjectMapperSubtypes();
    }
}
