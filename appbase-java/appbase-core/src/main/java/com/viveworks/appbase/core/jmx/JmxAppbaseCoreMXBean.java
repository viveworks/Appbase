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

package com.viveworks.appbase.core.jmx;

import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;
import com.viveworks.appbase.core.service.jmx.JmxJmxServiceMXBean;
import com.viveworks.appbase.core.service.registry.jmx.JmxServiceRegistryMXBean;

import javax.management.MBeanException;
import javax.management.MXBean;

/**
 * {@link MXBean} used to manage <code>core services</code>.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface JmxAppbaseCoreMXBean extends AppbaseServiceMXBean, JmxServiceRegistryMXBean, JmxJmxServiceMXBean {
    /**
     * Reload a service.
     *
     * @param appbaseServiceTypeName Appbase service type name to reload
     * @param appbaseServiceConfigurationYaml Appbase service configuration serialized as YAML
     * @param appbaseServiceConfigurationTypeName Appbase service configuration type name
     * @throws MBeanException If there were any errors
     */
    void reloadService(String appbaseServiceTypeName,
                       String appbaseServiceConfigurationYaml,
                       String appbaseServiceConfigurationTypeName) throws MBeanException;


    /**
     * Shutdown a service.
     *
     * @param appbaseServiceTypeName Appbase service type name to shutdown
     * @throws MBeanException If there were any errors
     */
    void shutdownService(String appbaseServiceTypeName) throws MBeanException;


    /**
     * Shutdown all services.
     *
     * @throws MBeanException If there were any errors
     */
    void shutdownAllServices() throws MBeanException;
}
