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

package com.viveworks.appbase.client.service.zookeeper.accessor.reactive;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.DefaultReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.zookeeper.accessor.ZooKeeperAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.Mediator;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.mediator.message.Message;
import com.viveworks.appbase.core.service.mediator.queues.MediatorException;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.service.tracer.TracerService;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Tests for {@link ReactiveZooKeeperAccessor}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class ReactiveZooKeeperAccessorTest {

    /**
     * Bundles a mocked {@link AppbaseCore} with the tracer/span/mediator/timer mocks it was
     * built from, so tests can both drive the accessor and verify the shared spine's
     * interactions.
     */
    private static final class CoreFixtures {
        final AppbaseCore appbaseCore;
        final Tracer tracer;
        final Span span;
        final Mediator mediator;
        final Timer timer;

        private CoreFixtures(final AppbaseCore appbaseCore, final Tracer tracer,
                              final Span span, final Mediator mediator, final Timer timer) {
            this.appbaseCore = appbaseCore;
            this.tracer = tracer;
            this.span = span;
            this.mediator = mediator;
            this.timer = timer;
        }

        static CoreFixtures create() {
            final AppbaseCore appbaseCore = Mockito.mock(AppbaseCore.class);

            final LogService logService = Mockito.mock(LogService.class);
            Mockito.when(appbaseCore.getLogService()).thenReturn(logService);
            Mockito.when(logService.getLogger(Mockito.any(Class.class)))
                    .thenReturn(LoggerFactory.getLogger(ReactiveZooKeeperAccessorTest.class));

            final MetricService metricService = Mockito.mock(MetricService.class, Mockito.CALLS_REAL_METHODS);
            final MetricRegistry metricRegistry = Mockito.mock(MetricRegistry.class);
            final Timer timer = Mockito.mock(Timer.class);
            final Timer.Context timerContext = Mockito.mock(Timer.Context.class);
            final Counter errorCounter = Mockito.mock(Counter.class);
            Mockito.when(appbaseCore.getMetricService()).thenReturn(metricService);
            Mockito.doReturn(metricRegistry).when(metricService).getMetricRegistry();
            Mockito.when(metricRegistry.timer(Mockito.anyString())).thenReturn(timer);
            Mockito.when(metricRegistry.counter(Mockito.anyString())).thenReturn(errorCounter);
            Mockito.when(timer.time()).thenReturn(timerContext);

            final TracerService tracerService = Mockito.mock(TracerService.class);
            final Tracer tracer = Mockito.mock(Tracer.class);
            final Tracer.SpanBuilder spanBuilder = Mockito.mock(Tracer.SpanBuilder.class);
            final Span span = Mockito.mock(Span.class);
            Mockito.when(appbaseCore.getTracerService()).thenReturn(tracerService);
            Mockito.when(tracerService.getTracer()).thenReturn(tracer);
            Mockito.when(tracer.buildSpan(Mockito.anyString())).thenReturn(spanBuilder);
            Mockito.when(spanBuilder.start()).thenReturn(span);

            final MediatorService mediatorService = Mockito.mock(MediatorService.class);
            final Mediator mediator = Mockito.mock(Mediator.class);
            Mockito.when(appbaseCore.getMediatorService()).thenReturn(mediatorService);
            Mockito.when(mediatorService.getMediator()).thenReturn(mediator);
            Mockito.when(mediator.getMessageFromPool()).thenReturn(new Message());

            return new CoreFixtures(appbaseCore, tracer, span, mediator, timer);
        }
    }

    /**
     * A real (never-{@code start()}ed, never-connected) {@link CuratorFramework}, built via
     * {@link CuratorFrameworkFactory} — {@link AsyncCuratorFramework#wrap(CuratorFramework)}
     * rejects any {@link CuratorFramework} not created through the factory (a Mockito mock
     * included), so this is the seam used to test {@link ReactiveZooKeeperAccessor#command}
     * without a live ZooKeeper server: the per-op stub {@link Function} passed to {@code command}
     * ignores its {@link AsyncCuratorFramework} argument, so no network call is ever made.
     */
    private static CuratorFramework unstartedClient() {
        return CuratorFrameworkFactory.newClient("localhost:2181", new RetryOneTime(1));
    }

    @SuppressWarnings("unchecked")
    private static AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> mockIntegrationService(
            final CuratorFramework client) {
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                Mockito.mock(AppbaseClientIntegrationService.class);
        Mockito.when(integrationService.getClient()).thenReturn(client);
        return integrationService;
    }

    private static ReactorService mockReactorService() {
        final ReactorService reactorService = Mockito.mock(ReactorService.class);
        Mockito.when(reactorService.getDefaultScheduler()).thenReturn(Schedulers.immediate());
        Mockito.when(reactorService.getSchedulerById(Mockito.anyString())).thenReturn(null);
        return reactorService;
    }

    private static ReactiveZooKeeperAccessor accessor(
            final CoreFixtures core,
            final DefaultReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow) {
        return new ReactiveZooKeeperAccessor(core.appbaseCore, mockReactorService(), configuration, borrow);
    }

    private static DefaultReactiveOperationConfiguration defaultConfig() {
        return DefaultReactiveOperationConfiguration.builder().retryEnabled(false).build();
    }

    @Test
    void commandEmitsValueFromCompletionStage() {
        final CuratorFramework client = unstartedClient();
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                mockIntegrationService(client);
        final ReactiveZooKeeperAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);
        final Function<AsyncCuratorFramework, CompletionStage<String>> fn =
                async -> CompletableFuture.completedFuture("value");

        StepVerifier.create(accessor.command("op", fn))
                .expectNext("value")
                .verifyComplete();
    }

    @Test
    void integrationServiceReturnedOnSuccessAndError() {
        final CuratorFramework client = unstartedClient();
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                mockIntegrationService(client);
        final ReactiveZooKeeperAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);
        final Function<AsyncCuratorFramework, CompletionStage<String>> success =
                async -> CompletableFuture.completedFuture("value");
        final Function<AsyncCuratorFramework, CompletionStage<String>> failure =
                async -> CompletableFuture.failedFuture(KeeperException.create(KeeperException.Code.NONODE));

        accessor.command("op", success).block();
        accessor.command("op", failure).onErrorResume(e -> Mono.empty()).block();

        Mockito.verify(integrationService, Mockito.times(2)).finalizeOperation();
    }

    @Test
    void keeperExceptionMappedToZooKeeperAccessorException() {
        final CuratorFramework client = unstartedClient();
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                mockIntegrationService(client);
        final ReactiveZooKeeperAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), () -> integrationService);
        final Function<AsyncCuratorFramework, CompletionStage<String>> failure =
                async -> CompletableFuture.failedFuture(KeeperException.create(KeeperException.Code.NONODE));

        StepVerifier.create(accessor.command("op", failure))
                .expectError(ZooKeeperAccessorException.class)
                .verify();
    }

    @Test
    void borrowIsLazyPerSubscription() {
        final CuratorFramework client = unstartedClient();
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                mockIntegrationService(client);
        @SuppressWarnings("unchecked")
        final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrowSupplier =
                Mockito.mock(Supplier.class);
        Mockito.when(borrowSupplier.get()).thenReturn(integrationService);
        final ReactiveZooKeeperAccessor accessor =
                accessor(CoreFixtures.create(), defaultConfig(), borrowSupplier);
        final Function<AsyncCuratorFramework, CompletionStage<String>> fn =
                async -> CompletableFuture.completedFuture("value");

        accessor.command("op", fn);

        Mockito.verifyNoInteractions(borrowSupplier);
    }

    @Test
    void timerRecordedForOperation() {
        final CuratorFramework client = unstartedClient();
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                mockIntegrationService(client);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveZooKeeperAccessor accessor =
                accessor(core, defaultConfig(), () -> integrationService);
        final Function<AsyncCuratorFramework, CompletionStage<String>> fn =
                async -> CompletableFuture.completedFuture("value");

        accessor.command("op", fn).block();

        Mockito.verify(core.timer).time();
    }

    @Test
    void notableFailureReportedToExceptionStoreWhenEnabled() throws MediatorException {
        final CuratorFramework client = unstartedClient();
        final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> integrationService =
                mockIntegrationService(client);
        final CoreFixtures core = CoreFixtures.create();
        final ReactiveZooKeeperAccessor accessor = accessor(core,
                DefaultReactiveOperationConfiguration.builder()
                        .retryEnabled(false)
                        .exceptionStoreEnabled(true)
                        .build(),
                () -> integrationService);
        final Function<AsyncCuratorFramework, CompletionStage<String>> failure =
                async -> CompletableFuture.failedFuture(KeeperException.create(KeeperException.Code.NONODE));

        accessor.command("op", failure).onErrorResume(e -> Mono.empty()).block();

        Mockito.verify(core.mediator).asyncHandleMessage(Mockito.any(), Mockito.anyBoolean());
    }
}
