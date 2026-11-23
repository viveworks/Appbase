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

package com.viveworks.appbase.bootstrap.core.factory;

import com.viveworks.appbase.bootstrap.core.AppbaseCoreServiceFactory;
import com.viveworks.appbase.bootstrap.core.AppbaseCoreServices;
import com.viveworks.appbase.bootstrap.core.CoreServiceOrder;
import com.viveworks.appbase.core.AppbaseException;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.read.DefaultReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundary;
import com.viveworks.appbase.core.service.reactive.read.ReactiveReadBoundaryConfiguration;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.validation.ValidationService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneService;
import com.viveworks.appbase.core.util.lucene.DefaultLuceneServiceConfiguration;
import com.viveworks.appbase.core.util.lucene.LuceneService;

import java.util.Objects;

/**
 * Factory that creates the optional {@link LuceneService}.
 *
 * <p>Unlike other config-dependent services, {@link DefaultLuceneService} accepts its
 * {@link DefaultLuceneServiceConfiguration} directly as a constructor argument rather than
 * fetching it from the {@link com.viveworks.appbase.core.service.configuration.ConfigurationService}.
 *
 * <p>This factory is only registered when
 * {@link com.viveworks.appbase.bootstrap.core.AppbaseCoreServicesConfiguration#getLuceneServiceConfiguration()}
 * returns a non-{@code null} value. Since N01 PR2, {@link DefaultLuceneService} also takes a
 * {@link ReactiveReadBoundary}, built here from a default {@link ReactiveReadBoundaryConfiguration}. That
 * default config carries no {@code offloadSchedulerId}, so the boundary uses the shared bounded-elastic
 * scheduler and never consults {@link ReactorService} — which is therefore <b>optional</b>: it is passed
 * through when present ({@link CoreServiceOrder#REACTOR} runs before {@link CoreServiceOrder#LUCENE}) and
 * {@code null} when Reactor is not enabled. <b>Enabling Lucene does NOT require enabling Reactor</b>;
 * Reactor would only be needed if a future config set a custom {@code offloadSchedulerId} to be resolved
 * through its scheduler registry.
 */
public final class LuceneServiceFactory implements AppbaseCoreServiceFactory<LuceneService> {

    private final DefaultLuceneServiceConfiguration config;

    /**
     * Creates the factory with the given Lucene service configuration.
     *
     * @param config Lucene service configuration (must not be null)
     */
    public LuceneServiceFactory(DefaultLuceneServiceConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Class<LuceneService> serviceType() {
        return LuceneService.class;
    }

    @Override
    public int initializationOrder() {
        return CoreServiceOrder.LUCENE.order();
    }

    @Override
    public LuceneService create(AppbaseCoreServices partial) throws AppbaseException {
        LogService        log        = partial.get(LogService.class);
        MetricService     metric     = partial.get(MetricService.class);
        ValidationService validation = partial.get(ValidationService.class);
        MediatorService   mediator   = partial.get(MediatorService.class);
        // getReactorService() returns null when Reactor is not enabled (unlike get(Class), which throws).
        // The default boundary config uses bounded-elastic and never consults Reactor, so null is fine.
        ReactorService    reactor    = partial.getReactorService();

        ReactiveReadBoundary reactiveReadBoundary = new DefaultReactiveReadBoundary(
                ReactiveReadBoundaryConfiguration.builder().build(), reactor);

        return new DefaultLuceneService(log, metric, validation, mediator, config, reactiveReadBoundary);
    }
}
