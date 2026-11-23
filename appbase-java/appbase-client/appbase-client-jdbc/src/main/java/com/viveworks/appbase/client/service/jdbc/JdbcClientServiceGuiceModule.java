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

package com.viveworks.appbase.client.service.jdbc;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link JdbcClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JdbcClientServiceGuiceModule extends AbstractGenericGuiceModule<JdbcClientService> {
    public JdbcClientServiceGuiceModule() {
        super(JdbcClientService.class, DefaultJdbcClientService.class);
    }

    public JdbcClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(JdbcClientService.class, DefaultJdbcClientService.class, qualifierBindingAnnotation);
    }

    public JdbcClientServiceGuiceModule(final String qualifierBindingName) {
        super(JdbcClientService.class, DefaultJdbcClientService.class, qualifierBindingName);
    }



    public JdbcClientServiceGuiceModule(final JdbcClientService jdbcClientService) {
        super(JdbcClientService.class, jdbcClientService);
    }

    public JdbcClientServiceGuiceModule(
            final JdbcClientService jdbcClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(JdbcClientService.class, jdbcClientService, qualifierBindingAnnotation);
    }

    public JdbcClientServiceGuiceModule(
            final JdbcClientService jdbcClientService,
            final String qualifierBindingName)
    {
        super(JdbcClientService.class, jdbcClientService, qualifierBindingName);
    }



    public JdbcClientServiceGuiceModule(final Provider<? extends JdbcClientService> jdbcClientServiceProvider) {
        super(JdbcClientService.class, jdbcClientServiceProvider);
    }

    public JdbcClientServiceGuiceModule(
            final Provider<? extends JdbcClientService> jdbcClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(JdbcClientService.class, jdbcClientServiceProvider, qualifierBindingAnnotation);
    }

    public JdbcClientServiceGuiceModule(
            final Provider<? extends JdbcClientService> jdbcClientServiceProvider,
            final String qualifierBindingName)
    {
        super(JdbcClientService.class, jdbcClientServiceProvider, qualifierBindingName);
    }



    static {
        DefaultJdbcClientService.registerObjectMapperSubtypes();
    }
}
