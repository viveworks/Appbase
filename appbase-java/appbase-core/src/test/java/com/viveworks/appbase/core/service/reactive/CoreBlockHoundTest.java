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

package com.viveworks.appbase.core.service.reactive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * The core-module BlockHound harness. Proves BlockHound is actually armed in {@code appbase-core}'s
 * tests, so that later "the reactive read path never blocks the event loop" assertions (the
 * end-to-end proof landing in N05 on the integrated epic) are not vacuous.
 *
 * <p>If BlockHound silently fails to install, a "does not block" assertion would pass whether or not
 * the code blocks. So the first test asserts that a deliberate blocking call on a non-blocking
 * (parallel) scheduler IS detected; the second asserts that blocking on the bounded-elastic
 * scheduler -- where the reactive-read offload for genuinely-blocking (non-Lucene) stores runs on
 * purpose -- is allowed.
 *
 * <p>BlockHound instruments JDK internals, which requires
 * {@code -XX:+AllowRedefinitionToAddDeleteMethods} on JDK 13+ (and BlockHound 1.0.13 is the first
 * release that instruments on JDK 25). That flag is supplied by this module's dedicated surefire
 * execution, which runs this class in its own fork -- BlockHound.install() is JVM-global, so
 * isolating it keeps the global instrumentation out of the rest of the (order-sensitive) core suite.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class CoreBlockHoundTest {

    @BeforeAll
    static void installBlockHound() {
        // Requires -XX:+AllowRedefinitionToAddDeleteMethods, supplied by the module's dedicated
        // surefire execution. Without it, install() fails on JDK 25 and this whole class is red --
        // which is exactly the signal that the harness is not armed.
        BlockHound.install();
    }

    @Test
    void blockHoundIsArmedAndDetectsABlockingCallOnANonBlockingThread() {
        Assertions.assertThrows(Throwable.class, () ->
                        Mono.fromRunnable(() -> {
                            try {
                                Thread.sleep(10);
                            } catch (final InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }).subscribeOn(Schedulers.parallel()).block(Duration.ofSeconds(5)),
                "BlockHound must detect Thread.sleep on a parallel scheduler thread; if this does not "
                        + "throw, BlockHound failed to install and every non-blocking assertion is vacuous");
    }

    @Test
    void boundedElasticIsAllowedToBlock() {
        // The reactive-read offload for genuinely-blocking non-Lucene stores runs blocking work on
        // boundedElastic on purpose. BlockHound must not flag it, or that offload path would fail its
        // own tests.
        final String result = Mono.fromCallable(() -> {
            Thread.sleep(1);
            return "ok";
        }).subscribeOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(5));

        Assertions.assertEquals("ok", result);
    }
}
