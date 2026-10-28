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
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.registry.AppbaseServiceEntry;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.AppbaseUtil;

import java.util.Collection;

/**
 * @author Navaneeth Mysore G
 * @author Claude
 * @since 1.0.0
 */
public class ServiceRegistryHealthChecker implements HealthChecker {
    private final ServiceRegistry serviceRegistry;

    public ServiceRegistryHealthChecker(final ServiceRegistry serviceRegistry) {
        AppbaseUtil.notNull(serviceRegistry, "serviceRegistry");

        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public HealthCheck runHealthCheck() {
        final Collection<AppbaseServiceEntry> appbaseServiceEntries = serviceRegistry.getAppbaseServices();

        // Lens 3 F5 / Lens 5 R-R1: an empty (or null) registry must not report UP by falling out of
        // the loop below having checked nothing -- that is "healthy" only in the sense that nothing
        // failed, which is indistinguishable from nothing having been verified at all.
        if (appbaseServiceEntries == null || appbaseServiceEntries.isEmpty()) {
            return new HealthCheck(ServiceRegistry.class.getName(), false);
        }

        for (final AppbaseServiceEntry appbaseServiceEntry : appbaseServiceEntries) {
            final AppbaseService service = appbaseServiceEntry.getService();
            if (service != null) {
                final ServiceStatus status = service.getStatus();
                if (status != ServiceStatus.STARTED) {
                    return new HealthCheck(service.getClass().getName(), false);
                }
            }
        }

        return new HealthCheck(ServiceRegistry.class.getName(), true);
    }
}
