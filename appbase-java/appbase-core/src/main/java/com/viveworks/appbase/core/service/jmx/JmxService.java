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

import com.viveworks.appbase.core.service.AppbaseService;
import com.viveworks.appbase.core.service.jmx.mxbean.entry.MXBeanEntry;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * <code>JMX</code> service.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public interface JmxService extends AppbaseService {
    /**
     * Get {@link MXBeanEntry} for a registered <code>MxBean</code>.
     *
     * @param mxBeanType MXBean type
     * @return MXBeanEntry
     */
    @Nullable
    MXBeanEntry getMXBeanEntry(Class<?> mxBeanType);


    /**
     * Get all registered {@link MXBeanEntry}s.
     *
     * @return MXBeanEntry collection
     */
    Collection<MXBeanEntry> getMXBeanEntries();


    /**
     * Check if a <code>MXBean</code> is registered.
     *
     * @param mxBeanType MXBean type
     * @param <T> MXBean type
     * @return true if registered, false otherwise
     * @throws JmxServiceException if an error occurred while checking for registration
     */
    <T> boolean isMXBeanRegistered(Class<T> mxBeanType) throws JmxServiceException;


    /**
     * Get a registered <code>MXBean</code> of a given type.
     *
     * @param mxBeanType MxBean type
     * @param <T> MXBean type
     * @return MXBean
     */
    <T> T getMXBean(Class<T> mxBeanType);


    /**
     * Register a <code>MXBean</code>.
     *
     * @param mxBean MXBean to register
     * @param <T> MXBean type
     * @throws JmxServiceException if an error occurred while registration
     */
    <T> void registerMXBean(T mxBean) throws JmxServiceException;


    /**
     * Unregister a <code>MXBean</code>.
     *
     * @param mxBeanType MXBean type to unregister
     * @param <T> MXBean type
     * @throws JmxServiceException if an error occurred during unregistering MxBean
     */
    <T> void unregisterMXBean(Class<T> mxBeanType) throws JmxServiceException;


    /**
     * Clear and unregister all <code>MXBeans</code>.
     *
     * @throws JmxServiceException if an error occurred
     */
    void clearMXBeans() throws JmxServiceException;
}
