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

/**
 * This package contains client services for {@code R2DBC} (Reactive Relational Database
 * Connectivity).
 *
 * <p>This module is reactive-only: each keyed integration service builds, holds, and disposes a
 * native {@code io.r2dbc.pool.ConnectionPool}. There is no synchronous accessor here &mdash; for
 * blocking JDBC access use {@code appbase-client-jdbc} instead. A reactive accessor over the pool
 * is added on top of this scaffold in a later task.
 *
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
package com.viveworks.appbase.client.service.r2dbc;
