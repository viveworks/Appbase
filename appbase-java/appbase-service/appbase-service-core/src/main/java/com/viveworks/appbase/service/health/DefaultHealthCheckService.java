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

package com.viveworks.appbase.service.health;

import com.viveworks.appbase.client.util.TimeUnitConfiguration;
import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class DefaultHealthCheckService
        extends AbstractStatelessAppbaseService<DefaultHealthCheckServiceConfiguration>
        implements HealthCheckService
{
    /**
     * The health result and the clock reading at which it was produced, published together as one
     * immutable value.
     *
     * <p>The snapshot and its production time used to live in two separate fields (an
     * {@code AtomicReference<HealthChecks>} plus a {@code volatile long}), both individually
     * volatile but not read or written together atomically. That let a reader observe a stale
     * snapshot paired with a fresher timestamp (or vice-versa) and judge stale data fresh, serving
     * it as healthy. It also left a startup window where the first snapshot could be read while the
     * timestamp field still held its uninitialised sentinel, producing an absurd age. Publishing
     * both fields as a single record through a single {@link AtomicReference} makes that tear
     * impossible: one volatile write, one volatile read.
     *
     * @param healthChecks the result of the most recent health check run
     * @param producedAtMillis the clock reading at which {@code healthChecks} was produced
     */
    private record Snapshot(HealthChecks healthChecks, long producedAtMillis) {
    }

    private final AtomicReference<Snapshot> snapshotRef;

    private final Map<String, HealthChecker> healthCheckerByTypeNameMap;

    private final ReentrantLock healthCheckLock;

    private final LongSupplier nowMillis;

    private volatile ScheduledFuture<?> schedulerFuture;

    ScheduledFuture<?> getSchedulerFuture() {
        return schedulerFuture;
    }

    public DefaultHealthCheckService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final DefaultHealthCheckServiceConfiguration defaultHealthCheckServiceConfiguration)
    {
        this(logService,
                metricService,
                mediatorService,
                serviceRegistry,
                mediatorService.getMediator().getSharedScheduledExecutorService(),
                defaultHealthCheckServiceConfiguration);
    }

    public DefaultHealthCheckService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final ScheduledExecutorService scheduledExecutorService,
            final DefaultHealthCheckServiceConfiguration defaultHealthCheckServiceConfiguration)
    {
        this(logService,
                metricService,
                mediatorService,
                serviceRegistry,
                scheduledExecutorService,
                defaultHealthCheckServiceConfiguration,
                System::currentTimeMillis);
    }

    /**
     * Test-only constructor that injects the clock, so staleness can be asserted deterministically
     * without sleeping the test thread.
     *
     * @param logService the log service
     * @param metricService the metric service
     * @param mediatorService the mediator service
     * @param serviceRegistry the service registry
     * @param scheduledExecutorService the executor the background refresh is scheduled on
     * @param defaultHealthCheckServiceConfiguration the configuration
     * @param nowMillis the clock; production code always passes {@code System::currentTimeMillis}
     */
    DefaultHealthCheckService(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final ScheduledExecutorService scheduledExecutorService,
            final DefaultHealthCheckServiceConfiguration defaultHealthCheckServiceConfiguration,
            final LongSupplier nowMillis)
    {
        super(logService, metricService, mediatorService, serviceRegistry, defaultHealthCheckServiceConfiguration);

        this.nowMillis = nowMillis;
        this.healthCheckerByTypeNameMap = new ConcurrentHashMap<>();
        this.snapshotRef = new AtomicReference<>();
        final TimeUnitConfiguration schedulerDelayConfiguration =
                defaultHealthCheckServiceConfiguration.getCacheSchedulerDelayConfiguration();
        this.healthCheckLock = new ReentrantLock();
        this.schedulerFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::runHealthCheck,
                0L,
                schedulerDelayConfiguration.getTime(),
                schedulerDelayConfiguration.getTimeUnit());
    }

    private void runHealthCheck() {
        healthCheckLock.lock();

        try {
            HealthChecks healthChecksResult = null;
            try {
                final List<HealthCheck> healthChecks = new ArrayList<>();
                final List<String> failedHealthCheckers = new ArrayList<>();
                final AtomicBoolean healthy = new AtomicBoolean(true);

                healthCheckerByTypeNameMap.forEach((s, healthChecker) -> {
                    try {
                        final HealthCheck healthCheck = healthChecker.runHealthCheck();
                        if (healthy.get() && !healthCheck.isHealthy()) {
                            healthy.set(false);
                        }

                        healthChecks.add(healthCheck);
                    } catch (Exception ex) {
                        if (healthy.get()) {
                            healthy.set(false);
                        }
                        failedHealthCheckers.add(healthChecker.getClass().getName());
                    }
                });

                healthChecksResult = new HealthChecks(healthy.get(), healthChecks, failedHealthCheckers);
            } catch (Exception ex) {
                logger.error("Error running health check", ex);
            }

            if (healthChecksResult == null) {
                healthChecksResult = new HealthChecks(false, List.of(), List.of());
            }

            // Exactly one publish per invocation, on every path: success or the fallback above.
            snapshotRef.set(new Snapshot(healthChecksResult, nowMillis.getAsLong()));
        } finally {
            healthCheckLock.unlock();
        }
    }

    @Override
    protected void shutdownImpl() {
        final ScheduledFuture<?> future = this.schedulerFuture;
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(HealthCheckService.class, this);
        serviceRegistry.registerAppbaseService(DefaultHealthCheckService.class, this);
    }

    /**
     * Run every registered health checker on the calling thread when {@code force} is {@code true};
     * otherwise serve the snapshot.
     *
     * <p>Historically this method ignored {@code force} entirely and always ran the checkers, so a
     * caller passing {@code false} blocked on every registered dependency (S3, ZooKeeper, Aerospike,
     * JDBC) under a contended lock. That is fixed: {@code force=false} now means what it says.
     *
     * @param force when {@code true}, re-run the checkers before returning
     * @return the health result
     */
    @Override
    public HealthChecks checkHealth(final boolean force) {
        if (force || !serviceConfiguration.isCacheEnabled()) {
            runHealthCheck();
        }

        return checkHealthSnapshot();
    }

    /**
     * Serve the snapshot maintained by the background scheduler.
     *
     * <p>This deliberately does NOT re-run the checkers when the last result was unhealthy. The
     * previous behaviour did, which meant the system performed the most blocking work precisely
     * when a dependency was already slow or down -- a retry storm by construction, amplified by the
     * contended {@code healthCheckLock}.
     *
     * @return the health result
     */
    @Override
    public HealthChecks checkHealth() {
        return checkHealthSnapshot();
    }

    @Override
    public HealthChecks checkHealthSnapshot() {
        final Snapshot snapshot = snapshotRef.get();
        if (snapshot == null) {
            return new HealthChecks(false, List.of(), List.of(), 0L);
        }

        final HealthChecks healthChecks = snapshot.healthChecks();
        final long age = nowMillis.getAsLong() - snapshot.producedAtMillis();
        final TimeUnitConfiguration staleAfter = serviceConfiguration.getStaleAfterConfiguration();
        final long staleAfterMillis = staleAfter.getTimeUnit().toMillis(staleAfter.getTime());

        if (staleAfter.isEnabled() && age > staleAfterMillis) {
            return new HealthChecks(false, healthChecks.getHealthChecks(), healthChecks.getFailedHealthCheckers(), age);
        }

        return new HealthChecks(
                healthChecks.isHealthy(), healthChecks.getHealthChecks(), healthChecks.getFailedHealthCheckers(), age);
    }

    @Override
    public <T extends HealthChecker> void addHealthChecker(final T healthChecker) {
        addHealthCheckerByName(healthChecker.getClass().getName(), healthChecker);
    }

    @Override
    public <T extends HealthChecker> void addHealthCheckerByName(
            final String name,
            final T healthChecker)
    {
        AppbaseUtil.notBlank(name, "name");
        AppbaseUtil.notNull(healthChecker, "healthChecker");

        healthCheckerByTypeNameMap.put(name, healthChecker);
    }

    @Override
    public <T extends HealthChecker> boolean existsHealthChecker(final Class<T> healthCheckerType) {
        return existsHealthCheckerByName(healthCheckerType.getName());
    }

    @Override
    public <T extends HealthChecker> boolean existsHealthCheckerByName(final String name) {
        AppbaseUtil.notBlank(name, "name");

        return healthCheckerByTypeNameMap.containsKey(name);
    }

    @Override
    public <T extends HealthChecker> void removeHealthChecker(final Class<T> healthCheckerType) {
        removeHealthCheckerByName(healthCheckerType.getName());
    }

    @Override
    public <T extends HealthChecker> void removeHealthCheckerByName(final String name) {
        AppbaseUtil.notBlank(name, "name");

        healthCheckerByTypeNameMap.remove(name);
    }
}
