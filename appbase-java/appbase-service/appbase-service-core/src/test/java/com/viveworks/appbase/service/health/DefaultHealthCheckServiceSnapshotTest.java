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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Proves that the health read path serves the scheduler-maintained snapshot rather than running
 * checkers inline, honours {@code force}, and does not turn an unhealthy result into a retry storm.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultHealthCheckServiceSnapshotTest {

    /** Counts how many times the checker actually ran, so we can prove the read path does not run it. */
    private static final class CountingHealthChecker implements HealthChecker {
        private final AtomicInteger runs = new AtomicInteger();
        private volatile boolean healthy = true;

        @Override
        public HealthCheck runHealthCheck() {
            runs.incrementAndGet();
            return new HealthCheck(getClass().getName(), healthy);
        }
    }

    @Test
    void checkHealthSnapshotNeverRunsTheCheckers() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, () -> 1_000L);

        service.checkHealthSnapshot();
        service.checkHealthSnapshot();

        Assertions.assertEquals(0, checker.runs.get(), "snapshot read must never invoke a checker");
    }

    @Test
    void checkHealthWithForceFalseServesTheSnapshotAndDoesNotRunCheckers() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, () -> 1_000L);

        service.checkHealth(false);

        Assertions.assertEquals(0, checker.runs.get(), "force=false must not run the checkers");
    }

    @Test
    void checkHealthWithForceTrueRunsTheCheckers() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, () -> 1_000L);

        service.checkHealth(true);

        Assertions.assertEquals(1, checker.runs.get(), "force=true must run the checkers exactly once");
    }

    @Test
    void anUnhealthySnapshotDoesNotTriggerARerun() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, () -> 1_000L);

        // Populate the snapshot once, then mark the dependency down.
        service.checkHealth(true);
        checker.healthy = false;
        service.checkHealth(true);
        final int runsAfterSeeding = checker.runs.get();

        // The read path must NOT re-run, even though the last result was unhealthy.
        service.checkHealth();
        service.checkHealth();

        Assertions.assertEquals(runsAfterSeeding, checker.runs.get(),
                "an unhealthy snapshot must not trigger a re-run: that is a retry storm when a dependency is down");
    }

    @Test
    void aSnapshotOlderThanStaleAfterReportsUnhealthy() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final AtomicLong now = new AtomicLong(1_000L);
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, now::get);

        service.checkHealth(true);
        Assertions.assertTrue(service.checkHealthSnapshot().isHealthy());

        // staleAfter is 90s in the test helper; advance 91s.
        now.addAndGet(TimeUnit.SECONDS.toMillis(91));

        final HealthChecks stale = service.checkHealthSnapshot();
        Assertions.assertFalse(stale.isHealthy(), "a stale snapshot must report unhealthy rather than serve stale data");
        Assertions.assertEquals(TimeUnit.SECONDS.toMillis(91), stale.getSnapshotAgeMillis());
    }

    @Test
    void aColdSnapshotReportsUnhealthyWithoutRunningCheckers() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, () -> 1_000L);

        final HealthChecks cold = service.checkHealthSnapshot();

        Assertions.assertFalse(cold.isHealthy(), "a cold snapshot is unhealthy, not a trigger to run checks inline");
        Assertions.assertEquals(0, cold.getSnapshotAgeMillis(), "a cold read has no snapshot, so age is 0");
        Assertions.assertEquals(0, checker.runs.get());
    }

    /**
     * Proves the fix for the snapshot/timestamp tear: the snapshot and its production time are
     * published together as one {@code Snapshot} record through a single {@code AtomicReference},
     * so the age reported by {@code checkHealthSnapshot()} is always derived from the clock reading
     * captured alongside that exact snapshot -- never from a fresh {@code nowMillis} read taken at
     * response time, which would silently zero out (or otherwise corrupt) the age and could let a
     * stale snapshot be served as fresh.
     */
    @Test
    void checkHealthSnapshotAgeAndHealthAreConsistentWithTheSeededSnapshot() {
        final CountingHealthChecker checker = new CountingHealthChecker();
        final AtomicLong now = new AtomicLong(1_000L);
        final DefaultHealthCheckService service = HealthCheckServiceTestHelper.serviceWith(checker, now::get);

        service.checkHealth(true);
        final int runsAfterSeeding = checker.runs.get();

        now.addAndGet(TimeUnit.SECONDS.toMillis(5));

        final HealthChecks result = service.checkHealthSnapshot();

        Assertions.assertEquals(TimeUnit.SECONDS.toMillis(5), result.getSnapshotAgeMillis(),
                "age must be derived from the clock reading captured alongside the snapshot, not a fresh one");
        Assertions.assertTrue(result.isHealthy(), "the healthy flag must pass through from the seeded snapshot");
        Assertions.assertEquals(runsAfterSeeding, checker.runs.get(), "checkHealthSnapshot must not run checkers");
    }
}
