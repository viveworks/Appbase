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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.event.ConfigurationListener;
import com.viveworks.appbase.core.service.mediator.MessageHandler;
import com.viveworks.appbase.core.util.AppbaseRegistry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A service to manage service registry.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface ServiceRegistry extends AppbaseRegistry, AppbaseService, MessageHandler, ConfigurationListener {
    /**
     * Poll and update statuses of all registered services.
     */
    void pollAndUpdateStatuses();

    /**
     * Get a {@link AppbaseServiceEntry} for a particular {@link AppbaseService} type.
     *
     * @param appbaseServiceType Appbase service type
     * @return Appbase service type entry
     */
    AppbaseServiceEntry getAppbaseService(Class<? extends AppbaseService> appbaseServiceType);


    /**
     * Get {@link AppbaseService}s.
     *
     * @return Appbase services
     */
    Collection<AppbaseServiceEntry> getAppbaseServices();


    /**
     * Check if the app is ready operationally.
     *
     * @return Unready appbase service entries or nothing if all ready, optionally
     */
    Optional<UnreadyAppbaseServiceEntries> checkReady();


    /**
     * Get overall status of services in the registry
     *
     * @return {@link OverallStatus}
     */
    OverallStatus overallStatus();


    /**
     * Poll (refresh) and get overall status of services in the registry
     *
     * @return {@link OverallStatus}
     */
    OverallStatus pollOverallStatus();


    /**
     * Register {@link AppbaseService}.
     *
     * @param service Appbase service to register
     */
    void registerAppbaseService(AppbaseService service);


    /**
     * Register {@link AppbaseService} type.
     *
     * @param appbaseServiceType Appbase service type
     * @param appbaseService Appbase service
     * @param <T> Appbase service type
     */
    <T extends AppbaseService> void registerAppbaseService(Class<T> appbaseServiceType, T appbaseService);


    /**
     * Unregister {@link AppbaseService}.
     *
     * @param appbaseServiceType Appbase service type
     */
    void unregisterAppbaseService(Class<? extends AppbaseService> appbaseServiceType);


    /**
     * Clear {@link AppbaseService}s.
     */
    void clearAppbaseServices();


    /**
     * Represents the overall status of the services in the registry.
     *
     * @author Navaneeth Mysore G
     * @since 1.0.0
     */
    class OverallStatus {
        private final boolean started;

        @Nullable
        @JsonSerialize(contentAs = String.class)
        @JsonDeserialize(contentAs = String.class)
        private final Set<String> startedServiceNames;

        @Nullable
        @JsonSerialize(keyAs = String.class, contentAs = ServiceStatus.class)
        @JsonDeserialize(keyAs = String.class, contentAs = ServiceStatus.class)
        private final Map<String, ServiceStatus> notStartedServiceStatusByNameMap;

        @JsonCreator
        public OverallStatus(
                @JsonProperty("started")
                final boolean started,
                @Nullable
                @JsonProperty("startedServiceNames")
                final Set<String> startedServiceNames,
                @Nullable
                @JsonProperty("notStartedServiceStatusByNameMap")
                final Map<String, ServiceStatus> notStartedServiceStatusByNameMap)
        {
            this.started = started;
            this.startedServiceNames = startedServiceNames;
            this.notStartedServiceStatusByNameMap = notStartedServiceStatusByNameMap;
        }

        public boolean isStarted() {
            return started;
        }

        @Nullable
        public Set<String> getStartedServiceNames() {
            return startedServiceNames;
        }

        @Nullable
        public Map<String, ServiceStatus> getNotStartedServiceStatusByNameMap() {
            return notStartedServiceStatusByNameMap;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final OverallStatus that = (OverallStatus) o;
            return started == that.started &&
                    Objects.equals(startedServiceNames, that.startedServiceNames) &&
                    Objects.equals(notStartedServiceStatusByNameMap, that.notStartedServiceStatusByNameMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(started, startedServiceNames, notStartedServiceStatusByNameMap);
        }

        @Override
        public String toString() {
            return "OverallStatus{" +
                    "started=" + started +
                    ", startedServiceNames=" + startedServiceNames +
                    ", notStartedServiceStatusByNameMap=" + notStartedServiceStatusByNameMap +
                    '}';
        }
    }
}
