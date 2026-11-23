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

package com.viveworks.appbase.core.service.registry.jmx;

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.annotation.Nullable;
import javax.management.MBeanException;
import javax.management.MXBean;
import java.util.Map;

/**
 * Represents a {@link MXBean} for {@link com.viveworks.appbase.core.service.registry.ServiceRegistry}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface JmxServiceRegistryMXBean extends AppbaseServiceMXBean {
    /**
     * Poll and update statuses of all registered services.
     */
    void pollAndUpdateStatuses();


    /**
     * Get {@link com.viveworks.appbase.core.service.registry.ServiceRegistry.OverallStatus} in JSON.
     *
     * @return {@link ServiceRegistry.OverallStatus}
     */
    String overallStatusJson() throws MBeanException;


    /**
     * Poll (refresh) and get {@link com.viveworks.appbase.core.service.registry.ServiceRegistry.OverallStatus} in JSON.
     *
     * @return {@link ServiceRegistry.OverallStatus}
     */
    String pollOverallStatusJson() throws MBeanException;


    /**
     * Get {@link ServiceStatus} for a particular {@link com.viveworks.appbase.core.service.AppbaseService}.
     *
     * @param appbaseServiceTypeName AppbaseService type name, whose status needs to be returned
     * @return Service status
     * @throws MBeanException if there were any errors
     */
    @Nullable
    ServiceStatus getAppbaseServiceStatus(String appbaseServiceTypeName) throws MBeanException;


    /**
     * Get {@link ServiceStatus} of all registered {@link com.viveworks.appbase.core.service.AppbaseService}s by
     * their type name.
     *
     * @return ServiceStatus of all registered AppbaseServices
     */
    @Unmodifiable
    Map<String, ServiceStatus> getAllServiceStatusByServiceTypeName();


    /**
     * Unregister an {@link com.viveworks.appbase.core.service.AppbaseService} by it's name.
     *
     * @param appbaseServiceTypeName Appbase service type name
     * @throws MBeanException if there were any errors
     */
    void unregisterAppbaseService(String appbaseServiceTypeName) throws MBeanException;


    /**
     * Clear {@link AppbaseService}s.
     */
    void clearAppbaseServices();
}
