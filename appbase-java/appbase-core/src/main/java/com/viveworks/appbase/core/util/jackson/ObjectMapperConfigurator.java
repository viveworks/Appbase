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

package com.viveworks.appbase.core.util.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.viveworks.appbase.core.service.exception.store.inmemory.InMemoryExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.exception.store.jpa.JpaExceptionStoreConfiguration;
import com.viveworks.appbase.core.service.log.common.lucene.DefaultLuceneLogIndexConfiguration;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.ExactTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.RangeTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.criteria.timestamp.SetTimestampCriteria;
import com.viveworks.appbase.core.service.log.store.lucene.LuceneLogStoreConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.BoundedElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ElasticSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ImmediateSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.MediatorExecutorServiceSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.ParallelSchedulerConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.scheduler.configuration.SingleSchedulerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.InMemoryReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.LoggingReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.reporter.RemoteReporterConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ConstSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.GuaranteedThroughputSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.PerOperationSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.ProbabilisticSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RateLimitingSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.sampler.RemoteControlledSamplerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.MDCScopeManagerConfiguration;
import com.viveworks.appbase.core.service.tracer.configuration.scopemanager.ThreadLocalScopeManagerConfiguration;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.BusySpinWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.LiteTimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.PhasedBackoffWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.SleepingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.TimeoutBlockingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.disruptor.configuration.waitstrategy.YieldingWaitStrategyConfiguration;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;

/**
 * Configures a {@link ObjectMapper} as required by this module.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public final class ObjectMapperConfigurator {
    private ObjectMapperConfigurator() {
    }

    /**
     * Configure {@link ObjectMapper} as required by this module.
     *
     * @param objectMapper Object mapper
     */
    public static void configure(final ObjectMapper objectMapper) {
        AppbaseUtil.notNull(objectMapper, "objectMapper");

        objectMapper
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, true)
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        registerSubType(objectMapper, InMemoryReporterConfiguration.class);
        registerSubType(objectMapper, LoggingReporterConfiguration.class);
        registerSubType(objectMapper, RemoteReporterConfiguration.class);

        registerSubType(objectMapper, ConstSamplerConfiguration.class);
        registerSubType(objectMapper, GuaranteedThroughputSamplerConfiguration.class);
        registerSubType(objectMapper, PerOperationSamplerConfiguration.class);
        registerSubType(objectMapper, ProbabilisticSamplerConfiguration.class);
        registerSubType(objectMapper, RateLimitingSamplerConfiguration.class);
        registerSubType(objectMapper, RemoteControlledSamplerConfiguration.class);

        registerSubType(objectMapper, MDCScopeManagerConfiguration.class);
        registerSubType(objectMapper, ThreadLocalScopeManagerConfiguration.class);

        registerSubType(objectMapper, BoundedElasticSchedulerConfiguration.class);
        registerSubType(objectMapper, ElasticSchedulerConfiguration.class);
        registerSubType(objectMapper, ExecutorServiceSchedulerConfiguration.class);
        registerSubType(objectMapper, ImmediateSchedulerConfiguration.class);
        registerSubType(objectMapper, MediatorExecutorServiceSchedulerConfiguration.class);
        registerSubType(objectMapper, ParallelSchedulerConfiguration.class);
        registerSubType(objectMapper, SingleSchedulerConfiguration.class);

        registerSubType(objectMapper, BlockingWaitStrategyConfiguration.class);
        registerSubType(objectMapper, BusySpinWaitStrategyConfiguration.class);
        registerSubType(objectMapper, LiteTimeoutBlockingWaitStrategyConfiguration.class);
        registerSubType(objectMapper, PhasedBackoffWaitStrategyConfiguration.class);
        registerSubType(objectMapper, SleepingWaitStrategyConfiguration.class);
        registerSubType(objectMapper, TimeoutBlockingWaitStrategyConfiguration.class);
        registerSubType(objectMapper, YieldingWaitStrategyConfiguration.class);

        registerSubType(objectMapper, JpaExceptionStoreConfiguration.class);
        registerSubType(objectMapper, InMemoryExceptionStoreConfiguration.class);

        registerSubType(objectMapper, DefaultLuceneServiceConfiguration.class);
        registerSubType(objectMapper, DefaultLuceneLogIndexConfiguration.class);
        registerSubType(objectMapper, LuceneLogStoreConfiguration.class);

        registerSubType(objectMapper, ExactTimestampCriteria.class);
        registerSubType(objectMapper, RangeTimestampCriteria.class);
        registerSubType(objectMapper, SetTimestampCriteria.class);
    }

    /**
     * Register allowed sub types of an interface or another class for polymorphic de-serialization. The convention
     * followed is to use the sub type's class name as the logical name in {@link NamedType}.
     *
     * @param objectMapper Object mapper
     * @param subType Sub type
     */
    public static void registerSubType(final ObjectMapper objectMapper, final Class<?> subType) {
        AppbaseUtil.notNull(objectMapper, "objectMapper");
        AppbaseUtil.notNull(subType, "subType");

        objectMapper.registerSubtypes(new NamedType(subType, subType.getName()));
    }
}
