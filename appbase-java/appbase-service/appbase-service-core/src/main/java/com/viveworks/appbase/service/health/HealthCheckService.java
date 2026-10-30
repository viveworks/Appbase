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

package com.viveworks.appbase.service.health;

import com.viveworks.appbase.core.service.AppbaseService;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface HealthCheckService extends AppbaseService {
    HealthChecks checkHealth();

    HealthChecks checkHealth(boolean force);

    /**
     * Return the last snapshot produced by the background scheduler, without running any health
     * checker. Never blocks, never locks: safe to call from an event-loop thread.
     *
     * <p>Reports unhealthy when the snapshot is cold (the scheduler has not produced one yet) or
     * older than the configured staleness threshold.
     *
     * @return the current snapshot
     */
    HealthChecks checkHealthSnapshot();

    <T extends HealthChecker> void addHealthChecker(T healthChecker);

    <T extends HealthChecker> void addHealthCheckerByName(String name, T healthChecker);

    <T extends HealthChecker> boolean existsHealthChecker(Class<T> healthCheckerType);

    <T extends HealthChecker> boolean existsHealthCheckerByName(String name);

    <T extends HealthChecker> void removeHealthChecker(Class<T> healthCheckerType);

    <T extends HealthChecker> void removeHealthCheckerByName(String name);
}
