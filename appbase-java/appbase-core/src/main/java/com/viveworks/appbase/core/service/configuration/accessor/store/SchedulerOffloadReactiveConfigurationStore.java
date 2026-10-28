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

package com.viveworks.appbase.core.service.configuration.accessor.store;

import com.viveworks.appbase.core.service.configuration.RawConfiguration;
import com.viveworks.appbase.core.service.configuration.accessor.ConfigurationAccessorContext;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.util.AppbaseUtil;
import reactor.core.publisher.Mono;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;

/**
 * {@link ReactiveConfigurationStore} adapter for a {@link ConfigurationStore}: the read offloads the
 * delegate's blocking sync {@link ConfigurationStore#readRawConfigurations(ConfigurationAccessorContext)}
 * onto the N01 {@link ReactiveReadBoundary} (via {@link ReactiveReadBoundary#read(java.util.concurrent.Callable)})
 * rather than blocking the calling thread. Config reads are offload-only — there is no Lucene-backed
 * cooperative-cancellation variant to bound here.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@ThreadSafe
public final class SchedulerOffloadReactiveConfigurationStore implements ReactiveConfigurationStore {
    private final ConfigurationStore delegate;
    private final ReactiveReadBoundary reactiveReadBoundary;

    public SchedulerOffloadReactiveConfigurationStore(
            final ConfigurationStore delegate, final ReactiveReadBoundary reactiveReadBoundary) {
        AppbaseUtil.notNull(delegate, "delegate");
        AppbaseUtil.notNull(reactiveReadBoundary, "reactiveReadBoundary");

        this.delegate = delegate;
        this.reactiveReadBoundary = reactiveReadBoundary;
    }

    @Override
    public Mono<Collection<RawConfiguration>> readRawConfigurationsReactive(
            final ConfigurationAccessorContext configurationAccessorContext) {
        return reactiveReadBoundary.read(() -> delegate.readRawConfigurations(configurationAccessorContext));
    }
}
