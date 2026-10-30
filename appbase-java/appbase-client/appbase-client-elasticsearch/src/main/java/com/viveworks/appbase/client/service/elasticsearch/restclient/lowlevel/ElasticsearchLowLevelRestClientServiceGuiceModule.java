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

package com.viveworks.appbase.client.service.elasticsearch.restclient.lowlevel;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link ElasticsearchLowLevelRestClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ElasticsearchLowLevelRestClientServiceGuiceModule
        extends AbstractGenericGuiceModule<ElasticsearchLowLevelRestClientService>
{
    public ElasticsearchLowLevelRestClientServiceGuiceModule() {
        super(ElasticsearchLowLevelRestClientService.class, DefaultElasticsearchLowLevelRestClientService.class);
    }

    public ElasticsearchLowLevelRestClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(ElasticsearchLowLevelRestClientService.class,
                DefaultElasticsearchLowLevelRestClientService.class,
                qualifierBindingAnnotation);
    }

    public ElasticsearchLowLevelRestClientServiceGuiceModule(final String qualifierBindingName) {
        super(ElasticsearchLowLevelRestClientService.class,
                DefaultElasticsearchLowLevelRestClientService.class,
                qualifierBindingName);
    }



    public ElasticsearchLowLevelRestClientServiceGuiceModule(
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService)
    {
        super(ElasticsearchLowLevelRestClientService.class, elasticsearchLowLevelRestClientService);
    }

    public ElasticsearchLowLevelRestClientServiceGuiceModule(
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super(ElasticsearchLowLevelRestClientService.class,
                elasticsearchLowLevelRestClientService,
                qualifierBindingAnnotation);
    }

    public ElasticsearchLowLevelRestClientServiceGuiceModule(
            final ElasticsearchLowLevelRestClientService elasticsearchLowLevelRestClientService,
            final String qualifierBindingName)
    {
        super(ElasticsearchLowLevelRestClientService.class,
                elasticsearchLowLevelRestClientService,
                qualifierBindingName);
    }



    public ElasticsearchLowLevelRestClientServiceGuiceModule(
            final Provider<? extends ElasticsearchLowLevelRestClientService> elasticsearchLowLevelRestClientServiceProvider)
    {
        super(ElasticsearchLowLevelRestClientService.class, elasticsearchLowLevelRestClientServiceProvider);
    }

    public ElasticsearchLowLevelRestClientServiceGuiceModule(
            final Provider<? extends ElasticsearchLowLevelRestClientService> elasticsearchLowLevelRestClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(ElasticsearchLowLevelRestClientService.class,
                elasticsearchLowLevelRestClientServiceProvider,
                qualifierBindingAnnotation);
    }

    public ElasticsearchLowLevelRestClientServiceGuiceModule(
            final Provider<? extends ElasticsearchLowLevelRestClientService> elasticsearchLowLevelRestClientServiceProvider,
            final String qualifierBindingName)
    {
        super(ElasticsearchLowLevelRestClientService.class,
                elasticsearchLowLevelRestClientServiceProvider,
                qualifierBindingName);
    }



    static {
        DefaultElasticsearchLowLevelRestClientService.registerObjectMapperSubtypes();
    }
}
