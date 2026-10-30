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

package com.viveworks.appbase.client.core;

import com.viveworks.appbase.client.core.clientkey.AppbaseClientKey;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.ComparableValue;
import com.viveworks.appbase.client.core.pool.AppbaseClientPool;
import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of {@link AppbaseClientIntegrationService}.
 *
 * @author Navaneeth Mysore G
 * @param <T> Client type
 * @param <C> {@link AppbaseClientIntegrationServiceConfiguration} type
 * @param <V> {@link ComparableValue} type
 * @since 1.0.0
 */
public abstract class AbstractAppbaseClientIntegrationService
        <T, V extends ComparableValue, C extends AppbaseClientIntegrationServiceConfiguration>
        implements AppbaseClientIntegrationService<T, V>
{
    protected final Logger logger;
    protected final AppbaseClientCore appbaseClientCore;
    protected final AppbaseClientPool<T, V> appbaseClientPool;
    protected final AppbaseClientKey<V> appbaseClientKey;
    protected final AtomicBoolean shutdown;

    private final Class<T> clientType;

    /**
     * Retains the core, pool, key, and runtime client type used by lifecycle methods. The references are
     * fixed, but a mutable {@link AppbaseClientKey} implementation can still change its own state.
     * Construction does not initialize the driver; subclasses invoke {@link #init()} when ready.
     *
     * @param appbaseClientCore client-core services
     * @param appbaseClientPool pool that owns this keyed integration
     * @param appbaseClientKey key identifying the integration; the reference is retained, not copied
     * @param clientType driver client class used for runtime checks
     */
    public AbstractAppbaseClientIntegrationService(
            final AppbaseClientCore appbaseClientCore,
            final AppbaseClientPool<T, V> appbaseClientPool,
            final AppbaseClientKey<V> appbaseClientKey,
            final Class<T> clientType)
    {
        AppbaseUtil.notNull(appbaseClientCore, "appbaseClientCore");
        AppbaseUtil.notNull(appbaseClientPool, "appbaseClientPool");
        AppbaseUtil.notNull(appbaseClientKey, "appbaseClientKey");
        AppbaseUtil.notNull(clientType, "clientType");

        this.logger = appbaseClientCore
                .getAppbaseCore()
                .getLogService()
                .getLogger(getClass());
        this.appbaseClientCore = appbaseClientCore;
        this.appbaseClientPool = appbaseClientPool;
        this.appbaseClientKey = appbaseClientKey;
        this.clientType = clientType;
        this.shutdown = new AtomicBoolean(false);
    }


    /**
     * Creates the driver client and any resources it needs. {@link #init()} calls it, and
     * {@link #defaultRepairImpl(RepairListener, AppbaseClientIntegrationServiceConfiguration)} calls it again
     * through {@link #init()} when repairing.
     *
     * @throws AppbaseClientException if the driver client cannot be created
     */
    protected abstract void initImpl() throws AppbaseClientException;

    /**
     * This method needs to be overridden in subclasses as it is used by
     * {@link #repairImpl(RepairListener)}.
     */
    protected void init() throws AppbaseClientException {
        logger.info("Initializing {}", getClass().getName());

        initImpl();
        postInit();
    }

    /**
     * Marks the integration as running again by clearing the shutdown flag. {@link #init()} calls it
     * after {@link #initImpl()} succeeds. Subclasses may extend it to start dependent work.
     */
    protected void postInit() {
        shutdown.set(false);
    }

    /**
     * Attempts to restore a failed integration. {@link #repair(RepairListener)} calls it and then reports
     * success or failure to the listener from {@link #isHealthy()}; an exception it throws is reported as
     * a failure. Implementations that need the standard shutdown-then-initialize sequence can delegate to
     * {@link #defaultRepairImpl(RepairListener, AppbaseClientIntegrationServiceConfiguration)}.
     *
     * @param repairListener listener to notify about the repair outcome
     * @throws AppbaseClientException if the repair cannot be attempted
     */
    protected abstract void repairImpl(RepairListener repairListener) throws AppbaseClientException;

    /**
     * Releases the driver client and its resources. {@link #shutdown()} calls it at most once per
     * initialization, and logs rather than propagates an exception it throws.
     */
    protected abstract void shutdownImpl();



    /** {@inheritDoc} */
    @Override
    public void finalizeOperation() {
        appbaseClientPool.giveAppbaseClientIntegrationService(this);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void repair(final RepairListener repairListener) {
        AppbaseUtil.notNull(repairListener, "repairListener");

        logger.info("Repairing {}", getClass().getName());

        try {
            repairImpl(repairListener);

            if (isHealthy()) {
                repairListener.onSuccess();
            } else {
                repairListener.onFailure();
            }
        } catch (Exception ex) {
            logger.error("Error while repairing {}", getClass().getName(), ex);
            repairListener.onFailure(ex);
        }
    }

    /**
     * Standard repair sequence: shuts the integration down, initializes it again, and notifies the listener
     * of success only when a client then exists. A {@code null} configuration is reported to the listener
     * as a failure without touching the running integration.
     *
     * @param repairListener listener to notify about the outcome
     * @param configuration configuration the integration was created from, or {@code null}
     * @throws AppbaseClientException if re-initialization fails
     */
    protected void defaultRepairImpl(
            final RepairListener repairListener, final C configuration) throws AppbaseClientException
    {
        AppbaseUtil.notNull(repairListener, "repairListener");

        logger.info("Repairing {} and notifying {} with {}",
                getClass().getName(), repairListener.getClass().getName(), configuration);
        if (configuration == null) {
            final AppbaseClientException appbaseClientException =
                    new AppbaseClientException("Cannot repair " + getClass().getName() + " , configuration is null");
            repairListener.onFailure(appbaseClientException);
            return;
        }

        shutdown();
        init();

        final T client = getClient();
        if (client == null) {
            final AppbaseClientException appbaseClientException =
                    new AppbaseClientException("Failed to repair " + getClass().getName());
            repairListener.onFailure(appbaseClientException);
            return;
        }

        repairListener.onSuccess();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() throws AppbaseRuntimeException {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        logger.info("Shutting down {}", getClass().getName());

        try {
            shutdownImpl();
        } catch (Exception ex) {
            logger.error("Error shutting down {}", getClass().getName(), ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AppbaseClientKey<V> getAppbaseClientKey() {
        return appbaseClientKey;
    }

    /** {@inheritDoc} */
    @Override
    public Class<T> getClientType() {
        return clientType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRepairEnabled() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractAppbaseClientIntegrationService<T, V, C> that = (AbstractAppbaseClientIntegrationService<T, V, C>) o;
        return Objects.equals(appbaseClientKey, that.appbaseClientKey) &&
                Objects.equals(clientType, that.clientType);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(appbaseClientKey, clientType);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AbstractAppbaseClientIntegrationService{" +
                "appbaseClientKey=" + appbaseClientKey +
                ", clientType=" + clientType +
                '}';
    }
}
