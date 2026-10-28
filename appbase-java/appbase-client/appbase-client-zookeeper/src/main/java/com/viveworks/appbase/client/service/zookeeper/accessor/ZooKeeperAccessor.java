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

package com.viveworks.appbase.client.service.zookeeper.accessor;

import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.accessor.AbstractAppbaseAccessor;
import com.viveworks.appbase.client.core.clientkey.clusterid.ClusterIdValue;
import com.viveworks.appbase.client.core.reactive.ReactiveOperationConfiguration;
import com.viveworks.appbase.client.service.zookeeper.accessor.reactive.ReactiveZooKeeperAccessor;
import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Synchronous accessor for Apache ZooKeeper, built directly on Curator's blocking fluent API. Each
 * operation borrows one pooled {@link AppbaseClientIntegrationService}, runs the corresponding
 * blocking {@link CuratorFramework} fluent call against its client, and returns the integration
 * service to the pool on completion or failure.
 *
 * <p>Cross-cutting concerns (metrics, tracing, and exception-store reporting) are supplied by the
 * inherited {@link AbstractAppbaseAccessor}; this class contributes only the Curator operation and
 * ZooKeeper error mapping, shared with the reactive twin ({@link ReactiveZooKeeperAccessor}) via
 * {@link ZooKeeperAccessorException#map(Throwable, Function)}.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ZooKeeperAccessor extends AbstractAppbaseAccessor {

    private final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow;

    /**
     * Create the accessor.
     *
     * @param appbaseCore Appbase core, providing metrics/tracing/mediator/log services
     * @param configuration Reactive operation configuration; its metrics/trace/exception-store
     *                      flags gate this accessor's cross-cutting behavior
     * @param borrow Supplier of a pooled ZooKeeper integration service, invoked once per operation
     */
    public ZooKeeperAccessor(
            final AppbaseCore appbaseCore,
            final ReactiveOperationConfiguration configuration,
            final Supplier<AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue>> borrow) {
        super(appbaseCore,
                AppbaseUtil.notNull(configuration, "configuration").isMetricsEnabled(),
                configuration.isTraceEnabled(),
                configuration.isExceptionStoreEnabled());
        this.borrow = AppbaseUtil.notNull(borrow, "borrow");
    }

    /**
     * Create a ZNode at {@code path} with {@code data}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @param data ZNode data
     * @return Path of the created ZNode
     * @throws Exception The mapped exception, if the operation fails
     */
    public String create(final String op, final String path, final byte[] data) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(data, "data");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().create().forPath(path, data);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Get the data stored at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return ZNode data
     * @throws Exception The mapped exception, if the operation fails
     */
    public byte[] getData(final String op, final String path) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().getData().forPath(path);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Set the data stored at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @param data New ZNode data
     * @return Resulting {@link Stat}
     * @throws Exception The mapped exception, if the operation fails
     */
    public Stat setData(final String op, final String path, final byte[] data) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        AppbaseUtil.notNull(data, "data");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().setData().forPath(path, data);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Delete the ZNode at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return {@code null}, once the delete completes
     * @throws Exception The mapped exception, if the operation fails
     */
    public Void delete(final String op, final String path) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().delete().forPath(path);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Check whether the ZNode at {@code path} exists.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return The {@link Stat}, or {@code null} if the ZNode does not exist
     * @throws Exception The mapped exception, if the operation fails
     */
    public Stat checkExists(final String op, final String path) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().checkExists().forPath(path);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * List the children of the ZNode at {@code path}.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param path ZNode path
     * @return Child ZNode names
     * @throws Exception The mapped exception, if the operation fails
     */
    public List<String> getChildren(final String op, final String path) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(path, "path");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return svc.getClient().getChildren().forPath(path);
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Run a general ZooKeeper command that returns a single value: borrows a pooled integration
     * service, applies {@code commandFn} to its {@link CuratorFramework}, and returns the
     * integration service to the pool on completion or failure.
     *
     * <p>Uses {@link ZooKeeperCommand} rather than {@link Function} so the lambda can call
     * Curator's blocking fluent API directly: its {@code forPath(...)} terminals throw checked
     * {@link Exception}, which {@code Function.apply} cannot declare.
     *
     * @param op Operation name, used for metrics/tracing and failure reporting
     * @param commandFn Checked-throwing command from a {@link CuratorFramework} to the result
     * @param <T> Value type
     * @return Result of {@code commandFn}
     * @throws Exception The mapped exception, if the operation fails
     */
    public <T> T command(final String op, final ZooKeeperCommand<T> commandFn) throws Exception {
        AppbaseUtil.notNull(op, "op");
        AppbaseUtil.notNull(commandFn, "commandFn");
        return execute(op, () -> {
            final AppbaseClientIntegrationService<CuratorFramework, ClusterIdValue> svc = borrow.get();
            try {
                return commandFn.apply(svc.getClient());
            } finally {
                svc.finalizeOperation();
            }
        });
    }

    /**
     * Maps Curator/ZooKeeper failures to {@link ZooKeeperAccessorException}; all other throwables
     * delegate to the inherited default mapping. Uses the shared static helper in {@link
     * ZooKeeperAccessorException} so behavior stays identical to the reactive twin.
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
