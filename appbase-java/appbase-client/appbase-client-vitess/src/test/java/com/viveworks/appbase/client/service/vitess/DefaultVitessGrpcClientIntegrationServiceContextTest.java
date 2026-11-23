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

package com.viveworks.appbase.client.service.vitess;

import com.viveworks.appbase.client.service.vitess.configuration.ContextConfiguration;
import io.vitess.client.Context;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for {@link DefaultVitessGrpcClientIntegrationService#context(ContextConfiguration)}
 * deadline semantics. No Vitess connection required — exercises the pure
 * {@code ContextConfiguration -> io.vitess.client.Context} mapping.
 *
 * <p>Guards the fix for the finding that the relative {@code deadlineDuration} (builder default
 * {@code PT10S}) was mapped with {@code Context.withDeadline(Instant.ofEpochMilli(duration.toMillis()))},
 * which treats the relative timeout as an absolute epoch instant — {@code PT10S} became
 * {@code 1970-01-01T00:00:10Z}, an already-expired deadline. Once S02 (internal work item 1438) began capturing the
 * previously discarded {@code withDeadline(...)} return, that expired deadline reached every configured
 * Vitess context and RPCs could time out immediately. The fix uses
 * {@code Context.withDeadlineAfter(<relative Duration>)} so the deadline lands in the future.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
class DefaultVitessGrpcClientIntegrationServiceContextTest {

    @Test
    void deadlineDurationIsAppliedAsRelativeTimeout_yieldingFutureDeadline() {
        final ContextConfiguration contextConfiguration = ContextConfiguration.builder()
                .deadlineDuration(Duration.ofSeconds(10))
                .build();

        final org.joda.time.Instant before = org.joda.time.Instant.now();
        final Context context = DefaultVitessGrpcClientIntegrationService.context(contextConfiguration);
        final org.joda.time.Instant after = org.joda.time.Instant.now();

        final org.joda.time.Instant deadline = context.getDeadline();
        assertNotNull(deadline, "a configured deadlineDuration must yield a deadline");

        // The bug mapped PT10S to Instant.ofEpochMilli(10_000) = 1970-01-01T00:00:10Z, ~56 years in the
        // PAST, so every RPC would time out immediately. A correct relative mapping is ~now + 10s.
        assertTrue(deadline.isAfter(before),
                "deadline must be in the future, not an expired 1970 epoch instant; was " + deadline);
        // Lower bound: at least ~9s out (tolerate scheduling skew below the configured 10s).
        assertTrue(deadline.isAfter(before.plus(org.joda.time.Duration.standardSeconds(9))),
                "PT10S must yield a deadline at least ~9s in the future; was " + deadline);
        // Upper bound: not farther than ~10s + generous skew out (proves the 10s offset is what is applied,
        // not some unrelated horizon).
        assertTrue(deadline.isBefore(after.plus(org.joda.time.Duration.standardSeconds(11))),
                "PT10S must yield a deadline ~10s in the future, not farther; was " + deadline);
    }

    @Test
    void defaultDeadlineDurationIsAlsoRelative() {
        // The builder default is PT10S; confirm the default path is equally future-dated, not 1970.
        final ContextConfiguration contextConfiguration = ContextConfiguration.builder().build();

        final org.joda.time.Instant before = org.joda.time.Instant.now();
        final Context context = DefaultVitessGrpcClientIntegrationService.context(contextConfiguration);

        final org.joda.time.Instant deadline = context.getDeadline();
        assertNotNull(deadline, "the default deadlineDuration must yield a deadline");
        assertTrue(deadline.isAfter(before),
                "default deadline must be in the future, not an expired 1970 epoch instant; was " + deadline);
    }
}
