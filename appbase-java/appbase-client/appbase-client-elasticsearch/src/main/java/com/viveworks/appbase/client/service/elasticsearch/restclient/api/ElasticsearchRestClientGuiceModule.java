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
package com.viveworks.appbase.client.service.elasticsearch.restclient.api;

import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class ElasticsearchRestClientGuiceModule
        extends AbstractGenericGuiceModule<ElasticsearchRestClient>
{
    public ElasticsearchRestClientGuiceModule() {
        super(ElasticsearchRestClient.class, DefaultElasticsearchRestClient.class);
    }

    public ElasticsearchRestClientGuiceModule(final Annotation qualifierBindingAnnotation) {
        super(ElasticsearchRestClient.class,
                DefaultElasticsearchRestClient.class,
                qualifierBindingAnnotation);
    }

    public ElasticsearchRestClientGuiceModule(final String qualifierBindingName) {
        super(ElasticsearchRestClient.class,
                DefaultElasticsearchRestClient.class,
                qualifierBindingName);
    }



    public ElasticsearchRestClientGuiceModule(
            final ElasticsearchRestClient elasticsearchRestClient)
    {
        super(ElasticsearchRestClient.class, elasticsearchRestClient);
    }

    public ElasticsearchRestClientGuiceModule(
            final ElasticsearchRestClient elasticsearchRestClient,
            final Annotation qualifierBindingAnnotation)
    {
        super(ElasticsearchRestClient.class,
                elasticsearchRestClient,
                qualifierBindingAnnotation);
    }

    public ElasticsearchRestClientGuiceModule(
            final ElasticsearchRestClient elasticsearchRestClient,
            final String qualifierBindingName)
    {
        super(ElasticsearchRestClient.class,
                elasticsearchRestClient,
                qualifierBindingName);
    }



    public ElasticsearchRestClientGuiceModule(
            final Provider<? extends ElasticsearchRestClient> elasticsearchRestClientProvider)
    {
        super(ElasticsearchRestClient.class, elasticsearchRestClientProvider);
    }

    public ElasticsearchRestClientGuiceModule(
            final Provider<? extends ElasticsearchRestClient> elasticsearchRestClientProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super(ElasticsearchRestClient.class,
                elasticsearchRestClientProvider,
                qualifierBindingAnnotation);
    }

    public ElasticsearchRestClientGuiceModule(
            final Provider<? extends ElasticsearchRestClient> elasticsearchRestClientProvider,
            final String qualifierBindingName)
    {
        super(ElasticsearchRestClient.class,
                elasticsearchRestClientProvider,
                qualifierBindingName);
    }



    static {
        DefaultElasticsearchRestClient.registerObjectMapperSubtypes();
    }
}
