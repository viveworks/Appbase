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

package com.viveworks.appbase.client.service.kafka.producer;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link KafkaProducerClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public class KafkaProducerClientServiceGuiceModule extends AbstractGenericGuiceModule<KafkaProducerClientService<?, ?>> {
    public KafkaProducerClientServiceGuiceModule() {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                (Class<DefaultKafkaProducerClientService<?, ?>>)
                        AppbaseUtil.notNullType(DefaultKafkaProducerClientService.class));
    }

    public KafkaProducerClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                (Class<DefaultKafkaProducerClientService<?, ?>>)
                        AppbaseUtil.notNullType(DefaultKafkaProducerClientService.class),
                qualifierBindingAnnotation);
    }

    public KafkaProducerClientServiceGuiceModule(final String qualifierBindingName) {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                (Class<DefaultKafkaProducerClientService<?, ?>>)
                        AppbaseUtil.notNullType(DefaultKafkaProducerClientService.class),
                qualifierBindingName);
    }



    public KafkaProducerClientServiceGuiceModule(final KafkaProducerClientService kafkaProducerClientService) {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                kafkaProducerClientService);
    }

    public KafkaProducerClientServiceGuiceModule(
            final KafkaProducerClientService kafkaProducerClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                kafkaProducerClientService,
                qualifierBindingAnnotation);
    }

    public KafkaProducerClientServiceGuiceModule(
            final KafkaProducerClientService kafkaProducerClientService,
            final String qualifierBindingName)
    {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                kafkaProducerClientService,
                qualifierBindingName);
    }



    public KafkaProducerClientServiceGuiceModule(
            final Provider<? extends KafkaProducerClientService<?, ?>> kafkaProducerClientServiceProvider)
    {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                kafkaProducerClientServiceProvider);
    }

    public KafkaProducerClientServiceGuiceModule(
            final Provider<? extends KafkaProducerClientService<?, ?>> kafkaProducerClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                kafkaProducerClientServiceProvider,
                qualifierBindingAnnotation);
    }

    public KafkaProducerClientServiceGuiceModule(
            final Provider<? extends KafkaProducerClientService<?, ?>> kafkaProducerClientServiceProvider,
            final String qualifierBindingName)
    {
        super((Class<KafkaProducerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaProducerClientService.class),
                kafkaProducerClientServiceProvider,
                qualifierBindingName);
    }



    static {
        DefaultKafkaProducerClientService.registerObjectMapperSubtypes();
    }
}
