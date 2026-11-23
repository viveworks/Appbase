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
package com.viveworks.appbase.service.supportmessage.store;

import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.health.HealthCheck;
import com.viveworks.appbase.service.health.HealthChecker;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JdbiSupportMessageStoreHealthChecker implements HealthChecker {
    private final Handle handle;

    private final JdbiSupportMessageStoreConfiguration jdbiSupportMessageStoreConfiguration;

    public JdbiSupportMessageStoreHealthChecker(
            final Handle handle,
            final JdbiSupportMessageStoreConfiguration jdbiSupportMessageStoreConfiguration)
    {
        AppbaseUtil.notNull(handle, "handle");
        AppbaseUtil.notNull(jdbiSupportMessageStoreConfiguration, "jdbiSupportMessageStoreConfiguration");

        this.handle = handle;
        this.jdbiSupportMessageStoreConfiguration = jdbiSupportMessageStoreConfiguration;
    }

    @Override
    public HealthCheck runHealthCheck() {
        try {
            if (handle.isClosed()) {
                return new HealthCheck(JdbiSupportMessageStore.class.getName(), false);
            }

            handle.select(
                    "SELECT * FROM " + jdbiSupportMessageStoreConfiguration.getSupportMessageTableName() + " LIMIT 1")
                    .mapToMap();
        } catch (JdbiException ex) {
            return new HealthCheck(JdbiSupportMessageStore.class.getName(), false);
        }

        return new HealthCheck(JdbiSupportMessageStore.class.getName(), true);
    }
}
