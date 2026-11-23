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

package com.viveworks.appbase.core.service.exception.jmx;

import com.viveworks.appbase.core.service.jmx.AppbaseServiceMXBean;
import com.viveworks.appbase.core.util.Unmodifiable;

import javax.management.MBeanException;
import javax.management.MXBean;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@link MXBean} for {@link com.viveworks.appbase.core.service.exception.ExceptionService}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@MXBean
public interface JmxExceptionServiceMXBean extends AppbaseServiceMXBean {
    /**
     * Read {@link com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails} JSON by ID, stored
     * in each {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore}.
     *
     * @param id ExceptionMessageDetails ID
     * @return ExceptionMessageDetails JSON by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, String> readExceptionMessageDetailsById(Integer id) throws MBeanException;


    /**
     * Read all {@link com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails} JSON, stored in
     * each {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore}.
     *
     * @return All ExceptionMessageDetails JSON collection by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readAllExceptionMessageDetails() throws MBeanException;


    /**
     * Read {@link com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails} JSON by timestamps,
     * stored in each {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore}.
     *
     * @param inclusiveMinTimestamp Minimum timestamp (inclusive)
     * @param exclusiveMaxTimestamp Maximum timestamp (exclusive)
     * @return ExceptionMessageDetails JSON collection by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readAllExceptionMessageDetailsByTimestamps(
            long inclusiveMinTimestamp, long exclusiveMaxTimestamp) throws MBeanException;


    /**
     * Read unique {@link Throwable} type names stored in each
     * {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore} type name.
     *
     * @return Unique Throwable type name collection by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readUniqueExceptionTypeNames() throws MBeanException;


    /**
     * Read {@link com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails} collection JSON by
     * {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore} type name.
     *
     * @param throwableTypeName Throwable type name
     * @return ExceptionMessageDetails JSON collection by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, List<String>> readExceptionMessageDetailsByThrowableTypeName(
            String throwableTypeName) throws MBeanException;


    /**
     * Remove {@link com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails} by ID from
     * all {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore}s.
     *
     * @param id ExceptionMessageDetails ID
     * @return Result by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, Boolean> removeExceptionMessageDetailsById(int id) throws MBeanException;


    /**
     * Remove {@link com.viveworks.appbase.core.service.exception.store.ExceptionMessageDetails} before certain
     * timestamp (exclusive).
     *
     * @param timestamp Exclusive before timestamp
     * @return Result by ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    @Unmodifiable
    Map<String, Boolean> removeExceptionMessageDetailsExclusiveBefore(long timestamp) throws MBeanException;


    /**
     * Get {@link com.viveworks.appbase.core.service.exception.listener.ExceptionListener} type names by
     * {@link Throwable} type name map.
     *
     * @return ExceptionListener type names by Throwable type name map
     */
    @Unmodifiable
    Map<String, List<String>> getExceptionListenersByExceptionTypeMap();


    /**
     * Unregister {@link com.viveworks.appbase.core.service.exception.listener.ExceptionListener} by
     * {@link Throwable} name.
     *
     * @param exceptionTypeName Throwable type name
     * @param exceptionListenerTypeName ExceptionListener type name
     * @throws MBeanException if there were any errors
     */
    void unregisterExceptionListenerByExceptionType(String exceptionTypeName,
                                                    String exceptionListenerTypeName) throws MBeanException;


    /**
     * Clear {@link com.viveworks.appbase.core.service.exception.listener.ExceptionListener} by
     * {@link Throwable} map.
     */
    void clearExceptionListenersByExceptionTypeMap();


    /**
     * Get global {@link com.viveworks.appbase.core.service.exception.listener.ExceptionListener} type names.
     *
     * @return Global ExceptionListener type names
     */
    @Unmodifiable
    List<String> getGlobalExceptionListeners();


    /**
     * Unregister global {@link com.viveworks.appbase.core.service.exception.listener.ExceptionListener} by type name.
     *
     * @param exceptionListenerTypeName ExceptionListener type name
     * @throws MBeanException if there were any errors
     */
    void unregisterGlobalExceptionListener(String exceptionListenerTypeName) throws MBeanException;


    /**
     * Clear global {@link com.viveworks.appbase.core.service.exception.listener.ExceptionListener}s.
     */
    void clearGlobalExceptionListeners();


    /**
     * Get {@link com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory} type name by
     * {@link com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration} type name.
     *
     * @return ExceptionStoreFactory type name by ExceptionStoreConfiguration type name map
     */
    @Unmodifiable
    Map<String, String> getExceptionStoreFactoryByConfigurationTypeNameMap();


    /**
     * Unregister {@link com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory} by
     * {@link com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration} type names.
     *
     * @param exceptionStoreConfigurationTypeName ExceptionStoreConfiguration type name
     * @param exceptionStoreFactoryTypeName ExceptionStoreFactory type name
     * @throws MBeanException if there were any errors
     */
    void unregisterExceptionStoreFactoryByConfigurationType(String exceptionStoreConfigurationTypeName,
                                                            String exceptionStoreFactoryTypeName) throws MBeanException;


    /**
     * Clear {@link com.viveworks.appbase.core.service.exception.store.ExceptionStoreFactory} by
     * {@link com.viveworks.appbase.core.service.exception.store.ExceptionStoreConfiguration} type name map.
     */
    void clearExceptionStoreFactoryByConfigurationTypeMap();


    /**
     * Get registered {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore} type names.
     *
     * @return Registered ExceptionStore type names
     */
    @Unmodifiable
    List<String> getExceptionStores();


    /**
     * Unregister {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore} by type name.
     *
     * @param exceptionStoreTypeName ExceptionStore type name
     * @throws MBeanException if there were any errors
     */
    void unregisterExceptionStore(String exceptionStoreTypeName) throws MBeanException;


    /**
     * Clear {@link com.viveworks.appbase.core.service.exception.store.ExceptionStore}s.
     */
    void clearExceptionStores();
}
