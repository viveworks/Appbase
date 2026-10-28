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

package com.viveworks.appbase.client.service.druid.sql;

import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DefaultDruidSqlParameterFactory;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClient;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlClientSharedResources;
import com.viveworks.appbase.client.service.druid.sql.client.DruidSqlParameterFactory;
import com.viveworks.appbase.core.util.di.guice.AbstractGeneric3GuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * {@code Guice} module for {@link DruidSqlClient}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DruidSqlClientServiceGuiceModule extends
        AbstractGeneric3GuiceModule<DruidSqlClientSharedResources, DruidSqlParameterFactory, DruidSqlClientService>
{
    public DruidSqlClientServiceGuiceModule() {
        super(DruidSqlClientSharedResources.class,
                DefaultDruidSqlClientSharedResources.class,
                DruidSqlParameterFactory.class,
                DefaultDruidSqlParameterFactory.class,
                DruidSqlClientService.class,
                DefaultDruidSqlClientService.class);
    }

    public DruidSqlClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(DruidSqlClientSharedResources.class,
                DefaultDruidSqlClientSharedResources.class,
                DruidSqlParameterFactory.class,
                DefaultDruidSqlParameterFactory.class,
                DruidSqlClientService.class,
                DefaultDruidSqlClientService.class,
                qualifierBindingAnnotation);
    }

    public DruidSqlClientServiceGuiceModule(final String qualifierBindingName) {
        super(DruidSqlClientSharedResources.class,
                DefaultDruidSqlClientSharedResources.class,
                DruidSqlParameterFactory.class,
                DefaultDruidSqlParameterFactory.class,
                DruidSqlClientService.class,
                DefaultDruidSqlClientService.class,
                qualifierBindingName);
    }



    public DruidSqlClientServiceGuiceModule(
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final DruidSqlClientService druidSqlClientService)
    {
        super(DruidSqlClientSharedResources.class,
                druidSqlClientSharedResources,
                DruidSqlParameterFactory.class,
                druidSqlParameterFactory,
                DruidSqlClientService.class,
                druidSqlClientService);
    }

    public DruidSqlClientServiceGuiceModule(
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final DruidSqlClientService druidSqlClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(DruidSqlClientSharedResources.class,
                druidSqlClientSharedResources,
                DruidSqlParameterFactory.class,
                druidSqlParameterFactory,
                DruidSqlClientService.class,
                druidSqlClientService,
                qualifierBindingAnnotation);
    }

    public DruidSqlClientServiceGuiceModule(
            final DruidSqlClientSharedResources druidSqlClientSharedResources,
            final DruidSqlParameterFactory druidSqlParameterFactory,
            final DruidSqlClientService druidSqlClientService,
            final String qualifierBindingName)
    {
        super(DruidSqlClientSharedResources.class,
                druidSqlClientSharedResources,
                DruidSqlParameterFactory.class,
                druidSqlParameterFactory,
                DruidSqlClientService.class,
                druidSqlClientService,
                qualifierBindingName);
    }



    public DruidSqlClientServiceGuiceModule(
            final Provider<? extends DruidSqlClientSharedResources> druidSqlClientSharedResourcesProvider,
            final Provider<? extends DruidSqlParameterFactory> druidSqlParameterFactoryProvider,
            final Provider<? extends DruidSqlClientService> druidSqlClientServiceProvider)
    {
        super(DruidSqlClientSharedResources.class,
                druidSqlClientSharedResourcesProvider,
                DruidSqlParameterFactory.class,
                druidSqlParameterFactoryProvider,
                DruidSqlClientService.class,
                druidSqlClientServiceProvider);
    }

    public DruidSqlClientServiceGuiceModule(
            final Provider<? extends DruidSqlClientSharedResources> druidSqlClientSharedResourcesProvider,
            final Provider<? extends DruidSqlParameterFactory> druidSqlParameterFactoryProvider,
            final Provider<? extends DruidSqlClientService> druidSqlClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(DruidSqlClientSharedResources.class,
                druidSqlClientSharedResourcesProvider,
                DruidSqlParameterFactory.class,
                druidSqlParameterFactoryProvider,
                DruidSqlClientService.class,
                druidSqlClientServiceProvider,
                qualifierBindingAnnotation);
    }

    public DruidSqlClientServiceGuiceModule(
            final Provider<? extends DruidSqlClientSharedResources> druidSqlClientSharedResourcesProvider,
            final Provider<? extends DruidSqlParameterFactory> druidSqlParameterFactoryProvider,
            final Provider<? extends DruidSqlClientService> druidSqlClientServiceProvider,
            final String qualifierBindingName)
    {
        super(DruidSqlClientSharedResources.class,
                druidSqlClientSharedResourcesProvider,
                DruidSqlParameterFactory.class,
                druidSqlParameterFactoryProvider,
                DruidSqlClientService.class,
                druidSqlClientServiceProvider,
                qualifierBindingName);
    }



    static {
        DefaultDruidSqlClientSharedResources.registerObjectMapperSubtypes();
        DefaultDruidSqlClientService.registerObjectMapperSubtypes();
    }
}
