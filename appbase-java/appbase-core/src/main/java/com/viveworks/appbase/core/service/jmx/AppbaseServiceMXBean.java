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

package com.viveworks.appbase.core.service.jmx;

import com.viveworks.appbase.core.service.AppbaseConfiguration;
import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.ServiceStatus;
import com.viveworks.appbase.core.service.configuration.ConfigurationService;

import javax.management.MBeanException;
import javax.management.MXBean;

/**
 * Represents an {@link AppbaseService} {@link MXBean}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface AppbaseServiceMXBean {
    /**
     * Get the {@link AppbaseService} status.
     *
     * @return Service status
     */
    ServiceStatus getStatus();


    /**
     * Reload {@link AppbaseService} by reading configuration from {@link ConfigurationService}.
     *
     * @throws MBeanException if there was any errors while re-loading.
     */
    void reload() throws MBeanException;


    /**
     * Reload {@link AppbaseService} using the YAML representing the {@link AppbaseConfiguration} that it uses.
     *
     * @param appbaseServiceConfigurationYaml YAML representing the AppbaseConfiguration
     * @throws MBeanException if there was any errors while re-loading
     */
    void reload(String appbaseServiceConfigurationYaml) throws MBeanException;


    /**
     * Shutdown the {@link AppbaseService}.
     */
    void shutdown();
}
