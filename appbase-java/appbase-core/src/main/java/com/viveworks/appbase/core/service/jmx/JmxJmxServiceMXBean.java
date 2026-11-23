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

import javax.management.MBeanException;
import javax.management.MXBean;
import java.util.List;

/**
 * Represents a {@link MXBean} for {@link JmxService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface JmxJmxServiceMXBean extends AppbaseServiceMXBean {
    /**
     * Get {@link com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry} JSON for a registered
     * {@link MXBean} type.
     *
     * @param mxBeanTypeName MXBean type name
     * @return MXBeanEntry JSON
     * @throws MBeanException If there were any errors
     */
    String getMXBeanEntryJson(String mxBeanTypeName) throws MBeanException;


    /**
     * Get all registered {@link com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry} as JSON.
     *
     * @return MXBeanEntry JSON strings
     * @throws MBeanException If there were any errors
     */
    List<String> getMXBeanEntriesJson() throws MBeanException;


    /**
     * Check if a particular <code>MXBean</code> is registered using its type name.
     *
     * @param mxBeanTypeName MXBean type name
     * @return true if registered, false otherwise
     * @throws MBeanException If there were any errors
     */
    boolean isMXBeanRegistered(String mxBeanTypeName) throws MBeanException;


    /**
     * Unregister a particular <code>MXBean</code> using its type name.
     *
     * @param mxBeanTypeName MXBean type name
     * @throws MBeanException If there were any errors
     */
    void unregisterMXBean(String mxBeanTypeName) throws MBeanException;


    /**
     * Unregister and clear all registered <code>MXBeans</code>.
     *
     * @throws MBeanException If there were any errors
     */
    void clearMXBeans() throws MBeanException;
}
