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
import com.viveworks.appbase.client.core.reactive.AbstractReactiveAccessor;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.zookeeper.accessor.ZooKeeperAccessorException;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.service.reactive.reactor.ReactorService;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.AsyncStage;
import org.apache.zookeeper.data.Stat;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive accessor for Apache ZooKeeper, built on Curator's {@code curator-x-async} extension.
 * Each operation borrows one pooled {@link AppbaseClientIntegrationService} per subscription,
 * wraps its {@link CuratorFramework} in an {@link AsyncCuratorFramework} to run the Curator
 * async call, and returns the integration service to the pool on completion, error, or
 * cancellation via {@link Mono#usingWhen}.
 *
 * <p>{@link AsyncCuratorFramework#wrap(CuratorFramework)} adapts a plain {@link
 * CuratorFramework} into Curator's async DSL; each of its operations returns an {@link
 * AsyncStage}, which extends {@link CompletionStage}. {@link Mono#fromCompletionStage(Supplier)}
 * bridges that {@link CompletionStage} into Reactor, unwrapping any {@link
 * java.util.concurrent.CompletionException} the stage completes with so failure mapping sees the
 * original Curator/ZooKeeper cause. {@link Mono#usingWhen} guarantees the borrowed integration
 * service is returned to the pool on completion, error, or cancellation.
 *
 * <p>Cross-cutting concerns (timeout, retry, backpressure, scheduler, metrics, tracing, and
 * exception-store reporting) are supplied by the inherited {@link AbstractReactiveAccessor};
 * this class contributes only the Curator operation and ZooKeeper error mapping.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ReactiveZooKeeperAccessor extends AbstractReactiveAccessor {

    private final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param reactorService Reactor scheduler lookup
     * @param configuration Reactive operation configuration (timeout/retry/backpressure/etc.)
     * @param borrow Supplier of a pooled ZooKeeper integration service, invoked once per subscription
     */
    public ReactiveZooKeeperAccessor(
            final AppbaseCore appbaseCore,
            final ReactorService reactorService,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow) {
        super(appbaseCore, reactorService, configuration);
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Create a ZNode at {@code path} with {@code data}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @param data ZNode data
     * @return Wrapped {@link Mono} of the created path
     */
    public Mono<String> create(final String op, final String path, final byte[] data) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(data, "data");
        return command(op, async -> async.create().forPath(path, data));
    }

    /**
     * Get the data stored at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return Wrapped {@link Mono} of the ZNode data
     */
    public Mono<byte[]> getData(final String op, final String path) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return command(op, async -> async.getData().forPath(path));
    }

    /**
     * Set the data stored at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @param data New ZNode data
     * @return Wrapped {@link Mono} of the resulting {@link Stat}
     */
    public Mono<Stat> setData(final String op, final String path, final byte[] data) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(data, "data");
        return command(op, async -> async.setData().forPath(path, data));
    }

    /**
     * Delete the ZNode at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return Wrapped {@link Mono} that completes when the delete finishes
     */
    public Mono<Void> delete(final String op, final String path) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return command(op, async -> async.delete().forPath(path));
    }

    /**
     * Check whether the ZNode at {@code path} exists.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return Wrapped {@link Mono} of the {@link Stat}, or empty if the ZNode does not exist
     */
    public Mono<Stat> checkExists(final String op, final String path) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return command(op, async -> async.checkExists().forPath(path));
    }

    /**
     * List the children of the ZNode at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return Wrapped {@link Mono} of the child ZNode names
     */
    public Mono<List<String>> getChildren(final String op, final String path) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return command(op, async -> async.getChildren().forPath(path));
    }

    /**
     * Run a general Curator async call: borrows a pooled integration service per subscription,
     * applies {@code fn} to an {@link AsyncCuratorFramework} wrapping its {@link
     * CuratorFramework}, and returns the integration service to the pool on completion, error, or
     * cancellation.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param fn Function from an {@link AsyncCuratorFramework} to the Curator {@link
     *     CompletionStage} it produces
     * @param <T> Value type
     * @return Wrapped {@link Mono}
     */
    public <T> Mono<T> command(final String op, final Function<AsyncCuratorFramework, CompletionStage<T>> fn) {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(fn, "fn");
        return mono(op, () -> Mono.usingWhen(
                Mono.fromSupplier(borrow),
                svc -> Mono.fromCompletionStage(() -> fn.apply(AsyncCuratorFramework.wrap(svc.getClient()))),
                svc -> Mono.fromRunnable(svc::finalizeOperation)));
    }

    /**
     * Maps Curator/ZooKeeper failures to {@link ZooKeeperAccessorException}; all other
     * throwables delegate to the inherited default mapping. Uses the shared static helper in
     * {@link ZooKeeperAccessorException} so behavior stays identical to the synchronous twin.
     *
     * @param throwable Raised throwable
     * @return Mapped throwable
     */
    @Override
    protected Throwable mapError(final Throwable throwable) {
        return ZooKeeperAccessorException.map(throwable, super::mapError);
    }

    /**
     * Always {@code true} for now: any mapped failure from this accessor is treated as notable.
     * TODO: refine once business-level ZooKeeper error classification is introduced.
     *
     * @param mapped Mapped throwable
     * @return {@code true}
     */
    @Override
    protected boolean isNotable(final Throwable mapped) {
        return true;
    }
}
