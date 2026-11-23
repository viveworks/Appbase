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

package com.viveworks.appbase.bootstrap.core;

/**
 * Initialization order constants for all built-in Appbase core services.
 *
 * <p>Gaps between values (e.g. 10, 20, 30...) allow external factories to slot in between
 * built-in ones without modifying this enum — return any {@code int} from
 * {@link AppbaseCoreServiceFactory#initializationOrder()}.
 */
public enum CoreServiceOrder {

    /** Log service — initialized first; all others may log. */
    LOG(10),

    /** Metric service. */
    METRIC(20),

    /** Validation service. */
    VALIDATION(30),

    /** Mediator service — internal message bus. */
    MEDIATOR(40),

    /** Configuration service. */
    CONFIGURATION(50),

    /** MicroProfile configuration service — wraps ConfigurationService. */
    MICROPROFILE_CONFIGURATION(51),

    /** Service registry. */
    SERVICE_REGISTRY(60),

    /** MicroProfile service registry — wraps ServiceRegistry. */
    MICROPROFILE_SERVICE_REGISTRY(61),

    /** Exception service. */
    EXCEPTION(70),

    /** Tracer service. */
    TRACER(80),

    /** JMX service. */
    JMX(90),

    /** Reactor service (optional). */
    REACTOR(100),

    /** Lucene service (optional). */
    LUCENE(110);

    private final int order;

    CoreServiceOrder(int order) {
        this.order = order;
    }

    /**
     * Returns the numeric initialization order. Lower values initialize first.
     *
     * @return numeric order
     */
    public int order() {
        return order;
    }
}
