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

package com.viveworks.appbase.client.service.kafka.consumer;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.di.guice.AbstractGenericGuiceModule;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Guice module for {@link KafkaConsumerClientService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public class KafkaConsumerClientServiceGuiceModule extends AbstractGenericGuiceModule<KafkaConsumerClientService<?, ?>> {
    public KafkaConsumerClientServiceGuiceModule() {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                (Class<DefaultKafkaConsumerClientService<?, ?>>)
                        AppbaseUtil.notNullType(DefaultKafkaConsumerClientService.class));
    }

    public KafkaConsumerClientServiceGuiceModule(final Annotation qualifierBindingAnnotation) {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                (Class<DefaultKafkaConsumerClientService<?, ?>>)
                        AppbaseUtil.notNullType(DefaultKafkaConsumerClientService.class),
                qualifierBindingAnnotation);
    }

    public KafkaConsumerClientServiceGuiceModule(final String qualifierBindingName) {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                (Class<DefaultKafkaConsumerClientService<?, ?>>)
                        AppbaseUtil.notNullType(DefaultKafkaConsumerClientService.class),
                qualifierBindingName);
    }



    public KafkaConsumerClientServiceGuiceModule(final KafkaConsumerClientService<?, ?> kafkaConsumerClientService) {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                kafkaConsumerClientService);
    }

    public KafkaConsumerClientServiceGuiceModule(
            final KafkaConsumerClientService<?, ?> kafkaConsumerClientService,
            final Annotation qualifierBindingAnnotation)
    {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                kafkaConsumerClientService,
                qualifierBindingAnnotation);
    }

    public KafkaConsumerClientServiceGuiceModule(
            final KafkaConsumerClientService<?, ?> kafkaConsumerClientService,
            final String qualifierBindingName)
    {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                kafkaConsumerClientService,
                qualifierBindingName);
    }



    public KafkaConsumerClientServiceGuiceModule(
            final Provider<? extends KafkaConsumerClientService<?, ?>> kafkaConsumerClientServiceProvider)
    {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                kafkaConsumerClientServiceProvider);
    }

    public KafkaConsumerClientServiceGuiceModule(
            final Provider<? extends KafkaConsumerClientService<?, ?>> kafkaConsumerClientServiceProvider,
            final Annotation qualifierBindingAnnotation)
    {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                kafkaConsumerClientServiceProvider,
                qualifierBindingAnnotation);
    }

    public KafkaConsumerClientServiceGuiceModule(
            final Provider<? extends KafkaConsumerClientService<?, ?>> kafkaConsumerClientServiceProvider,
            final String qualifierBindingName)
    {
        super((Class<KafkaConsumerClientService<?, ?>>) AppbaseUtil.notNullType(KafkaConsumerClientService.class),
                kafkaConsumerClientServiceProvider,
                qualifierBindingName);
    }



    static {
        DefaultKafkaConsumerClientService.registerObjectMapperSubtypes();
    }
}
