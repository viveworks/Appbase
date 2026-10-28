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

package com.viveworks.appbase.client.core.reactive;

import com.viveworks.appbase.core.service.AppbaseConfiguration;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * Per-accessor reactive operation knobs: which scheduler to run on, per-attempt
 * timeout, retry/backoff policy, and Flux backpressure buffering. Every value has
 * a sensible default (see {@link DefaultReactiveOperationConfiguration}); a caller
 * that configures nothing still gets a working reactive setup.
 *
 * <p>Extends {@link AppbaseConfiguration} so the config-wizard {@code ConfigScanner}
 * discovers it and it participates in Appbase validation; it may also be embedded
 * as a field on a reactive client-service configuration.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public interface ReactiveOperationConfiguration extends AppbaseConfiguration {

    boolean isEnabled();

    @Nullable
    String getSchedulerId();

    Duration getTimeout();

    boolean isRetryEnabled();

    int getRetryMaxAttempts();

    Duration getRetryMinBackoff();

    Duration getRetryMaxBackoff();

    double getRetryJitterFactor();

    int getBackpressureBufferSize();

    boolean isMetricsEnabled();

    boolean isTraceEnabled();

    boolean isExceptionStoreEnabled();
}
