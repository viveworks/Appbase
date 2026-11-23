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

package com.viveworks.appbase.client.service.jasync;

import com.github.jasync.sql.db.Connection;
import com.viveworks.appbase.client.core.AppbaseClientIntegrationService;
import com.viveworks.appbase.client.core.clientkey.comparablevalue.TextValue;

/**
 * Represents {@link AppbaseClientIntegrationService} for <a href="https://github.com/jasync-sql/jasync-sql">jasync-sql</a>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface JasyncClientIntegrationService extends AppbaseClientIntegrationService<Connection, TextValue> {
    /**
     * Whether jasync connection-pool size counters should be bridged onto Appbase's shared
     * Micrometer composite registry (→ JMX) as polling gauges. Defaults to {@code true}.
     *
     * @return {@code true} if driver connection-pool metrics should be enabled
     */
    boolean isDriverMetricsEnabled();
}
