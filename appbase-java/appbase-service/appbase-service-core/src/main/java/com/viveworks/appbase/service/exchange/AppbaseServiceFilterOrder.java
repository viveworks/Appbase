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

package com.viveworks.appbase.service.exchange;

/**
 * Chain order for the standard Appbase web filters. Lower runs first.
 *
 * <p>These values reproduce the servlet chain's order exactly (see {@code
 * AppbaseServletRegistrar#addServiceFilters}). <strong>Authorization runs LAST</strong>, after the
 * user session is resolved and the IDP token is checked. This ordering is a security invariant, not
 * a style choice: authorizing before the session is resolved would authorize an unauthenticated
 * principal. Any change here must be justified as a security decision and covered by a test that
 * asserts the resolved order.
 *
 * <p>The servlet chain's {@code AppbaseSyncServletFilter} has no counterpart here: it exists to
 * bridge async to sync on the servlet path, which is meaningless on a reactive face.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public final class AppbaseServiceFilterOrder {

    /** Exception translation runs first so downstream failures are mapped. */
    public static final int EXCEPTION_HANDLER = 100;
    /** Cross-origin handling follows exception translation. */
    public static final int CORS = 200;
    /** Metric timing wraps tracing, identity, and authorization work. */
    public static final int METRIC = 300;
    /** Trace identifiers are established before identity resolution. */
    public static final int TRACE_ID = 400;
    /** User sessions resolve before identity-provider token checks. */
    public static final int USER_SESSION = 500;
    /** Identity-provider tokens are checked before authorization. */
    public static final int IDP_TOKEN_CHECK = 600;
    /** Authorization runs last with resolved session and token state. */
    public static final int AUTHORIZATION = 700;

    private AppbaseServiceFilterOrder() {
    }
}
