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

package com.viveworks.appbase.core.service.registry;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.util.AppbaseUtil;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

/**
 * Discovery entry for implementations of {@link com.viveworks.appbase.core.service.AppbaseService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@Immutable
public final class AppbaseServiceEntry {
    private final AppbaseService service;

    private final ServiceStatus serviceStatus;

    public AppbaseServiceEntry(final AppbaseService service, final ServiceStatus serviceStatus) {
        AppbaseUtil.notNull(service, "service");
        AppbaseUtil.notNull(serviceStatus, "serviceStatus");

        this.service = service;
        this.serviceStatus = serviceStatus;
    }

    public AppbaseService getService() {
        return service;
    }

    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AppbaseServiceEntry that = (AppbaseServiceEntry) o;
        return Objects.equals(service, that.service) &&
                serviceStatus == that.serviceStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, serviceStatus);
    }

    @Override
    public String toString() {
        return "AppbaseServiceEntry{" +
                "service=" + service +
                ", serviceStatus=" + serviceStatus +
                '}';
    }
}
